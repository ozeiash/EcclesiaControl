package br.com.bitabit.ecclesiacontrol.member.service;

import br.com.bitabit.ecclesiacontrol.auth.repository.UserRepository;
import br.com.bitabit.ecclesiacontrol.core.exception.ResourceNotFoundException;
import br.com.bitabit.ecclesiacontrol.core.security.AuthenticatedUser;
import br.com.bitabit.ecclesiacontrol.core.util.TenantContext;
import br.com.bitabit.ecclesiacontrol.finance.domain.Contribution;
import br.com.bitabit.ecclesiacontrol.finance.dto.ContributionResponse;
import br.com.bitabit.ecclesiacontrol.finance.repository.ContributionRepository;
import br.com.bitabit.ecclesiacontrol.member.domain.Member;
import br.com.bitabit.ecclesiacontrol.member.dto.MemberRoleHistoryResponse;
import br.com.bitabit.ecclesiacontrol.member.dto.MyMemberResponse;
import br.com.bitabit.ecclesiacontrol.member.mapper.MemberMapper;
import br.com.bitabit.ecclesiacontrol.member.repository.MemberRepository;
import br.com.bitabit.ecclesiacontrol.member.repository.MemberRoleHistoryRepository;
import br.com.bitabit.ecclesiacontrol.tenant.repository.TenantRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class MyMemberService {
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final MemberRoleHistoryRepository historyRepository;
    private final ContributionRepository contributionRepository;
    private final TenantRepository tenantRepository;
    private final MemberMapper mapper;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public MyMemberResponse member(AuthenticatedUser principal) {
        return forOwnMember(principal, member -> mapper.toMyResponse(member,
                tenantRepository.findById(member.getTenantId()).orElseThrow(
                        () -> new ResourceNotFoundException("Filial não encontrada")).getName()));
    }

    @Transactional(readOnly = true)
    public List<MemberRoleHistoryResponse> roleHistory(AuthenticatedUser principal) {
        return forOwnMember(principal, member -> historyRepository.findByMemberOrderByStartedAtDesc(member)
                .stream().map(h -> MemberRoleHistoryResponse.builder().id(h.getId()).role(h.getRole())
                        .startedAt(h.getStartedAt()).endedAt(h.getEndedAt()).build()).toList());
    }

    @Transactional(readOnly = true)
    public List<ContributionResponse> contributions(AuthenticatedUser principal) {
        return forOwnMember(principal, member -> contributionRepository.findByMember(member).stream()
                .sorted(Comparator.comparing((Contribution c) -> c.getTransaction().getTransactionDate()).reversed()
                        .thenComparing(Contribution::getCreatedAt, Comparator.reverseOrder()))
                .map(c -> ContributionResponse.builder().id(c.getId()).amount(c.getTransaction().getAmount())
                        .transactionDate(c.getTransaction().getTransactionDate())
                        .accountName(c.getTransaction().getAccount().getName()).anonymous(c.isAnonymous())
                        .memberId(member.getId()).memberName(member.getFullName()).build()).toList());
    }

    private <T> T forOwnMember(AuthenticatedUser principal, Function<Member, T> read) {
        UUID memberId = userRepository.findById(principal.getUserId())
                .map(user -> user.getMemberId()).orElseThrow(this::notLinked);
        Session session = entityManager.unwrap(Session.class);
        boolean wasEnabled = session.getEnabledFilter("tenantFilter") != null;
        UUID activeTenantId = TenantContext.getTenantId();
        // The member id comes exclusively from the authenticated user's persisted link,
        // never from client input. The token's branch can differ from that member's branch.
        // Only this owner's reads (including history/contributions) bypass the tenant filter.
        // Materialize DTOs inside the scope and restore filtering even on exceptions.
        session.disableFilter("tenantFilter");
        try {
            Member member = memberRepository.findById(memberId).orElseThrow(this::notLinked);
            return read.apply(member);
        } finally {
            session.disableFilter("tenantFilter");
            if (wasEnabled) session.enableFilter("tenantFilter").setParameter("tenantId", activeTenantId);
        }
    }

    private ResourceNotFoundException notLinked() {
        return new ResourceNotFoundException("Sua conta ainda não está ligada a uma ficha de membro. Procure a secretaria.");
    }
}
