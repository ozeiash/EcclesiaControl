package br.com.bitabit.ecclesiacontrol.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkMemberRequest {
    @NotNull
    private UUID memberId;
}
