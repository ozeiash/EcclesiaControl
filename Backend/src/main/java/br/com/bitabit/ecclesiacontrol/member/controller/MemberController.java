package br.com.bitabit.ecclesiacontrol.member.controller;

import br.com.bitabit.ecclesiacontrol.core.security.AuthenticatedUser;
import br.com.bitabit.ecclesiacontrol.core.service.AuditActor;
import br.com.bitabit.ecclesiacontrol.member.domain.Member;
import br.com.bitabit.ecclesiacontrol.member.dto.*;
import br.com.bitabit.ecclesiacontrol.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_MEMBER_WRITE')")
    public ResponseEntity<MemberResponse> create(@RequestBody @Valid MemberRequest request,
                                                 @AuthenticationPrincipal AuthenticatedUser principal) {
        MemberResponse created = memberService.create(request, AuditActor.from(principal));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_MEMBER_READ')")
    public ResponseEntity<MemberResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(memberService.findById(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_MEMBER_READ')")
    public ResponseEntity<Page<MemberSummaryResponse>> findAll(
            @RequestParam(required = false) Member.MembershipStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(memberService.findAll(status, pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_MEMBER_WRITE')")
    public ResponseEntity<MemberResponse> update(@PathVariable UUID id,
                                                 @RequestBody @Valid MemberRequest request,
                                                 @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(memberService.update(id, request, AuditActor.from(principal)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('PERM_MEMBER_WRITE')")
    public ResponseEntity<MemberResponse> updateStatus(@PathVariable UUID id,
                                                       @RequestBody @Valid MemberStatusUpdateRequest request,
                                                       @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(memberService.updateStatus(id, request, AuditActor.from(principal)));
    }
}