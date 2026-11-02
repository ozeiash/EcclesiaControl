package br.com.bitabit.ecclesiacontrol.auth.dto;

import lombok.*;
import br.com.bitabit.ecclesiacontrol.auth.domain.User;
import jakarta.validation.constraints.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserStatusRequest {
    @NotNull
    private User.UserStatus status;
}
