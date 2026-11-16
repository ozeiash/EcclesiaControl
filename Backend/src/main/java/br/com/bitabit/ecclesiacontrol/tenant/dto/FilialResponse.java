package br.com.bitabit.ecclesiacontrol.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

@Value
@Builder
public class FilialResponse {
    UUID id;
    String name;
    String legalName;
    String type;
    UUID parentTenantId;
    String cnpj;
    String email;
    String phone;
    @Schema(description = "Chave Pix da filial", example = "financeiro@igreja.com")
    String pixKey;
    String address;
    String city;
    String state;
    String postalCode;
    boolean active;
    @Schema(type = "string", example = "2026-11")
    YearMonth accountingStartMonth;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

}
