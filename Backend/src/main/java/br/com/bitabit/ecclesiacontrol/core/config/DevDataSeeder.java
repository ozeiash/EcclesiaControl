package br.com.bitabit.ecclesiacontrol.core.config;

import br.com.bitabit.ecclesiacontrol.auth.domain.*;
import br.com.bitabit.ecclesiacontrol.auth.repository.*;
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

        User admin = userRepository.findByEmail("admin@dev.local")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("admin@dev.local")
                        .passwordHash(passwordEncoder.encode("admin123"))
                        .fullName("Administrador (Dev)")
                        .tenant(sede)
                        .status(User.UserStatus.ACTIVE)
                        .build()));

        Role superAdminRole = roleRepository.findByName(Role.RoleName.SUPER_ADMIN)
                .orElseThrow(() -> new IllegalStateException(
                        "Role SUPER_ADMIN não encontrada — rode a migration de seed de roles antes"));

        boolean hasAssignment = userFilialRoleRepository.findByUserAndFilial(admin, sede.getId())
                .stream().anyMatch(ufr -> ufr.getRole().getName() == Role.RoleName.SUPER_ADMIN);

        if (!hasAssignment) {
            userFilialRoleRepository.save(UserFilialRole.builder()
                    .user(admin)
                    .filial(sede)
                    .role(superAdminRole)
                    .build());
            log.info("Vínculo SUPER_ADMIN criado para admin@dev.local");
        }

        log.info("=== USUÁRIO DE TESTE (DEV) DISPONÍVEL ===");
        log.info("Email: admin@dev.local | Senha: admin123");
        log.info("==========================================");

        Tenant filialTeste = tenantRepository.findByName("Filial Teste (Dev)")
                .orElseGet(() -> tenantRepository.save(Tenant.builder()
                        .name("Filial Teste (Dev)")
                        .type(Tenant.TenantType.FILIAL)
                        .parentTenantId(sede.getId())
                        .email("filial.teste@dev.local")
                        .build()));

        log.info("=== FILIAL DE TESTE (DEV) DISPONÍVEL ===");
        log.info("ID: {}", filialTeste.getId());
        log.info("==========================================");
    }
}