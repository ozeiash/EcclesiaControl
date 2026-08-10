package br.com.bitabit.ecclesiacontrol.tenant.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenant")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "legal_name")
    private String legalName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantType type;

    @Column(name = "parent_tenant_id")
    private UUID parentTenantId;

    @Column(name = "cnpj")
    private String cnpj;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "address")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum TenantType {
        SEDE, FILIAL, CONGREGACAO, DEPARTAMENTO
    }
}