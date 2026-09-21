package br.com.bitabit.ecclesiacontrol.auth.dto;

import br.com.bitabit.ecclesiacontrol.auth.domain.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AssignRoleRequest {
    private UUID filialId; // opcional para atores LOCAL — obrigatório para GLOBAL

    @NotNull
    private Role.RoleName role;
}