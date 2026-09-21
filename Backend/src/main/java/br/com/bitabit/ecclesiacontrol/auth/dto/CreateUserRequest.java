package br.com.bitabit.ecclesiacontrol.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateUserRequest {
    @NotBlank @Email
    private String email;

    @NotBlank
    private String fullName;

    @NotBlank @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
    private String initialPassword;
}