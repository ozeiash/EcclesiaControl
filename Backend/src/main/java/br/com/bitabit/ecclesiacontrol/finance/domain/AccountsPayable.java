package br.com.bitabit.ecclesiacontrol.finance.domain;

import br.com.bitabit.ecclesiacontrol.core.domain.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "accounts_payable")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AccountsPayable extends TenantAwareEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private ChartOfAccount account;

    @Column(name = "payee_name", nullable = false)
    private String payeeName;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "expense_kind", nullable = false, length = 20)
    @Builder.Default
    private ExpenseKind expenseKind = ExpenseKind.FIXA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PayableStatus status = PayableStatus.PENDENTE;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}