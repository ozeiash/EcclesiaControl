package br.com.bitabit.ecclesiacontrol.member.repository;

import br.com.bitabit.ecclesiacontrol.member.domain.Member;
import br.com.bitabit.ecclesiacontrol.tenant.domain.Tenant;
import br.com.bitabit.ecclesiacontrol.tenant.repository.TenantRepository;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class MemberTenantIsolationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TenantRepository tenantRepository;

    private Tenant filialA;
    private Tenant filialB;

    @BeforeEach
    void setUp() {
        filialA = tenantRepository.save(Tenant.builder()
                .name("Filial A")
                .type(Tenant.TenantType.FILIAL)
                .email("filialA@church.com")
                .build());

        filialB = tenantRepository.save(Tenant.builder()
                .name("Filial B")
                .type(Tenant.TenantType.FILIAL)
                .email("filialB@church.com")
                .build());

        Member memberA = Member.builder()
                .fullName("Membro da Filial A")
                .membershipStatus(Member.MembershipStatus.ATIVO)
                .build();
        memberA.setTenantId(filialA.getId());
        memberRepository.save(memberA);

        Member memberB = Member.builder()
                .fullName("Membro da Filial B")
                .membershipStatus(Member.MembershipStatus.ATIVO)
                .build();
        memberB.setTenantId(filialB.getId());
        memberRepository.save(memberB);

        entityManager.flush();
        entityManager.clear();
    }

    @AfterEach
    void tearDown() {
        Session session = entityManager.getEntityManager().unwrap(Session.class);
        if (session.getEnabledFilter("tenantFilter") != null) {
            session.disableFilter("tenantFilter");
        }
    }

    @Test
    void shouldOnlyReturnMembersFromActiveFilial() {
        Session session = entityManager.getEntityManager().unwrap(Session.class);
        session.enableFilter("tenantFilter").setParameter("tenantId", filialA.getId());

        List<Member> visibleMembers = memberRepository.findAll();

        assertThat(visibleMembers)
                .hasSize(1)
                .allMatch(m -> m.getTenantId().equals(filialA.getId()))
                .noneMatch(m -> m.getTenantId().equals(filialB.getId()));
    }

    @Test
    void shouldReturnAllMembersWhenFilterDisabled() {
        // Documenta o comportamento SEM o filtro habilitado — mostra
        // exatamente o cenário de vazamento que os Bugs 1 e 2 originais
        // causavam silenciosamente antes da correção.
        List<Member> allMembers = memberRepository.findAll();

        assertThat(allMembers).hasSize(2);
    }
}