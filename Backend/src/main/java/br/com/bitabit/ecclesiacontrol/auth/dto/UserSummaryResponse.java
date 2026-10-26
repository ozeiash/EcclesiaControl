package br.com.bitabit.ecclesiacontrol.auth.dto;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;

@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)
@Data
@Builder
public class UserSummaryResponse {
    private UUID id;
    private UUID memberId;
    private String memberName;
    private String email;
    private String fullName;
    private String phone;
    private String status;
    private LocalDateTime lastLogin;
    private List<UserRoleAssignmentResponse> roles;
}
