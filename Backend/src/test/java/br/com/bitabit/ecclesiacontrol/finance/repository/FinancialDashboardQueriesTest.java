package br.com.bitabit.ecclesiacontrol.finance.repository;

import br.com.bitabit.ecclesiacontrol.core.testsupport.AbstractRepositoryTest;
import br.com.bitabit.ecclesiacontrol.finance.domain.ChartOfAccount;
import br.com.bitabit.ecclesiacontrol.finance.domain.FinancialTransaction;
import br.com.bitabit.ecclesiacontrol.finance.domain.TransactionType;
import br.com.bitabit.ecclesiacontrol.tenant.domain.Tenant;
import br.com.bitabit.ecclesiacontrol.tenant.repository.TenantRepository;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


class FinancialDashboardQueriesTest extends AbstractRepositoryTest {

    private static final LocalDate START = LocalDate.of(2026, 10, 1);
    private static final LocalDate END = LocalDate.of(2026, 10, 31);

    @Autowired
    private TestEntityManager entityManager;
    @Autowired private FinancialTransactionRepository transactionRepository;
    @Autowired private ChartOfAccountRepository accountRepository;
    @Autowired private TenantRepository tenantRepository;

    private Tenant filialA;
    private Tenant filialB;

    @BeforeEach
    void setUp() {
        filialA = tenantRepository.save(Tenant.builder().name("Filial A")
                .type(Tenant.TenantType.FILIAL).email("a@church.com").build());
        filialB = tenantRepository.save(Tenant.builder().name("Filial B")
                .type(Tenant.TenantType.FILIAL).email("b@church.com").build());

        ChartOfAccount tithe = account("REC-001");
        ChartOfAccount admin = account("DESP-001");
        ChartOfAccount repasse = account("DESP-003");

        save(filialA, tithe, TransactionType.RECEITA, "100.00");
        save(filialA, admin, TransactionType.DESPESA, "40.00");
        save(filialA, repasse, TransactionType.DESPESA, "50.00");
        save(filialB, tithe, TransactionType.RECEITA, "300.00");
        entityManager.flush();
        entityManager.clear();
    }

    @AfterEach
    void tearDown() {
        Session session = entityManager.getEntityManager().unwrap(Session.class);
        if (session.getEnabledFilter("tenantFilter") != null) session.disableFilter("tenantFilter");
    }

    @Test
    void consolidatedSumsAllFiliaisAndEliminatesRepasse() {
        var totals = transactionRepository.totalsByType(START, END, true);
        assertThat(total(totals, TransactionType.RECEITA)).isEqualByComparingTo("400.00");
        assertThat(total(totals, TransactionType.DESPESA)).isEqualByComparingTo("40.00");
    }

    @Test
    void filialViewOnlySumsItsOwnDataAndKeepsRepasseAsRealExpense() {
        Session session = entityManager.getEntityManager().unwrap(Session.class);
        session.enableFilter("tenantFilter").setParameter("tenantId", filialA.getId());

        var totals = transactionRepository.totalsByType(START, END, false);
        assertThat(total(totals, TransactionType.RECEITA)).isEqualByComparingTo("100.00");
        assertThat(total(totals, TransactionType.DESPESA)).isEqualByComparingTo("90.00");

        assertThat(transactionRepository.totalsByTenant(START, END, false))
                .allMatch(r -> r.getTenantId().equals(filialA.getId()));
    }

    private BigDecimal total(List<FinancialTransactionRepository.TypeTotalView> list, TransactionType type) {
        return list.stream().filter(t -> t.getType() == type)
                .map(FinancialTransactionRepository.TypeTotalView::getTotal)
                .findFirst().orElse(BigDecimal.ZERO);
    }

    private ChartOfAccount account(String code) {
        return accountRepository.findAll().stream()
                .filter(a -> a.getCode().equals(code)).findFirst().orElseThrow();
    }

    private void save(Tenant tenant, ChartOfAccount account, TransactionType type, String amount) {
        FinancialTransaction t = FinancialTransaction.builder()
                .account(account).type(type).amount(new BigDecimal(amount))
                .transactionDate(START.plusDays(1)).build();
        t.setTenantId(tenant.getId());
        transactionRepository.save(t);
    }
}