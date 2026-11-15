package br.com.bitabit.ecclesiacontrol.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.YearMonth;

@Data
@EqualsAndHashCode(callSuper = true)
public class CreateFilialRequest extends UpdateFilialRequest {

    @Schema(description = "Mês de início da escrituração (AAAA-MM). Opcional; pode ser definido depois pelo endpoint de períodos.",
            example = "2026-11", type = "string")
    private YearMonth accountingStartMonth;
}
