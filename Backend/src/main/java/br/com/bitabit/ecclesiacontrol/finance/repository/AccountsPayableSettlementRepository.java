package br.com.bitabit.ecclesiacontrol.finance.repository;

import br.com.bitabit.ecclesiacontrol.finance.domain.AccountsPayable;
import br.com.bitabit.ecclesiacontrol.finance.domain.AccountsPayableSettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface AccountsPayableSettlementRepository extends JpaRepository<AccountsPayableSettlement, UUID> {

    List<AccountsPayableSettlement> findByAccountsPayable(AccountsPayable accountsPayable);

    @Query("SELECT COALESCE(SUM(s.amount), 0) FROM AccountsPayableSettlement s WHERE s.accountsPayable.id = :payableId")
    BigDecimal sumSettledAmount(@Param("payableId") UUID payableId);
}