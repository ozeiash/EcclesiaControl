package br.com.bitabit.ecclesiacontrol.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UserRoleAssignmentResponse {
    private UUID id;
    private UUID filialId;
    private String filialName;
    private String role;
    private String scope;
}