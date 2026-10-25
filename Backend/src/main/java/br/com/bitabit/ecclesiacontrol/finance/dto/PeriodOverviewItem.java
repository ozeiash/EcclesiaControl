package br.com.bitabit.ecclesiacontrol.finance.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PeriodOverviewItem {
    private UUID filialId;
    private String filialName;
    private String status;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private LocalDateTime closedAt;
    private String accountingStart;
}