package br.com.bitabit.ecclesiacontrol.finance.service;

import br.com.bitabit.ecclesiacontrol.core.exception.BusinessRuleException;
import br.com.bitabit.ecclesiacontrol.finance.domain.AccountingPeriod;
import br.com.bitabit.ecclesiacontrol.finance.domain.PeriodStatus;
import br.com.bitabit.ecclesiacontrol.finance.repository.AccountingPeriodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PeriodGuard {

    private final AccountingPeriodRepository periodRepository;

    public void requireOpen(UUID tenantId, LocalDate competenceDate) {
        periodRepository.findFirstByTenantIdAndStatusOrderByReferenceMonthDesc(tenantId, PeriodStatus.FECHADO)
                .ifPresent(latest -> {
                    if (!YearMonth.from(competenceDate).isAfter(YearMonth.from(latest.getReferenceMonth()))) {
                        throw new BusinessRuleException("O período " + YearMonth.from(competenceDate)
                                + " está fechado. Peça a reabertura a quem tem permissão.");
                    }
                });
    }
}