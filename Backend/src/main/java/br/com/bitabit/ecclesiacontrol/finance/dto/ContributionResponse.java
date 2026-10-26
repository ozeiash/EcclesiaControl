package br.com.bitabit.ecclesiacontrol.finance.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class ContributionResponse {
    private UUID id;
    private BigDecimal amount;
    private LocalDate transactionDate;
    private String accountName;
    private boolean anonymous;
    // Preenchidos com PERM_TITHE_DETAIL_READ ou no acesso do próprio membro pelo portal.
    private UUID memberId;
    private String memberName;
}