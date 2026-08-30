package br.com.bitabit.ecclesiacontrol.member.domain;

import br.com.bitabit.ecclesiacontrol.core.domain.TenantAwareEntity;
import br.com.bitabit.ecclesiacontrol.core.security.EncryptedStringConverter;
import jakarta.persistence.*;

import java.util.List;

/*
// Tithes (dízimos) também sensíveis
    @OneToMany(mappedBy = "member")
    private List<Tithe> tithes;

@Entity
@Table(name = "tithes") public class Tithe extends TenantAwareEntity {

    @Column(name = "amount")
    @Convert(converter = EncryptedStringConverter.class)
    private String amountEncrypted;  // ← Ou criptografar valor
}
*/