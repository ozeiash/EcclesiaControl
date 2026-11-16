package br.com.bitabit.ecclesiacontrol.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank
    private String currentPassword;

    @NotBlank @Size(min = 8, max = 72, message = "A nova senha deve ter entre 8 e 72 caracteres")
    private String newPassword;
}
