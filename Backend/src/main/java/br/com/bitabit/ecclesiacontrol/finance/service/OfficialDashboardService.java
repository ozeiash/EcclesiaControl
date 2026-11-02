package br.com.bitabit.ecclesiacontrol.finance.service;

import br.com.bitabit.ecclesiacontrol.core.exception.BusinessRuleException;
import br.com.bitabit.ecclesiacontrol.core.util.TenantContext;
import br.com.bitabit.ecclesiacontrol.finance.domain.AccountingPeriod;
import br.com.bitabit.ecclesiacontrol.finance.domain.PeriodStatus;
import br.com.bitabit.ecclesiacontrol.finance.domain.TransactionType;
import br.com.bitabit.ecclesiacontrol.finance.dto.FinanceDashboardResponse;
import br.com.bitabit.ecclesiacontrol.finance.dto.OfficialFinanceDashboardResponse;
import br.com.bitabit.ecclesiacontrol.finance.repository.AccountingPeriodRepository;
import br.com.bitabit.ecclesiacontrol.finance.repository.FinancialTransactionRepository;
import br.com.bitabit.ecclesiacontrol.tenant.domain.Tenant;
import br.com.bitabit.ecclesiacontrol.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OfficialDashboardService {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private final FinancialTransactionRepository transactionRepository;
    private final AccountingPeriodRepository periodRepository;
    private final TenantRepository tenantRepository;

    @Value("${app.dashboard.max-months:24}")
    private int maxMonths;

    @Transactional(readOnly = true)   // no método, por causa do aspect do filtro de tenant
    public OfficialFinanceDashboardResponse officialFinance(YearMonth from, YearMonth to) {
        validate(from, to);
        boolean consolidated = TenantContext.isGlobalScope();

        // A lista de filiais é sempre calculada aqui, pelo escopo do token. Nunca vem do cliente.
        List<Tenant> candidates = consolidated
                ? tenantRepository.findByIsActiveTrue()
                : tenantRepository.findById(TenantContext.getTenantId()).map(List::of).orElse(List.of());

        Map<UUID, List<AccountingPeriod>> closedByTenant = candidates.isEmpty() ? Map.of()
                : periodRepository.findByTenantIdInAndReferenceMonthBetweenAndStatus(
                        candidates.stream().map(Tenant::getId).toList(),
                        from.atDay(1), to.atDay(1), PeriodStatus.FECHADO)
                .stream().collect(Collectors.groupingBy(AccountingPeriod::getTenantId));

        List<Tenant> included = new ArrayList<>();
        List<OfficialFinanceDashboardResponse.PendingFilial> pending = new ArrayList<>();

        for (Tenant t : candidates) {
            YearMonth bookStart = AccountingPeriodService.bookStart(
                    t, transactionRepository.earliestTransactionDate(t.getId()));
            if (bookStart == null || bookStart.isAfter(to)) continue;   // sem escrituração no intervalo
            YearMonth first = bookStart.isAfter(from) ? bookStart : from;

            Set<YearMonth> closed = closedByTenant.getOrDefault(t.getId(), List.of()).stream()
                    .map(p -> YearMonth.from(p.getReferenceMonth())).collect(Collectors.toSet());
            List<String> missing = new ArrayList<>();
            for (YearMonth m = first; !m.isAfter(to); m = m.plusMonths(1)) {
                if (!closed.contains(m)) missing.add(m.toString());
            }

            if (missing.isEmpty()) {
                included.add(t);
            } else {
                pending.add(OfficialFinanceDashboardResponse.PendingFilial.builder()
                        .filialId(t.getId()).filialName(t.getName()).missingMonths(missing).build());
            }
        }
        pending.sort(Comparator.comparing(OfficialFinanceDashboardResponse.PendingFilial::getFilialName));

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        List<FinanceDashboardResponse.AccountTotal> byAccount = List.of();
        List<OfficialFinanceDashboardResponse.FilialResult> byFilial = List.of();

        if (!included.isEmpty()) {   // IN () com lista vazia não é válido no Postgres
            List<UUID> ids = included.stream().map(Tenant::getId).toList();
            LocalDate rangeStart = from.atDay(1);
            LocalDate rangeEnd = to.atEndOfMonth();

            for (var r : transactionRepository.officialTotalsByType(ids, rangeStart, rangeEnd, consolidated)) {
                if (r.getType() == TransactionType.RECEITA) income = r.getTotal(); else expense = r.getTotal();
            }

            byAccount = transactionRepository.officialTotalsByAccount(ids, rangeStart, rangeEnd, consolidated).stream()
                    .map(a -> FinanceDashboardResponse.AccountTotal.builder()
                            .code(a.getCode()).name(a.getName()).type(a.getType().name()).total(a.getTotal()).build())
                    .toList();

            if (consolidated) {
                Map<UUID, Map<TransactionType, BigDecimal>> totals = new HashMap<>();
                for (var r : transactionRepository.officialTotalsByTenant(ids, rangeStart, rangeEnd, true)) {
                    totals.computeIfAbsent(r.getTenantId(), k -> new EnumMap<>(TransactionType.class))
                            .put(r.getType(), r.getTotal());
                }
                byFilial = included.stream()
                        .sorted(Comparator.comparing(Tenant::getName))
                        .map(t -> {
                            Map<TransactionType, BigDecimal> m = totals.getOrDefault(t.getId(), Map.of());
                            BigDecimal i = m.getOrDefault(TransactionType.RECEITA, BigDecimal.ZERO);
                            BigDecimal x = m.getOrDefault(TransactionType.DESPESA, BigDecimal.ZERO);
                            long count = closedByTenant.getOrDefault(t.getId(), List.of()).stream()
                                    .mapToLong(p -> p.getTransactionCount() != null ? p.getTransactionCount() : 0).sum();
                            return OfficialFinanceDashboardResponse.FilialResult.builder()
                                    .filialId(t.getId()).filialName(t.getName())
                                    .income(i).expense(x).balance(i.subtract(x)).transactionCount(count).build();
                        }).toList();
            }
        }

        return OfficialFinanceDashboardResponse.builder()
                .fromMonth(from.toString()).toMonth(to.toString())
                .consolidated(consolidated)
                .complete(pending.isEmpty())
                .notice(pending.isEmpty() ? null
                        : "Os valores apresentados consideram apenas as filiais com todos os períodos fechados.")
                .totalIncome(income).totalExpense(expense).balance(income.subtract(expense))
                .byAccount(byAccount).byFilial(byFilial).pending(pending)
                .build();
    }

    private void validate(YearMonth from, YearMonth to) {
        if (from.isAfter(to)) {
            throw new BusinessRuleException("A competência inicial não pode ser posterior à final");
        }
        if (ChronoUnit.MONTHS.between(from, to) + 1 > maxMonths) {
            throw new BusinessRuleException("O intervalo máximo é de " + maxMonths + " meses por consulta");
        }
        YearMonth lastEnded = YearMonth.now(ZONE).minusMonths(1);   // o mês corrente ainda não terminou
        if (to.isAfter(lastEnded)) {
            throw new BusinessRuleException(
                    "O relatório oficial só considera meses já encerrados; a última competência possível é " + lastEnded);
        }
    }
}
