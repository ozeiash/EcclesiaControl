package br.com.bitabit.ecclesiacontrol.finance.repository;

import br.com.bitabit.ecclesiacontrol.core.repository.TenantScopedRepository;
import br.com.bitabit.ecclesiacontrol.finance.domain.FinancialTransaction;
import br.com.bitabit.ecclesiacontrol.finance.domain.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;



@Repository
public interface FinancialTransactionRepository extends TenantScopedRepository<FinancialTransaction, UUID> {

    interface TypeTotalView {
        TransactionType getType();
        BigDecimal getTotal();
    }

    interface AccountTotalView {
        String getCode();
        String getName();
        TransactionType getType();
        BigDecimal getTotal();
    }

    interface TenantTypeTotalView {
        UUID getTenantId();
        TransactionType getType();
        BigDecimal getTotal();
    }


    Page<FinancialTransaction> findByTransactionDateBetween(LocalDate start, LocalDate end, Pageable pageable);

    @Query("""
    select ft.type as type, sum(ft.amount) as total
      from FinancialTransaction ft
     where ft.transactionDate between :start and :end
       and (:consolidated = false or ft.account.category <> 'REPASSE')
     group by ft.type
    """)
    List<TypeTotalView> totalsByType(@Param("start") LocalDate start, @Param("end") LocalDate end,
                                     @Param("consolidated") boolean consolidated);

    @Query("""
    select ft.account.code as code, ft.account.name as name, ft.type as type, sum(ft.amount) as total
      from FinancialTransaction ft
     where ft.transactionDate between :start and :end
       and (:consolidated = false or ft.account.category <> 'REPASSE')
     group by ft.account.code, ft.account.name, ft.type
     order by ft.account.code
    """)
    List<AccountTotalView> totalsByAccount(@Param("start") LocalDate start, @Param("end") LocalDate end,
                                           @Param("consolidated") boolean consolidated);

    @Query("""
    select ft.tenantId as tenantId, ft.type as type, sum(ft.amount) as total
      from FinancialTransaction ft
     where ft.transactionDate between :start and :end
       and (:consolidated = false or ft.account.category <> 'REPASSE')
     group by ft.tenantId, ft.type
    """)
    List<TenantTypeTotalView> totalsByTenant(@Param("start") LocalDate start, @Param("end") LocalDate end,
                                             @Param("consolidated") boolean consolidated);

    interface PeriodTotalView { TransactionType getType(); BigDecimal getTotal(); Long getCnt(); }

    @Query("""
    select ft.type as type, sum(ft.amount) as total, count(ft) as cnt
      from FinancialTransaction ft
     where ft.tenantId = :tenantId and ft.transactionDate between :start and :end
     group by ft.type
    """)
    List<PeriodTotalView> periodTotals(@Param("tenantId") UUID tenantId,
                                       @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("select min(ft.transactionDate) from FinancialTransaction ft where ft.tenantId = :tenantId")
    LocalDate earliestTransactionDate(@Param("tenantId") UUID tenantId);

    @Query("""
    select ft.type as type, sum(ft.amount) as total
      from FinancialTransaction ft
     where ft.tenantId in :tenantIds and ft.transactionDate between :start and :end
       and (:eliminate = false or ft.account.category <> 'REPASSE')
     group by ft.type
    """)
    List<TypeTotalView> officialTotalsByType(@Param("tenantIds") Collection<UUID> tenantIds,
                                             @Param("start") LocalDate start, @Param("end") LocalDate end, @Param("eliminate") boolean eliminate);

    @Query("""
    select ft.account.code as code, ft.account.name as name, ft.type as type, sum(ft.amount) as total
      from FinancialTransaction ft
     where ft.tenantId in :tenantIds and ft.transactionDate between :start and :end
       and (:eliminate = false or ft.account.category <> 'REPASSE')
     group by ft.account.code, ft.account.name, ft.type
     order by ft.account.code
    """)
    List<AccountTotalView> officialTotalsByAccount(@Param("tenantIds") Collection<UUID> tenantIds,
                                                   @Param("start") LocalDate start, @Param("end") LocalDate end, @Param("eliminate") boolean eliminate);

    @Query("""
    select ft.tenantId as tenantId, ft.type as type, sum(ft.amount) as total
      from FinancialTransaction ft
     where ft.tenantId in :tenantIds and ft.transactionDate between :start and :end
       and (:eliminate = false or ft.account.category <> 'REPASSE')
     group by ft.tenantId, ft.type
    """)
    List<TenantTypeTotalView> officialTotalsByTenant(@Param("tenantIds") Collection<UUID> tenantIds,
                                                     @Param("start") LocalDate start, @Param("end") LocalDate end, @Param("eliminate") boolean eliminate);
}