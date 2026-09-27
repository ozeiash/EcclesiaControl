package br.com.bitabit.ecclesiacontrol.finance.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class FinancialTransactionResponse {
    private UUID id;
    private String accountCode;
    private String accountName;
    private String type;
    private BigDecimal amount;
    private LocalDate transactionDate;
    private String description;
    private String attachmentUrl;
    private LocalDateTime createdAt;
}