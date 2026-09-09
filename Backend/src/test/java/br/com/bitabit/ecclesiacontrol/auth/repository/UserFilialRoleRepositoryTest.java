package br.com.bitabit.ecclesiacontrol.auth.repository;

import br.com.bitabit.ecclesiacontrol.auth.domain.Role;
import br.com.bitabit.ecclesiacontrol.auth.domain.User;
import br.com.bitabit.ecclesiacontrol.auth.domain.UserFilialRole;
import br.com.bitabit.ecclesiacontrol.core.testsupport.AbstractRepositoryTest;
import br.com.bitabit.ecclesiacontrol.tenant.domain.Tenant;
import br.com.bitabit.ecclesiacontrol.tenant.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;


class UserFilialRoleRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private UserFilialRoleRepository userFilialRoleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TenantRepository tenantRepository;

    private User testUser;
    private Tenant salaFilial;
    private Tenant filialA;
    private Role globalRole;
    private Role localRole;

    @BeforeEach
    void setUp() {
        salaFilial = tenantRepository.save(Tenant.builder()
                .name("Sede Geral")
                .type(Tenant.TenantType.SEDE)
                .email("sede@church.com")
                .build());

        filialA = tenantRepository.save(Tenant.builder()
                .name("Filial A")
                .parentTenantId(salaFilial.getId())
                .type(Tenant.TenantType.FILIAL)
                .email("filialA@church.com")
                .build());

        testUser = userRepository.save(User.builder()
                .email("pastor@church.com")
                .passwordHash("hashed_password")
                .fullName("Pastor João")
                .tenant(salaFilial)
                .status(User.UserStatus.ACTIVE)
                .build());

        globalRole = roleRepository.findByName(Role.RoleName.PASTOR_SEDE)
                .orElseThrow(() -> new IllegalStateException(
                        "Role PASTOR_SEDE não encontrada — confirme que a Migration 6 (seed) foi aplicada"));

        localRole = roleRepository.findByName(Role.RoleName.PASTOR_FILIAL)
                .orElseThrow(() -> new IllegalStateException(
                        "Role PASTOR_FILIAL não encontrada — confirme que a Migration 6 (seed) foi aplicada"));
    }

    @Test
    void shouldFindByUser() {
        // Arrange
        userFilialRoleRepository.save(UserFilialRole.builder()
                .user(testUser)
                .filial(salaFilial)
                .role(globalRole)
                .build());

        // Act
        List<UserFilialRole> result = userFilialRoleRepository.findByUser(testUser);

        // Assert
        assertThat(result)
                .isNotEmpty()
                .hasSize(1)
                .allMatch(ufr -> ufr.getUser().equals(testUser));
    }

    @Test
    void shouldFindByUserAndFilial() {
        // Arrange
        userFilialRoleRepository.save(UserFilialRole.builder()
                .user(testUser)
                .filial(salaFilial)
                .role(globalRole)
                .build());

        // Act
        List<UserFilialRole> result = userFilialRoleRepository
                .findByUserAndFilial(testUser, salaFilial.getId());

        // Assert
        assertThat(result)
                .isNotEmpty()
                .allMatch(ufr -> ufr.getFilial().getId().equals(salaFilial.getId()));
    }

    @Test
    void shouldFindGlobalRolesByUser() {
        // Arrange
        userFilialRoleRepository.save(UserFilialRole.builder()
                .user(testUser)
                .filial(salaFilial)
                .role(globalRole)
                .build());

        userFilialRoleRepository.save(UserFilialRole.builder()
                .user(testUser)
                .filial(filialA)
                .role(localRole)
                .build());

        // Act
        List<UserFilialRole> globalRoles = userFilialRoleRepository
                .findGlobalRolesByUser(testUser);

        // Assert
        assertThat(globalRoles)
                .hasSize(1)
                .allMatch(ufr -> ufr.getRole().getScope() == Role.RoleScope.GLOBAL);
    }

    @Test
    void shouldCheckIfUserHasRole() {
        // Arrange
        userFilialRoleRepository.save(UserFilialRole.builder()
                .user(testUser)
                .filial(salaFilial)
                .role(globalRole)
                .build());

        // Act
        boolean hasRole = userFilialRoleRepository.hasRole(
                testUser,
                salaFilial.getId(),
                Role.RoleName.PASTOR_SEDE
        );

        // Assert
        assertThat(hasRole).isTrue();
    }

    @Test
    void shouldFindAllAccessibleFilials() {
        // Arrange
        userFilialRoleRepository.save(UserFilialRole.builder()
                .user(testUser)
                .filial(salaFilial)
                .role(globalRole)
                .build());

        userFilialRoleRepository.save(UserFilialRole.builder()
                .user(testUser)
                .filial(filialA)
                .role(localRole)
                .build());

        // Act
        List<UUID> accessibleFilials = userFilialRoleRepository
                .findAllAccessibleFilials(testUser);

        // Assert
        assertThat(accessibleFilials)
                .hasSize(2)
                .contains(salaFilial.getId(), filialA.getId());
    }
}