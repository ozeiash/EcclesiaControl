package br.com.bitabit.ecclesiacontrol.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TransferLetterRequest {
    @NotBlank
    private String destinationChurchName;

    @NotBlank
    private String destinationCity;

    private String reason;
}