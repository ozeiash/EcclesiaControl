package br.com.bitabit.ecclesiacontrol.finance.service;

import br.com.bitabit.ecclesiacontrol.core.exception.BusinessRuleException;
import br.com.bitabit.ecclesiacontrol.core.service.AuditService;
import br.com.bitabit.ecclesiacontrol.core.testsupport.AbstractRepositoryTest;
import br.com.bitabit.ecclesiacontrol.core.util.TenantContext;
import br.com.bitabit.ecclesiacontrol.finance.domain.ChartOfAccount;
import br.com.bitabit.ecclesiacontrol.finance.domain.FinancialTransaction;
import br.com.bitabit.ecclesiacontrol.finance.domain.PeriodStatus;
import br.com.bitabit.ecclesiacontrol.finance.domain.TransactionType;
import br.com.bitabit.ecclesiacontrol.finance.dto.PeriodResponse;
import br.com.bitabit.ecclesiacontrol.finance.repository.AccountingPeriodEventRepository;
import br.com.bitabit.ecclesiacontrol.finance.repository.AccountingPeriodRepository;
import br.com.bitabit.ecclesiacontrol.finance.repository.ChartOfAccountRepository;
import br.com.bitabit.ecclesiacontrol.finance.repository.FinancialTransactionRepository;
import br.com.bitabit.ecclesiacontrol.tenant.domain.Tenant;
import br.com.bitabit.ecclesiacontrol.tenant.repository.TenantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import({AccountingPeriodService.class, PeriodGuard.class})
class AccountingPeriodServiceTest extends AbstractRepositoryTest {

    @MockBean
    private AuditService auditService;
    @Autowired
    private AccountingPeriodService service;
    @Autowired private PeriodGuard periodGuard;
    @Autowired private AccountingPeriodRepository periodRepository;
    @Autowired private AccountingPeriodEventRepository eventRepository;
    @Autowired private FinancialTransactionRepository transactionRepository;
    @Autowired private ChartOfAccountRepository accountRepository;
    @Autowired private TenantRepository tenantRepository;

    private Tenant filial;
    private YearMonth m1, m2, m3;

    @BeforeEach
    void setUp() {
        filial = tenantRepository.save(Tenant.builder().name("Filial Fechamento")
                .type(Tenant.TenantType.FILIAL).email("fech@church.com").build());
        TenantContext.setTenantId(filial.getId());

        m1 = YearMonth.now().minusMonths(6);
        m2 = m1.plusMonths(1);
        m3 = m1.plusMonths(2);

        ChartOfAccount tithe = accountRepository.findAll().stream()
                .filter(a -> a.getCode().equals("REC-001")).findFirst().orElseThrow();
        for (YearMonth m : List.of(m1, m2, m3)) {
            FinancialTransaction t = FinancialTransaction.builder().account(tithe)
                    .type(TransactionType.RECEITA).amount(new BigDecimal("100.00"))
                    .transactionDate(m.atDay(10)).build();
            t.setTenantId(filial.getId());
            transactionRepository.save(t);
        }
    }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    @Test
    void firstClosingMustBeTheEarliestMonthWithMovement() {
        assertThatThrownBy(() -> service.close(m2, null)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void cannotSkipAMonth() {
        service.close(m1, null);
        assertThatThrownBy(() -> service.close(m3, null)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void closingSnapshotsTotalsAndLocksTheMonth() {
        PeriodResponse closed = service.close(m1, null);
        assertThat(closed.getStatus()).isEqualTo("FECHADO");
        assertThat(closed.getTotalIncome()).isEqualByComparingTo("100.00");
        assertThat(closed.getTransactionCount()).isEqualTo(1);

        periodGuard.requireOpen(filial.getId(), m2.atDay(5));   // mês seguinte continua aberto
        assertThatThrownBy(() -> periodGuard.requireOpen(filial.getId(), m1.atDay(5)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void reopeningCascadesToLaterPeriodsAndKeepsHistory() {
        service.close(m1, null);
        service.close(m2, null);
        service.close(m3, null);

        assertThat(service.reopen(m2, "Correção de lançamento duplicado", null)).hasSize(2);

        assertThat(periodRepository.findByTenantIdAndReferenceMonth(filial.getId(), m1.atDay(1)).get().getStatus())
                .isEqualTo(PeriodStatus.FECHADO);
        assertThat(periodRepository.findByTenantIdAndReferenceMonth(filial.getId(), m3.atDay(1)).get().getStatus())
                .isEqualTo(PeriodStatus.ABERTO);

        assertThat(eventRepository.findAll()).hasSize(5);        // 3 CLOSED + 2 REOPENED, nada apagado
        periodGuard.requireOpen(filial.getId(), m2.atDay(5));    // reaberto
        assertThatThrownBy(() -> periodGuard.requireOpen(filial.getId(), m1.atDay(5)))
                .isInstanceOf(BusinessRuleException.class);          // continua fechado
    }
}