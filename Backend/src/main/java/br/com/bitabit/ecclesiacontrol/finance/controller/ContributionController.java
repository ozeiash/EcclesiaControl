package br.com.bitabit.ecclesiacontrol.finance.controller;

import br.com.bitabit.ecclesiacontrol.core.security.AuthenticatedUser;
import br.com.bitabit.ecclesiacontrol.core.service.AuditActor;
import br.com.bitabit.ecclesiacontrol.finance.dto.ContributionRequest;
import br.com.bitabit.ecclesiacontrol.finance.dto.ContributionResponse;
import br.com.bitabit.ecclesiacontrol.finance.dto.IdentifyMemberRequest;
import br.com.bitabit.ecclesiacontrol.finance.service.ContributionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/finance/contributions")
@RequiredArgsConstructor
public class ContributionController {

    private final ContributionService contributionService;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_FINANCE_WRITE')")
    public ResponseEntity<ContributionResponse> create(@RequestBody @Valid ContributionRequest request,
                                                       @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(contributionService.create(request, AuditActor.from(principal)));
    }

    @PatchMapping("/{id}/member")
    @PreAuthorize("hasAuthority('PERM_FINANCE_WRITE')")
    public ResponseEntity<ContributionResponse> identifyMember(@PathVariable UUID id,
                                                               @RequestBody @Valid IdentifyMemberRequest request,
                                                               @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(contributionService.identifyMember(id, request, AuditActor.from(principal)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_FINANCE_READ')")
    public ResponseEntity<Page<ContributionResponse>> findAll(Pageable pageable, Authentication authentication) {
        return ResponseEntity.ok(contributionService.findAll(pageable, hasDetailPermission(authentication)));
    }

    @GetMapping("/member/{memberId}")
    @PreAuthorize("hasAuthority('PERM_CONTRIBUTION_DETAIL_READ')")
    public ResponseEntity<List<ContributionResponse>> findByMember(@PathVariable UUID memberId) {
        return ResponseEntity.ok(contributionService.findByMember(memberId, true));
    }

    private boolean hasDetailPermission(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("PERM_CONTRIBUTION_DETAIL_READ"));
    }
}