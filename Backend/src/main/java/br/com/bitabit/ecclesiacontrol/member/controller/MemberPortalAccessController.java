package br.com.bitabit.ecclesiacontrol.member.controller;

import br.com.bitabit.ecclesiacontrol.core.security.AuthenticatedUser;
import br.com.bitabit.ecclesiacontrol.member.dto.PortalAccessResponse;
import br.com.bitabit.ecclesiacontrol.member.service.MemberPortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Membros: acesso ao portal", description = "A secretaria libera ou revoga o login do membro.")
@RestController
@RequestMapping("/members/{memberId}/portal-access")
@RequiredArgsConstructor
public class MemberPortalAccessController {

    private final MemberPortalService portalService;

    @Operation(summary = "Liberar acesso ao portal",
            description = "Cria a conta (papel MEMBRO) com o e-mail do cadastro e devolve uma senha provisória, mostrada uma única vez. "
                    + "Exige membro ativo, maior de 18 anos e com e-mail. Exige USER_MANAGE.")
    @ApiResponse(responseCode = "201", description = "Acesso liberado")
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_USER_MANAGE')")
    public ResponseEntity<PortalAccessResponse> grant(
            @Parameter(description = "Id do membro") @PathVariable UUID memberId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(portalService.grantAccess(memberId, principal));
    }

    @Operation(summary = "Revogar acesso ao portal",
            description = "Suspende a conta e encerra as sessões. O cadastro do membro não muda. Exige USER_MANAGE.")
    @ApiResponse(responseCode = "204", description = "Acesso revogado")
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_USER_MANAGE')")
    public ResponseEntity<Void> revoke(
            @Parameter(description = "Id do membro") @PathVariable UUID memberId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        portalService.revokeAccess(memberId, principal);
        return ResponseEntity.noContent().build();
    }
}
