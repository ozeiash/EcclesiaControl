package br.com.bitabit.ecclesiacontrol.finance.domain;

import br.com.bitabit.ecclesiacontrol.core.domain.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "accounts_payable_settlements")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AccountsPayableSettlement extends TenantAwareEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accounts_payable_id", nullable = false)
    private AccountsPayable accountsPayable;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "financial_transaction_id", nullable = false, unique = true)
    private FinancialTransaction financialTransaction;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}