package br.com.bitabit.ecclesiacontrol.auth.dto;

import lombok.*;
import jakarta.validation.constraints.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {
    @NotBlank @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
    private String newPassword;
}
