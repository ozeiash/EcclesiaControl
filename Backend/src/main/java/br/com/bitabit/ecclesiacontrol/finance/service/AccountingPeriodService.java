package br.com.bitabit.ecclesiacontrol.finance.service;


import br.com.bitabit.ecclesiacontrol.core.exception.BusinessRuleException;
import br.com.bitabit.ecclesiacontrol.core.service.AuditActor;
import br.com.bitabit.ecclesiacontrol.core.service.AuditService;
import br.com.bitabit.ecclesiacontrol.core.util.TenantContext;
import br.com.bitabit.ecclesiacontrol.finance.domain.AccountingPeriod;
import br.com.bitabit.ecclesiacontrol.finance.domain.AccountingPeriodEvent;
import br.com.bitabit.ecclesiacontrol.finance.domain.PeriodEventType;
import br.com.bitabit.ecclesiacontrol.finance.domain.PeriodStatus;
import br.com.bitabit.ecclesiacontrol.finance.domain.TransactionType;
import br.com.bitabit.ecclesiacontrol.finance.dto.PeriodOverviewItem;
import br.com.bitabit.ecclesiacontrol.finance.dto.PeriodResponse;
import br.com.bitabit.ecclesiacontrol.finance.repository.AccountingPeriodEventRepository;
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
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class AccountingPeriodService {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private final AccountingPeriodRepository periodRepository;
    private final AccountingPeriodEventRepository eventRepository;
    private final FinancialTransactionRepository transactionRepository;
    private final TenantRepository tenantRepository;
    private final AuditService auditService;

    @Value("${app.finance.closing-grace-days:10}")
    private int graceDays;

    @Transactional
    public PeriodResponse close(YearMonth month, AuditActor actor) {
        UUID tenantId = TenantContext.getTenantId();
        LocalDate start = month.atDay(1);

        if (!LocalDate.now(ZONE).isAfter(month.atEndOfMonth())) {
            throw new BusinessRuleException("O mês " + month + " ainda não terminou");
        }

        Optional<AccountingPeriod> existing = periodRepository.findByTenantIdAndReferenceMonth(tenantId, start);
        if (existing.isPresent() && existing.get().getStatus() == PeriodStatus.FECHADO) {
            throw new BusinessRuleException("O período " + month + " já está fechado");
        }

        LocalDate earliest = transactionRepository.earliestTransactionDate(tenantId);
        Optional<AccountingPeriod> latestClosed = periodRepository
                .findFirstByTenantIdAndStatusOrderByReferenceMonthDesc(tenantId, PeriodStatus.FECHADO);

        if (latestClosed.isPresent()) {
            YearMonth expected = YearMonth.from(latestClosed.get().getReferenceMonth()).plusMonths(1);
            if (!month.equals(expected)) {
                throw new BusinessRuleException("Os períodos fecham em ordem: o próximo a fechar é " + expected);
            }
        } else if (earliest != null && !month.equals(YearMonth.from(earliest))) {
            throw new BusinessRuleException(
                    "O primeiro fechamento deve ser o do mês mais antigo com movimento: " + YearMonth.from(earliest));
        }

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        int count = 0;
        for (var t : transactionRepository.periodTotals(tenantId, start, month.atEndOfMonth())) {
            if (t.getType() == TransactionType.RECEITA) income = t.getTotal(); else expense = t.getTotal();
            count += t.getCnt().intValue();
        }

        AccountingPeriod period = existing.orElseGet(() -> {
            AccountingPeriod p = new AccountingPeriod();
            p.setTenantId(tenantId);
            p.setReferenceMonth(start);
            return p;
        });
        period.setStatus(PeriodStatus.FECHADO);
        period.setTotalIncome(income);
        period.setTotalExpense(expense);
        period.setTransactionCount(count);
        period.setClosedAt(LocalDateTime.now(ZONE));
        period.setClosedBy(actor != null ? actor.id() : null);
        AccountingPeriod saved = periodRepository.save(period);

        record(PeriodEventType.CLOSED, saved, null, false, actor);
        auditService.log(actor, tenantId, "CLOSE_PERIOD", "AccountingPeriod", saved.getId(), null,
                Map.of("month", month.toString(), "income", income, "expense", expense));

        return toResponse(month, saved, earliest);
    }

    @Transactional
    public List<PeriodResponse> reopen(YearMonth month, String reason, AuditActor actor) {
        UUID tenantId = TenantContext.getTenantId();
        LocalDate start = month.atDay(1);

        AccountingPeriod target = periodRepository.findByTenantIdAndReferenceMonth(tenantId, start)
                .filter(p -> p.getStatus() == PeriodStatus.FECHADO)
                .orElseThrow(() -> new BusinessRuleException("O período " + month + " não está fechado"));

        LocalDate earliest = transactionRepository.earliestTransactionDate(tenantId);
        List<PeriodResponse> reopened = new ArrayList<>();

        // Cascata: reabrir um mês reabre todos os posteriores, para os números não ficarem inconsistentes
        for (AccountingPeriod p : periodRepository
                .findByTenantIdAndReferenceMonthGreaterThanEqualAndStatusOrderByReferenceMonthAsc(
                        tenantId, start, PeriodStatus.FECHADO)) {
            record(PeriodEventType.REOPENED, p, reason, !p.getId().equals(target.getId()), actor);
            p.setStatus(PeriodStatus.ABERTO);
            p.setTotalIncome(null);
            p.setTotalExpense(null);
            p.setTransactionCount(null);
            p.setClosedAt(null);
            p.setClosedBy(null);
            periodRepository.save(p);
            reopened.add(toResponse(YearMonth.from(p.getReferenceMonth()), p, earliest));
        }

        auditService.log(actor, tenantId, "REOPEN_PERIOD", "AccountingPeriod", target.getId(), null,
                Map.of("fromMonth", month.toString(), "reason", reason,
                        "reopenedMonths", reopened.stream().map(PeriodResponse::getMonth).toList()));
        return reopened;
    }

    @Transactional(readOnly = true)
    public List<PeriodResponse> listYear(int year) {
        UUID tenantId = TenantContext.getTenantId();
        LocalDate earliest = transactionRepository.earliestTransactionDate(tenantId);
        Map<LocalDate, AccountingPeriod> byMonth = periodRepository
                .findByTenantIdAndReferenceMonthBetween(tenantId, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 1))
                .stream().collect(Collectors.toMap(AccountingPeriod::getReferenceMonth, p -> p));

        return IntStream.rangeClosed(1, 12).mapToObj(m -> {
            YearMonth ym = YearMonth.of(year, m);
            return toResponse(ym, byMonth.get(ym.atDay(1)), earliest);
        }).toList();
    }

    // Visão da Sede: só um token GLOBAL enxerga todas as filiais; os demais veem apenas a própria
    @Transactional(readOnly = true)
    public List<PeriodOverviewItem> overview(YearMonth month) {
        List<Tenant> tenants = TenantContext.isGlobalScope()
                ? tenantRepository.findAll()   // se o Tenant tiver flag de ativo, filtrem as inativas aqui
                : tenantRepository.findById(TenantContext.getTenantId()).map(List::of).orElse(List.of());

        Map<UUID, AccountingPeriod> periods = periodRepository
                .findByTenantIdInAndReferenceMonth(tenants.stream().map(Tenant::getId).toList(), month.atDay(1))
                .stream().collect(Collectors.toMap(AccountingPeriod::getTenantId, p -> p));

        return tenants.stream()
                .sorted(Comparator.comparing(Tenant::getName))
                .map(t -> {
                    PeriodResponse r = toResponse(month, periods.get(t.getId()),
                            transactionRepository.earliestTransactionDate(t.getId()));
                    return PeriodOverviewItem.builder()
                            .filialId(t.getId()).filialName(t.getName()).status(r.getStatus())
                            .totalIncome(r.getTotalIncome()).totalExpense(r.getTotalExpense())
                            .closedAt(r.getClosedAt()).build();
                }).toList();
    }

    private void record(PeriodEventType type, AccountingPeriod p, String reason, boolean cascaded, AuditActor actor) {
        AccountingPeriodEvent e = new AccountingPeriodEvent();
        e.setTenantId(p.getTenantId());
        e.setReferenceMonth(p.getReferenceMonth());
        e.setEventType(type);
        e.setReason(reason);
        e.setCascaded(cascaded);
        e.setTotalIncome(p.getTotalIncome());
        e.setTotalExpense(p.getTotalExpense());
        e.setTransactionCount(p.getTransactionCount());
        e.setActorId(actor != null ? actor.id() : null);
        eventRepository.save(e);
    }

    private PeriodResponse toResponse(YearMonth month, AccountingPeriod p, LocalDate earliest) {
        boolean closed = p != null && p.getStatus() == PeriodStatus.FECHADO;
        return PeriodResponse.builder()
                .month(month.toString())
                .status(closed ? "FECHADO" : (isLate(month, earliest) ? "ATRASADO" : "ABERTO"))
                .totalIncome(closed ? p.getTotalIncome() : null)
                .totalExpense(closed ? p.getTotalExpense() : null)
                .transactionCount(closed ? p.getTransactionCount() : null)
                .closedAt(closed ? p.getClosedAt() : null)
                .build();
    }

    // Atrasado só faz sentido a partir do primeiro mês com movimento
    private boolean isLate(YearMonth month, LocalDate earliest) {
        return earliest != null
                && !month.isBefore(YearMonth.from(earliest))
                && LocalDate.now(ZONE).isAfter(month.atEndOfMonth().plusDays(graceDays));
    }
}