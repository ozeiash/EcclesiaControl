package br.com.bitabit.ecclesiacontrol.finance.controller;

import br.com.bitabit.ecclesiacontrol.finance.dto.FinanceDashboardResponse;
import br.com.bitabit.ecclesiacontrol.finance.dto.MembersDashboardResponse;
import br.com.bitabit.ecclesiacontrol.finance.dto.OfficialFinanceDashboardResponse;
import br.com.bitabit.ecclesiacontrol.finance.service.DashboardService;
import br.com.bitabit.ecclesiacontrol.finance.service.OfficialDashboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

@Tag(name = "Dashboards", description = "Operacional (estado atual) e oficial (só competências fechadas). O escopo vem do token.")
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final OfficialDashboardService officialDashboardService;

    @Operation(summary = "Dashboard financeiro operacional",
            description = """
                    Reflete o banco neste momento, inclusive meses ainda abertos (valores provisórios). Sem datas, usa o
                    mês corrente. Token GLOBAL: consolidado de todas as filiais, com quebra por filial e sem as contas de
                    repasse. Token LOCAL: somente a filial do token, com o repasse como despesa real.""")
    @GetMapping("/finance")
    @PreAuthorize("hasAuthority('PERM_FINANCE_READ')")
    public ResponseEntity<FinanceDashboardResponse> finance(
            @Parameter(description = "Data inicial (yyyy-MM-dd). Padrão: primeiro dia do mês corrente")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @Parameter(description = "Data final (yyyy-MM-dd). Padrão: último dia do mês corrente")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(dashboardService.finance(start, end));
    }

    @Operation(summary = "Dashboard de membros",
            description = """
                    Total por status (cadastros duplicados não contam), admissões no período e, no consolidado, membros
                    ativos por filial. Exige MEMBER_READ, permissão separada da financeira: quem só tem acesso
                    financeiro (como o Contador) não vê dados de membros.""")
    @GetMapping("/members")
    @PreAuthorize("hasAuthority('PERM_MEMBER_READ')")
    public ResponseEntity<MembersDashboardResponse> members(
            @Parameter(description = "Data inicial (yyyy-MM-dd). Padrão: primeiro dia do mês corrente")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @Parameter(description = "Data final (yyyy-MM-dd). Padrão: último dia do mês corrente")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(dashboardService.members(start, end));
    }

    @Operation(summary = "Dashboard financeiro oficial (competências fechadas)",
            description = """
                    Competências inteiras. Só entram as filiais com **todos** os meses do intervalo fechados; as demais
                    vêm em `pending` com os meses que faltam, `complete` fica falso e um aviso acompanha o resultado.
                    Uma filial fechada sem movimento entra com valores zerados. O intervalo termina, no máximo, no
                    último mês já encerrado e tem limite de 24 meses. A lista de filiais é calculada pelo servidor a
                    partir do token: o cliente não a informa.""")
    @GetMapping("/finance/closed")
    @PreAuthorize("hasAuthority('PERM_FINANCE_READ')")
    public ResponseEntity<OfficialFinanceDashboardResponse> officialFinance(
            @Parameter(description = "Competência inicial (yyyy-MM)", example = "2026-01")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth from,
            @Parameter(description = "Competência final (yyyy-MM)", example = "2026-09")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth to) {
        return ResponseEntity.ok(officialDashboardService.officialFinance(from, to));
    }
}