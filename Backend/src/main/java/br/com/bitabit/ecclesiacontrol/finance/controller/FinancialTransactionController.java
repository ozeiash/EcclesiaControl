package br.com.bitabit.ecclesiacontrol.finance.controller;

import br.com.bitabit.ecclesiacontrol.core.security.AuthenticatedUser;
import br.com.bitabit.ecclesiacontrol.core.service.AuditActor;
import br.com.bitabit.ecclesiacontrol.finance.dto.FinancialTransactionRequest;
import br.com.bitabit.ecclesiacontrol.finance.dto.FinancialTransactionResponse;
import br.com.bitabit.ecclesiacontrol.finance.service.FinancialTransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/finance/transactions")
@RequiredArgsConstructor
public class FinancialTransactionController {

    private final FinancialTransactionService transactionService;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_FINANCE_WRITE')")
    public ResponseEntity<FinancialTransactionResponse> create(@RequestBody @Valid FinancialTransactionRequest request,
                                                               @AuthenticationPrincipal AuthenticatedUser principal) {
        var created = transactionService.create(request, AuditActor.from(principal));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_FINANCE_READ')")
    public ResponseEntity<Page<FinancialTransactionResponse>> findAll(Pageable pageable) {
        return ResponseEntity.ok(transactionService.findAll(pageable));
    }
}