package br.com.bitabit.ecclesiacontrol.auth.dto;

import lombok.*;
import jakarta.validation.constraints.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {
    @NotBlank
    @Size(min = 6, max = 72, message = "A senha deve ter entre 6 e 72 caracteres")
    private String newPassword;
}
