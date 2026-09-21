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

import java.util.UUID;

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
    public UserRoleAssignmentResponse assignRole(UUID userId, AssignRoleRequest request, AuditActor actor) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

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

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .status(user.getStatus().name())
                .build();
    }
}