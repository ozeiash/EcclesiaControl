package br.com.bitabit.ecclesiacontrol.tenant.dto;

import br.com.bitabit.ecclesiacontrol.core.validation.ValidCnpj;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Dados cadastrais editáveis. Tipo, filial-mãe, situação e início da escrituração não mudam por aqui. */
@Data
public class UpdateFilialRequest {

    @NotBlank @Size(max = 150)
    @Schema(example = "Filial Centro")
    private String name;

    @Size(max = 200)
    @Schema(example = "Igreja Exemplo - Filial Centro")
    private String legalName;

    @ValidCnpj
    @Schema(description = "Com ou sem máscara; aceita o formato alfanumérico", example = "11.222.333/0001-81")
    private String cnpj;

    @Email @Size(max = 255)
    private String email;

    @Size(max = 20)
    private String phone;

    @Size(max = 255)
    private String address;

    @Size(max = 100)
    private String city;

    @Pattern(regexp = "^[A-Za-z]{2}$", message = "Use a sigla da UF com 2 letras")
    @Schema(example = "SP")
    private String state;

    @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "CEP deve ter 8 dígitos")
    @Schema(example = "01001-000")
    private String postalCode;

    @Size(max = 255)
    @Schema(description = "Chave Pix da filial (e-mail, CPF, CNPJ, telefone ou chave aleatória)", example = "financeiro@igreja.com")
    private String pixKey;
}
