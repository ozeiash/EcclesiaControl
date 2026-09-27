package br.com.bitabit.ecclesiacontrol.finance.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SettlePayableRequest {
    @NotNull @DecimalMin(value = "0.01")
    private BigDecimal amount;

    @NotNull
    private LocalDate transactionDate;

    @Size(max = 500)
    private String attachmentUrl;

    @Size(max = 255)
    private String description;
}