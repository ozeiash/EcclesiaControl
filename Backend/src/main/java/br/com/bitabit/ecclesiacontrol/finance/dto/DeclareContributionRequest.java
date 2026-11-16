package br.com.bitabit.ecclesiacontrol.finance.dto;

import br.com.bitabit.ecclesiacontrol.finance.domain.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class DeclareContributionRequest {

    @NotNull
    @Schema(description = "Conta de dízimo, oferta ou missões (veja GET /me/contribution-accounts)")
    private UUID accountId;

    @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2)
    private BigDecimal amount;

    @NotNull
    @Schema(description = "Data em que você fez a contribuição (não pode ser futura)")
    private LocalDate declaredDate;

    @NotNull
    private PaymentMethod paymentMethod;

    @Pattern(regexp = "^[A-Za-z0-9._-]{1,64}$", message = "Use só letras, números, ponto, hífen ou sublinhado (até 64)")
    @Schema(description = "Código da transação PIX (ID fim a fim). Ajuda o tesoureiro a conferir e evita declaração em duplicidade.")
    private String pixReference;

    @Size(max = 255)
    private String note;
}
