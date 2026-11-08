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

    @Size(min = 6, max = 72, message = "A senha deve ter entre 6 e 72 caracteres")
    private String initialPassword;
}