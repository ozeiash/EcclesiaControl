package br.com.bitabit.ecclesiacontrol.auth.controller;

import br.com.bitabit.ecclesiacontrol.auth.dto.*;
import br.com.bitabit.ecclesiacontrol.auth.service.UserManagementService;
import br.com.bitabit.ecclesiacontrol.core.security.AuthenticatedUser;
import br.com.bitabit.ecclesiacontrol.core.service.AuditActor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import br.com.bitabit.ecclesiacontrol.auth.domain.User;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserManagementService userManagementService;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_USER_MANAGE')")
    public ResponseEntity<UserResponse> create(@RequestBody @Valid CreateUserRequest request,
                                               @AuthenticationPrincipal AuthenticatedUser principal) {
        var created = userManagementService.createUser(request, AuditActor.from(principal));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{userId}/roles")
    @PreAuthorize("hasAuthority('PERM_USER_MANAGE')")
    public ResponseEntity<UserRoleAssignmentResponse> assignRole(@PathVariable UUID userId,
                                                                 @RequestBody @Valid AssignRoleRequest request,
                                                                 @AuthenticationPrincipal AuthenticatedUser principal) {
        var assignment = userManagementService.assignRole(userId, request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(assignment);
    }

    @PutMapping("/{id}/member")
    @PreAuthorize("hasAuthority('PERM_USER_MANAGE')")
    public UserDetailResponse linkMember(@PathVariable UUID id, @RequestBody @Valid LinkMemberRequest request,
                                          @AuthenticationPrincipal AuthenticatedUser principal) {
        return userManagementService.linkMember(id, request, principal);
    }

    @DeleteMapping("/{id}/member")
    @PreAuthorize("hasAuthority('PERM_USER_MANAGE')")
    public ResponseEntity<Void> unlinkMember(@PathVariable UUID id,
                                              @AuthenticationPrincipal AuthenticatedUser principal) {
        userManagementService.unlinkMember(id, principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_USER_MANAGE')")
    public Page<UserSummaryResponse> list(
            @RequestParam(required = false) UUID filialId,
            @RequestParam(required = false) User.UserStatus status,
            @PageableDefault(sort = "fullName") Pageable pageable) {
        return userManagementService.listUsers(filialId, status, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_USER_MANAGE')")
    public UserDetailResponse detail(@PathVariable UUID id) {
        return userManagementService.getUser(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_USER_MANAGE')")
    public UserDetailResponse update(@PathVariable UUID id, @RequestBody @Valid UpdateUserRequest request,
                                    @AuthenticationPrincipal AuthenticatedUser principal) {
        return userManagementService.updateUser(id, request, principal);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('PERM_USER_MANAGE')")
    public UserDetailResponse status(@PathVariable UUID id, @RequestBody @Valid UpdateUserStatusRequest request,
                                    @AuthenticationPrincipal AuthenticatedUser principal) {
        return userManagementService.updateStatus(id, request, principal);
    }

    @DeleteMapping("/{userId}/roles/{assignmentId}")
    @PreAuthorize("hasAuthority('PERM_USER_MANAGE')")
    public ResponseEntity<Void> removeRole(@PathVariable UUID userId, @PathVariable UUID assignmentId,
                                           @AuthenticationPrincipal AuthenticatedUser principal) {
        userManagementService.removeRole(userId, assignmentId, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('PERM_USER_MANAGE')")
    public ResponseEntity<Void> resetPassword(@PathVariable UUID id, @RequestBody @Valid ResetPasswordRequest request,
                                             @AuthenticationPrincipal AuthenticatedUser principal) {
        userManagementService.resetPassword(id, request, principal);
        return ResponseEntity.noContent().build();
    }
}
