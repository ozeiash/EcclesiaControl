package br.com.bitabit.ecclesiacontrol.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Value;

import java.util.UUID;

@Value
public class PortalAccessResponse {
    UUID userId;
    String email;

    @Schema(description = "Senha provisória, mostrada uma única vez. Entregue ao membro e peça para trocá-la no primeiro acesso.")
    String temporaryPassword;
}
