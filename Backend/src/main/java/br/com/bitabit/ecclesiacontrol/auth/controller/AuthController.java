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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Autenticação", description = "Login, renovação de token, troca de filial e logout.")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    @Operation(summary = "Entrar",
            description = "Autentica por e-mail e senha e devolve o access token e o refresh token. Não exige token.")
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @Operation(summary = "Renovar token",
            description = "Troca o refresh token por um novo par de tokens. O refresh token usado é revogado (rotação). Não exige token.")
    @SecurityRequirements
    @PostMapping("/refresh-token")
    public ResponseEntity<LoginResponse> refresh(@RequestBody @Valid RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshAccessToken(request.getRefreshToken()));
    }

    @Operation(summary = "Trocar de filial",
            description = "Emite um token LOCAL para a filial escolhida. Papéis GLOBAL passam a valer nessa filial. Exige estar autenticado.")
    @PostMapping("/switch-filial")
    public ResponseEntity<LoginResponse> switchFilial(@AuthenticationPrincipal UserDetails userDetails,
                                                      @RequestBody @Valid SwitchFilialRequest request) {
        User user = userRepository.findByEmailIgnoreCase(userDetails.getUsername())
                .orElseThrow(() -> new AuthenticationFailedException("Credenciais inválidas"));
        return ResponseEntity.ok(authService.switchFilial(user.getId(), request.getTargetFilialId()));
    }

    @Operation(summary = "Sair",
            description = "Revoga todos os refresh tokens do usuário. O access token atual vale até expirar.")
    @ApiResponse(responseCode = "204", description = "Sessões revogadas")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmailIgnoreCase(userDetails.getUsername())
                .orElseThrow(() -> new AuthenticationFailedException("Credenciais inválidas"));
        refreshTokenService.revokeAllForUser(user);
        return ResponseEntity.noContent().build();
    }
}