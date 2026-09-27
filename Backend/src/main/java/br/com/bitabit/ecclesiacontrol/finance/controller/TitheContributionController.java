package br.com.bitabit.ecclesiacontrol.finance.controller;

import br.com.bitabit.ecclesiacontrol.core.security.AuthenticatedUser;
import br.com.bitabit.ecclesiacontrol.core.service.AuditActor;
import br.com.bitabit.ecclesiacontrol.finance.dto.TitheContributionRequest;
import br.com.bitabit.ecclesiacontrol.finance.dto.TitheContributionResponse;
import br.com.bitabit.ecclesiacontrol.finance.service.TitheContributionService;
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
@RequestMapping("/finance/tithes")
@RequiredArgsConstructor
public class TitheContributionController {

    private final TitheContributionService titheService;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_FINANCE_WRITE')")
    public ResponseEntity<TitheContributionResponse> create(@RequestBody @Valid TitheContributionRequest request,
                                                            @AuthenticationPrincipal AuthenticatedUser principal) {
        var created = titheService.create(request, AuditActor.from(principal));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_FINANCE_READ')")
    public ResponseEntity<Page<TitheContributionResponse>> findAll(Pageable pageable, Authentication authentication) {
        boolean canViewDetail = hasDetailPermission(authentication);
        return ResponseEntity.ok(titheService.findAll(pageable, canViewDetail));
    }

    @GetMapping("/member/{memberId}")
    @PreAuthorize("hasAuthority('PERM_TITHE_DETAIL_READ')")
    public ResponseEntity<List<TitheContributionResponse>> findByMember(@PathVariable UUID memberId,
                                                                        Authentication authentication) {
        return ResponseEntity.ok(titheService.findByMember(memberId, true));
    }

    private boolean hasDetailPermission(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("PERM_TITHE_DETAIL_READ"));
    }
}