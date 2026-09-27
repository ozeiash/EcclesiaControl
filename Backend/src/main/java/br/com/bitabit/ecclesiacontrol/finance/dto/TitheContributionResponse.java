package br.com.bitabit.ecclesiacontrol.finance.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class TitheContributionResponse {
    private UUID id;
    private BigDecimal amount;
    private LocalDate transactionDate;
    private String accountName;
    private boolean anonymous;
    // Preenchidos só se o ator tiver PERM_TITHE_DETAIL_READ — nulos caso contrário
    private UUID memberId;
    private String memberName;
}