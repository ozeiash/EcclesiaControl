package br.com.bitabit.ecclesiacontrol.auth.service;

import br.com.bitabit.ecclesiacontrol.auth.domain.*;
import br.com.bitabit.ecclesiacontrol.auth.dto.*;
import br.com.bitabit.ecclesiacontrol.auth.repository.*;
import br.com.bitabit.ecclesiacontrol.core.exception.*;
import br.com.bitabit.ecclesiacontrol.core.security.AuthenticatedUser;
import br.com.bitabit.ecclesiacontrol.core.testsupport.AbstractIntegrationTest;
import br.com.bitabit.ecclesiacontrol.core.util.TenantContext;
import br.com.bitabit.ecclesiacontrol.tenant.domain.Tenant;
import br.com.bitabit.ecclesiacontrol.tenant.repository.TenantRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@Transactional
class UserManagementServiceTest extends AbstractIntegrationTest {
    @Autowired UserManagementService service;
    @Autowired AuthService auth;
    @Autowired RefreshTokenService refresh;
    @Autowired UserRepository users;
    @Autowired UserFilialRoleRepository assignments;
    @Autowired RoleRepository roles;
    @Autowired TenantRepository tenants;
    @Autowired PasswordEncoder encoder;
    @Autowired MockMvc mvc;
    Tenant home, other;
    User target, actor;
    AuthenticatedUser principal;

