package br.com.bitabit.ecclesiacontrol.e2e;

import br.com.bitabit.ecclesiacontrol.auth.domain.Role;
import br.com.bitabit.ecclesiacontrol.auth.domain.User;
import br.com.bitabit.ecclesiacontrol.auth.domain.UserFilialRole;
import br.com.bitabit.ecclesiacontrol.auth.repository.RoleRepository;
import br.com.bitabit.ecclesiacontrol.auth.repository.UserFilialRoleRepository;
import br.com.bitabit.ecclesiacontrol.auth.repository.UserRepository;
import br.com.bitabit.ecclesiacontrol.core.testsupport.AbstractIntegrationTest;
import br.com.bitabit.ecclesiacontrol.finance.repository.ChartOfAccountRepository;
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
import java.time.YearMonth;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de ponta a ponta da cadeia de seguranca: JWT -> TenantContext -> filtro do Hibernate
 * -> @PreAuthorize -> regra de negocio -> banco.
 *
 * IMPORTANTE: esta classe NAO e @Transactional de proposito. Com a transacao do teste, todas as
 * requisicoes compartilhariam a mesma sessao do Hibernate, e o filtro de tenant habilitado numa
 * requisicao continuaria ligado na seguinte, escondendo justamente os bugs de escopo que este teste
 * procura. Sem a transacao, cada requisicao e confirmada como em producao, e a limpeza e feita
 * por TRUNCATE.
 */
