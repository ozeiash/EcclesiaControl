package br.com.bitabit.ecclesiacontrol.finance.repository;

import br.com.bitabit.ecclesiacontrol.core.repository.TenantScopedRepository;
import br.com.bitabit.ecclesiacontrol.finance.domain.AccountingPeriodEvent;

import java.util.UUID;

public interface AccountingPeriodEventRepository extends TenantScopedRepository<AccountingPeriodEvent, UUID> { }