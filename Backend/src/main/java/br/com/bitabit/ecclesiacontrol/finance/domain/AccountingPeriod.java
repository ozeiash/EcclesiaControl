package br.com.bitabit.ecclesiacontrol.finance.domain;


import br.com.bitabit.ecclesiacontrol.core.domain.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Table(name = "accounting_periods")
@Getter
@Setter
@NoArgsConstructor
public class AccountingPeriod extends TenantAwareEntity {
    @Id
    @UuidGenerator
    private UUID id;
    @Column(name = "reference_month", nullable = false) private LocalDate referenceMonth;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private PeriodStatus status;
    @Column(name = "total_income", precision = 14, scale = 2) private BigDecimal totalIncome;
    @Column(name = "total_expense", precision = 14, scale = 2) private BigDecimal totalExpense;
    @Column(name = "transaction_count") private Integer transactionCount;
    @Column(name = "closed_at") private LocalDateTime closedAt;
    @Column(name = "closed_by") private UUID closedBy;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;

}