@AutoConfigureMockMvc
class SecurityChainHttpTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "password123";
    private static String passwordHash;   // bcrypt e lento: calcula uma vez so

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TenantRepository tenants;
    @Autowired private UserRepository users;
    @Autowired private UserFilialRoleRepository assignments;
    @Autowired private RoleRepository roles;
    @Autowired private ChartOfAccountRepository accounts;
    @Autowired private PasswordEncoder encoder;

    private Tenant filialA, filialB;
    private String secretaryA, secretaryB, treasurerA, treasurerB, pastorA, admin;

    @BeforeEach
    void setUp() throws Exception {
        clean();
        filialA = tenant("E2E Filial A");
        filialB = tenant("E2E Filial B");
        secretaryA = login(user(filialA, Role.RoleName.SECRETARIO_FILIAL));
        secretaryB = login(user(filialB, Role.RoleName.SECRETARIO_FILIAL));
        treasurerA = login(user(filialA, Role.RoleName.TESOUREIRO_FILIAL));
        treasurerB = login(user(filialB, Role.RoleName.TESOUREIRO_FILIAL));
        pastorA = login(user(filialA, Role.RoleName.PASTOR_FILIAL));
        admin = login(user(filialA, Role.RoleName.SUPER_ADMIN));   // papel GLOBAL
    }

    @AfterEach
    void tearDown() {
        clean();
    }

    // ---------------------------------------------------------------- autenticacao e RBAC

    @Test
    void unauthenticatedAndMalformedTokensAreRejected() throws Exception {
        mvc.perform(get("/members")).andExpect(status().isUnauthorized());
        mvc.perform(get("/members").header("Authorization", "Bearer \"abc\""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rbacBlocksFinanceForSecretaryButNotForTreasurer() throws Exception {
        mvc.perform(get("/finance/transactions").header("Authorization", secretaryA))
                .andExpect(status().isForbidden());
        mvc.perform(get("/finance/transactions").header("Authorization", treasurerA))
                .andExpect(status().isOk());
    }

    @Test
    void loginIgnoresEmailCase() throws Exception {
        User u = user(filialA, Role.RoleName.SECRETARIO_FILIAL);
        mvc.perform(post("/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"" + u.getEmail().toUpperCase(Locale.ROOT)
                                + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- isolamento entre filiais

    @Test
    void aMemberOfOneFilialIsInvisibleToTheOther() throws Exception {
        String id = createMember(secretaryA, "Membro da Filial A");

        mvc.perform(get("/members/" + id).header("Authorization", secretaryA)).andExpect(status().isOk());
        mvc.perform(get("/members/" + id).header("Authorization", secretaryB)).andExpect(status().isNotFound());
        mvc.perform(get("/members").header("Authorization", secretaryB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void globalTokenSeesEverythingUntilItSwitchesIntoAFilial() throws Exception {
        String id = createMember(secretaryA, "Membro da Filial A");

        mvc.perform(get("/members/" + id).header("Authorization", admin)).andExpect(status().isOk());

        String switched = switchTo(admin, filialB);
        mvc.perform(get("/members/" + id).header("Authorization", switched)).andExpect(status().isNotFound());
    }

    @Test
    void aFilialCannotReferenceAMemberOfAnotherFilial() throws Exception {
        String memberOfA = createMember(secretaryA, "Membro da Filial A");

        mvc.perform(post("/finance/contributions").header("Authorization", treasurerB)
                        .contentType(APPLICATION_JSON).content(contributionJson(memberOfA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void dashboardScopeFollowsTheToken() throws Exception {
        mvc.perform(get("/dashboard/finance").header("Authorization", treasurerA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.consolidated").value(false));
        mvc.perform(get("/dashboard/finance").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.consolidated").value(true));
        mvc.perform(get("/dashboard/finance").header("Authorization", switchTo(admin, filialB)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.consolidated").value(false));
    }

    // ---------------------------------------------------------------- sigilo e fechamento

    @Test
    void contributorIdentityIsVisibleOnlyWithTheDetailPermission() throws Exception {
        String memberId = createMember(secretaryA, "Contribuinte A");
        mvc.perform(post("/finance/contributions").header("Authorization", treasurerA)
                        .contentType(APPLICATION_JSON).content(contributionJson(memberId)))
                .andExpect(status().isCreated());

        mvc.perform(get("/finance/contributions").header("Authorization", treasurerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].memberName").value("Contribuinte A"));
        mvc.perform(get("/finance/contributions").header("Authorization", pastorA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].memberName").doesNotExist());
    }

    @Test
    void aClosedMonthRejectsNewTransactionsOverHttp() throws Exception {
        YearMonth month = YearMonth.now().minusMonths(3);
        String body = transactionJson(month.atDay(10));

        mvc.perform(post("/finance/transactions").header("Authorization", treasurerA)
                        .contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/finance/periods/" + month + "/close").header("Authorization", treasurerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FECHADO"));

        mvc.perform(post("/finance/transactions").header("Authorization", treasurerA)
                        .contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    // ---------------------------------------------------------------- teto de permissoes e auditoria

    @Test
    void aSecretaryCannotTakeOverOrCreateATreasurer() throws Exception {
        User treasurer = user(filialA, Role.RoleName.TESOUREIRO_FILIAL);
        User plain = plainUser(filialA);

        // Controles positivos: a Secretaria TEM a permissao USER_MANAGE e gerencia contas sem privilegio extra.
        // Sem eles, os 403 abaixo poderiam vir da falta de permissao, e nao do teto.
        mvc.perform(post("/users/" + plain.getId() + "/reset-password")
                        .header("Authorization", secretaryA).contentType(APPLICATION_JSON)
                        .content("{\"newPassword\":\"outrasenha123\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/users/" + plain.getId() + "/roles")
                        .header("Authorization", secretaryA).contentType(APPLICATION_JSON)
                        .content("{\"role\":\"MEMBRO\"}"))
                .andExpect(status().isCreated());

        // Tomar a conta de quem tem mais permissoes
        mvc.perform(post("/users/" + treasurer.getId() + "/reset-password")
                        .header("Authorization", secretaryA).contentType(APPLICATION_JSON)
                        .content("{\"newPassword\":\"outrasenha123\"}"))
                .andExpect(status().isForbidden());

        // Criar um "tesoureiro" atribuindo o papel a uma conta qualquer
        User another = plainUser(filialA);
        mvc.perform(post("/users/" + another.getId() + "/roles")
                        .header("Authorization", secretaryA).contentType(APPLICATION_JSON)
                        .content("{\"role\":\"TESOUREIRO_FILIAL\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/users/" + another.getId() + "/roles")
                        .header("Authorization", pastorA).contentType(APPLICATION_JSON)
                        .content("{\"role\":\"TESOUREIRO_FILIAL\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditLogNeverContainsTheMemberCpf() throws Exception {
        String cpf = "12345678901";
        mvc.perform(post("/members").header("Authorization", secretaryA)
                        .contentType(APPLICATION_JSON)
                        .content("{\"fullName\":\"Membro com CPF\",\"cpf\":\"" + cpf + "\"}"))
                .andExpect(status().isCreated());

        Integer entries = jdbc.queryForObject(
                "select count(*) from audit_log where entity_type = 'Member'", Integer.class);
        Integer leaks = jdbc.queryForObject(
                "select count(*) from audit_log where entity_type = 'Member' "
                        + "and (coalesce(new_value::text, '') like ? or coalesce(old_value::text, '') like ?)",
                Integer.class, "%" + cpf + "%", "%" + cpf + "%");

        assertThat(entries).isPositive();   // evita um teste vazio: a auditoria precisa ter sido gravada
        assertThat(leaks).isZero();
    }

    // ---------------------------------------------------------------- apoio

    /** O TRUNCATE apaga dados: so pode rodar no banco de testes, nunca no de desenvolvimento. */
    private void clean() {
        assertThat(jdbc.queryForObject("select current_database()", String.class)).endsWith("_test");
        jdbc.execute("TRUNCATE TABLE tenant, users, audit_log CASCADE");
    }

    private Tenant tenant(String name) {
        return tenants.save(Tenant.builder().name(name + " " + UUID.randomUUID())
                .type(Tenant.TenantType.FILIAL).build());
    }

    private User plainUser(Tenant filial) {
        if (passwordHash == null) passwordHash = encoder.encode(PASSWORD);
        return users.save(User.builder().tenant(filial).email(UUID.randomUUID() + "@e2e.local")
                .fullName("E2E sem papel").passwordHash(passwordHash).status(User.UserStatus.ACTIVE).build());
    }

    private User user(Tenant filial, Role.RoleName role) {
        User u = plainUser(filial);
        assignments.save(UserFilialRole.builder().user(u).filial(filial)
                .role(roles.findByName(role).orElseThrow()).build());
        return u;
    }

    private String login(User u) throws Exception {
        String body = mvc.perform(post("/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"" + u.getEmail() + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.accessToken");
    }

    private String switchTo(String token, Tenant filial) throws Exception {
        String body = mvc.perform(post("/auth/switch-filial").header("Authorization", token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"targetFilialId\":\"" + filial.getId() + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.accessToken");
    }

    // Se o seu MemberRequest exigir mais campos alem do nome, acrescente-os aqui.
    private String createMember(String token, String name) throws Exception {
        String body = mvc.perform(post("/members").header("Authorization", token)
                        .contentType(APPLICATION_JSON).content("{\"fullName\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private String accountId(String code) {
        return accounts.findAll().stream().filter(a -> a.getCode().equals(code))
                .findFirst().orElseThrow().getId().toString();
    }

    private String contributionJson(String memberId) {
        return "{\"accountId\":\"" + accountId("REC-001") + "\",\"amount\":100.00,"
                + "\"transactionDate\":\"" + LocalDate.now() + "\",\"memberId\":\"" + memberId + "\"}";
    }

    private String transactionJson(LocalDate date) {
        return "{\"accountId\":\"" + accountId("REC-001") + "\",\"amount\":50.00,"
                + "\"transactionDate\":\"" + date + "\",\"description\":\"E2E\"}";
    }
}