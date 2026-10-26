package br.com.bitabit.ecclesiacontrol.auth.service;

import br.com.bitabit.ecclesiacontrol.auth.domain.Role;
import br.com.bitabit.ecclesiacontrol.auth.domain.User;
import br.com.bitabit.ecclesiacontrol.auth.domain.UserFilialRole;
import br.com.bitabit.ecclesiacontrol.auth.dto.*;
import br.com.bitabit.ecclesiacontrol.auth.repository.RoleRepository;
import br.com.bitabit.ecclesiacontrol.auth.repository.UserFilialRoleRepository;
import br.com.bitabit.ecclesiacontrol.auth.repository.UserRepository;
import br.com.bitabit.ecclesiacontrol.core.exception.AccessForbiddenException;
import br.com.bitabit.ecclesiacontrol.core.exception.BusinessRuleException;
import br.com.bitabit.ecclesiacontrol.core.exception.ResourceNotFoundException;
import br.com.bitabit.ecclesiacontrol.core.service.AuditActor;
import br.com.bitabit.ecclesiacontrol.core.service.AuditService;
import br.com.bitabit.ecclesiacontrol.core.util.TenantContext;
import br.com.bitabit.ecclesiacontrol.tenant.domain.Tenant;
import br.com.bitabit.ecclesiacontrol.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.*;
import br.com.bitabit.ecclesiacontrol.core.security.AuthenticatedUser;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TenantRepository tenantRepository;
    private final UserFilialRoleRepository userFilialRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional
    public UserResponse createUser(CreateUserRequest request, AuditActor actor) {
        request.setEmail(normalizeEmail(request.getEmail()));
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BusinessRuleException("Já existe um usuário com esse e-mail");
        }

        Tenant homeTenant = tenantRepository.findById(TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Filial não encontrada"));

        User user = userRepository.save(User.builder()
                .email(request.getEmail())
                .fullName(request.getFullName())
                .passwordHash(passwordEncoder.encode(request.getInitialPassword()))
                .tenant(homeTenant)
                .status(User.UserStatus.ACTIVE)
                .build());

        auditService.log(actor, TenantContext.getTenantId(), "CREATE_USER", "User",
                user.getId(), null, request.getEmail());

        return toResponse(user);
    }

    @Transactional
    public UserRoleAssignmentResponse assignRole(UUID userId, AssignRoleRequest request, AuthenticatedUser principal) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (request.getRole() == Role.RoleName.SUPER_ADMIN && !isSuperAdmin(principal)) {
            throw new AccessForbiddenException("Só um administrador geral pode atribuir o papel de administrador geral");
        }
        AuditActor actor = AuditActor.from(principal);
        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new ResourceNotFoundException("Papel não encontrado"));

        UUID targetFilialId;

        if (!TenantContext.isGlobalScope()) {
            // Ator LOCAL: a filial só pode ser a própria — ignora o que vier no request
            // (ou exige que bata, se vier preenchido, para evitar confusão silenciosa)
            if (request.getFilialId() != null && !request.getFilialId().equals(TenantContext.getTenantId())) {
                throw new AccessForbiddenException("Você só pode gerenciar usuários da sua própria filial");
            }
            targetFilialId = TenantContext.getTenantId();

            if (role.getScope() == Role.RoleScope.GLOBAL) {
                throw new AccessForbiddenException("Você não tem permissão para atribuir um papel de escopo global");
            }
        } else {
            // Ator GLOBAL: precisa informar explicitamente qual filial
            if (request.getFilialId() == null) {
                throw new BusinessRuleException("Informe a filial de destino");
            }
            targetFilialId = request.getFilialId();
        }

        Tenant filial = tenantRepository.findById(targetFilialId)
                .orElseThrow(() -> new ResourceNotFoundException("Filial não encontrada"));

        boolean alreadyAssigned = userFilialRoleRepository.findByUserAndFilial(user, filial.getId())
                .stream().anyMatch(ufr -> ufr.getRole().getId().equals(role.getId()));
        if (alreadyAssigned) {
            throw new BusinessRuleException("Usuário já possui esse papel nessa filial");
        }

        UserFilialRole assignment = userFilialRoleRepository.save(UserFilialRole.builder()
                .user(user)
                .filial(filial)
                .role(role)
                .build());

        auditService.log(actor, TenantContext.getTenantId(), "ASSIGN_ROLE", "User",
                user.getId(), null, java.util.Map.of("filialId", filial.getId(), "role", role.getName()));

        return UserRoleAssignmentResponse.builder()
                .id(assignment.getId())
                .filialId(filial.getId())
                .filialName(filial.getName())
                .role(role.getName().name())
                .scope(role.getScope().name())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> listUsers(UUID filialId, User.UserStatus status, Pageable pageable) {
        if (!TenantContext.isGlobalScope()) {
            if (filialId != null && !filialId.equals(TenantContext.getTenantId())) {
                throw new AccessForbiddenException("Você só pode gerenciar usuários da sua própria filial");
            }
            filialId = TenantContext.getTenantId();
        }
        Page<User> page = userRepository.findManagedUsers(filialId, status, pageable);
        Map<UUID, List<UserFilialRole>> roles = page.isEmpty() ? Map.of() :
                userFilialRoleRepository.findByUserIn(page.getContent()).stream()
                        .collect(Collectors.groupingBy(r -> r.getUser().getId()));
        return page.map(u -> UserSummaryResponse.builder().id(u.getId()).email(u.getEmail())
                .fullName(u.getFullName()).phone(u.getPhone()).status(u.getStatus().name())
                .lastLogin(u.getLastLogin()).roles(visibleRoles(roles.getOrDefault(u.getId(), List.of()))).build());
    }

    @Transactional(readOnly = true)
    public UserDetailResponse getUser(UUID id) {
        User user = findUser(id);
        List<UserFilialRole> roles = userFilialRoleRepository.findByUser(user);
        if (!TenantContext.isGlobalScope() && !belongsToActiveFilial(user, roles)) {
            throw new ResourceNotFoundException("Usuário não encontrado");
        }
        return detail(user, roles);
    }

    private User findUser(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }

    private boolean belongsToActiveFilial(User user, List<UserFilialRole> roles) {
        return user.getTenant().getId().equals(TenantContext.getTenantId()) || roles.stream()
                .anyMatch(r -> r.getFilial().getId().equals(TenantContext.getTenantId()));
    }

    private boolean isSuperAdmin(AuthenticatedUser principal) {
        return principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }

    private String normalizeEmail(String email) { return email.trim().toLowerCase(Locale.ROOT); }

    private List<UserRoleAssignmentResponse> visibleRoles(List<UserFilialRole> roles) {
        return roles.stream().filter(r -> TenantContext.isGlobalScope() || r.getFilial().getId().equals(TenantContext.getTenantId()))
                .map(r -> UserRoleAssignmentResponse.builder().id(r.getId()).filialId(r.getFilial().getId())
                        .filialName(r.getFilial().getName()).role(r.getRole().getName().name()).scope(r.getRole().getScope().name()).build()).toList();
    }

    private UserDetailResponse detail(User user, List<UserFilialRole> roles) {
        return UserDetailResponse.builder().id(user.getId()).email(user.getEmail()).fullName(user.getFullName())
                .phone(user.getPhone()).status(user.getStatus().name()).lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt()).updatedAt(user.getUpdatedAt()).homeFilialId(user.getTenant().getId())
                .homeFilialName(user.getTenant().getName()).roles(visibleRoles(roles)).build();
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .status(user.getStatus().name())
                .build();
    }
}