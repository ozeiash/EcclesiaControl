package br.com.bitabit.ecclesiacontrol.finance.controller;

import br.com.bitabit.ecclesiacontrol.core.security.AuthenticatedUser;
import br.com.bitabit.ecclesiacontrol.core.service.AuditActor;
import br.com.bitabit.ecclesiacontrol.finance.dto.FinancialTransactionRequest;
import br.com.bitabit.ecclesiacontrol.finance.dto.FinancialTransactionResponse;
import br.com.bitabit.ecclesiacontrol.finance.service.FinancialTransactionService;
import io.swagger.v3.oas.annotations.Operation;
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

@Tag(name = "Financeiro: lançamentos", description = "Receitas e despesas. Um mês fechado recusa lançamentos novos (422).")
@RestController
@RequestMapping("/finance/transactions")
@RequiredArgsConstructor
public class FinancialTransactionController {

    private final FinancialTransactionService transactionService;

    @Operation(summary = "Lançar receita ou despesa",
            description = "Cria um lançamento na filial ativa. Se o mês da data já estiver fechado, responde 422. Exige FINANCE_WRITE.")
    @ApiResponse(responseCode = "201", description = "Lançamento criado")
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_FINANCE_WRITE')")
    public ResponseEntity<FinancialTransactionResponse> create(@RequestBody @Valid FinancialTransactionRequest request,
                                                               @AuthenticationPrincipal AuthenticatedUser principal) {
        var created = transactionService.create(request, AuditActor.from(principal));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Listar lançamentos",
            description = "Lista paginada dos lançamentos da filial ativa. Exige FINANCE_READ.")
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_FINANCE_READ')")
    public ResponseEntity<Page<FinancialTransactionResponse>> findAll(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(transactionService.findAll(pageable));
    }
}