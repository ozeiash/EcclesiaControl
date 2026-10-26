package br.com.bitabit.ecclesiacontrol.auth.controller;

import br.com.bitabit.ecclesiacontrol.core.security.AuthenticatedUser;
import br.com.bitabit.ecclesiacontrol.finance.dto.ContributionResponse;
import br.com.bitabit.ecclesiacontrol.member.dto.MemberRoleHistoryResponse;
import br.com.bitabit.ecclesiacontrol.member.dto.MyMemberResponse;
import br.com.bitabit.ecclesiacontrol.member.service.MyMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/me")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class MeController {
    private final MyMemberService service;

    @GetMapping("/member")
    public MyMemberResponse member(@AuthenticationPrincipal AuthenticatedUser principal) {
        return service.member(principal);
    }

    @GetMapping("/role-history")
    public List<MemberRoleHistoryResponse> roleHistory(@AuthenticationPrincipal AuthenticatedUser principal) {
        return service.roleHistory(principal);
    }

    @GetMapping("/contributions")
    public List<ContributionResponse> contributions(@AuthenticationPrincipal AuthenticatedUser principal) {
        return service.contributions(principal);
    }
}
