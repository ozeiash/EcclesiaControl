package br.com.bitabit.ecclesiacontrol.finance.controller;

import br.com.bitabit.ecclesiacontrol.core.security.AuthenticatedUser;
import br.com.bitabit.ecclesiacontrol.finance.domain.DeclarationStatus;
import br.com.bitabit.ecclesiacontrol.finance.dto.ConfirmDeclarationRequest;
import br.com.bitabit.ecclesiacontrol.finance.dto.ContributionDeclarationResponse;
import br.com.bitabit.ecclesiacontrol.finance.dto.ContributionResponse;
import br.com.bitabit.ecclesiacontrol.finance.dto.RejectDeclarationRequest;
import br.com.bitabit.ecclesiacontrol.finance.service.ContributionDeclarationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Financeiro: declarações de contribuição", description = "Fila do tesoureiro: confirma ou rejeita as contribuições declaradas pelos membros. A confirmação cria a contribuição pelo mesmo caminho do caixa (mês fechado recusa).")
@RestController
@RequestMapping("/finance/contribution-declarations")
@RequiredArgsConstructor
public class ContributionDeclarationController {

    private final ContributionDeclarationService service;

    @Operation(summary = "Listar declarações",
            description = "Filtre por `status` (use PENDENTE para a fila). Mais antigas primeiro. Revela o membro, então exige CONTRIBUTION_DETAIL_READ.")
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_CONTRIBUTION_DETAIL_READ')")
    public ResponseEntity<Page<ContributionDeclarationResponse>> list(
            @Parameter(description = "Filtra pela situação") @RequestParam(required = false) DeclarationStatus status,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(service.list(status, pageable));
    }

    @Operation(summary = "Confirmar declaração",
            description = "Cria a contribuição identificada do membro. Sem `transactionDate`, usa a data declarada; "
                    + "se o mês dela estiver fechado, responde 422 e a declaração segue pendente: informe uma data em mês aberto. "
                    + "Exige FINANCE_WRITE e CONTRIBUTION_DETAIL_READ.")
    @ApiResponse(responseCode = "201", description = "Contribuição criada")
    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('PERM_FINANCE_WRITE') and hasAuthority('PERM_CONTRIBUTION_DETAIL_READ')")
    public ResponseEntity<ContributionResponse> confirm(
            @Parameter(description = "Id da declaração") @PathVariable UUID id,
            @RequestBody(required = false) @Valid ConfirmDeclarationRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.confirm(id, request, principal));
    }

    @Operation(summary = "Rejeitar declaração",
            description = "Exige o motivo, que o membro verá no extrato. Não cria lançamento. Exige FINANCE_WRITE e CONTRIBUTION_DETAIL_READ.")
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('PERM_FINANCE_WRITE') and hasAuthority('PERM_CONTRIBUTION_DETAIL_READ')")
    public ResponseEntity<ContributionDeclarationResponse> reject(
            @Parameter(description = "Id da declaração") @PathVariable UUID id,
            @RequestBody @Valid RejectDeclarationRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(service.reject(id, request, principal));
    }
}
