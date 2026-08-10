package br.com.bitabit.ecclesiacontrol.auth.domain;

import br.com.bitabit.ecclesiacontrol.tenant.domain.Tenant;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "roles", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name", "tenant_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleName name;

    @Column(nullable = false)
    private String description;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();

    public enum RoleName {
        SUPER_ADMIN,
        PASTOR_SEDE,
        PASTOR_FILIAL,
        TESOUREIRO,
        SECRETARIO,
        LIDER_MINISTERIO,
        LIDER_CELULA,
        MEMBRO,
        VOLUNTARIO
    }
}