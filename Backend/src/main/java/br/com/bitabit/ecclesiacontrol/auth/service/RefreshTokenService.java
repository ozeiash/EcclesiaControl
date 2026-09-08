package br.com.bitabit.ecclesiacontrol.auth.service;

import br.com.bitabit.ecclesiacontrol.auth.domain.RefreshToken;
import br.com.bitabit.ecclesiacontrol.auth.domain.Role;
import br.com.bitabit.ecclesiacontrol.auth.domain.User;
import br.com.bitabit.ecclesiacontrol.auth.repository.RefreshTokenRepository;
import br.com.bitabit.ecclesiacontrol.core.exception.AuthenticationFailedException;
import br.com.bitabit.ecclesiacontrol.core.security.CryptUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMs;

    @Transactional
    public String issue(User user, UUID filialId, Role.RoleScope scope) {
        String rawToken = generateSecureToken();

        RefreshToken entity = RefreshToken.builder()
                .user(user)
                .tokenHash(CryptUtils.hashForLookup(rawToken))
                .filialId(filialId)
                .scope(scope)
                .expiresAt(LocalDateTime.now().plus(refreshExpirationMs, ChronoUnit.MILLIS))
                .build();
        refreshTokenRepository.save(entity);

        return rawToken;
    }

    @Transactional
    public RefreshToken validateAndConsume(String rawToken) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(CryptUtils.hashForLookup(rawToken))
                .orElseThrow(() -> new AuthenticationFailedException("Refresh token inválido"));

        if (token.isRevoked()) {
            // Detecção de reuso: um refresh token já revogado sendo usado de
            // novo é sinal de possível roubo — revoga TODAS as sessões ativas
            // do usuário como medida de contenção.
            log.warn("Tentativa de reuso de refresh token revogado — user_id={}", token.getUser().getId());
            refreshTokenRepository.findByUserAndRevokedFalse(token.getUser())
                    .forEach(t -> { t.setRevoked(true); refreshTokenRepository.save(t); });
            throw new AuthenticationFailedException("Refresh token inválido");
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AuthenticationFailedException("Refresh token expirado");
        }

        token.setRevoked(true); // rotação: um token só pode ser usado uma vez
        refreshTokenRepository.save(token);

        return token;
    }

    @Transactional
    public void revokeAllForUser(User user) {
        refreshTokenRepository.findByUserAndRevokedFalse(user)
                .forEach(t -> { t.setRevoked(true); refreshTokenRepository.save(t); });
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}