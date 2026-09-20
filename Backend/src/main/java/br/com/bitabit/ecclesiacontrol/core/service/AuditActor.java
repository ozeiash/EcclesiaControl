package br.com.bitabit.ecclesiacontrol.core.service;

import br.com.bitabit.ecclesiacontrol.auth.domain.User;
import br.com.bitabit.ecclesiacontrol.core.security.AuthenticatedUser;

import java.util.UUID;

public record AuditActor(UUID id, String email) {

    public static AuditActor from(AuthenticatedUser principal) {
        return principal == null ? null : new AuditActor(principal.getUserId(), principal.getUsername());
    }

    public static AuditActor of(User user) {
        return user == null ? null : new AuditActor(user.getId(), user.getEmail());
    }
}