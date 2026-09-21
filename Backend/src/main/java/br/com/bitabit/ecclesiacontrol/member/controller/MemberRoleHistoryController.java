package br.com.bitabit.ecclesiacontrol.member.controller;

import br.com.bitabit.ecclesiacontrol.core.security.AuthenticatedUser;
import br.com.bitabit.ecclesiacontrol.core.service.AuditActor;
import br.com.bitabit.ecclesiacontrol.member.dto.MemberRoleHistoryRequest;
import br.com.bitabit.ecclesiacontrol.member.dto.MemberRoleHistoryResponse;
import br.com.bitabit.ecclesiacontrol.member.service.MemberRoleHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/members/{memberId}/role-history")
@RequiredArgsConstructor
public class MemberRoleHistoryController {

    private final MemberRoleHistoryService service;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_MEMBER_WRITE')")
    public ResponseEntity<MemberRoleHistoryResponse> add(@PathVariable UUID memberId,
                                                         @RequestBody @Valid MemberRoleHistoryRequest request,
                                                         @AuthenticationPrincipal AuthenticatedUser principal) {
        var created = service.addEntry(memberId, request, AuditActor.from(principal));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_MEMBER_READ')")
    public ResponseEntity<List<MemberRoleHistoryResponse>> findByMember(@PathVariable UUID memberId) {
        return ResponseEntity.ok(service.findByMember(memberId));
    }

    @PatchMapping("/{historyId}/close")
    @PreAuthorize("hasAuthority('PERM_MEMBER_WRITE')")
    public ResponseEntity<MemberRoleHistoryResponse> close(@PathVariable UUID memberId,
                                                           @PathVariable UUID historyId,
                                                           @RequestParam(required = false) LocalDate endedAt,
                                                           @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(service.closeEntry(memberId, historyId, endedAt, AuditActor.from(principal)));
    }
}