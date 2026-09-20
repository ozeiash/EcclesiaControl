package br.com.bitabit.ecclesiacontrol.core.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.UUID;

public class AuthenticatedUser extends User {

    private final UUID userId;

    public AuthenticatedUser(UUID userId, String email, String passwordHash, boolean enabled,
                             Collection<? extends GrantedAuthority> authorities) {
        super(email, passwordHash, enabled, true, true, true, authorities);
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }
}