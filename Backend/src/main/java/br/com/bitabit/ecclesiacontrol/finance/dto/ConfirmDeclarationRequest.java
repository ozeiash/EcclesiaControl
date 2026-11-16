package br.com.bitabit.ecclesiacontrol.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ConfirmDeclarationRequest {

    @Schema(description = "Data do lançamento. Sem ela, vale a data declarada pelo membro. "
            + "Se o mês dessa data já estiver fechado, informe uma data em mês aberto.")
    private LocalDate transactionDate;
}
