package br.com.bitabit.ecclesiacontrol.finance.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "chart_of_accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChartOfAccount {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Column(nullable = false, length = 50)
    private String category;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}