package br.com.bitabit.ecclesiacontrol.finance.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class FinanceDashboardResponse {
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private boolean consolidated;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal balance;
    private BigDecimal openPayables;
    private BigDecimal overduePayables;
    private List<AccountTotal> byAccount;
    private List<FilialTotal> byFilial; // vazio quando não consolidado

    @Data @Builder
    public static class AccountTotal {
        private String code; private String name; private String type; private BigDecimal total;
    }

    @Data @Builder
    public static class FilialTotal {
        private UUID filialId; private String filialName;
        private BigDecimal income; private BigDecimal expense; private BigDecimal balance;
    }
}