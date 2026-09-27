package br.com.bitabit.ecclesiacontrol.finance.domain;

import br.com.bitabit.ecclesiacontrol.core.domain.TenantAwareEntity;
import br.com.bitabit.ecclesiacontrol.member.domain.Member;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tithe_contributions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TitheContribution extends TenantAwareEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false, unique = true)
    private FinancialTransaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member; // null quando anônima

    @Builder.Default
    @Column(name = "is_anonymous", nullable = false)
    private boolean anonymous = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}