package br.com.bitabit.ecclesiacontrol.auth.service;

import br.com.bitabit.ecclesiacontrol.auth.domain.*;
import br.com.bitabit.ecclesiacontrol.auth.dto.*;
import br.com.bitabit.ecclesiacontrol.core.exception.*;
import br.com.bitabit.ecclesiacontrol.core.security.AuthenticatedUser;
import br.com.bitabit.ecclesiacontrol.core.service.AuditActor;
import br.com.bitabit.ecclesiacontrol.core.testsupport.UserMemberIntegrationTest;
import br.com.bitabit.ecclesiacontrol.member.domain.Member;
import br.com.bitabit.ecclesiacontrol.member.dto.MemberStatusUpdateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserMemberLinkTest extends UserMemberIntegrationTest {
    LinkMemberRequest link(Member m) { return LinkMemberRequest.builder().memberId(m.getId()).build(); }
    CreateUserRequest createRequest(UUID memberId) {
        var r = new CreateUserRequest(); r.setEmail(UUID.randomUUID()+"@test.local"); r.setFullName("Novo usuário");
        r.setInitialPassword("password123"); r.setMemberId(memberId); return r;
    }
    @Test void linkDetailListAndIdempotence() throws Exception {
        Member m = member(home);
        String token = token(actor, home, Role.RoleName.SUPER_ADMIN);
        mvc.perform(put("/users/"+target.getId()+"/member").header("Authorization",token).contentType("application/json")
                .content("{\"memberId\":\""+m.getId()+"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.memberId").value(m.getId().toString()))
                .andExpect(jsonPath("$.memberName").value(m.getFullName()));
        global();
        assertThat(management.linkMember(target.getId(),link(m),principal).getMemberId()).isEqualTo(m.getId());
        assertThat(management.getUser(target.getId()).getMemberName()).isEqualTo(m.getFullName());
        assertThat(management.listUsers(null,null,PageRequest.of(0,100)).getContent())
                .filteredOn(u -> u.getId().equals(target.getId())).singleElement()
                .satisfies(u -> { assertThat(u.getMemberId()).isEqualTo(m.getId()); assertThat(u.getMemberName()).isEqualTo(m.getFullName()); });
    }
    @Test void createsLinkedAndInvalidMemberDoesNotCreateAccount() throws Exception {
        Member m=member(home); var valid=createRequest(m.getId());
        var created=management.createUser(valid,AuditActor.from(principal));
        assertThat(users.findById(created.getId()).orElseThrow().getMemberId()).isEqualTo(m.getId());
        var invalid=createRequest(UUID.randomUUID()); long before=users.count();
        String token=token(actor,home,Role.RoleName.SUPER_ADMIN);
        mvc.perform(post("/users").header("Authorization",token).contentType("application/json")
                .content("{\"fullName\":\"Novo\",\"email\":\""+invalid.getEmail()+"\",\"initialPassword\":\"password123\",\"memberId\":\""+invalid.getMemberId()+"\"}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Membro não encontrado"));
        assertThat(users.count()).isEqualTo(before);
        assertThat(users.findByEmail(invalid.getEmail())).isEmpty();
    }
    @Test void rejectsMemberAlreadyClaimedAndAccountAlreadyLinked() {
        Member first=member(home), second=member(home);
        management.linkMember(target.getId(),link(first),principal);
        assertThatThrownBy(() -> management.linkMember(actor.getId(),link(first),principal))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Este membro já está vinculado ao usuário "+target.getEmail());
        assertThatThrownBy(() -> management.linkMember(target.getId(),link(second),principal))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Este usuário já está vinculado a outra ficha; desvincule antes");
        assertThatThrownBy(() -> management.createUser(createRequest(first.getId()),AuditActor.from(principal)))
                .isInstanceOf(BusinessRuleException.class);
    }
    @Test void rejectsDeceasedOrDuplicate() {
        for (var status:List.of(Member.MembershipStatus.FALECIDO,Member.MembershipStatus.DUPLICATE)) {
            Member m=member(home); m.setMembershipStatus(status); members.saveAndFlush(m);
            assertThatThrownBy(() -> management.linkMember(target.getId(),link(m),principal)).isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Não é possível dar acesso a um membro com situação Falecido ou Duplicado");
            assertThatThrownBy(() -> management.createUser(createRequest(m.getId()),AuditActor.from(principal))).isInstanceOf(BusinessRuleException.class);
        }
    }
    @Test void localCannotLinkForeignMemberAndNamesStayHidden() {
        Member foreign=member(other);
        assertThatThrownBy(() -> management.linkMember(target.getId(),link(foreign),principal))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Membro não encontrado");
        global(); management.linkMember(target.getId(),link(foreign),principal);
        local(home);
        assertThat(management.getUser(target.getId()).getMemberId()).isEqualTo(foreign.getId());
        assertThat(management.getUser(target.getId()).getMemberName()).isNull();
        assertThat(management.listUsers(null,null,PageRequest.of(0,100)).getContent()).filteredOn(u -> u.getId().equals(target.getId()))
                .singleElement().satisfies(u -> { assertThat(u.getMemberId()).isEqualTo(foreign.getId()); assertThat(u.getMemberName()).isNull(); });
    }
    @Test void ownAccountCanLinkAndUnlinkAndEmptyLinkIs422() throws Exception {
        Member m=member(home);
        String token=token(actor,home,Role.RoleName.SECRETARIO_FILIAL);
        mvc.perform(delete("/users/"+actor.getId()+"/member").header("Authorization",token)).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Este usuário não está vinculado a uma ficha de membro"));
        mvc.perform(put("/users/"+actor.getId()+"/member").header("Authorization",token).contentType("application/json")
                .content("{\"memberId\":\""+m.getId()+"\"}")).andExpect(status().isOk());
        mvc.perform(delete("/users/"+actor.getId()+"/member").header("Authorization",token)).andExpect(status().isNoContent());
        assertThat(users.findById(actor.getId()).orElseThrow().getMemberId()).isNull();
    }
    @Test void reusesManageabilityAndSuperAdminProtection() {
        Member m=member(home); assign(target,other,Role.RoleName.MEMBRO);
        assertThatThrownBy(() -> management.linkMember(target.getId(),link(m),principal)).isInstanceOf(AccessForbiddenException.class);
        assertThatThrownBy(() -> management.unlinkMember(target.getId(),principal)).isInstanceOf(AccessForbiddenException.class);
        global(); assign(target,home,Role.RoleName.SUPER_ADMIN);
        var nonAdmin=new AuthenticatedUser(actor.getId(),actor.getEmail(),"",true,List.of(new SimpleGrantedAuthority("ROLE_TI_ADMIN")));
        assertThatThrownBy(() -> management.linkMember(target.getId(),link(m),nonAdmin)).isInstanceOf(AccessForbiddenException.class);
    }
    @Test void deceasedMemberDisablesAccessRevokesTokensAndAuditsReason() {
        Member m=member(home); attach(target,m); assign(target,home,Role.RoleName.MEMBRO);
        LoginRequest login=new LoginRequest(); login.setEmail(target.getEmail()); login.setPassword("password123");
        var tokens=auth.authenticate(login);
        var status=new MemberStatusUpdateRequest(); status.setMembershipStatus(Member.MembershipStatus.FALECIDO);
        memberService.updateStatus(m.getId(),status,AuditActor.from(principal));
        assertThat(users.findById(target.getId()).orElseThrow().getStatus()).isEqualTo(User.UserStatus.INACTIVE);
        assertThatThrownBy(() -> auth.authenticate(login)).isInstanceOf(AuthenticationFailedException.class);
        assertThatThrownBy(() -> auth.refreshAccessToken(tokens.getRefreshToken())).isInstanceOf(AuthenticationFailedException.class);
        assertThat(em.createQuery("select a.newValue from AuditLog a where a.entityId = :id and a.action = 'UPDATE_USER_STATUS'",String.class)
                .setParameter("id",target.getId()).getResultList()).anyMatch(json -> json.contains("MEMBER_DECEASED"));
    }
    @Test void accessMetadataRequiresManagePermission() throws Exception {
        Member m=member(home); attach(target,m);
        String admin=token(actor,home,Role.RoleName.SUPER_ADMIN);
        mvc.perform(get("/members/"+m.getId()).header("Authorization",admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(target.getId().toString())).andExpect(jsonPath("$.userStatus").value("ACTIVE"));
        var reader=user(home);
        String readerToken=token(reader,home,Role.RoleName.TESOUREIRO_FILIAL);
        var result=mvc.perform(get("/members/"+m.getId()).header("Authorization",readerToken)).andExpect(status().isOk()).andReturn();
        var body=new com.fasterxml.jackson.databind.ObjectMapper().readTree(result.getResponse().getContentAsString());
        assertThat(body.has("userId")).isTrue(); assertThat(body.get("userId").isNull()).isTrue();
        assertThat(body.has("userStatus")).isTrue(); assertThat(body.get("userStatus").isNull()).isTrue();
    }
    @Test void memberNamesAreFetchedInOneQueryForTheWholePage() {
        attach(target,member(home)); attach(actor,member(home));
        var statistics=em.getEntityManagerFactory().unwrap(org.hibernate.SessionFactory.class).getStatistics();
        boolean enabled=statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true); statistics.clear();
        try {
            management.listUsers(null,null,PageRequest.of(0,100));
            String query=Arrays.stream(statistics.getQueries()).filter(q -> q.contains("m.fullName as fullName")).findFirst().orElseThrow();
            assertThat(statistics.getQueryStatistics(query).getExecutionCount()).isEqualTo(1);
        } finally {
            statistics.setStatisticsEnabled(enabled);
        }
    }
}
