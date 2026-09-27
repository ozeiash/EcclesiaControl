package br.com.bitabit.ecclesiacontrol.finance.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ChartOfAccountResponse {
    private UUID id;
    private String code;
    private String name;
    private String type;
    private String category;
}