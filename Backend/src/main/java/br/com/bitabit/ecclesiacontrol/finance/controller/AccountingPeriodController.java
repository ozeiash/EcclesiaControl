package br.com.bitabit.ecclesiacontrol.finance.controller;

import br.com.bitabit.ecclesiacontrol.core.security.AuthenticatedUser;
import br.com.bitabit.ecclesiacontrol.core.service.AuditActor;
import br.com.bitabit.ecclesiacontrol.finance.dto.PeriodOverviewItem;
import br.com.bitabit.ecclesiacontrol.finance.dto.PeriodResponse;
import br.com.bitabit.ecclesiacontrol.finance.dto.ReopenPeriodRequest;
import br.com.bitabit.ecclesiacontrol.finance.service.AccountingPeriodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/finance/periods")
@RequiredArgsConstructor
public class AccountingPeriodController {

    private final AccountingPeriodService periodService;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_FINANCE_READ')")
    public ResponseEntity<List<PeriodResponse>> listYear(@RequestParam int year) {
        return ResponseEntity.ok(periodService.listYear(year));
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('PERM_FINANCE_READ')")
    public ResponseEntity<List<PeriodOverviewItem>> overview(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return ResponseEntity.ok(periodService.overview(month));
    }

    @PostMapping("/{month}/close")
    @PreAuthorize("hasAuthority('PERM_PERIOD_CLOSE')")
    public ResponseEntity<PeriodResponse> close(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(periodService.close(month, AuditActor.from(principal)));
    }

    @PostMapping("/{month}/reopen")
    @PreAuthorize("hasAuthority('PERM_PERIOD_REOPEN')")
    public ResponseEntity<List<PeriodResponse>> reopen(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @RequestBody @Valid ReopenPeriodRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(periodService.reopen(month, request.getReason(), AuditActor.from(principal)));
    }
}