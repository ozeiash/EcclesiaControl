package br.com.bitabit.ecclesiacontrol.finance.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateAccountsPayableRequest {
    @NotNull
    private UUID accountId;

    @NotBlank
    private String payeeName;

    @NotNull @DecimalMin(value = "0.01")
    private BigDecimal amount;

    @NotNull
    private LocalDate dueDate;

    @NotNull
    private br.com.bitabit.ecclesiacontrol.finance.domain.ExpenseKind expenseKind;
}