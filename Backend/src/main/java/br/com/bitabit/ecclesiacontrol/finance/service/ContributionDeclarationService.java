package br.com.bitabit.ecclesiacontrol.finance.service;

import br.com.bitabit.ecclesiacontrol.core.exception.BusinessRuleException;
import br.com.bitabit.ecclesiacontrol.core.exception.ResourceNotFoundException;
import br.com.bitabit.ecclesiacontrol.core.security.AuthenticatedUser;
import br.com.bitabit.ecclesiacontrol.core.security.TenantGuard;
import br.com.bitabit.ecclesiacontrol.core.service.AuditActor;
import br.com.bitabit.ecclesiacontrol.core.service.AuditService;
import br.com.bitabit.ecclesiacontrol.finance.domain.ChartOfAccount;
import br.com.bitabit.ecclesiacontrol.finance.domain.ContributionDeclaration;
import br.com.bitabit.ecclesiacontrol.finance.domain.DeclarationStatus;
import br.com.bitabit.ecclesiacontrol.finance.dto.*;
import br.com.bitabit.ecclesiacontrol.finance.repository.ChartOfAccountRepository;
import br.com.bitabit.ecclesiacontrol.finance.repository.ContributionDeclarationRepository;
import br.com.bitabit.ecclesiacontrol.finance.repository.ContributionRepository;
import br.com.bitabit.ecclesiacontrol.member.domain.Member;
import br.com.bitabit.ecclesiacontrol.member.service.MemberPortalService;
import br.com.bitabit.ecclesiacontrol.tenant.domain.Tenant;
import br.com.bitabit.ecclesiacontrol.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Contribuição declarada pelo membro.
 *
 * A declaração fica numa tabela própria e NÃO aparece em lançamentos, fechamento nem dashboards.
 * A confirmação reaproveita ContributionService.create, então PeriodGuard, TenantGuard, regras de
 * categoria e auditoria são exatamente os do caminho do caixa: não existe um segundo jeito de gravar
 * contribuição.
 */
@Service
@RequiredArgsConstructor
public class ContributionDeclarationService {

    private static final int MAX_PENDING_PER_MEMBER = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final Set<DeclarationStatus> ACTIVE_STATUSES =
            Set.of(DeclarationStatus.PENDENTE, DeclarationStatus.CONFIRMADA);

    private final ContributionDeclarationRepository repository;
    private final ContributionRepository contributionRepository;
    private final ChartOfAccountRepository accountRepository;
    private final FinancialTransactionService transactionService;
    private final ContributionService contributionService;
    private final MemberPortalService portalService;
    private final TenantRepository tenantRepository;
    private final TenantGuard tenantGuard;
    private final AuditService auditService;

    // ------------------------------------------------------------ lado do membro

