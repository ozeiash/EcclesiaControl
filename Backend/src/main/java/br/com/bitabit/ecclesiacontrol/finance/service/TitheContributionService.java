package br.com.bitabit.ecclesiacontrol.finance.service;

import br.com.bitabit.ecclesiacontrol.core.exception.BusinessRuleException;
import br.com.bitabit.ecclesiacontrol.core.exception.ResourceNotFoundException;
import br.com.bitabit.ecclesiacontrol.core.service.AuditActor;
import br.com.bitabit.ecclesiacontrol.core.service.AuditService;
import br.com.bitabit.ecclesiacontrol.core.util.TenantContext;
import br.com.bitabit.ecclesiacontrol.finance.domain.*;
import br.com.bitabit.ecclesiacontrol.finance.dto.TitheContributionRequest;
import br.com.bitabit.ecclesiacontrol.finance.dto.TitheContributionResponse;
import br.com.bitabit.ecclesiacontrol.finance.repository.TitheContributionRepository;
import br.com.bitabit.ecclesiacontrol.member.domain.Member;
import br.com.bitabit.ecclesiacontrol.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TitheContributionService {

    private final TitheContributionRepository titheRepository;
    private final MemberRepository memberRepository;
    private final FinancialTransactionService transactionService;
    private final AuditService auditService;

    @Transactional
    public TitheContributionResponse create(TitheContributionRequest request, AuditActor actor) {
        ChartOfAccount account = transactionService.findAccountOrThrow(request.getAccountId());

        if (!account.getCategory().equals("DIZIMO") && !account.getCategory().equals("OFERTA")
                && !account.getCategory().equals("MISSOES")) {
            throw new BusinessRuleException("Conta informada não é uma categoria de contribuição (dízimo/oferta)");
        }

        if (!request.isAnonymous() && request.getMemberId() == null) {
            throw new BusinessRuleException("Informe o membro ou marque a contribuição como anônima");
        }

        Member member = null;
        if (!request.isAnonymous()) {
            member = memberRepository.findById(request.getMemberId())
                    .orElseThrow(() -> new ResourceNotFoundException("Membro não encontrado"));
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

        TitheContribution contribution = TitheContribution.builder()
                .transaction(transaction)
                .member(member)
                .anonymous(request.isAnonymous())
                .build();
        contribution.setTenantId(TenantContext.getTenantId());

        // Persiste a transação primeiro (a contribuição depende do id gerado dela)
        transaction = transactionService.save(transaction);
        contribution.setTransaction(transaction);
        TitheContribution saved = titheRepository.save(contribution);

        // Log de auditoria SEMPRE registra o vínculo completo — é aqui, e só
        // aqui (visível apenas a quem tem AUDIT_READ), que o rastro fica.
        auditService.log(actor, TenantContext.getTenantId(), "CREATE_TITHE_CONTRIBUTION",
                "TitheContribution", saved.getId(), null,
                java.util.Map.of("memberId", member != null ? member.getId() : "ANONIMO", "amount", request.getAmount()));

        return toResponse(saved, true); // quem lança sempre vê o próprio lançamento completo
    }

    @Transactional(readOnly = true)
    public Page<TitheContributionResponse> findAll(Pageable pageable, boolean canViewDetail) {
        return titheRepository.findAll(pageable).map(t -> toResponse(t, canViewDetail));
    }

    @Transactional(readOnly = true)
    public java.util.List<TitheContributionResponse> findByMember(UUID memberId, boolean canViewDetail) {
        if (!canViewDetail) {
            throw new BusinessRuleException("Sem permissão para ver contribuições individuais");
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Membro não encontrado"));
        return titheRepository.findByMember(member).stream()
                .map(t -> toResponse(t, true)).toList();
    }

    private TitheContributionResponse toResponse(TitheContribution t, boolean canViewDetail) {
        var builder = TitheContributionResponse.builder()
                .id(t.getId())
                .amount(t.getTransaction().getAmount())
                .transactionDate(t.getTransaction().getTransactionDate())
                .accountName(t.getTransaction().getAccount().getName())
                .anonymous(t.isAnonymous());

        if (canViewDetail && !t.isAnonymous() && t.getMember() != null) {
            builder.memberId(t.getMember().getId());
            builder.memberName(t.getMember().getFullName());
        }

        return builder.build();
    }
}