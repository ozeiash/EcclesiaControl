package br.com.bitabit.ecclesiacontrol.auth.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import br.com.bitabit.ecclesiacontrol.auth.domain.Permission;
import br.com.bitabit.ecclesiacontrol.auth.domain.Role;
import org.junit.jupiter.api.Test;


class PermissionCeilingTest {

    private static Role role(String... codes) {
        Set<Permission> perms = new java.util.HashSet<>();
        for (String c : codes) {
            Permission p = mock(Permission.class);
            when(p.getCode()).thenReturn(c);
            perms.add(p);
        }
        Role r = mock(Role.class);
        when(r.getPermissions()).thenReturn(perms);
        return r;
    }

    @Test
    void selfServicePermissionDoesNotCountAgainstTheCeiling() {
        // gestor sem PORTAL_ACCESS atribui um papel que só tem PORTAL_ACCESS (ex.: MEMBRO)
        assertTrue(PermissionCeiling.within(Set.of("PERM_USER_MANAGE"), java.util.List.of(role("PORTAL_ACCESS"))));
    }

    @Test
    void rolePermissionsTheActorLacksStillBlock() {
        assertFalse(PermissionCeiling.within(Set.of("PERM_USER_MANAGE"),
                java.util.List.of(role("PORTAL_ACCESS", "FINANCE_WRITE"))));
    }

    @Test
    void roleWithinTheActorsPermissionsIsAllowed() {
        assertTrue(PermissionCeiling.within(Set.of("PERM_MEMBER_READ", "PERM_MEMBER_WRITE"),
                java.util.List.of(role("MEMBER_READ"))));
    }
}
