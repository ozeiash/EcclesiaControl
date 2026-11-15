package br.com.bitabit.ecclesiacontrol.finance.controller;

import br.com.bitabit.ecclesiacontrol.core.security.AuthenticatedUser;
import br.com.bitabit.ecclesiacontrol.core.service.AuditActor;
import br.com.bitabit.ecclesiacontrol.finance.domain.PayableStatus;
import br.com.bitabit.ecclesiacontrol.finance.dto.*;
import br.com.bitabit.ecclesiacontrol.finance.service.AccountsPayableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
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

@Tag(name = "Financeiro: contas a pagar", description = "Obrigações com baixas parciais; cada baixa gera um lançamento e não pode exceder o saldo.")
@RestController
@RequestMapping("/finance/payables")
@RequiredArgsConstructor
public class AccountsPayableController {

    private final AccountsPayableService payableService;

    @Operation(summary = "Cadastrar conta a pagar",
            description = "Registra uma obrigação com vencimento e valor total. Exige FINANCE_WRITE.")
    @ApiResponse(responseCode = "201", description = "Conta criada")
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_FINANCE_WRITE')")
    public ResponseEntity<AccountsPayableResponse> create(@RequestBody @Valid CreateAccountsPayableRequest request,
                                                          @AuthenticationPrincipal AuthenticatedUser principal) {
        var created = payableService.create(request, AuditActor.from(principal));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Dar baixa (parcial ou total)",
            description = "Cada baixa gera um lançamento de despesa e não pode exceder o saldo em aberto. Mês fechado responde 422. Exige FINANCE_WRITE.")
    @PostMapping("/{id}/settlements")
    @PreAuthorize("hasAuthority('PERM_FINANCE_WRITE')")
    public ResponseEntity<AccountsPayableResponse> settle(
            @Parameter(description = "Id da conta a pagar") @PathVariable UUID id,
                                                          @RequestBody @Valid SettlePayableRequest request,
                                                          @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(payableService.settle(id, request, AuditActor.from(principal)));
    }

    @Operation(summary = "Listar contas a pagar",
            description = "Lista paginada, com filtro opcional por situação. Exige FINANCE_READ.")
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_FINANCE_READ')")
    public ResponseEntity<Page<AccountsPayableResponse>> findAll(
            @Parameter(description = "Filtra pela situação da conta") @RequestParam(required = false) PayableStatus status,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(payableService.findAll(status, pageable));
    }
}