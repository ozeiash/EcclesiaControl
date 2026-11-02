package br.com.bitabit.ecclesiacontrol.auth.dto;

import lombok.*;
import jakarta.validation.constraints.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    @NotBlank @Size(max = 255)
    private String fullName;
    @NotBlank @Email @Size(max = 255)
    private String email;
    @Size(max = 20)
    private String phone;
}
