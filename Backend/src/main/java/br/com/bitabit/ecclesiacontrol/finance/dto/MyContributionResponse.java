package br.com.bitabit.ecclesiacontrol.finance.dto;

import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Extrato do próprio membro: só o que ele precisa ver. */
@Value
public class MyContributionResponse {
    UUID id;
    BigDecimal amount;
    LocalDate transactionDate;
    String accountName;
}
