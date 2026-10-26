package br.com.bitabit.ecclesiacontrol.auth.dto;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;

@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)
@Data
@Builder
public class UserDetailResponse {
    private UUID id;
    private UUID memberId;
    private String memberName;
    private String email;
    private String fullName;
    private String phone;
    private String status;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID homeFilialId;
    private String homeFilialName;
    private List<UserRoleAssignmentResponse> roles;
}
