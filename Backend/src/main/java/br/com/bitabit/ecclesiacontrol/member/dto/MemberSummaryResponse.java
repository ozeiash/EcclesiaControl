package br.com.bitabit.ecclesiacontrol.member.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class MemberSummaryResponse {
    private UUID id;
    private String fullName;
    private String membershipStatus;
    private String currentRole;
    private String phonePrimary;
}