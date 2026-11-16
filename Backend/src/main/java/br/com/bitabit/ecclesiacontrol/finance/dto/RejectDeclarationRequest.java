package br.com.bitabit.ecclesiacontrol.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RejectDeclarationRequest {

    @NotBlank @Size(max = 255)
    private String reason;
}
