package br.com.bitabit.ecclesiacontrol.auth.service;

import br.com.bitabit.ecclesiacontrol.auth.domain.Role;
import br.com.bitabit.ecclesiacontrol.auth.domain.User;
import br.com.bitabit.ecclesiacontrol.auth.domain.UserFilialRole;
import br.com.bitabit.ecclesiacontrol.auth.dto.LoginRequest;
import br.com.bitabit.ecclesiacontrol.auth.dto.LoginResponse;
import br.com.bitabit.ecclesiacontrol.auth.repository.UserFilialRoleRepository;
import br.com.bitabit.ecclesiacontrol.auth.repository.UserRepository;
import br.com.bitabit.ecclesiacontrol.core.security.JwtTokenProvider;
import br.com.bitabit.ecclesiacontrol.tenant.domain.Tenant;
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

    /**
     * Login inicial do usuário
     */
    public LoginResponse authenticate(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Credenciais inválidas"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Credenciais inválidas");
        }

        var userFilialRoles = userFilialRoleRepository.findByUser(user);
        if (userFilialRoles.isEmpty()) {
            throw new RuntimeException("Usuário não possui filiais associadas");
        }

        UserFilialRole defaultRole = userFilialRoles.stream()
                .filter(ufr -> ufr.getRole().getScope().equals(Role.RoleScope.GLOBAL))
                .findFirst()
                .orElseGet(() -> userFilialRoles.get(0));

        String token = jwtTokenProvider.generateToken(
                user.getEmail(), defaultRole.getFilial().getId(), defaultRole.getRole().getScope());

        log.info("User logged in: {} at filial: {}", user.getEmail(), defaultRole.getFilial().getName());

        return LoginResponse.builder()
                .accessToken(token)
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

    /**
     * Trocar de contexto (mudança de filial para Pastor Sede)
     */
    public LoginResponse switchFilial(UUID userId, UUID targetFilialId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Credenciais inválidas"));

        var userFilialRoles = userFilialRoleRepository.findByUserAndFilial(user, targetFilialId);
        if (userFilialRoles.isEmpty()) {
            throw new RuntimeException("Usuário não possui acesso a essa filial");
        }

        boolean hasGlobalRole = userFilialRoleRepository.findGlobalRolesByUser(user).stream().findAny().isPresent();
        if (!hasGlobalRole) {
            throw new RuntimeException("Usuário não possui papel GLOBAL para trocar de filial");
        }

        String token = jwtTokenProvider.generateToken(user.getEmail(), targetFilialId, Role.RoleScope.LOCAL);

        log.info("User switched filial: {} to {}", user.getEmail(), targetFilialId);

        return LoginResponse.builder()
                .accessToken(token)
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