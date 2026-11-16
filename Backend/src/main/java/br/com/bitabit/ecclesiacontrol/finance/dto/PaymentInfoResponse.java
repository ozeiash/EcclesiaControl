package br.com.bitabit.ecclesiacontrol.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Value;

@Value
public class PaymentInfoResponse {
    String filialName;

    @Schema(description = "Chave PIX da filial para o membro contribuir fora do app. Nulo se a sede ainda não cadastrou.")
    String pixKey;
}
