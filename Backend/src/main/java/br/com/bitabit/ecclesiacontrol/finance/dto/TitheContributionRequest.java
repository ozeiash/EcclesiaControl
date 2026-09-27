package br.com.bitabit.ecclesiacontrol.finance.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class TitheContributionRequest {
    @NotNull
    private UUID accountId; // deve ser categoria DIZIMO ou OFERTA

    @NotNull @DecimalMin(value = "0.01")
    private BigDecimal amount;

    @NotNull
    private LocalDate transactionDate;

    private UUID memberId; // nulo se anonymous = true

    private boolean anonymous;

    @Size(max = 255)
    private String description;
}