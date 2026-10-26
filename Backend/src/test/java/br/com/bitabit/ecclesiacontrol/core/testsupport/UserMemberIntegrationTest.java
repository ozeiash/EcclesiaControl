package br.com.bitabit.ecclesiacontrol.core.testsupport;

import br.com.bitabit.ecclesiacontrol.auth.domain.*;
import br.com.bitabit.ecclesiacontrol.auth.repository.*;
import br.com.bitabit.ecclesiacontrol.auth.service.*;
import br.com.bitabit.ecclesiacontrol.core.security.*;
import br.com.bitabit.ecclesiacontrol.core.util.TenantContext;
import br.com.bitabit.ecclesiacontrol.member.domain.Member;
import br.com.bitabit.ecclesiacontrol.member.repository.MemberRepository;
import br.com.bitabit.ecclesiacontrol.member.service.MemberService;
import br.com.bitabit.ecclesiacontrol.tenant.domain.Tenant;
import br.com.bitabit.ecclesiacontrol.tenant.repository.TenantRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@AutoConfigureMockMvc
@Transactional
public abstract class UserMemberIntegrationTest extends AbstractIntegrationTest {
    @Autowired protected UserManagementService management;
    @Autowired protected AuthService auth;
    @Autowired protected RefreshTokenService refresh;
    @Autowired protected UserRepository users;
    @Autowired protected UserFilialRoleRepository assignments;
    @Autowired protected RoleRepository roles;
    @Autowired protected TenantRepository tenants;
    @Autowired protected MemberRepository members;
    @Autowired protected MemberService memberService;
    @Autowired protected JwtTokenProvider jwt;
    @Autowired protected PasswordEncoder encoder;
    @Autowired protected MockMvc mvc;
    @Autowired protected EntityManager em;
    protected Tenant home, other;
    protected User actor, target;
    protected AuthenticatedUser principal;
    private static String passwordHash;

    @BeforeEach void setupLinkFixture() {
        if (passwordHash == null) passwordHash = encoder.encode("password123");
        home = tenants.save(Tenant.builder().name("Filial A").type(Tenant.TenantType.FILIAL).build());
        other = tenants.save(Tenant.builder().name("Filial B").type(Tenant.TenantType.FILIAL).build());
        actor = user(home); target = user(home);
        principal = new AuthenticatedUser(actor.getId(), actor.getEmail(), actor.getPasswordHash(), true,
                List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));
        local(home);
    }
    @AfterEach void clearLinkFixture() {
        TenantContext.clear(); SecurityContextHolder.clearContext();
        em.unwrap(Session.class).disableFilter("tenantFilter");
    }
    protected void local(Tenant filial) {
        TenantContext.setTenantId(filial.getId()); TenantContext.setGlobalScope(false);
        em.unwrap(Session.class).disableFilter("tenantFilter");
    }
    protected void global() {
        TenantContext.setTenantId(home.getId()); TenantContext.setGlobalScope(true);
        em.unwrap(Session.class).disableFilter("tenantFilter");
    }
    protected User user(Tenant tenant) {
        return users.saveAndFlush(User.builder().tenant(tenant).email(UUID.randomUUID()+"@test.local")
                .fullName("Conta teste").status(User.UserStatus.ACTIVE).passwordHash(passwordHash).build());
    }
    protected Member member(Tenant tenant) {
        Member member = Member.builder().fullName("Membro " + UUID.randomUUID()).cpf(String.format("%011d", Math.abs(UUID.randomUUID().getLeastSignificantBits() % 100000000000L))).rg("1234567").build();
        member.setTenantId(tenant.getId()); return members.saveAndFlush(member);
    }
    protected void assign(User user, Tenant filial, Role.RoleName name) {
        assignments.saveAndFlush(UserFilialRole.builder().user(user).filial(filial)
                .role(roles.findByName(name).orElseThrow()).build());
    }
    protected String token(User user, Tenant filial, Role.RoleName name) {
        assign(user, filial, name);
        var scope = roles.findByName(name).orElseThrow().getScope();
        return "Bearer " + jwt.generateToken(user.getEmail(), filial.getId(), scope);
    }
    protected void attach(User user, Member member) {
        user.setMemberId(member.getId()); users.saveAndFlush(user);
    }
}
