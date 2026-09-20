package br.com.bitabit.ecclesiacontrol.member.dto;

import br.com.bitabit.ecclesiacontrol.member.domain.Member;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MemberStatusUpdateRequest {
    @NotNull
    private Member.MembershipStatus membershipStatus;
    private LocalDate exitDate;
    @Size(max = 30)
    private String exitWay;
}