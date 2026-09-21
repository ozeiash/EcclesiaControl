package br.com.bitabit.ecclesiacontrol.member.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class MemberRoleHistoryResponse {
    private UUID id;
    private String role;
    private LocalDate startedAt;
    private LocalDate endedAt;
}