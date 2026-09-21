package br.com.bitabit.ecclesiacontrol.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UserResponse {
    private UUID id;
    private String email;
    private String fullName;
    private String status;
}