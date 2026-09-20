package br.com.bitabit.ecclesiacontrol.member.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MemberRequest {

    @NotBlank
    @Size(max = 255)
    private String fullName;

    @Size(max = 255)
    private String socialName;

    @Past
    private LocalDate birthDate;

    @Size(max = 20)
    private String gender;

    @Size(min = 11, max = 14, message = "Informe o CPF com 11 dígitos (com ou sem máscara)")
    private String cpf;

    @Size(max = 20)
    private String rg;

    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 20)
    private String phonePrimary;

    @Size(max = 20)
    private String phoneSecondary;

    @Size(max = 10)
    private String postalCode;

    @Size(max = 255)
    private String street;

    @Size(max = 20)
    private String number;

    @Size(max = 100)
    private String complement;

    @Size(max = 100)
    private String neighborhood;

    @Size(max = 100)
    private String city;

    @Size(max = 2, message = "Use a sigla do estado (UF), ex: GO")
    private String state;

    private LocalDate admissionDate;

    @Size(max = 30)
    private String admissionWay;

    private boolean baptized;
    private LocalDate baptismDate;
    private Boolean confirmedOrProfessed;

    @Size(max = 20)
    private String maritalStatus;

    private LocalDate weddingDate;

    @Size(max = 255)
    private String spouseName;

    @Size(max = 255)
    private String fatherName;

    @Size(max = 255)
    private String motherName;

    @Size(max = 100)
    private String currentRole;

    @Size(max = 500)
    private String photoUrl;

    private String observations; // TEXT no banco — sem limite prático
}