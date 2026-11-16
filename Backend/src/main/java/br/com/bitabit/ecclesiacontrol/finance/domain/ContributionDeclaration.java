package br.com.bitabit.ecclesiacontrol.finance.domain;

import br.com.bitabit.ecclesiacontrol.core.domain.TenantAwareEntity;
import br.com.bitabit.ecclesiacontrol.member.domain.Member;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** O membro diz "contribuí". Só vira lançamento quando o tesoureiro confirma. */
@Entity
@Table(name = "contribution_declarations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ContributionDeclaration extends TenantAwareEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private ChartOfAccount account;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "declared_date", nullable = false)
    private LocalDate declaredDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Column(name = "pix_reference", length = 64)
    private String pixReference;

    @Column(length = 255)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DeclarationStatus status = DeclarationStatus.PENDENTE;

    @Column(name = "decided_by")
    private UUID decidedBy;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @Column(name = "contribution_id")
    private UUID contributionId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
