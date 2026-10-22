package br.com.bitabit.ecclesiacontrol.finance.dto;


import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PeriodResponse {
    private String month;          // "2026-10"
    private String status;         // ABERTO | FECHADO | ATRASADO
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private Integer transactionCount;
    private LocalDateTime closedAt;
}
