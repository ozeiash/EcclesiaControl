package br.com.bitabit.ecclesiacontrol.auth.domain;

import br.com.bitabit.ecclesiacontrol.tenant.domain.Tenant;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_filial_role", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "filial_id", "role_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFilialRole {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "filial_id", nullable = false)
    private Tenant filial;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}