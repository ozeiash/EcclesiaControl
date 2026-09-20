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
        @UniqueConstraint(columnNames = {"name"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @UuidGenerator
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleName name;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleScope scope;  // ← ADICIONAR

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
        TI_ADMIN,
        CONTADOR,
        PASTOR_SEDE,
        PASTOR_FILIAL,
        SECRETARIO_SEDE,
        SECRETARIO_FILIAL,
        TESOUREIRO_SEDE,
        TESOUREIRO_FILIAL,
        LIDER_MINISTERIO,
        LIDER_CELULA,
        VOLUNTARIO,
        MEMBRO
    }

    public enum RoleScope {
        LOCAL,  // Acesso limitado a uma filial
        GLOBAL  // Acesso a todas as filiais
    }
}