package br.com.bitabit.ecclesiacontrol.finance.controller;

import br.com.bitabit.ecclesiacontrol.core.security.AuthenticatedUser;
import br.com.bitabit.ecclesiacontrol.core.service.AuditActor;
import br.com.bitabit.ecclesiacontrol.finance.dto.*;
import br.com.bitabit.ecclesiacontrol.finance.service.AccountingPeriodService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

@Tag(name = "Financeiro: períodos", description = "Fechamento mensal por filial, reabertura em cascata com motivo, início da escrituração e status da Sede.")
@RestController
@RequestMapping("/finance/periods")
@RequiredArgsConstructor
public class AccountingPeriodController {

    private final AccountingPeriodService periodService;

    @Operation(summary = "Status dos 12 meses de um ano",
            description = """
                    Para cada mês da filial ativa: ABERTO, FECHADO (com os totais gravados no fechamento) ou ATRASADO
                    (aberto e já fora do prazo de fechamento). Só conta como atrasado a partir do início da escrituração.""")
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_FINANCE_READ')")
    public ResponseEntity<List<PeriodResponse>> listYear(
            @Parameter(description = "Ano com quatro dígitos", example = "2026") @RequestParam int year) {
        return ResponseEntity.ok(periodService.listYear(year));
    }

    @Operation(summary = "Status de um mês em todas as filiais (visão da Sede)",
            description = """
                    Token GLOBAL: todas as filiais ativas. Token LOCAL: somente a própria. Mostra também o início da
                    escrituração de cada filial; quem ainda não configurou vem sem esse campo.""")
    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('PERM_FINANCE_READ')")
    public ResponseEntity<List<PeriodOverviewItem>> overview(
            @Parameter(description = "Competência no formato yyyy-MM", example = "2026-09")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return ResponseEntity.ok(periodService.overview(month));
    }

    @Operation(summary = "Fechar um mês da filial ativa",
            description = """
                    O mês precisa já ter terminado, e os meses fecham em ordem cronológica, inclusive os sem movimento.
                    O primeiro fechamento é o do início da escrituração (ou, se não houver, o do mês com o lançamento mais
                    antigo). O fechamento grava as receitas, as despesas e a quantidade de lançamentos. Depois de fechado,
                    o mês recusa lançamentos novos (422). O token GLOBAL age sobre a filial do token; para fechar outra
                    filial, use `switch-filial` antes.""")
    @PostMapping("/{month}/close")
    @PreAuthorize("hasAuthority('PERM_PERIOD_CLOSE')")
    public ResponseEntity<PeriodResponse> close(
            @Parameter(description = "Competência no formato yyyy-MM", example = "2026-09")
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(periodService.close(month, AuditActor.from(principal)));
    }

    @Operation(summary = "Reabrir um mês fechado",
            description = """
                    Exige permissão própria e um motivo (de 10 a 500 caracteres). **Reabre também todos os meses
                    seguintes**, que precisam ser fechados de novo, em ordem. O histórico de fechamentos e reaberturas
                    é preservado. A resposta lista os meses reabertos.""")
    @PostMapping("/{month}/reopen")
    @PreAuthorize("hasAuthority('PERM_PERIOD_REOPEN')")
    public ResponseEntity<List<PeriodResponse>> reopen(
            @Parameter(description = "Competência no formato yyyy-MM", example = "2026-09")
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @RequestBody @Valid ReopenPeriodRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(periodService.reopen(month, request.getReason(), AuditActor.from(principal)));
    }

    @Operation(summary = "Consultar o início da escrituração",
            description = "Devolve `configured: false` quando a filial ainda não definiu o mês de início.")
    @GetMapping("/start-month")
    @PreAuthorize("hasAuthority('PERM_FINANCE_READ')")
    public ResponseEntity<StartMonthResponse> startMonth() {
        return ResponseEntity.ok(periodService.getStartMonth());
    }

    @Operation(summary = "Definir o mês de início da escrituração",
            description = """
                    Lançamentos anteriores ao mês definido passam a ser recusados, e o primeiro fechamento é esse mês.
                    Só pode mudar enquanto não houver período fechado, não pode ser futuro e não pode ser posterior ao
                    lançamento mais antigo da filial.""")
    @PutMapping("/start-month")
    @PreAuthorize("hasAuthority('PERM_PERIOD_SETUP')")
    public ResponseEntity<StartMonthResponse> setStartMonth(@RequestBody @Valid SetStartMonthRequest request,
                                                            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(periodService.setStartMonth(request.getMonth(), AuditActor.from(principal)));
    }
}