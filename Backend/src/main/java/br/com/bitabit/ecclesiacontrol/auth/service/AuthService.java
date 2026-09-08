package br.com.bitabit.ecclesiacontrol.auth.service;

import br.com.bitabit.ecclesiacontrol.auth.domain.RefreshToken;
import br.com.bitabit.ecclesiacontrol.auth.domain.Role;
import br.com.bitabit.ecclesiacontrol.auth.domain.User;
import br.com.bitabit.ecclesiacontrol.auth.domain.UserFilialRole;
import br.com.bitabit.ecclesiacontrol.auth.dto.LoginRequest;
import br.com.bitabit.ecclesiacontrol.auth.dto.LoginResponse;
import br.com.bitabit.ecclesiacontrol.auth.repository.UserFilialRoleRepository;
import br.com.bitabit.ecclesiacontrol.auth.repository.UserRepository;
import br.com.bitabit.ecclesiacontrol.core.exception.AccessForbiddenException;
import br.com.bitabit.ecclesiacontrol.core.exception.AuthenticationFailedException;
import br.com.bitabit.ecclesiacontrol.core.exception.BusinessRuleException;
import br.com.bitabit.ecclesiacontrol.core.exception.ResourceNotFoundException;
import br.com.bitabit.ecclesiacontrol.core.security.JwtTokenProvider;
import br.com.bitabit.ecclesiacontrol.core.service.AuditService;
import br.com.bitabit.ecclesiacontrol.tenant.domain.Tenant;
import br.com.bitabit.ecclesiacontrol.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final UserFilialRoleRepository userFilialRoleRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final RefreshTokenService refreshTokenService;
    private final TenantRepository tenantRepository;

    /**
     * Login inicial do usuário
     */
    public LoginResponse authenticate(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthenticationFailedException("Credenciais inválidas"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthenticationFailedException("Credenciais inválidas");
        }

        var userFilialRoles = userFilialRoleRepository.findByUser(user);
        if (userFilialRoles.isEmpty()) {
            throw new BusinessRuleException("Usuário não possui filiais associadas");
        }

        UserFilialRole defaultRole = userFilialRoles.stream()
                .filter(ufr -> ufr.getRole().getScope().equals(Role.RoleScope.GLOBAL))
                .findFirst()
                .orElseGet(() -> userFilialRoles.get(0));

        String accessToken = jwtTokenProvider.generateToken(
                user.getEmail(), defaultRole.getFilial().getId(), defaultRole.getRole().getScope());

        String refreshToken = refreshTokenService.issue(
                user, defaultRole.getFilial().getId(), defaultRole.getRole().getScope());

        auditService.log(user, defaultRole.getFilial().getId(), "LOGIN", "User", user.getId(), null, null);

        log.info("User logged in: {} at filial: {}", user.getEmail(), defaultRole.getFilial().getName());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(3600L)
                .user(LoginResponse.UserInfo.builder()
                        .id(user.getId().toString())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .roles(extractRoles(userFilialRoles))
                        .build())
                .build();
    }

    public LoginResponse refreshAccessToken(String rawRefreshToken) {
        RefreshToken oldToken = refreshTokenService.validateAndConsume(rawRefreshToken);
        User user = oldToken.getUser();

        String newAccessToken = jwtTokenProvider.generateToken(
                user.getEmail(), oldToken.getFilialId(), oldToken.getScope());
        String newRefreshToken = refreshTokenService.issue(
                user, oldToken.getFilialId(), oldToken.getScope());

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(3600L)
                .build();
    }

    /**
     * Trocar de contexto (mudança de filial para Pastor Sede)
     */
    public LoginResponse switchFilial(UUID userId, UUID targetFilialId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationFailedException("Credenciais inválidas"));

        boolean hasGlobalRole = userFilialRoleRepository.findGlobalRolesByUser(user)
                .stream().findAny().isPresent();
        if (!hasGlobalRole) {
            throw new AccessForbiddenException("Usuário não possui papel GLOBAL para trocar de filial");
        }

        Tenant targetFilial = tenantRepository.findById(targetFilialId)
                .orElseThrow(() -> new ResourceNotFoundException("Filial não encontrada"));

        String token = jwtTokenProvider.generateToken(user.getEmail(), targetFilialId, Role.RoleScope.LOCAL);
        String refreshToken = refreshTokenService.issue(user, targetFilialId, Role.RoleScope.LOCAL);

        auditService.log(user, targetFilialId, "SWITCH_FILIAL", "User", user.getId(),
                null, java.util.Map.of("targetFilialId", targetFilialId));

        log.info("User switched filial: {} to {}", user.getEmail(), targetFilialId);

        return LoginResponse.builder()
                .accessToken(token)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(3600L)
                .build();
    }

    private Set<String> extractRoles(java.util.List<UserFilialRole> userFilialRoles) {
        return userFilialRoles.stream()
                .map(ufr -> ufr.getRole().getName().toString())
                .collect(Collectors.toSet());
    }
}