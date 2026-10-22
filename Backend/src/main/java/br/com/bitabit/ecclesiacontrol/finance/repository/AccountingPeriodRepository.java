package br.com.bitabit.ecclesiacontrol.finance.repository;

import br.com.bitabit.ecclesiacontrol.core.repository.TenantScopedRepository;
import br.com.bitabit.ecclesiacontrol.finance.domain.AccountingPeriod;
import br.com.bitabit.ecclesiacontrol.finance.domain.PeriodStatus;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountingPeriodRepository extends TenantScopedRepository<AccountingPeriod, UUID> {
    Optional<AccountingPeriod> findByTenantIdAndReferenceMonth(UUID tenantId, LocalDate referenceMonth);

    Optional<AccountingPeriod> findFirstByTenantIdAndStatusOrderByReferenceMonthDesc(UUID tenantId, PeriodStatus status);

    List<AccountingPeriod> findByTenantIdAndReferenceMonthGreaterThanEqualAndStatusOrderByReferenceMonthAsc(
            UUID tenantId, LocalDate from, PeriodStatus status);

    List<AccountingPeriod> findByTenantIdAndReferenceMonthBetween(UUID tenantId, LocalDate from, LocalDate to);

    List<AccountingPeriod> findByTenantIdInAndReferenceMonth(Collection<UUID> tenantIds, LocalDate referenceMonth);
}
