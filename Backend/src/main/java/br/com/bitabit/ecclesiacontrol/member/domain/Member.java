package br.com.bitabit.ecclesiacontrol.member.domain;

import br.com.bitabit.ecclesiacontrol.core.domain.TenantAwareEntity;
import br.com.bitabit.ecclesiacontrol.core.security.CryptUtils;
import br.com.bitabit.ecclesiacontrol.core.security.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member extends TenantAwareEntity {

    @Id
    @UuidGenerator
    private UUID id;

    // --- Informações pessoais básicas ---
    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "social_name")
    private String socialName;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "cpf_encrypted")
    @Convert(converter = EncryptedStringConverter.class)
    private String cpf;

    @Column(name = "cpf_hash", length = 64)
    private String cpfHash;

    @Column(name = "rg_encrypted")
    @Convert(converter = EncryptedStringConverter.class)
    private String rg;

    // --- Contato e localização ---
    private String email;

    @Column(name = "phone_primary", length = 20)
    private String phonePrimary;

    @Column(name = "phone_secondary", length = 20)
    private String phoneSecondary;

    @Column(name = "postal_code", length = 10)
    private String postalCode;

    private String street;
    private String number;
    private String complement;
    private String neighborhood;
    private String city;

    @Column(length = 2)
    private String state;

    // --- Vida eclesiástica ---
    @Enumerated(EnumType.STRING)
    @Column(name = "membership_status", nullable = false)
    @Builder.Default
    private MembershipStatus membershipStatus = MembershipStatus.ATIVO;

    @Column(name = "admission_date")
    private LocalDate admissionDate;

    @Column(name = "admission_way", length = 30)
    private String admissionWay;

    @Column(name = "exit_date")
    private LocalDate exitDate;

    @Column(name = "exit_way", length = 30)
    private String exitWay;

    @Column(name = "is_baptized", nullable = false)
    @Builder.Default
    private boolean baptized = false;

    @Column(name = "baptism_date")
    private LocalDate baptismDate;

    @Column(name = "is_confirmed_or_professed")
    private Boolean confirmedOrProfessed;

    // --- Estrutura familiar ---
    @Column(name = "marital_status", length = 20)
    private String maritalStatus;

    @Column(name = "wedding_date")
    private LocalDate weddingDate;

    @Column(name = "spouse_name")
    private String spouseName;

    @Column(name = "father_name")
    private String fatherName;

    @Column(name = "mother_name")
    private String motherName;

    // --- Ministério ---
    @Column(name = "ministry_role")
    private String currentRole;

    // --- Técnicos ---
    @Column(name = "photo_url")
    private String photoUrl;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    private void computeCpfHash() {
        this.cpfHash = (cpf != null) ? CryptUtils.hashForLookup(cpf) : null;
    }

    public enum MembershipStatus {
        ATIVO, INATIVO, AFASTADO, SOB_DISCIPLINA, FALECIDO
    }
}