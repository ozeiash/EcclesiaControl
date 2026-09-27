package br.com.bitabit.ecclesiacontrol.finance.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class AccountsPayableResponse {
    private UUID id;
    private String accountName;
    private String payeeName;
    private BigDecimal amount;
    private BigDecimal settledAmount;
    private BigDecimal remainingAmount;
    private LocalDate dueDate;
    private String expenseKind;
    private String status;
}