package br.com.bitabit.ecclesiacontrol.member.dto;

import lombok.Builder;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
public class MyMemberResponse {
    private UUID id;
    private String fullName;
    private String socialName;
    private LocalDate birthDate;
    private String gender;
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
    private boolean baptized;
    private LocalDate baptismDate;
    private String maritalStatus;
    private LocalDate weddingDate;
    private String currentRole;
    private String photoUrl;
    private String filialName;
}
