package br.com.bitabit.ecclesiacontrol.member.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CertificateRequest {
    @NotNull
    private CertificateType type;

    private String customMessage; // usado apenas quando type = OUTRO

    public enum CertificateType {
        BATISMO, MEMBRO, CASAMENTO, OUTRO
    }
}