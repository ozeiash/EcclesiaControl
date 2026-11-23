package br.com.bitabit.ecclesiacontrol.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class LinkMemberRequest {
    @NotNull
    private UUID memberId;
    private UUID filialId;
}