    @Transactional(readOnly = true)
    public List<ChartOfAccountResponse> contributionAccounts() {
        return accountRepository.findByActiveTrue().stream()
                .filter(a -> ContributionService.CONTRIBUTION_CATEGORIES.contains(a.getCategory()))
                .map(a -> ChartOfAccountResponse.builder().id(a.getId()).code(a.getCode()).name(a.getName())
                        .type(a.getType().name()).category(a.getCategory()).build())
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentInfoResponse paymentInfo(AuthenticatedUser principal) {
        Member member = portalService.currentMember(principal);
        Tenant filial = tenantRepository.findById(member.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Filial não encontrada"));
        return new PaymentInfoResponse(filial.getName(), filial.getPixKey());
    }

    @Transactional
    public ContributionDeclarationResponse declare(DeclareContributionRequest r, AuthenticatedUser principal) {
        Member member = portalService.currentMember(principal);
        if (member.getMembershipStatus() != Member.MembershipStatus.ATIVO) {
            throw new BusinessRuleException("Só membros ativos podem declarar contribuições");
        }
        ChartOfAccount account = transactionService.findAccountOrThrow(r.getAccountId());
        if (!ContributionService.CONTRIBUTION_CATEGORIES.contains(account.getCategory())) {
            throw new BusinessRuleException("Escolha uma conta de dízimo, oferta ou missões");
        }
        if (r.getDeclaredDate().isAfter(LocalDate.now())) {
            throw new BusinessRuleException("A data da contribuição não pode ser futura");
        }
        if (repository.countByMemberAndStatus(member, DeclarationStatus.PENDENTE) >= MAX_PENDING_PER_MEMBER) {
            throw new BusinessRuleException("Você já tem " + MAX_PENDING_PER_MEMBER
                    + " declarações aguardando confirmação. Aguarde o tesoureiro antes de enviar outras.");
        }
        String pixRef = blankToNull(r.getPixReference());
        if (pixRef != null && repository.existsByPixReferenceAndStatusIn(pixRef, ACTIVE_STATUSES)) {
            throw new BusinessRuleException("Esse código PIX já foi declarado");
        }

        ContributionDeclaration d = ContributionDeclaration.builder()
                .member(member).account(account).amount(r.getAmount()).declaredDate(r.getDeclaredDate())
                .paymentMethod(r.getPaymentMethod()).pixReference(pixRef).note(blankToNull(r.getNote()))
                .build();
        d.setTenantId(member.getTenantId());
        d = repository.save(d);

        auditService.log(AuditActor.from(principal), d.getTenantId(), "DECLARE_CONTRIBUTION",
                "ContributionDeclaration", d.getId(), null,
                Map.of("amount", d.getAmount(), "accountId", account.getId()));
        return toResponse(d, false);
    }

    @Transactional(readOnly = true)
    public Page<ContributionDeclarationResponse> myDeclarations(Pageable pageable, AuthenticatedUser principal) {
        Member member = portalService.currentMember(principal);
        Pageable fixed = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return repository.findByMember(member, fixed).map(d -> toResponse(d, false));
    }

    @Transactional(readOnly = true)
    public Page<MyContributionResponse> myContributions(Pageable pageable, AuthenticatedUser principal) {
        Member member = portalService.currentMember(principal);
        Pageable fixed = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), MAX_PAGE_SIZE));
        return contributionRepository.findPageByMember(member, fixed)
                .map(c -> new MyContributionResponse(c.getId(), c.getTransaction().getAmount(),
                        c.getTransaction().getTransactionDate(), c.getTransaction().getAccount().getName()));
    }

    @Transactional
    public void cancel(UUID id, AuthenticatedUser principal) {
        Member member = portalService.currentMember(principal);
        ContributionDeclaration d = repository.findForUpdate(id)
                .filter(x -> x.getMember().getId().equals(member.getId()))   // de outro membro: 404, não 403
                .orElseThrow(() -> new ResourceNotFoundException("Declaração não encontrada"));
        requirePending(d);
        d.setStatus(DeclarationStatus.CANCELADA);
        d.setDecidedAt(LocalDateTime.now());
        repository.save(d);
        auditService.log(AuditActor.from(principal), d.getTenantId(), "CANCEL_CONTRIBUTION_DECLARATION",
                "ContributionDeclaration", d.getId(), Map.of("status", "PENDENTE"), Map.of("status", "CANCELADA"));
    }

    // ------------------------------------------------------------ lado do tesoureiro

    @Transactional(readOnly = true)
    public Page<ContributionDeclarationResponse> list(DeclarationStatus status, Pageable pageable) {
        Pageable fixed = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.ASC, "createdAt"));   // mais antigas primeiro: é uma fila
        Page<ContributionDeclaration> page = status != null
                ? repository.findByStatus(status, fixed)
                : repository.findAll(fixed);
        return page.map(d -> toResponse(d, true));
    }

    @Transactional
    public ContributionResponse confirm(UUID id, ConfirmDeclarationRequest request, AuthenticatedUser principal) {
        ContributionDeclaration d = repository.findForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Declaração não encontrada"));
        tenantGuard.requireSameTenant(d, "Declaração");
        requirePending(d);

        LocalDate date = request != null && request.getTransactionDate() != null
                ? request.getTransactionDate() : d.getDeclaredDate();
        if (date.isAfter(LocalDate.now())) {
            throw new BusinessRuleException("A data do lançamento não pode ser futura");
        }

        ContributionRequest cr = new ContributionRequest();
        cr.setAccountId(d.getAccount().getId());
        cr.setAmount(d.getAmount());
        cr.setTransactionDate(date);
        cr.setMemberId(d.getMember().getId());
        cr.setAnonymous(false);
        cr.setDescription(description(d));
        // PeriodGuard roda aqui dentro: mês fechado = 422 e a declaração continua pendente (a transação desfaz tudo).
        ContributionResponse created = contributionService.create(cr, AuditActor.from(principal));

        d.setStatus(DeclarationStatus.CONFIRMADA);
        d.setDecidedBy(principal.getUserId());
        d.setDecidedAt(LocalDateTime.now());
        d.setContributionId(created.getId());
        repository.save(d);

        auditService.log(AuditActor.from(principal), d.getTenantId(), "CONFIRM_CONTRIBUTION_DECLARATION",
                "ContributionDeclaration", d.getId(), Map.of("status", "PENDENTE"),
                Map.of("status", "CONFIRMADA", "contributionId", created.getId(),
                        "declaredDate", d.getDeclaredDate().toString(), "effectiveDate", date.toString()));
        return created;
    }

    @Transactional
    public ContributionDeclarationResponse reject(UUID id, RejectDeclarationRequest request, AuthenticatedUser principal) {
        ContributionDeclaration d = repository.findForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Declaração não encontrada"));
        tenantGuard.requireSameTenant(d, "Declaração");
        requirePending(d);

        d.setStatus(DeclarationStatus.REJEITADA);
        d.setRejectionReason(request.getReason().trim());
        d.setDecidedBy(principal.getUserId());
        d.setDecidedAt(LocalDateTime.now());
        repository.save(d);

        auditService.log(AuditActor.from(principal), d.getTenantId(), "REJECT_CONTRIBUTION_DECLARATION",
                "ContributionDeclaration", d.getId(), Map.of("status", "PENDENTE"),
                Map.of("status", "REJEITADA", "reason", d.getRejectionReason()));
        return toResponse(d, true);
    }

    // ------------------------------------------------------------ apoio

    private void requirePending(ContributionDeclaration d) {
        if (d.getStatus() != DeclarationStatus.PENDENTE) {
            throw new BusinessRuleException("Esta declaração já foi decidida (" + d.getStatus() + ")");
        }
    }

    private String description(ContributionDeclaration d) {
        String base = "Contribuição declarada pelo membro (" + d.getPaymentMethod() + ")";
        return d.getPixReference() != null ? base + " ref. " + d.getPixReference() : base;
    }

    private ContributionDeclarationResponse toResponse(ContributionDeclaration d, boolean withMember) {
        var b = ContributionDeclarationResponse.builder()
                .id(d.getId()).amount(d.getAmount()).declaredDate(d.getDeclaredDate())
                .accountName(d.getAccount().getName()).paymentMethod(d.getPaymentMethod().name())
                .pixReference(d.getPixReference()).note(d.getNote()).status(d.getStatus().name())
                .rejectionReason(d.getRejectionReason()).createdAt(d.getCreatedAt()).decidedAt(d.getDecidedAt());
        if (withMember) {
            b.memberId(d.getMember().getId()).memberName(d.getMember().getFullName());
        }
        return b.build();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
