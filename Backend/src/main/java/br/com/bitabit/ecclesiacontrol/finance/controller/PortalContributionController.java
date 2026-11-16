package br.com.bitabit.ecclesiacontrol.finance.controller;

import br.com.bitabit.ecclesiacontrol.core.security.AuthenticatedUser;
import br.com.bitabit.ecclesiacontrol.finance.dto.*;
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

import java.util.List;
import java.util.UUID;

@Tag(name = "Portal do membro: contribuições", description = "O membro declara a contribuição que fez e acompanha o extrato. A declaração só vira lançamento quando o tesoureiro confirma. O membro vê apenas as próprias contribuições.")
@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PERM_PORTAL_ACCESS')")
public class PortalContributionController {

    private final ContributionDeclarationService service;

    @Operation(summary = "Contas disponíveis para contribuir",
            description = "Dízimo, oferta e missões.")
    @GetMapping("/contribution-accounts")
    public ResponseEntity<List<ChartOfAccountResponse>> accounts() {
        return ResponseEntity.ok(service.contributionAccounts());
    }

    @Operation(summary = "Como contribuir (chave PIX da filial)",
            description = "Pague fora do app e depois declare aqui. A chave é cadastrada pela sede.")
    @GetMapping("/payment-info")
    public ResponseEntity<PaymentInfoResponse> paymentInfo(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(service.paymentInfo(principal));
    }

    @Operation(summary = "Declarar uma contribuição",
            description = "Fica PENDENTE até o tesoureiro confirmar ou rejeitar. Não entra no financeiro antes disso. Máximo de 10 pendentes por membro.")
    @ApiResponse(responseCode = "201", description = "Declaração registrada")
    @PostMapping("/contribution-declarations")
    public ResponseEntity<ContributionDeclarationResponse> declare(
            @RequestBody @Valid DeclareContributionRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.declare(request, principal));
    }

    @Operation(summary = "Minhas declarações",
            description = "Todas as situações (pendente, confirmada, rejeitada com o motivo, cancelada), da mais recente para a mais antiga.")
    @GetMapping("/contribution-declarations")
    public ResponseEntity<Page<ContributionDeclarationResponse>> declarations(
            @ParameterObject Pageable pageable, @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(service.myDeclarations(pageable, principal));
    }

    @Operation(summary = "Cancelar uma declaração pendente",
            description = "Só enquanto estiver PENDENTE. Declaração de outro membro responde 404.")
    @ApiResponse(responseCode = "204", description = "Declaração cancelada")
    @DeleteMapping("/contribution-declarations/{id}")
    public ResponseEntity<Void> cancel(@Parameter(description = "Id da declaração") @PathVariable UUID id,
                                       @AuthenticationPrincipal AuthenticatedUser principal) {
        service.cancel(id, principal);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Meu extrato de contribuições",
            description = "Contribuições registradas em seu nome, inclusive as lançadas pelo tesoureiro, da mais recente para a mais antiga.")
    @GetMapping("/contributions")
    public ResponseEntity<Page<MyContributionResponse>> contributions(
            @ParameterObject Pageable pageable, @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(service.myContributions(pageable, principal));
    }
}
