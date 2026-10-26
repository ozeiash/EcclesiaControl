package br.com.bitabit.ecclesiacontrol.auth.controller;

import br.com.bitabit.ecclesiacontrol.auth.domain.Role;
import br.com.bitabit.ecclesiacontrol.core.testsupport.UserMemberIntegrationTest;
import br.com.bitabit.ecclesiacontrol.finance.domain.*;
import br.com.bitabit.ecclesiacontrol.finance.repository.*;
import br.com.bitabit.ecclesiacontrol.member.domain.*;
import br.com.bitabit.ecclesiacontrol.member.repository.MemberRoleHistoryRepository;
import br.com.bitabit.ecclesiacontrol.member.service.MyMemberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MeControllerTest extends UserMemberIntegrationTest {
    @Autowired MemberRoleHistoryRepository history;
    @Autowired ContributionRepository contributions;
    @Autowired FinancialTransactionRepository transactions;
    @Autowired ChartOfAccountRepository accounts;
    @Autowired ObjectMapper json;
    @Autowired MyMemberService myService;

    @Test void memberWithoutPermissionsCanReadOwnDataButCannotListMembers() throws Exception {
        Member own=member(home); attach(target,own);
        String token=token(target,home,Role.RoleName.MEMBRO);
        mvc.perform(get("/me/member").header("Authorization",token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(own.getId().toString())).andExpect(jsonPath("$.filialName").value(home.getName()));
        mvc.perform(get("/me/role-history").header("Authorization",token)).andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/me/contributions").header("Authorization",token)).andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/members").header("Authorization",token)).andExpect(status().isForbidden());
        mvc.perform(get("/me/member")).andExpect(status().isUnauthorized());
    }
    @Test void memberResponseHasExactAllowlistedFieldsAndNoCpfOrRg() throws Exception {
        Member own=member(home); attach(target,own);
        var result=mvc.perform(get("/me/member").header("Authorization",token(target,home,Role.RoleName.MEMBRO)))
                .andExpect(status().isOk()).andReturn();
        var node=json.readTree(result.getResponse().getContentAsString());
        Set<String> keys=new HashSet<>(); node.fieldNames().forEachRemaining(keys::add);
        assertThat(keys).containsExactlyInAnyOrder("id","fullName","socialName","birthDate","gender","email","phonePrimary","phoneSecondary",
                "postalCode","street","number","complement","neighborhood","city","state","membershipStatus","admissionDate","baptized",
                "baptismDate","maritalStatus","weddingDate","currentRole","photoUrl","filialName");
        assertThat(node.has("cpf")).isFalse(); assertThat(node.has("rg")).isFalse();
        assertThat(result.getResponse().getContentAsString()).doesNotContain(own.getCpf(),own.getRg());
    }
    @Test void allMeRoutesFindOwnerEvenWhenTokenIsInAnotherBranch() throws Exception {
        Member own=member(home); attach(target,own);
        addHistory(own,"Diácono",LocalDate.of(2024,1,1)); addHistory(own,"Presbítero",LocalDate.of(2025,1,1));
        addContribution(own,false,LocalDate.of(2025,1,1),"150.00");
        String token=token(target,other,Role.RoleName.MEMBRO);
        mvc.perform(get("/me/member").header("Authorization",token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(own.getId().toString()));
        mvc.perform(get("/me/role-history").header("Authorization",token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].role").value("Presbítero"));
        mvc.perform(get("/me/contributions").header("Authorization",token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].amount").value(150.0));
        // The bypass restores the original filter and does not expose other members afterward.
        local(other);
        assertThat(myService.member(new br.com.bitabit.ecclesiacontrol.core.security.AuthenticatedUser(target.getId(),target.getEmail(),"",true,List.of())).getId()).isEqualTo(own.getId());
        assertThat(em.unwrap(Session.class).getEnabledFilter("tenantFilter")).isNotNull();
        assertThat(members.findById(own.getId())).isEmpty();
    }
    @Test void missingLinkReturnsContract404OnEveryMeRoute() throws Exception {
        String token=token(target,home,Role.RoleName.MEMBRO);
        for (String path:List.of("/me/member","/me/role-history","/me/contributions")) {
            mvc.perform(get(path).header("Authorization",token)).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Sua conta ainda não está ligada a uma ficha de membro. Procure a secretaria."));
        }
    }
    @Test void historyAndContributionsOnlyContainOwnersRecordsAndAreNewestFirst() throws Exception {
        Member own=member(home), another=member(home); attach(target,own);
        addHistory(own,"Antigo",LocalDate.of(2024,1,1)); addHistory(own,"Atual",LocalDate.of(2025,1,1));
        addHistory(another,"Privado",LocalDate.of(2026,1,1));
        addContribution(own,false,LocalDate.of(2024,1,1),"25.00");
        var recent=addContribution(own,false,LocalDate.of(2025,1,1),"75.00");
        addContribution(another,false,LocalDate.of(2026,1,1),"999.00");
        addContribution(null,true,LocalDate.of(2026,1,1),"500.00");
        String token=token(target,home,Role.RoleName.MEMBRO);
        mvc.perform(get("/me/contributions").param("memberId",another.getId().toString()).header("Authorization",token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].id").value(recent.getId().toString()))
                .andExpect(jsonPath("$[0].memberId").value(own.getId().toString())).andExpect(jsonPath("$[0].amount").value(75.0));
        mvc.perform(get("/me/role-history").header("Authorization",token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].role").value("Atual"));
    }
    @Test void openApiDocumentsRoutesTagsAndNewFields() throws Exception {
        var result=mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        var doc=json.readTree(result.getResponse().getContentAsString());
        for (String path:List.of("/me/member","/me/role-history","/me/contributions")) {
            assertThat(doc.at("/paths/"+path.replace("/","~1")+"/get/tags/0").asText()).isEqualTo("me-controller");
        }
        var link=doc.path("paths").path("/users/{id}/member");
        assertThat(link.path("put").path("tags").get(0).asText()).isEqualTo("user-management-controller");
        assertThat(link.has("delete")).isTrue();
        var schemas=doc.path("components").path("schemas");
        for(String schema:List.of("UserSummaryResponse","UserDetailResponse")) {
            assertThat(schemas.path(schema).path("properties").has("memberId")).isTrue();
            assertThat(schemas.path(schema).path("properties").has("memberName")).isTrue();
        }
        assertThat(schemas.path("CreateUserRequest").path("properties").has("memberId")).isTrue();
        assertThat(schemas.path("MemberResponse").path("properties").has("userId")).isTrue();
        assertThat(schemas.path("MemberResponse").path("properties").has("userStatus")).isTrue();
        assertThat(schemas.path("LinkMemberRequest").path("required").toString()).contains("memberId");
        assertThat(schemas.path("MyMemberResponse").path("properties").has("cpf")).isFalse();
        assertThat(schemas.path("MyMemberResponse").path("properties").has("rg")).isFalse();
    }
    void addHistory(Member member,String name,LocalDate date) {
        var entry=MemberRoleHistory.builder().member(member).role(name).startedAt(date).build();
        entry.setTenantId(member.getTenantId()); history.saveAndFlush(entry);
    }
    Contribution addContribution(Member member,boolean anonymous,LocalDate date,String amount) {
        var account=accounts.findAll().stream().filter(a -> a.getCode().equals("REC-001")).findFirst().orElseThrow();
        var transaction=FinancialTransaction.builder().account(account).type(TransactionType.RECEITA)
                .amount(new BigDecimal(amount)).transactionDate(date).build();
        transaction.setTenantId(home.getId()); transactions.saveAndFlush(transaction);
        var contribution=Contribution.builder().member(member).anonymous(anonymous).transaction(transaction).build();
        contribution.setTenantId(home.getId()); return contributions.saveAndFlush(contribution);
    }
}
