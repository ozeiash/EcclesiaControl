package br.com.bitabit.ecclesiacontrol.member.controller;

import br.com.bitabit.ecclesiacontrol.core.security.AuthenticatedUser;
import br.com.bitabit.ecclesiacontrol.member.dto.ChangePasswordRequest;
import br.com.bitabit.ecclesiacontrol.member.dto.MyProfileResponse;
import br.com.bitabit.ecclesiacontrol.member.dto.UpdateMyProfileRequest;
import br.com.bitabit.ecclesiacontrol.member.service.MemberPortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Portal do membro", description = "O membro vê e atualiza os próprios dados. Nenhum endpoint recebe o id do membro: o vínculo vem da conta autenticada.")
@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PERM_PORTAL_ACCESS')")
public class PortalController {

    private final MemberPortalService portalService;

    @Operation(summary = "Meu perfil",
            description = "CPF mascarado; RG e observações da liderança não são mostrados.")
    @GetMapping("/profile")
    public ResponseEntity<MyProfileResponse> profile(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(portalService.myProfile(principal));
    }

    @Operation(summary = "Atualizar meus dados de contato e endereço",
            description = "Substitui contato e endereço. Nome, CPF, situação e vida eclesiástica só a secretaria altera.")
    @PutMapping("/profile")
    public ResponseEntity<MyProfileResponse> update(@RequestBody @Valid UpdateMyProfileRequest request,
                                                    @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(portalService.updateMyProfile(request, principal));
    }

    @Operation(summary = "Trocar minha senha",
            description = "Exige a senha atual. Encerra as demais sessões: entre novamente quando o token atual expirar.")
    @ApiResponse(responseCode = "204", description = "Senha alterada")
    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(@RequestBody @Valid ChangePasswordRequest request,
                                               @AuthenticationPrincipal AuthenticatedUser principal) {
        portalService.changePassword(request, principal);
        return ResponseEntity.noContent().build();
    }
}
