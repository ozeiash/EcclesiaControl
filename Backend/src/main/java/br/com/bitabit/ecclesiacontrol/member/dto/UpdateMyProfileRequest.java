package br.com.bitabit.ecclesiacontrol.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Só contato e endereço. Identificação, situação e vida eclesiástica só a secretaria altera. */
@Data
public class UpdateMyProfileRequest {

    @Email @Size(max = 255)
    @Schema(description = "E-mail de contato. Não altera o e-mail de login.")
    private String email;

    @Size(max = 20) private String phonePrimary;
    @Size(max = 20) private String phoneSecondary;
    @Size(max = 10) private String postalCode;
    @Size(max = 255) private String street;
    @Size(max = 20) private String number;
    @Size(max = 100) private String complement;
    @Size(max = 100) private String neighborhood;
    @Size(max = 100) private String city;

    @Pattern(regexp = "^[A-Za-z]{2}$", message = "Use a sigla do estado (UF), ex: GO")
    private String state;
}
