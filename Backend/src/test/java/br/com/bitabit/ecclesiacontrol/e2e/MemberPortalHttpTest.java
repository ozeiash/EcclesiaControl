package br.com.bitabit.ecclesiacontrol.e2e;

import br.com.bitabit.ecclesiacontrol.auth.domain.Role;
import br.com.bitabit.ecclesiacontrol.auth.domain.User;
import br.com.bitabit.ecclesiacontrol.auth.domain.UserFilialRole;
import br.com.bitabit.ecclesiacontrol.auth.repository.RoleRepository;
import br.com.bitabit.ecclesiacontrol.auth.repository.UserFilialRoleRepository;
import br.com.bitabit.ecclesiacontrol.auth.repository.UserRepository;
import br.com.bitabit.ecclesiacontrol.core.testsupport.AbstractIntegrationTest;
import br.com.bitabit.ecclesiacontrol.tenant.domain.Tenant;
import br.com.bitabit.ecclesiacontrol.tenant.repository.TenantRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ponta a ponta do portal do membro. Sem @Transactional de propósito (ver SecurityChainHttpTest). */
@AutoConfigureMockMvc
class MemberPortalHttpTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "password123";
    private static String passwordHash;

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TenantRepository tenants;
    @Autowired private UserRepository users;
    @Autowired private UserFilialRoleRepository assignments;
    @Autowired private RoleRepository roles;
    @Autowired private PasswordEncoder encoder;

    private Tenant filialA, filialB;
    private String secretaryA, secretaryB;

    @BeforeEach
    void setUp() throws Exception {
        clean();
        filialA = tenants.save(Tenant.builder().name("E2E A " + UUID.randomUUID()).type(Tenant.TenantType.FILIAL).build());
        filialB = tenants.save(Tenant.builder().name("E2E B " + UUID.randomUUID()).type(Tenant.TenantType.FILIAL).build());
        secretaryA = loginStaff(filialA);
        secretaryB = loginStaff(filialB);
    }

    @AfterEach
    void tearDown() {
        clean();
    }

    @Test
    void secretaryGrantsAccessAndTheMemberSeesOnlyAMaskedProfile() throws Exception {
        String memberId = createMember(secretaryA, "Maria Membro", "maria@e2e.local", "1990-05-10");
        String[] cred = grant(secretaryA, memberId);

        String token = loginWith(cred[0], cred[1]);
        mvc.perform(get("/me/profile").header("Authorization", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(memberId))
            .andExpect(jsonPath("$.maskedCpf").value("***.***.***-25"))
            .andExpect(jsonPath("$.cpf").doesNotExist())
            .andExpect(jsonPath("$.rg").doesNotExist())
            .andExpect(jsonPath("$.observations").doesNotExist());
    }

    @Test
    void memberTokenCannotReachStaffEndpoints() throws Exception {
        String memberId = createMember(secretaryA, "Maria Membro", "maria@e2e.local", "1990-05-10");
        String token = loginWith(grant(secretaryA, memberId));

        mvc.perform(get("/members").header("Authorization", token)).andExpect(status().isForbidden());
        mvc.perform(get("/members/" + memberId).header("Authorization", token)).andExpect(status().isForbidden());
        mvc.perform(get("/finance/transactions").header("Authorization", token)).andExpect(status().isForbidden());
        mvc.perform(get("/users").header("Authorization", token)).andExpect(status().isForbidden());
    }

    @Test
    void staffWithoutPortalPermissionCannotUseMe() throws Exception {
        mvc.perform(get("/me/profile").header("Authorization", secretaryA)).andExpect(status().isForbidden());
    }

    @Test
    void anotherFilialCannotGrantOrRevokeAccess() throws Exception {
        String memberId = createMember(secretaryA, "Maria Membro", "maria@e2e.local", "1990-05-10");
        mvc.perform(post("/members/" + memberId + "/portal-access").header("Authorization", secretaryB))
            .andExpect(status().isNotFound());

        grant(secretaryA, memberId);
        mvc.perform(delete("/members/" + memberId + "/portal-access").header("Authorization", secretaryB))
            .andExpect(status().isNotFound());
    }

    @Test
    void grantRulesMinorsMissingEmailDuplicatesAndInactiveMembers() throws Exception {
        String minor = createMember(secretaryA, "Menor", "menor@e2e.local", LocalDate.now().minusYears(15).toString());
        mvc.perform(post("/members/" + minor + "/portal-access").header("Authorization", secretaryA))
            .andExpect(status().isUnprocessableEntity());

        String noEmail = createMember(secretaryA, "Sem Email", null, "1985-01-01");
        mvc.perform(post("/members/" + noEmail + "/portal-access").header("Authorization", secretaryA))
            .andExpect(status().isUnprocessableEntity());

        String ok = createMember(secretaryA, "Maria", "maria@e2e.local", "1990-05-10");
        grant(secretaryA, ok);
        mvc.perform(post("/members/" + ok + "/portal-access").header("Authorization", secretaryA))
            .andExpect(status().isUnprocessableEntity());   // já tem acesso

        String inactive = createMember(secretaryA, "Inativo", "inativo@e2e.local", "1980-01-01");
        mvc.perform(patch("/members/" + inactive + "/status").header("Authorization", secretaryA)
                .contentType(APPLICATION_JSON).content("{\"membershipStatus\":\"INATIVO\"}"))
            .andExpect(status().isOk());
        mvc.perform(post("/members/" + inactive + "/portal-access").header("Authorization", secretaryA))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void memberEditsContactAndAddressButNeverIdentityOrInternalNotes() throws Exception {
        String memberId = createMember(secretaryA, "Maria Membro", "maria@e2e.local", "1990-05-10");
        String token = loginWith(grant(secretaryA, memberId));

        mvc.perform(put("/me/profile").header("Authorization", token).contentType(APPLICATION_JSON)
                .content("{\"phonePrimary\":\"62999990000\",\"city\":\"Goiânia\",\"state\":\"go\","
                        + "\"fullName\":\"Tentativa de trocar o nome\",\"cpf\":\"00000000000\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.phonePrimary").value("62999990000"))
            .andExpect(jsonPath("$.state").value("GO"))
            .andExpect(jsonPath("$.fullName").value("Maria Membro"));   // campos fora do contrato são ignorados

        // Visão da secretaria: CPF e observações intactos
        mvc.perform(get("/members/" + memberId).header("Authorization", secretaryA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cpf").value("52998224725"))
            .andExpect(jsonPath("$.observations").value("nota interna da liderança"))
            .andExpect(jsonPath("$.phonePrimary").value("62999990000"));

        Integer audits = jdbc.queryForObject(
                "select count(*) from audit_log where action = 'UPDATE_MY_PROFILE' and entity_id = ?::uuid",
                Integer.class, memberId);
        assertThat(audits).isEqualTo(1);
    }

    @Test
    void changePasswordRequiresTheCurrentOneAndTheOldOneStopsWorking() throws Exception {
        String memberId = createMember(secretaryA, "Maria Membro", "maria@e2e.local", "1990-05-10");
        String[] cred = grant(secretaryA, memberId);
        String token = loginWith(cred);

        mvc.perform(post("/me/password").header("Authorization", token).contentType(APPLICATION_JSON)
                .content("{\"currentPassword\":\"errada-errada\",\"newPassword\":\"NovaSenha#2026\"}"))
            .andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/me/password").header("Authorization", token).contentType(APPLICATION_JSON)
                .content("{\"currentPassword\":\"" + cred[1] + "\",\"newPassword\":\"curta\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/me/password").header("Authorization", token).contentType(APPLICATION_JSON)
                .content("{\"currentPassword\":\"" + cred[1] + "\",\"newPassword\":\"NovaSenha#2026\"}"))
            .andExpect(status().isNoContent());

        mvc.perform(post("/auth/login").contentType(APPLICATION_JSON)
                .content("{\"email\":\"" + cred[0] + "\",\"password\":\"" + cred[1] + "\"}"))
            .andExpect(status().isUnauthorized());
        loginWith(cred[0], "NovaSenha#2026");

        // a senha nova nunca aparece no audit_log
        Integer leaks = jdbc.queryForObject(
                "select count(*) from audit_log where old_value::text like ? or new_value::text like ?",
                Integer.class, "%NovaSenha%", "%NovaSenha%");
        assertThat(leaks).isZero();
    }

    @Test
    void revokingAccessBlocksTheLoginAndInactivatingTheMemberDoesTheSame() throws Exception {
        String m1 = createMember(secretaryA, "Maria", "maria@e2e.local", "1990-05-10");
        String[] c1 = grant(secretaryA, m1);
        mvc.perform(delete("/members/" + m1 + "/portal-access").header("Authorization", secretaryA))
            .andExpect(status().isNoContent());
        mvc.perform(post("/auth/login").contentType(APPLICATION_JSON)
                .content("{\"email\":\"" + c1[0] + "\",\"password\":\"" + c1[1] + "\"}"))
            .andExpect(status().is4xxClientError());

        String m2 = createMember(secretaryA, "Joao", "joao@e2e.local", "1988-03-03");
        String[] c2 = grant(secretaryA, m2);
        mvc.perform(patch("/members/" + m2 + "/status").header("Authorization", secretaryA)
                .contentType(APPLICATION_JSON).content("{\"membershipStatus\":\"INATIVO\"}"))
            .andExpect(status().isOk());
        mvc.perform(post("/auth/login").contentType(APPLICATION_JSON)
                .content("{\"email\":\"" + c2[0] + "\",\"password\":\"" + c2[1] + "\"}"))
            .andExpect(status().is4xxClientError());
    }

    // ---------------------------------------------------------------- apoio

    private String createMember(String token, String name, String email, String birthDate) throws Exception {
        String json = "{\"fullName\":\"" + name + "\",\"cpf\":\"52998224725\","
                + "\"birthDate\":\"" + birthDate + "\","
                + (email == null ? "" : "\"email\":\"" + email + "\",")
                + "\"observations\":\"nota interna da liderança\"}";
        // CPF é único por filial: para criar vários membros na mesma filial, só o primeiro leva o CPF.
        if (cpfUsed++ > 0) json = json.replace("\"cpf\":\"52998224725\",", "");
        String body = mvc.perform(post("/members").header("Authorization", token)
                .contentType(APPLICATION_JSON).content(json))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private int cpfUsed = 0;

    /** @return {email, temporaryPassword} */
    private String[] grant(String staffToken, String memberId) throws Exception {
        String body = mvc.perform(post("/members/" + memberId + "/portal-access").header("Authorization", staffToken))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.temporaryPassword").isNotEmpty())
            .andReturn().getResponse().getContentAsString();
        return new String[]{JsonPath.read(body, "$.email"), JsonPath.read(body, "$.temporaryPassword")};
    }

    private String loginWith(String[] cred) throws Exception {
        return loginWith(cred[0], cred[1]);
    }

    private String loginWith(String email, String password) throws Exception {
        String body = mvc.perform(post("/auth/login").contentType(APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.accessToken");
    }

    private String loginStaff(Tenant filial) throws Exception {
        if (passwordHash == null) passwordHash = encoder.encode(PASSWORD);
        User u = users.save(User.builder().tenant(filial).email(UUID.randomUUID() + "@e2e.local")
                .fullName("E2E secretaria").passwordHash(passwordHash).status(User.UserStatus.ACTIVE).build());
        assignments.save(UserFilialRole.builder().user(u).filial(filial)
                .role(roles.findByName(Role.RoleName.SECRETARIO_FILIAL).orElseThrow()).build());
        return loginWith(u.getEmail(), PASSWORD);
    }

    private void clean() {
        assertThat(jdbc.queryForObject("select current_database()", String.class)).endsWith("_test");
        jdbc.execute("TRUNCATE TABLE tenant, users, audit_log CASCADE");
    }
}