    @BeforeEach void setup() {
        home = tenants.save(Tenant.builder().name("Gestão A").type(Tenant.TenantType.FILIAL).build());
        other = tenants.save(Tenant.builder().name("Gestão B").type(Tenant.TenantType.FILIAL).build());
        target = user(home);
        actor = user(home);
        principal = new AuthenticatedUser(actor.getId(), actor.getEmail(), actor.getPasswordHash(), true,
                List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));
        TenantContext.setTenantId(home.getId());
        TenantContext.setGlobalScope(false);
    }
    @AfterEach void cleanup() { TenantContext.clear(); }
    User user(Tenant tenant) {
        return users.saveAndFlush(User.builder().tenant(tenant).email(UUID.randomUUID()+"@test.local")
                .fullName("Usuário teste").status(User.UserStatus.ACTIVE).passwordHash(encoder.encode("password123")).build());
    }
    UserFilialRole role(User user, Tenant filial, Role.RoleName name) {
        return assignments.saveAndFlush(UserFilialRole.builder().user(user).filial(filial)
                .role(roles.findByName(name).orElseThrow()).build());
    }
    UpdateUserRequest update(String email) { return UpdateUserRequest.builder().fullName("Novo nome").email(email).build(); }
    LoginRequest login(String password) {
        LoginRequest request = new LoginRequest(); request.setEmail(target.getEmail()); request.setPassword(password); return request;
    }
    @Test void listsByMembershipAndHomeAndFiltersGlobally() {
        User foreign = user(other);
        User linked = user(other); role(linked, home, Role.RoleName.MEMBRO);
        var local = service.listUsers(null, null, PageRequest.of(0, 100));
        assertThat(local.getContent()).extracting(UserSummaryResponse::getId).contains(target.getId(), linked.getId()).doesNotContain(foreign.getId());
        assertThatThrownBy(() -> service.listUsers(other.getId(), null, PageRequest.of(0, 10))).isInstanceOf(AccessForbiddenException.class);
        TenantContext.setGlobalScope(true);
        assertThat(service.listUsers(null, null, PageRequest.of(0, 100)).getTotalElements()).isEqualTo(users.count());
        assertThat(service.listUsers(other.getId(), null, PageRequest.of(0, 100)).getContent()).extracting(UserSummaryResponse::getId).contains(foreign.getId(), linked.getId()).doesNotContain(target.getId());
    }
    @Test void hidesForeignRolesAndForeignUser() {
        role(target, home, Role.RoleName.MEMBRO); role(target, other, Role.RoleName.MEMBRO);
        assertThat(service.getUser(target.getId()).getRoles()).hasSize(1).allMatch(r -> r.getFilialId().equals(home.getId()));
        assertThat(service.listUsers(null, null, PageRequest.of(0,100)).getContent().stream().filter(u -> u.getId().equals(target.getId())).findFirst().orElseThrow().getRoles()).hasSize(1);
        User foreign = user(other);
        assertThatThrownBy(() -> service.getUser(foreign.getId())).isInstanceOf(ResourceNotFoundException.class);
    }
    @Test void localCannotChangeMultiFilialOrGlobalUser() {
        role(target, other, Role.RoleName.MEMBRO);
        assertThatThrownBy(() -> service.updateUser(target.getId(), update(target.getEmail()), principal)).isInstanceOf(AccessForbiddenException.class);
        User global = user(home); role(global, home, Role.RoleName.PASTOR_SEDE);
        assertThatThrownBy(() -> service.resetPassword(global.getId(), ResetPasswordRequest.builder().newPassword("newpassword").build(), principal)).isInstanceOf(AccessForbiddenException.class);
    }
    @Test void cannotChangeOwnStatusRoleOrPasswordButCanEdit() {
        assertThatThrownBy(() -> service.updateStatus(actor.getId(), UpdateUserStatusRequest.builder().status(User.UserStatus.INACTIVE).build(), principal)).isInstanceOf(BusinessRuleException.class).hasMessage("Você não pode alterar a sua própria conta por aqui");
        assertThatThrownBy(() -> service.removeRole(actor.getId(), UUID.randomUUID(), principal)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.resetPassword(actor.getId(), ResetPasswordRequest.builder().newPassword("password456").build(), principal)).isInstanceOf(BusinessRuleException.class);
        assertThat(service.updateUser(actor.getId(), update("  EDIT@TEST.LOCAL  "), principal).getEmail()).isEqualTo("edit@test.local");
    }
    @Test void rejectsDuplicateEmail() {
        assertThatThrownBy(() -> service.updateUser(target.getId(), update(actor.getEmail().toUpperCase(Locale.ROOT)), principal)).isInstanceOf(BusinessRuleException.class).hasMessage("Já existe um usuário com esse e-mail");
    }
    @Test void removesRoleAndValidatesOwnerAndScope() {
        var assignment = role(target, home, Role.RoleName.MEMBRO);
        var foreignAssignment = role(actor, home, Role.RoleName.MEMBRO);
        assertThatThrownBy(() -> service.removeRole(target.getId(), foreignAssignment.getId(), principal)).isInstanceOf(ResourceNotFoundException.class).hasMessage("Papel não encontrado para este usuário");
        service.removeRole(target.getId(), assignment.getId(), principal);
        assertThat(assignments.findById(assignment.getId())).isEmpty();
        var another = role(target, other, Role.RoleName.MEMBRO);
        assertThatThrownBy(() -> service.removeRole(target.getId(), another.getId(), principal)).isInstanceOf(AccessForbiddenException.class);
    }
    @Test void resetPasswordRevokesSessionsAndChangesLogin() {
        role(target, home, Role.RoleName.MEMBRO);
        var tokens = auth.authenticate(login("password123"));
        service.resetPassword(target.getId(), ResetPasswordRequest.builder().newPassword("newpassword123").build(), principal);
        assertThatThrownBy(() -> auth.authenticate(login("password123"))).isInstanceOf(AuthenticationFailedException.class);
        assertThat(auth.authenticate(login("newpassword123")).getAccessToken()).isNotBlank();
        assertThatThrownBy(() -> auth.refreshAccessToken(tokens.getRefreshToken())).isInstanceOf(AuthenticationFailedException.class);
    }
    @Test void inactiveAndSuspendedBlockLoginRefreshAndExistingAccessThenReactivate() throws Exception {
        role(target, home, Role.RoleName.SECRETARIO_FILIAL);
        var tokens = auth.authenticate(login("password123"));
        for (var status : List.of(User.UserStatus.INACTIVE, User.UserStatus.SUSPENDED)) {
            service.updateStatus(target.getId(), UpdateUserStatusRequest.builder().status(status).build(), principal);
            assertThatThrownBy(() -> auth.authenticate(login("password123"))).isInstanceOf(AuthenticationFailedException.class).hasMessage("Usuário inativo. Procure a secretaria da sua igreja.");
            assertThatThrownBy(() -> auth.refreshAccessToken(tokens.getRefreshToken())).isInstanceOf(AuthenticationFailedException.class);
            // Also exercise the status guard with an unrevoked token.
            String inactiveRefresh = refresh.issue(target, home.getId(), Role.RoleScope.LOCAL);
            assertThatThrownBy(() -> auth.refreshAccessToken(inactiveRefresh)).isInstanceOf(AuthenticationFailedException.class).hasMessage("Usuário inativo. Procure a secretaria da sua igreja.");
            mvc.perform(get("/users").header("Authorization", "Bearer " + tokens.getAccessToken())).andExpect(status().isUnauthorized());
            mvc.perform(post("/auth/login").contentType("application/json").content("{\"email\":\""+target.getEmail()+"\",\"password\":\"password123\"}")).andExpect(status().isUnauthorized());
            TenantContext.setTenantId(home.getId());
            service.updateStatus(target.getId(), UpdateUserStatusRequest.builder().status(User.UserStatus.ACTIVE).build(), principal);
            assertThat(auth.authenticate(login("password123")).getAccessToken()).isNotBlank();
        }
    }
    @Test void globalNonAdminCannotAssignOrModifySuperAdmin() {
        TenantContext.setGlobalScope(true);
        var nonAdmin = new AuthenticatedUser(actor.getId(), actor.getEmail(), "", true, List.of(new SimpleGrantedAuthority("ROLE_TI_ADMIN")));
        var request = new AssignRoleRequest(); request.setRole(Role.RoleName.SUPER_ADMIN); request.setFilialId(home.getId());
        assertThatThrownBy(() -> service.assignRole(target.getId(), request, nonAdmin)).isInstanceOf(AccessForbiddenException.class).hasMessage("Só um administrador geral pode atribuir o papel de administrador geral");
        role(target, home, Role.RoleName.SUPER_ADMIN);
        assertThatThrownBy(() -> service.updateUser(target.getId(), update(target.getEmail()), nonAdmin)).isInstanceOf(AccessForbiddenException.class);
    }

    String actorToken(Role.RoleName name) {
        role(actor, home, name);
        LoginRequest request = new LoginRequest();
        request.setEmail(actor.getEmail()); request.setPassword("password123");
        return "Bearer " + auth.authenticate(request).getAccessToken();
    }
    @Test void httpContractSuccessAndValidation() throws Exception {
        String token = actorToken(Role.RoleName.SUPER_ADMIN);
        var assignment = role(target, home, Role.RoleName.MEMBRO);
        mvc.perform(get("/users").param("size", "1").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].roles").isArray())
                .andExpect(jsonPath("$.totalElements").value(users.count()));
        mvc.perform(get("/users/"+target.getId()).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.homeFilialId").value(home.getId().toString()));
        mvc.perform(put("/users/"+target.getId()).header("Authorization", token).contentType("application/json")
                .content("{\"fullName\":\"Nome novo\",\"email\":\"novo@teste.local\",\"phone\":\"123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("novo@teste.local"));
        mvc.perform(patch("/users/"+target.getId()+"/status").header("Authorization", token).contentType("application/json").content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INACTIVE"));
        mvc.perform(post("/users/"+target.getId()+"/reset-password").header("Authorization", token).contentType("application/json").content("{\"newPassword\":\"updated123\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/users/"+target.getId()+"/roles/"+assignment.getId()).header("Authorization", token)).andExpect(status().isNoContent());
        mvc.perform(post("/users/"+target.getId()+"/reset-password").header("Authorization", token).contentType("application/json").content("{\"newPassword\":\"short\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/users/"+actor.getId()+"/status").header("Authorization", token).contentType("application/json").content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isUnprocessableEntity());
    }
    @Test void httpScopeAndAssignmentErrors() throws Exception {
        String token = actorToken(Role.RoleName.SECRETARIO_FILIAL);
        var own = role(actor, home, Role.RoleName.MEMBRO);
        mvc.perform(delete("/users/"+target.getId()+"/roles/"+own.getId()).header("Authorization", token)).andExpect(status().isNotFound());
        var foreign = role(target, other, Role.RoleName.MEMBRO);
        mvc.perform(delete("/users/"+target.getId()+"/roles/"+foreign.getId()).header("Authorization", token)).andExpect(status().isForbidden());
        mvc.perform(get("/users").param("filialId",other.getId().toString()).header("Authorization",token)).andExpect(status().isForbidden());
    }
}
