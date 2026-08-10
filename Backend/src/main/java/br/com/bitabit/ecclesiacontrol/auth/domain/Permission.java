package br.com.bitabit.ecclesiacontrol.auth.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "permissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    private PermissionCategory category;

    public enum PermissionCategory {
        TENANT_MANAGEMENT,
        USER_MANAGEMENT,
        MEMBER_MANAGEMENT,
        FINANCE,
        REPORTS,
        AUDIT,
        COMMUNICATION,
        SETTINGS
    }
}