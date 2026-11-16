package br.com.bitabit.ecclesiacontrol.member.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.UUID;

/**
 * O que o próprio membro enxerga. Fica de fora de propósito: RG, observações internas da liderança,
 * dados de saída, foto, vínculo de duplicidade e CPF completo (vai só mascarado).
 */
@Value
@Builder
public class MyProfileResponse {
    UUID id;
    UUID filialId;
    String filialName;
    String fullName;
    String socialName;
    LocalDate birthDate;
    String gender;
    String maskedCpf;
    String email;
    String phonePrimary;
    String phoneSecondary;
    String postalCode;
    String street;
    String number;
    String complement;
    String neighborhood;
    String city;
    String state;
    String membershipStatus;
    LocalDate admissionDate;
    boolean baptized;
    LocalDate baptismDate;
    String maritalStatus;
    LocalDate weddingDate;
    String spouseName;
    String fatherName;
    String motherName;
    String currentRole;
}
