package br.com.bitabit.ecclesiacontrol.core.security;

import br.com.bitabit.ecclesiacontrol.auth.domain.Role;
import br.com.bitabit.ecclesiacontrol.auth.domain.User;
import br.com.bitabit.ecclesiacontrol.auth.domain.UserFilialRole;
import br.com.bitabit.ecclesiacontrol.auth.repository.UserFilialRoleRepository;
import br.com.bitabit.ecclesiacontrol.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserFilialRoleRepository userFilialRoleRepository;

    // Mantido só por compatibilidade com a interface — não usar pra autorização real por filial
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return buildUserDetails(loadUser(email), Set.of());
    }

    // Método real usado pelo JwtAuthenticationFilter, que já sabe a filial ativa do token
    public UserDetails loadUserByEmailAndFilial(String email, UUID filialId) {
        User user = loadUser(email);
        var rolesInContext = userFilialRoleRepository.findByUserAndFilial(user, filialId)
                .stream().map(UserFilialRole::getRole).collect(Collectors.toSet());
        return buildUserDetails(user, rolesInContext);
    }

    private User loadUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    private UserDetails buildUserDetails(User user, Set<Role> rolesInContext) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        for (Role role : rolesInContext) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
            role.getPermissions().forEach(p ->
                    authorities.add(new SimpleGrantedAuthority("PERM_" + p.getCode())));
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .disabled(user.getStatus() != User.UserStatus.ACTIVE)
                .build();
    }
}