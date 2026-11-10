package br.com.bitabit.ecclesiacontrol.core.config;

import br.com.bitabit.ecclesiacontrol.auth.domain.Role;
import br.com.bitabit.ecclesiacontrol.auth.domain.User;
import br.com.bitabit.ecclesiacontrol.auth.domain.UserFilialRole;
import br.com.bitabit.ecclesiacontrol.auth.repository.RoleRepository;
import br.com.bitabit.ecclesiacontrol.auth.repository.UserFilialRoleRepository;
import br.com.bitabit.ecclesiacontrol.auth.repository.UserRepository;
import br.com.bitabit.ecclesiacontrol.tenant.domain.Tenant;
import br.com.bitabit.ecclesiacontrol.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("dev") // NUNCA roda em "prod" nem em "test"
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserFilialRoleRepository userFilialRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        Tenant sede = tenantRepository.findByName("Sede Geral (Dev)")
                .orElseGet(() -> tenantRepository.save(Tenant.builder()
                        .name("Sede Geral (Dev)")
                        .type(Tenant.TenantType.SEDE)
                        .email("sede@dev.local")
                        .build()));

        Tenant filialTeste = tenantRepository.findByName("Filial Teste (Dev)")
                .orElseGet(() -> tenantRepository.save(Tenant.builder()
                        .name("Filial Teste (Dev)")
                        .type(Tenant.TenantType.FILIAL)
                        .parentTenantId(sede.getId())
                        .email("filial.teste@dev.local")
                        .build()));

        // Cada usuário é criado só se ainda não existir, então o seeder pode rodar
        // várias vezes e também completa um banco de desenvolvimento já existente.
        ensureUser("admin@dev.local", "admin123", "Administrador (Dev)",
                sede, Role.RoleName.SUPER_ADMIN);
        ensureUser("secretaria@dev.local", "secretaria123", "Secretária da Filial (Dev)",
                filialTeste, Role.RoleName.SECRETARIO_FILIAL);
        ensureUser("tesoureiro@dev.local", "tesoureiro123", "Tesoureiro da Filial (Dev)",
                filialTeste, Role.RoleName.TESOUREIRO_FILIAL);
        ensureUser("pastor@dev.local", "pastor123", "Pastor da Filial (Dev)",
                filialTeste, Role.RoleName.PASTOR_FILIAL);

        log.info("=== USUÁRIOS DE TESTE (DEV) DISPONÍVEIS ===");
        log.info("admin@dev.local      | admin123      | SUPER_ADMIN       | Sede Geral (Dev)");
        log.info("secretaria@dev.local | secretaria123 | SECRETARIO_FILIAL | Filial Teste (Dev)");
        log.info("tesoureiro@dev.local | tesoureiro123 | TESOUREIRO_FILIAL | Filial Teste (Dev)");
        log.info("pastor@dev.local     | pastor123     | PASTOR_FILIAL     | Filial Teste (Dev)");
        log.info("ID da Filial Teste (Dev): {}", filialTeste.getId());
        log.info("============================================");
    }

    /**
     * Garante que o usuário existe e tem o papel indicado na filial informada.
     * Idempotente: não duplica usuário nem vínculo.
     */
    private void ensureUser(String email, String rawPassword, String fullName,
                            Tenant filial, Role.RoleName roleName) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(email)
                        .passwordHash(passwordEncoder.encode(rawPassword))
                        .fullName(fullName)
                        .tenant(filial)
                        .status(User.UserStatus.ACTIVE)
                        .build()));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException(
                        "Role " + roleName + " não encontrada — confirme que as migrations de roles foram aplicadas"));

        boolean alreadyAssigned = userFilialRoleRepository.findByUserAndFilial(user, filial.getId())
                .stream().anyMatch(ufr -> ufr.getRole().getName() == roleName);

        if (!alreadyAssigned) {
            userFilialRoleRepository.save(UserFilialRole.builder()
                    .user(user)
                    .filial(filial)
                    .role(role)
                    .build());
            log.info("Vínculo {} criado para {}", roleName, email);
        }
    }
}