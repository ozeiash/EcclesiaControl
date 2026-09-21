package br.com.bitabit.ecclesiacontrol.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MemberRoleHistoryRequest {
    @NotBlank
    @Size(max = 100)
    private String role;

    @NotNull
    private LocalDate startedAt;

    private LocalDate endedAt;
}