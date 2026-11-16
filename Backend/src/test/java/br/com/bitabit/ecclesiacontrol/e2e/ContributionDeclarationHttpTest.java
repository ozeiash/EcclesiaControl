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
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ponta a ponta: o membro declara, o tesoureiro decide. Sem @Transactional de propósito. */
@AutoConfigureMockMvc
class ContributionDeclarationHttpTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "password123";
    private static String passwordHash;

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TenantRepository tenants;
    @Autowired private UserRepository users;
    @Autowired private UserFilialRoleRepository assignments;
    @Autowired private RoleRepository roles;
    @Autowired private ChartOfAccountRepository accounts;
    @Autowired private PasswordEncoder encoder;

    private Tenant filialA, filialB;
    private String secretaryA, treasurerA, treasurerB;
    private String memberA1, memberA2;      // tokens de membros da filial A
    private String memberA1Id, memberA2Id;  // ids dos cadastros

    @BeforeEach
    void setUp() throws Exception {
        clean();
        filialA = tenants.save(Tenant.builder().name("E2E A " + UUID.randomUUID()).type(Tenant.TenantType.FILIAL).build());
        filialB = tenants.save(Tenant.builder().name("E2E B " + UUID.randomUUID()).type(Tenant.TenantType.FILIAL).build());
        secretaryA = login(staff(filialA, Role.RoleName.SECRETARIO_FILIAL));
        treasurerA = login(staff(filialA, Role.RoleName.TESOUREIRO_FILIAL));
        treasurerB = login(staff(filialB, Role.RoleName.TESOUREIRO_FILIAL));

        memberA1Id = createMember(secretaryA, "Maria Membro", "maria@e2e.local");
        memberA2Id = createMember(secretaryA, "Joao Membro", "joao@e2e.local");
        memberA1 = memberToken(memberA1Id);
        memberA2 = memberToken(memberA2Id);
    }

    @AfterEach
    void tearDown() {
        clean();
    }

    // ---------------------------------------------------------------- fluxo principal

    @Test
    void aDeclarationStaysOutOfTheBooksUntilTheTreasurerConfirmsIt() throws Exception {
        String id = declareAndGetId(memberA1, accountId("REC-001"), "150.00", LocalDate.now().minusDays(1), "E2E0001");

        // pendente: nada no financeiro nem no extrato do membro
        mvc.perform(get("/finance/transactions").header("Authorization", treasurerA))
            .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/me/contributions").header("Authorization", memberA1))
            .andExpect(jsonPath("$.totalElements").value(0));

        // fila do tesoureiro
        mvc.perform(get("/finance/contribution-declarations?status=PENDENTE").header("Authorization", treasurerA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].memberName").value("Maria Membro"));

        mvc.perform(post("/finance/contribution-declarations/" + id + "/confirm").header("Authorization", treasurerA))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.memberName").value("Maria Membro"))
            .andExpect(jsonPath("$.transactionDate").value(LocalDate.now().minusDays(1).toString()));

        mvc.perform(get("/finance/transactions").header("Authorization", treasurerA))
            .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/me/contributions").header("Authorization", memberA1))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].amount").value(150.0));
        mvc.perform(get("/me/contribution-declarations").header("Authorization", memberA1))
            .andExpect(jsonPath("$.content[0].status").value("CONFIRMADA"));

        // decidida: não confirma duas vezes (sem lançamento em duplicidade)
        mvc.perform(post("/finance/contribution-declarations/" + id + "/confirm").header("Authorization", treasurerA))
            .andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/finance/transactions").header("Authorization", treasurerA))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void confirmationInAClosedMonthIsRefusedAndTheTreasurerCanPickAnOpenDate() throws Exception {
        YearMonth month = YearMonth.now().minusMonths(3);
        String id = declareAndGetId(memberA1, accountId("REC-001"), "80.00", month.atDay(10), null);

        // fecha o mês (como no SecurityChainHttpTest, com um lançamento dentro dele)
        mvc.perform(post("/finance/transactions").header("Authorization", treasurerA)
                .contentType(APPLICATION_JSON)
                .content("{\"accountId\":\"" + accountId("REC-001") + "\",\"amount\":10.00,\"transactionDate\":\""
                        + month.atDay(5) + "\"}"))
            .andExpect(status().isCreated());
        mvc.perform(post("/finance/periods/" + month + "/close").header("Authorization", treasurerA))
            .andExpect(status().isOk());

        mvc.perform(post("/finance/contribution-declarations/" + id + "/confirm").header("Authorization", treasurerA))
            .andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/finance/contribution-declarations?status=PENDENTE").header("Authorization", treasurerA))
            .andExpect(jsonPath("$.totalElements").value(1));   // continua na fila

        mvc.perform(post("/finance/contribution-declarations/" + id + "/confirm").header("Authorization", treasurerA)
                .contentType(APPLICATION_JSON).content("{\"transactionDate\":\"" + LocalDate.now() + "\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.transactionDate").value(LocalDate.now().toString()));
    }

    @Test
    void rejectionNeedsAReasonKeepsItOutOfTheBooksAndTheMemberSeesTheReason() throws Exception {
        String id = declareAndGetId(memberA1, accountId("REC-001"), "50.00", LocalDate.now(), null);

        mvc.perform(post("/finance/contribution-declarations/" + id + "/reject").header("Authorization", treasurerA)
                .contentType(APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/finance/contribution-declarations/" + id + "/reject").header("Authorization", treasurerA)
                .contentType(APPLICATION_JSON).content("{\"reason\":\"Não encontrei o PIX no extrato\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REJEITADA"));

        mvc.perform(get("/finance/transactions").header("Authorization", treasurerA))
            .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/me/contribution-declarations").header("Authorization", memberA1))
            .andExpect(jsonPath("$.content[0].rejectionReason").value("Não encontrei o PIX no extrato"));
        mvc.perform(post("/finance/contribution-declarations/" + id + "/confirm").header("Authorization", treasurerA))
            .andExpect(status().isUnprocessableEntity());
    }

    // ---------------------------------------------------------------- isolamento

    @Test
    void aMemberSeesAndCancelsOnlyHisOwnDeclarationsAndContributions() throws Exception {
        String id = declareAndGetId(memberA1, accountId("REC-001"), "30.00", LocalDate.now(), null);

        mvc.perform(get("/me/contribution-declarations").header("Authorization", memberA2))
            .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(delete("/me/contribution-declarations/" + id).header("Authorization", memberA2))
            .andExpect(status().isNotFound());

        // contribuição lançada direto pelo tesoureiro aparece só para o dono
        mvc.perform(post("/finance/contributions").header("Authorization", treasurerA)
                .contentType(APPLICATION_JSON).content("{\"accountId\":\"" + accountId("REC-001")
                        + "\",\"amount\":100.00,\"transactionDate\":\"" + LocalDate.now()
                        + "\",\"memberId\":\"" + memberA1Id + "\"}"))
            .andExpect(status().isCreated());
        mvc.perform(get("/me/contributions").header("Authorization", memberA1))
            .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/me/contributions").header("Authorization", memberA2))
            .andExpect(jsonPath("$.totalElements").value(0));

        mvc.perform(delete("/me/contribution-declarations/" + id).header("Authorization", memberA1))
            .andExpect(status().isNoContent());
        mvc.perform(get("/me/contribution-declarations").header("Authorization", memberA1))
            .andExpect(jsonPath("$.content[0].status").value("CANCELADA"));
        mvc.perform(delete("/me/contribution-declarations/" + id).header("Authorization", memberA1))
            .andExpect(status().isUnprocessableEntity());   // já decidida
    }

    @Test
    void aTreasurerOfAnotherFilialNeitherSeesNorDecides() throws Exception {
        String id = declareAndGetId(memberA1, accountId("REC-001"), "30.00", LocalDate.now(), null);

        mvc.perform(get("/finance/contribution-declarations").header("Authorization", treasurerB))
            .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(post("/finance/contribution-declarations/" + id + "/confirm").header("Authorization", treasurerB))
            .andExpect(status().isNotFound());
        mvc.perform(post("/finance/contribution-declarations/" + id + "/reject").header("Authorization", treasurerB)
                .contentType(APPLICATION_JSON).content("{\"reason\":\"x\"}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void rolesAreSeparated() throws Exception {
        mvc.perform(get("/finance/contribution-declarations").header("Authorization", secretaryA))
            .andExpect(status().isForbidden());
        mvc.perform(get("/finance/contribution-declarations").header("Authorization", memberA1))
            .andExpect(status().isForbidden());
        mvc.perform(get("/me/contribution-declarations").header("Authorization", treasurerA))
            .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- regras da declaração

    @Test
    void declarationRules() throws Exception {
        // conta que não é de contribuição (REC-004 é a conta reservada ao repasse)
        declare(memberA1, accountId("REC-004"), "10.00", LocalDate.now(), null)
            .andExpect(status().isUnprocessableEntity());
        // data futura
        declare(memberA1, accountId("REC-001"), "10.00", LocalDate.now().plusDays(1), null)
            .andExpect(status().isUnprocessableEntity());
        // valor inválido e referência PIX com caracteres inválidos
        declare(memberA1, accountId("REC-001"), "0.00", LocalDate.now(), null).andExpect(status().isBadRequest());
        declare(memberA1, accountId("REC-001"), "10.00", LocalDate.now(), "ref com espaço!").andExpect(status().isBadRequest());

        // mesma referência PIX duas vezes (mesmo de outro membro da filial)
        declare(memberA1, accountId("REC-001"), "10.00", LocalDate.now(), "E2EDUP").andExpect(status().isCreated());
        declare(memberA2, accountId("REC-001"), "10.00", LocalDate.now(), "E2EDUP").andExpect(status().isUnprocessableEntity());
    }

    @Test
    void aMemberCannotFloodTheQueue() throws Exception {
        for (int i = 0; i < 10; i++) {
            declare(memberA1, accountId("REC-001"), "5.00", LocalDate.now(), null).andExpect(status().isCreated());
        }
        declare(memberA1, accountId("REC-001"), "5.00", LocalDate.now(), null)
            .andExpect(status().isUnprocessableEntity());
        // o limite é por membro
        declare(memberA2, accountId("REC-001"), "5.00", LocalDate.now(), null).andExpect(status().isCreated());
    }

    @Test
    void paymentInfoShowsTheFilialPixKeyAndTheContributionAccounts() throws Exception {
        mvc.perform(get("/me/payment-info").header("Authorization", memberA1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pixKey").doesNotExist());

        filialA.setPixKey("igreja@pix.e2e");
        tenants.save(filialA);
        mvc.perform(get("/me/payment-info").header("Authorization", memberA1))
            .andExpect(jsonPath("$.pixKey").value("igreja@pix.e2e"))
            .andExpect(jsonPath("$.filialName").value(filialA.getName()));

        mvc.perform(get("/me/contribution-accounts").header("Authorization", memberA1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.category=='REPASSE')]").isEmpty());
    }

    // ---------------------------------------------------------------- apoio

    private ResultActions declare(String token, String accountId, String amount, LocalDate date, String pixRef) throws Exception {
        String json = "{\"accountId\":\"" + accountId + "\",\"amount\":" + amount + ",\"declaredDate\":\"" + date
                + "\",\"paymentMethod\":\"PIX\"" + (pixRef == null ? "" : ",\"pixReference\":\"" + pixRef + "\"") + "}";
        return mvc.perform(post("/me/contribution-declarations").header("Authorization", token)
                .contentType(APPLICATION_JSON).content(json));
    }

    private String declareAndGetId(String token, String accountId, String amount, LocalDate date, String pixRef) throws Exception {
        String body = declare(token, accountId, amount, date, pixRef)
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private String accountId(String code) {
        return accounts.findAll().stream().filter(a -> a.getCode().equals(code))
            .findFirst().orElseThrow().getId().toString();
    }

    private String createMember(String token, String name, String email) throws Exception {
        String body = mvc.perform(post("/members").header("Authorization", token).contentType(APPLICATION_JSON)
                .content("{\"fullName\":\"" + name + "\",\"birthDate\":\"1990-05-10\",\"email\":\"" + email + "\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private String memberToken(String memberId) throws Exception {
        String body = mvc.perform(post("/members/" + memberId + "/portal-access").header("Authorization", secretaryA))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return loginWith(JsonPath.read(body, "$.email"), JsonPath.read(body, "$.temporaryPassword"));
    }

    private User staff(Tenant filial, Role.RoleName role) {
        if (passwordHash == null) passwordHash = encoder.encode(PASSWORD);
        User u = users.save(User.builder().tenant(filial).email(UUID.randomUUID() + "@e2e.local")
                .fullName("E2E").passwordHash(passwordHash).status(User.UserStatus.ACTIVE).build());
        assignments.save(UserFilialRole.builder().user(u).filial(filial)
                .role(roles.findByName(role).orElseThrow()).build());
        return u;
    }

    private String login(User u) throws Exception {
        return loginWith(u.getEmail(), PASSWORD);
    }

    private String loginWith(String email, String password) throws Exception {
        String body = mvc.perform(post("/auth/login").contentType(APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.accessToken");
    }

    private void clean() {
        assertThat(jdbc.queryForObject("select current_database()", String.class)).endsWith("_test");
        jdbc.execute("TRUNCATE TABLE tenant, users, audit_log CASCADE");
    }
}
