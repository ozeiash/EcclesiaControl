package br.com.bitabit.ecclesiacontrol.finance.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class OfficialFinanceDashboardResponse {
    private String fromMonth;
    private String toMonth;
    private boolean consolidated;
    private boolean complete;
    private String notice;                 // só quando há filiais pendentes
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal balance;
    private List<FinanceDashboardResponse.AccountTotal> byAccount;
    private List<FilialResult> byFilial;   // só no consolidado
    private List<PendingFilial> pending;

    @Data @Builder
    public static class FilialResult {
        private UUID filialId; private String filialName;
        private BigDecimal income; private BigDecimal expense; private BigDecimal balance;
        private long transactionCount;     // 0 = fechada sem movimento
    }

    @Data @Builder
    public static class PendingFilial {
        private UUID filialId; private String filialName;
        private List<String> missingMonths;
    }
}