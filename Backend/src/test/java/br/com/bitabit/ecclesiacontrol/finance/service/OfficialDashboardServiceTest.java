package br.com.bitabit.ecclesiacontrol.finance.service;

import br.com.bitabit.ecclesiacontrol.core.testsupport.AbstractRepositoryTest;
import br.com.bitabit.ecclesiacontrol.core.util.TenantContext;
import br.com.bitabit.ecclesiacontrol.finance.domain.*;
import br.com.bitabit.ecclesiacontrol.finance.dto.OfficialFinanceDashboardResponse;
import br.com.bitabit.ecclesiacontrol.finance.repository.AccountingPeriodRepository;
import br.com.bitabit.ecclesiacontrol.finance.repository.ChartOfAccountRepository;
import br.com.bitabit.ecclesiacontrol.finance.repository.FinancialTransactionRepository;
import br.com.bitabit.ecclesiacontrol.tenant.domain.Tenant;
import br.com.bitabit.ecclesiacontrol.tenant.repository.TenantRepository;
import br.com.bitabit.ecclesiacontrol.core.exception.BusinessRuleException;
import br.com.bitabit.ecclesiacontrol.finance.service.OfficialDashboardService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.context.annotation.Import;

import java.math.BigDecimal;

import java.time.YearMonth;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(OfficialDashboardService.class)
class OfficialDashboardServiceTest extends AbstractRepositoryTest {

    @Autowired private OfficialDashboardService service;
    @Autowired private AccountingPeriodRepository periodRepository;
    @Autowired private FinancialTransactionRepository transactionRepository;
    @Autowired
    private ChartOfAccountRepository accountRepository;
    @Autowired private TenantRepository tenantRepository;

    private YearMonth m1, m2;
    private Tenant filialA, filialB;

    @BeforeEach
    void setUp() {
        TenantContext.setGlobalScope(true);
        m1 = YearMonth.now().minusMonths(6);
        m2 = m1.plusMonths(1);
        filialA = tenant("Filial A", "a@church.com");
        filialB = tenant("Filial B", "b@church.com");

        ChartOfAccount tithe = accountRepository.findAll().stream()
                .filter(a -> a.getCode().equals("REC-001")).findFirst().orElseThrow();
        income(filialA, tithe, m1, "100.00");
        income(filialA, tithe, m2, "50.00");
        income(filialB, tithe, m1, "300.00");
        income(filialB, tithe, m2, "70.00");
    }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    @Test
    void includesOnlyFiliaisWithEveryMonthClosedAndListsThePendingOnes() {
        closed(filialA, m1, 1);
        closed(filialA, m2, 1);
        closed(filialB, m1, 1);                       // o m2 de B continua aberto

        var r = service.officialFinance(m1, m2);

        assertThat(r.isComplete()).isFalse();
        assertThat(r.getNotice()).isNotBlank();
        assertThat(r.getTotalIncome()).isEqualByComparingTo("150.00");   // só a Filial A
        assertThat(r.getByFilial()).extracting(OfficialFinanceDashboardResponse.FilialResult::getFilialName)
                .containsExactly("Filial A");
        assertThat(r.getPending()).hasSize(1);
        assertThat(r.getPending().get(0).getFilialName()).isEqualTo("Filial B");
        assertThat(r.getPending().get(0).getMissingMonths()).containsExactly(m2.toString());
    }

    @Test
    void closedFilialWithoutMovementIsIncludedWithZerosAndNotPending() {
        Tenant empty = tenant("Filial Vazia", "v@church.com");
        empty.setAccountingStartMonth(m1.atDay(1));
        tenantRepository.save(empty);
        closed(empty, m1, 0);
        closed(empty, m2, 0);
        closed(filialA, m1, 1);
        closed(filialA, m2, 1);                       // a Filial B não fechou nada

        var r = service.officialFinance(m1, m2);

        assertThat(r.getByFilial()).anySatisfy(f -> {
            assertThat(f.getFilialName()).isEqualTo("Filial Vazia");
            assertThat(f.getIncome()).isEqualByComparingTo("0");
            assertThat(f.getTransactionCount()).isZero();
        });
        assertThat(r.getPending()).extracting(OfficialFinanceDashboardResponse.PendingFilial::getFilialName)
                .containsExactly("Filial B");
    }

    @Test
    void rejectsInvalidRanges() {
        assertThatThrownBy(() -> service.officialFinance(m2, m1)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.officialFinance(m1, YearMonth.now())).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.officialFinance(m1.minusMonths(30), m1)).isInstanceOf(BusinessRuleException.class);
    }

    private Tenant tenant(String name, String email) {
        return tenantRepository.save(Tenant.builder().name(name)
                .type(Tenant.TenantType.FILIAL).email(email).build());
    }

    private void income(Tenant t, ChartOfAccount account, YearMonth month, String amount) {
        FinancialTransaction ft = FinancialTransaction.builder().account(account)
                .type(TransactionType.RECEITA).amount(new BigDecimal(amount))
                .transactionDate(month.atDay(10)).build();
        ft.setTenantId(t.getId());
        transactionRepository.save(ft);
    }

    private void closed(Tenant t, YearMonth month, int count) {
        AccountingPeriod p = new AccountingPeriod();
        p.setTenantId(t.getId());
        p.setReferenceMonth(month.atDay(1));
        p.setStatus(PeriodStatus.FECHADO);
        p.setTotalIncome(BigDecimal.ZERO);
        p.setTotalExpense(BigDecimal.ZERO);
        p.setTransactionCount(count);
        periodRepository.save(p);
    }
}