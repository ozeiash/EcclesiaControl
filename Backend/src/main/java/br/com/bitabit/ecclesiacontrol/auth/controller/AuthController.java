package br.com.bitabit.ecclesiacontrol.auth.controller;

import br.com.bitabit.ecclesiacontrol.auth.domain.User;
import br.com.bitabit.ecclesiacontrol.auth.dto.LoginRequest;
import br.com.bitabit.ecclesiacontrol.auth.dto.LoginResponse;
import br.com.bitabit.ecclesiacontrol.auth.dto.RefreshTokenRequest;
import br.com.bitabit.ecclesiacontrol.auth.dto.SwitchFilialRequest;
import br.com.bitabit.ecclesiacontrol.auth.repository.UserRepository;
import br.com.bitabit.ecclesiacontrol.auth.service.AuthService;
import br.com.bitabit.ecclesiacontrol.auth.service.RefreshTokenService;
import br.com.bitabit.ecclesiacontrol.core.exception.AuthenticationFailedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<LoginResponse> refresh(@RequestBody @Valid RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshAccessToken(request.getRefreshToken()));
    }

    @PostMapping("/switch-filial")
    public ResponseEntity<LoginResponse> switchFilial(@AuthenticationPrincipal UserDetails userDetails,
                                                      @RequestBody @Valid SwitchFilialRequest request) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new AuthenticationFailedException("Credenciais inválidas"));
        return ResponseEntity.ok(authService.switchFilial(user.getId(), request.getTargetFilialId()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new AuthenticationFailedException("Credenciais inválidas"));
        refreshTokenService.revokeAllForUser(user);
        return ResponseEntity.noContent().build();
    }
}