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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ponta a ponta do CRUD de filiais. Sem @Transactional de propósito (mesma razão do SecurityChainHttpTest). */
@AutoConfigureMockMvc
class FilialHttpTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "password123";
    private static String passwordHash;

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TenantRepository tenants;
    @Autowired private UserRepository users;
    @Autowired private UserFilialRoleRepository assignments;
    @Autowired private RoleRepository roles;
    @Autowired private PasswordEncoder encoder;

    private Tenant sede, filialA;
    private String admin, pastorSede, pastorA, secretaryA;

    @BeforeEach
    void setUp() throws Exception {
        clean();
        sede = tenants.save(Tenant.builder().name("E2E Sede " + UUID.randomUUID()).type(Tenant.TenantType.SEDE).build());
        filialA = tenants.save(Tenant.builder().name("E2E Filial A " + UUID.randomUUID())
                .type(Tenant.TenantType.FILIAL).parentTenantId(sede.getId()).build());
        admin = login(user(sede, Role.RoleName.SUPER_ADMIN));
        pastorSede = login(user(sede, Role.RoleName.PASTOR_SEDE));
        pastorA = login(user(filialA, Role.RoleName.PASTOR_FILIAL));
        secretaryA = login(user(filialA, Role.RoleName.SECRETARIO_FILIAL));
    }

    @AfterEach
    void tearDown() {
        clean();
    }

    @Test
    void pastorSedeCreatesAFilialUnderTheSedeAndItIsAudited() throws Exception {
        String body = mvc.perform(post("/filiais").header("Authorization", pastorSede).contentType(APPLICATION_JSON)
                .content("{\"name\":\"Filial Norte\",\"city\":\"Campinas\",\"state\":\"sp\","
                        + "\"postalCode\":\"13000-000\",\"accountingStartMonth\":\"2026-11\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.type").value("FILIAL"))
            .andExpect(jsonPath("$.parentTenantId").value(sede.getId().toString()))
            .andExpect(jsonPath("$.state").value("SP"))
            .andExpect(jsonPath("$.postalCode").value("13000000"))
            .andExpect(jsonPath("$.accountingStartMonth").value("2026-11"))
            .andExpect(jsonPath("$.active").value(true))
            .andReturn().getResponse().getContentAsString();

        String id = JsonPath.read(body, "$.id");
        Integer audits = jdbc.queryForObject(
                "select count(*) from audit_log where action = 'CREATE_FILIAL' and entity_id = ?::uuid", Integer.class, id);
        assertThat(audits).isEqualTo(1);
    }

    @Test
    void localRolesCannotCreateOrEditFiliais() throws Exception {
        mvc.perform(post("/filiais").header("Authorization", pastorA).contentType(APPLICATION_JSON)
                .content("{\"name\":\"Tentativa\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(put("/filiais/" + filialA.getId()).header("Authorization", secretaryA).contentType(APPLICATION_JSON)
                .content("{\"name\":\"Novo nome\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void aLocalTokenSeesOnlyItsOwnFilial() throws Exception {
        mvc.perform(get("/filiais").header("Authorization", secretaryA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(filialA.getId().toString()));
        mvc.perform(get("/filiais/" + sede.getId()).header("Authorization", secretaryA))
            .andExpect(status().isNotFound());

        mvc.perform(get("/filiais").header("Authorization", admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].type").value("SEDE"));
    }

    @Test
    void nameIsUniqueIgnoringCaseAndCnpjIsValidatedAndUnique() throws Exception {
        create("{\"name\":\"Filial Centro\",\"cnpj\":\"11.222.333/0001-81\"}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.cnpj").value("11222333000181"));

        create("{\"name\":\"filial centro\"}").andExpect(status().isUnprocessableEntity());
        create("{\"name\":\"Filial Sul\",\"cnpj\":\"11.222.333/0001-82\"}").andExpect(status().isBadRequest());
        create("{\"name\":\"Filial Sul\",\"cnpj\":\"11222333000181\"}").andExpect(status().isUnprocessableEntity());
        create("{\"name\":\"Filial Leste\",\"cnpj\":\"12.ABC.345/01DE-35\"}").andExpect(status().isCreated());
    }

    @Test
    void updateKeepsTypeParentAndAccountingStartMonth() throws Exception {
        String body = create("{\"name\":\"Filial Oeste\",\"accountingStartMonth\":\"2026-10\"}")
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");

        // accountingStartMonth no corpo do PUT é ignorado: não faz parte do UpdateFilialRequest
        mvc.perform(put("/filiais/" + id).header("Authorization", admin).contentType(APPLICATION_JSON)
                .content("{\"name\":\"Filial Oeste II\",\"accountingStartMonth\":\"2020-01\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Filial Oeste II"))
            .andExpect(jsonPath("$.type").value("FILIAL"))
            .andExpect(jsonPath("$.parentTenantId").value(sede.getId().toString()))
            .andExpect(jsonPath("$.accountingStartMonth").value("2026-10"));
    }

    @Test
    void aNewFilialCanBeEnteredWithSwitchFilial() throws Exception {
        String body = create("{\"name\":\"Filial Recém-criada\"}")
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");

        mvc.perform(post("/auth/switch-filial").header("Authorization", admin).contentType(APPLICATION_JSON)
                .content("{\"targetFilialId\":\"" + id + "\"}"))
            .andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- apoio

    private org.springframework.test.web.servlet.ResultActions create(String json) throws Exception {
        return mvc.perform(post("/filiais").header("Authorization", admin).contentType(APPLICATION_JSON).content(json));
    }

    private void clean() {
        assertThat(jdbc.queryForObject("select current_database()", String.class)).endsWith("_test");
        jdbc.execute("TRUNCATE TABLE tenant, users, audit_log CASCADE");
    }

    private User user(Tenant filial, Role.RoleName role) {
        if (passwordHash == null) passwordHash = encoder.encode(PASSWORD);
        User u = users.save(User.builder().tenant(filial).email(UUID.randomUUID() + "@e2e.local")
                .fullName("E2E").passwordHash(passwordHash).status(User.UserStatus.ACTIVE).build());
        assignments.save(UserFilialRole.builder().user(u).filial(filial)
                .role(roles.findByName(role).orElseThrow()).build());
        return u;
    }

    private String login(User u) throws Exception {
        String body = mvc.perform(post("/auth/login").contentType(APPLICATION_JSON)
                .content("{\"email\":\"" + u.getEmail() + "\",\"password\":\"" + PASSWORD + "\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.accessToken");
    }
}
