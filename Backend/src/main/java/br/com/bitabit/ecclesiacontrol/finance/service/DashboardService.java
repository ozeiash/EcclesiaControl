package br.com.bitabit.ecclesiacontrol.finance.service;

import br.com.bitabit.ecclesiacontrol.core.exception.BusinessRuleException;
import br.com.bitabit.ecclesiacontrol.core.util.TenantContext;
import br.com.bitabit.ecclesiacontrol.finance.domain.PayableStatus;
import br.com.bitabit.ecclesiacontrol.finance.domain.TransactionType;
import br.com.bitabit.ecclesiacontrol.finance.dto.FinanceDashboardResponse;
import br.com.bitabit.ecclesiacontrol.finance.dto.MembersDashboardResponse;
import br.com.bitabit.ecclesiacontrol.finance.repository.AccountsPayableRepository;
import br.com.bitabit.ecclesiacontrol.finance.repository.AccountsPayableSettlementRepository;
import br.com.bitabit.ecclesiacontrol.finance.repository.FinancialTransactionRepository;
import br.com.bitabit.ecclesiacontrol.member.domain.Member;
import br.com.bitabit.ecclesiacontrol.member.repository.MemberRepository;
import br.com.bitabit.ecclesiacontrol.tenant.domain.Tenant;
import br.com.bitabit.ecclesiacontrol.tenant.repository.TenantRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo"); // o servidor pode rodar em UTC
    private static final LocalDate NO_LIMIT = LocalDate.of(9999, 12, 31);
    private static final List<PayableStatus> OPEN =
            List.of(PayableStatus.PENDENTE, PayableStatus.PARCIAL, PayableStatus.VENCIDO);

    private final FinancialTransactionRepository transactionRepository;
    private final AccountsPayableRepository payableRepository;
    private final AccountsPayableSettlementRepository settlementRepository;
    private final MemberRepository memberRepository;
    private final TenantRepository tenantRepository;

    @Transactional(readOnly = true) // no método: o aspect do filtro de tenant exige isso
    public FinanceDashboardResponse finance(LocalDate start, LocalDate end) {
        LocalDate from = start != null ? start : YearMonth.now(ZONE).atDay(1);
        LocalDate to = end != null ? end : YearMonth.now(ZONE).atEndOfMonth();
        validatePeriod(from, to);

        boolean consolidated = TenantContext.isGlobalScope();

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        for (var t : transactionRepository.totalsByType(from, to, consolidated)) {
            if (t.getType() == TransactionType.RECEITA) income = t.getTotal();
            else expense = t.getTotal();
        }

        var byAccount = transactionRepository.totalsByAccount(from, to, consolidated).stream()
                .map(a -> FinanceDashboardResponse.AccountTotal.builder()
                        .code(a.getCode()).name(a.getName()).type(a.getType().name()).total(a.getTotal()).build())
                .toList();

        List<FinanceDashboardResponse.FilialTotal> byFilial = List.of();
        if (consolidated) {
            Map<UUID, Map<TransactionType, BigDecimal>> acc = new LinkedHashMap<>();
            for (var r : transactionRepository.totalsByTenant(from, to, true)) {
                acc.computeIfAbsent(r.getTenantId(), k -> new EnumMap<>(TransactionType.class))
                        .put(r.getType(), r.getTotal());
            }
            // Duas consultas simples em vez de join de entidades: o filtro de tenant fica garantido
            Map<UUID, String> names = tenantRepository.findAllById(acc.keySet()).stream()
                    .collect(Collectors.toMap(Tenant::getId, Tenant::getName));
            byFilial = acc.entrySet().stream().map(e -> {
                BigDecimal i = e.getValue().getOrDefault(TransactionType.RECEITA, BigDecimal.ZERO);
                BigDecimal x = e.getValue().getOrDefault(TransactionType.DESPESA, BigDecimal.ZERO);
                return FinanceDashboardResponse.FilialTotal.builder()
                        .filialId(e.getKey()).filialName(names.getOrDefault(e.getKey(), "—"))
                        .income(i).expense(x).balance(i.subtract(x)).build();
            }).sorted(Comparator.comparing(FinanceDashboardResponse.FilialTotal::getFilialName)).toList();
        }

        return FinanceDashboardResponse.builder()
                .periodStart(from).periodEnd(to).consolidated(consolidated)
                .totalIncome(income).totalExpense(expense).balance(income.subtract(expense))
                .openPayables(remaining(NO_LIMIT))
                // VENCIDO não é atualizado por nenhum processo hoje; o vencimento é calculado pela data
                .overduePayables(remaining(LocalDate.now(ZONE).minusDays(1)))
                .byAccount(byAccount).byFilial(byFilial)
                .build();
    }

    @Transactional(readOnly = true)
    public MembersDashboardResponse members(LocalDate start, LocalDate end) {
        LocalDate from = start != null ? start : YearMonth.now(ZONE).atDay(1);
        LocalDate to = end != null ? end : YearMonth.now(ZONE).atEndOfMonth();
        validatePeriod(from, to);

        boolean consolidated = TenantContext.isGlobalScope();
        var dup = Member.MembershipStatus.DUPLICATE;

        Map<String, Long> byStatus = new LinkedHashMap<>();
        long total = 0;
        for (var s : memberRepository.countByStatus(dup)) {
            byStatus.put(s.getStatus().name(), s.getTotal());
            total += s.getTotal();
        }

        List<MembersDashboardResponse.FilialCount> byFilial = List.of();
        if (consolidated) {
            var counts = memberRepository.countByTenant(Member.MembershipStatus.ATIVO);
            Map<UUID, String> names = tenantRepository
                    .findAllById(counts.stream().map(MemberRepository.TenantCountView::getTenantId).toList())
                    .stream().collect(Collectors.toMap(Tenant::getId, Tenant::getName));
            byFilial = counts.stream().map(c -> MembersDashboardResponse.FilialCount.builder()
                            .filialId(c.getTenantId()).filialName(names.getOrDefault(c.getTenantId(), "—"))
                            .activeMembers(c.getTotal()).build())
                    .sorted(Comparator.comparing(MembersDashboardResponse.FilialCount::getFilialName)).toList();
        }

        return MembersDashboardResponse.builder()
                .periodStart(from).periodEnd(to).consolidated(consolidated)
                .totalMembers(total)
                .admissionsInPeriod(memberRepository.countAdmissions(from, to, dup))
                .byStatus(byStatus).activeByFilial(byFilial)
                .build();
    }

    private BigDecimal remaining(LocalDate limit) {
        BigDecimal due = payableRepository.sumAmount(OPEN, limit);
        BigDecimal settled = settlementRepository.sumSettled(OPEN, limit);
        return (due != null ? due : BigDecimal.ZERO).subtract(settled != null ? settled : BigDecimal.ZERO);
    }

    private void validatePeriod(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessRuleException("A data inicial não pode ser posterior à data final");
        }
    }
}