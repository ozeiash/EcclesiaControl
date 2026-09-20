package br.com.bitabit.ecclesiacontrol.member.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class MemberResponse {
    private UUID id;
    private String fullName;
    private String socialName;
    private LocalDate birthDate;
    private String gender;
    private String cpf;
    private String rg;
    private String email;
    private String phonePrimary;
    private String phoneSecondary;
    private String postalCode;
    private String street;
    private String number;
    private String complement;
    private String neighborhood;
    private String city;
    private String state;
    private String membershipStatus;
    private LocalDate admissionDate;
    private String admissionWay;
    private LocalDate exitDate;
    private String exitWay;
    private boolean baptized;
    private LocalDate baptismDate;
    private Boolean confirmedOrProfessed;
    private String maritalStatus;
    private LocalDate weddingDate;
    private String spouseName;
    private String fatherName;
    private String motherName;
    private String currentRole;
    private String photoUrl;
    private String observations;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}