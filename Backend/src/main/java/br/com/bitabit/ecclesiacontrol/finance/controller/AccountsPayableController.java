package br.com.bitabit.ecclesiacontrol.finance.controller;

import br.com.bitabit.ecclesiacontrol.core.security.AuthenticatedUser;
import br.com.bitabit.ecclesiacontrol.core.service.AuditActor;
import br.com.bitabit.ecclesiacontrol.finance.domain.PayableStatus;
import br.com.bitabit.ecclesiacontrol.finance.dto.*;
import br.com.bitabit.ecclesiacontrol.finance.service.AccountsPayableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/finance/payables")
@RequiredArgsConstructor
public class AccountsPayableController {

    private final AccountsPayableService payableService;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_FINANCE_WRITE')")
    public ResponseEntity<AccountsPayableResponse> create(@RequestBody @Valid CreateAccountsPayableRequest request,
                                                          @AuthenticationPrincipal AuthenticatedUser principal) {
        var created = payableService.create(request, AuditActor.from(principal));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{id}/settlements")
    @PreAuthorize("hasAuthority('PERM_FINANCE_WRITE')")
    public ResponseEntity<AccountsPayableResponse> settle(@PathVariable UUID id,
                                                          @RequestBody @Valid SettlePayableRequest request,
                                                          @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(payableService.settle(id, request, AuditActor.from(principal)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_FINANCE_READ')")
    public ResponseEntity<Page<AccountsPayableResponse>> findAll(@RequestParam(required = false) PayableStatus status,
                                                                 Pageable pageable) {
        return ResponseEntity.ok(payableService.findAll(status, pageable));
    }
}