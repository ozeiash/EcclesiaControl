package br.com.bitabit.ecclesiacontrol.finance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class IdentifyMemberRequest {
    @NotNull
    private UUID memberId;
}