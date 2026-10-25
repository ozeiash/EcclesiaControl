package br.com.bitabit.ecclesiacontrol.finance.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StartMonthResponse {
    private boolean configured;
    private String accountingStartMonth;   // "2026-08"; ausente se não configurado
}

