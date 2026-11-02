package br.com.bitabit.ecclesiacontrol.finance.controller;

import br.com.bitabit.ecclesiacontrol.finance.dto.FinanceDashboardResponse;
import br.com.bitabit.ecclesiacontrol.finance.dto.MembersDashboardResponse;
import br.com.bitabit.ecclesiacontrol.finance.dto.OfficialFinanceDashboardResponse;
import br.com.bitabit.ecclesiacontrol.finance.service.DashboardService;
import br.com.bitabit.ecclesiacontrol.finance.service.OfficialDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final OfficialDashboardService officialDashboardService;

    @GetMapping("/finance")
    @PreAuthorize("hasAuthority('PERM_FINANCE_READ')")
    public ResponseEntity<FinanceDashboardResponse> finance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(dashboardService.finance(start, end));
    }

    @GetMapping("/members")
    @PreAuthorize("hasAuthority('PERM_MEMBER_READ')")
    public ResponseEntity<MembersDashboardResponse> members(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(dashboardService.members(start, end));
    }

    @GetMapping("/finance/closed")
    @PreAuthorize("hasAuthority('PERM_FINANCE_READ')")
    public ResponseEntity<OfficialFinanceDashboardResponse> officialFinance(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth from,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth to) {
        return ResponseEntity.ok(officialDashboardService.officialFinance(from, to));
    }
}