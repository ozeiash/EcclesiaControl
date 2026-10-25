package br.com.bitabit.ecclesiacontrol.finance.service;

import br.com.bitabit.ecclesiacontrol.core.exception.BusinessRuleException;
import br.com.bitabit.ecclesiacontrol.finance.domain.AccountingPeriod;
import br.com.bitabit.ecclesiacontrol.finance.domain.PeriodStatus;
import br.com.bitabit.ecclesiacontrol.finance.repository.AccountingPeriodRepository;
import br.com.bitabit.ecclesiacontrol.tenant.domain.Tenant;
import br.com.bitabit.ecclesiacontrol.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PeriodGuard {

    private final AccountingPeriodRepository periodRepository;
    private final TenantRepository tenantRepository;

    public void requireOpen(UUID tenantId, LocalDate competenceDate) {
        YearMonth month = YearMonth.from(competenceDate);

        tenantRepository.findById(tenantId)
                .map(Tenant::getAccountingStartMonth)
                .filter(start -> month.isBefore(YearMonth.from(start)))
                .ifPresent(start -> {
                    throw new BusinessRuleException("A escrituração desta filial começa em "
                            + YearMonth.from(start) + "; lançamentos de " + month + " não são aceitos");
                });

        periodRepository.findFirstByTenantIdAndStatusOrderByReferenceMonthDesc(tenantId, PeriodStatus.FECHADO)
                .ifPresent(latest -> {
                    if (!month.isAfter(YearMonth.from(latest.getReferenceMonth()))) {
                        throw new BusinessRuleException("O período " + month
                                + " está fechado. Peça a reabertura a quem tem permissão.");
                    }
                });
    }
}