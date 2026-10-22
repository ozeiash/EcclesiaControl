package br.com.bitabit.ecclesiacontrol.finance.service;

import br.com.bitabit.ecclesiacontrol.core.exception.BusinessRuleException;
import br.com.bitabit.ecclesiacontrol.core.exception.ResourceNotFoundException;
import br.com.bitabit.ecclesiacontrol.core.security.TenantGuard;
import br.com.bitabit.ecclesiacontrol.core.service.AuditActor;
import br.com.bitabit.ecclesiacontrol.core.service.AuditService;
import br.com.bitabit.ecclesiacontrol.core.util.TenantContext;
import br.com.bitabit.ecclesiacontrol.finance.domain.*;
import br.com.bitabit.ecclesiacontrol.finance.dto.ContributionRequest;
import br.com.bitabit.ecclesiacontrol.finance.dto.ContributionResponse;
import br.com.bitabit.ecclesiacontrol.finance.dto.IdentifyMemberRequest;
import br.com.bitabit.ecclesiacontrol.finance.repository.ContributionRepository;
import br.com.bitabit.ecclesiacontrol.member.domain.Member;
import br.com.bitabit.ecclesiacontrol.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContributionService {

    private static final Set<String> CONTRIBUTION_CATEGORIES = Set.of("DIZIMO", "OFERTA", "MISSOES");

    private final ContributionRepository contributionRepository;
    private final MemberRepository memberRepository;
    private final FinancialTransactionService transactionService;
    private final TenantGuard tenantGuard;
    private final AuditService auditService;
    private final PeriodGuard periodGuard;

    @Transactional
    public ContributionResponse create(ContributionRequest request, AuditActor actor) {
        ChartOfAccount account = transactionService.findAccountOrThrow(request.getAccountId());

        if (!CONTRIBUTION_CATEGORIES.contains(account.getCategory())) {
            throw new BusinessRuleException("Conta informada não é uma categoria de contribuição (dízimo/oferta)");
        }
        // Dízimo sem contribuinte identificado é permitido de propósito (operação do caixa);
        // a identificação pode ser feita depois via identifyMember.
        if (!request.isAnonymous() && request.getMemberId() == null) {
            throw new BusinessRuleException("Informe o membro ou marque a contribuição como anônima");
        }

        Member member = null;
        if (!request.isAnonymous()) {
            member = memberRepository.findById(request.getMemberId())
                    .orElseThrow(() -> new ResourceNotFoundException("Membro não encontrado"));
            tenantGuard.requireSameTenant(member, "Membro");
        }

        FinancialTransaction transaction = FinancialTransaction.builder()
                .account(account)
                .type(TransactionType.RECEITA)
                .amount(request.getAmount())
                .transactionDate(request.getTransactionDate())
                .description(request.getDescription())
                .createdBy(actor != null ? actor.id() : null)
                .build();
        transaction.setTenantId(TenantContext.getTenantId());
        transaction = transactionService.save(transaction);

        Contribution contribution = Contribution.builder()
                .transaction(transaction)
                .member(member)
                .anonymous(request.isAnonymous())
                .build();
        contribution.setTenantId(TenantContext.getTenantId());
        Contribution saved = contributionRepository.save(contribution);

        auditService.log(actor, TenantContext.getTenantId(), "CREATE_CONTRIBUTION", "Contribution",
                saved.getId(), null,
                java.util.Map.of("memberId", member != null ? member.getId() : "NAO_IDENTIFICADO",
                        "amount", request.getAmount()));

        return toResponse(saved, true);
    }

    @Transactional
    public ContributionResponse identifyMember(UUID contributionId, IdentifyMemberRequest request, AuditActor actor) {
        Contribution contribution = contributionRepository.findById(contributionId)
                .orElseThrow(() -> new ResourceNotFoundException("Contribuição não encontrada"));
        tenantGuard.requireSameTenant(contribution, "Contribuição");

        periodGuard.requireOpen(contribution.getTenantId(), contribution.getTransaction().getTransactionDate());

        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new ResourceNotFoundException("Membro não encontrado"));
        tenantGuard.requireSameTenant(member, "Membro");

        Object previous = contribution.getMember() != null
                ? contribution.getMember().getId() : "NAO_IDENTIFICADO";

        contribution.setMember(member);
        contribution.setAnonymous(false);
        Contribution saved = contributionRepository.save(contribution);

        // Trocar quem contribuiu é sensível: o antes e o depois ficam no log
        auditService.log(actor, TenantContext.getTenantId(), "IDENTIFY_CONTRIBUTION_MEMBER", "Contribution",
                saved.getId(), previous, member.getId());

        return toResponse(saved, true);
    }

    @Transactional(readOnly = true)
    public Page<ContributionResponse> findAll(Pageable pageable, boolean canViewDetail) {
        return contributionRepository.findAll(pageable).map(c -> toResponse(c, canViewDetail));
    }

    @Transactional(readOnly = true)
    public List<ContributionResponse> findByMember(UUID memberId, boolean canViewDetail) {
        if (!canViewDetail) {
            throw new BusinessRuleException("Sem permissão para ver contribuições individuais");
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Membro não encontrado"));
        return contributionRepository.findByMember(member).stream()
                .map(c -> toResponse(c, true)).toList();
    }

    private ContributionResponse toResponse(Contribution c, boolean canViewDetail) {
        var builder = ContributionResponse.builder()
                .id(c.getId())
                .amount(c.getTransaction().getAmount())
                .transactionDate(c.getTransaction().getTransactionDate())
                .accountName(c.getTransaction().getAccount().getName())
                .anonymous(c.isAnonymous());

        if (canViewDetail && !c.isAnonymous() && c.getMember() != null) {
            builder.memberId(c.getMember().getId());
            builder.memberName(c.getMember().getFullName());
        }
        return builder.build();
    }
}