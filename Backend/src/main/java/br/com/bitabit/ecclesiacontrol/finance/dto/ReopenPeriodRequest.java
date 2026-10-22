package br.com.bitabit.ecclesiacontrol.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReopenPeriodRequest {
    @NotBlank
    @Size(min = 10, max = 500)
    private String reason;
}