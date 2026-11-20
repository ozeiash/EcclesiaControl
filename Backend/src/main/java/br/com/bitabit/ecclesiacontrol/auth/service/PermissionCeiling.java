package br.com.bitabit.ecclesiacontrol.auth.service;

import br.com.bitabit.ecclesiacontrol.auth.domain.Role;

import java.util.Collection;
import java.util.Set;

/**
 * Teto de permissões: quem gerencia usuários só mexe em papéis cujas permissões ele mesmo possui.
 * Permissões de autoatendimento (só dão acesso aos dados do próprio usuário, via /me) não contam
 * para o teto, senão um papel como MEMBRO exigiria que o gestor também tivesse PORTAL_ACCESS.
 */
public final class PermissionCeiling {

    /** Códigos (sem o prefixo PERM_) que só liberam dados do próprio usuário. */
    static final Set<String> SELF_SERVICE = Set.of("PORTAL_ACCESS");

    private PermissionCeiling() {}

    /** @param authorities autoridades do ator (ex.: "PERM_MEMBER_READ"); @param roles papéis alvo */
    public static boolean within(Set<String> authorities, Collection<Role> roles) {
        return roles.stream()
                .flatMap(r -> r.getPermissions().stream())
                .allMatch(p -> SELF_SERVICE.contains(p.getCode()) || authorities.contains("PERM_" + p.getCode()));
    }
}
