package br.com.bitabit.ecclesiacontrol.finance.service;

import br.com.bitabit.ecclesiacontrol.core.exception.BusinessRuleException;
import br.com.bitabit.ecclesiacontrol.core.exception.ResourceNotFoundException;
import br.com.bitabit.ecclesiacontrol.core.service.AuditActor;
import br.com.bitabit.ecclesiacontrol.core.service.AuditService;
import br.com.bitabit.ecclesiacontrol.core.util.TenantContext;
import br.com.bitabit.ecclesiacontrol.finance.domain.*;
import br.com.bitabit.ecclesiacontrol.finance.dto.*;
import br.com.bitabit.ecclesiacontrol.finance.repository.AccountsPayableRepository;
import br.com.bitabit.ecclesiacontrol.finance.repository.AccountsPayableSettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountsPayableService {

    private final AccountsPayableRepository payableRepository;
    private final AccountsPayableSettlementRepository settlementRepository;
    private final FinancialTransactionService transactionService;
    private final AuditService auditService;

    @Transactional
    public AccountsPayableResponse create(CreateAccountsPayableRequest request, AuditActor actor) {
        ChartOfAccount account = transactionService.findAccountOrThrow(request.getAccountId());

        AccountsPayable payable = AccountsPayable.builder()
                .account(account)
                .payeeName(request.getPayeeName())
                .amount(request.getAmount())
                .dueDate(request.getDueDate())
                .expenseKind(request.getExpenseKind())
                .createdBy(actor != null ? actor.id() : null)
                .build();
        payable.setTenantId(TenantContext.getTenantId());

        AccountsPayable saved = payableRepository.save(payable);

        auditService.log(actor, TenantContext.getTenantId(), "CREATE_ACCOUNTS_PAYABLE",
                "AccountsPayable", saved.getId(), null, request.getPayeeName());

        return toResponse(saved);
    }

    @Transactional
    public AccountsPayableResponse settle(UUID payableId, SettlePayableRequest request, AuditActor actor) {
        AccountsPayable payable = payableRepository.findById(payableId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta a pagar não encontrada"));

        if (payable.getStatus() == PayableStatus.PAGO || payable.getStatus() == PayableStatus.CANCELADO) {
            throw new BusinessRuleException("Não é possível dar baixa numa conta já paga ou cancelada");
        }

        BigDecimal alreadySettled = settlementRepository.sumSettledAmount(payableId);
        BigDecimal remaining = payable.getAmount().subtract(alreadySettled);

        if (request.getAmount().compareTo(remaining) > 0) {
            throw new BusinessRuleException(
                    "Valor da baixa (" + request.getAmount() + ") excede o saldo restante (" + remaining + ")");
        }

        // Gera o lançamento financeiro (DESPESA) e a baixa na mesma transação —
        // exatamente a regra de negócio combinada: baixa sempre gera lançamento.
        FinancialTransaction transaction = FinancialTransaction.builder()
                .account(payable.getAccount())
                .type(TransactionType.DESPESA)
                .amount(request.getAmount())
                .transactionDate(request.getTransactionDate())
                .description(request.getDescription() != null ? request.getDescription() : "Baixa: " + payable.getPayeeName())
                .attachmentUrl(request.getAttachmentUrl())
                .createdBy(actor != null ? actor.id() : null)
                .build();
        transaction.setTenantId(TenantContext.getTenantId());
        transaction = transactionService.save(transaction);

        AccountsPayableSettlement settlement = AccountsPayableSettlement.builder()
                .accountsPayable(payable)
                .financialTransaction(transaction)
                .amount(request.getAmount())
                .build();
        settlement.setTenantId(TenantContext.getTenantId());
        settlementRepository.save(settlement);

        BigDecimal newSettledTotal = alreadySettled.add(request.getAmount());
        payable.setStatus(newSettledTotal.compareTo(payable.getAmount()) >= 0
                ? PayableStatus.PAGO : PayableStatus.PARCIAL);
        AccountsPayable saved = payableRepository.save(payable);

        auditService.log(actor, TenantContext.getTenantId(), "SETTLE_ACCOUNTS_PAYABLE",
                "AccountsPayable", saved.getId(), null,
                java.util.Map.of("settledAmount", request.getAmount(), "newStatus", saved.getStatus()));

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<AccountsPayableResponse> findAll(PayableStatus status, Pageable pageable) {
        Page<AccountsPayable> page = status != null
                ? payableRepository.findByStatus(status, pageable)
                : payableRepository.findAll(pageable);
        return page.map(this::toResponse);
    }

    private AccountsPayableResponse toResponse(AccountsPayable p) {
        BigDecimal settled = settlementRepository.sumSettledAmount(p.getId());
        return AccountsPayableResponse.builder()
                .id(p.getId())
                .accountName(p.getAccount().getName())
                .payeeName(p.getPayeeName())
                .amount(p.getAmount())
                .settledAmount(settled)
                .remainingAmount(p.getAmount().subtract(settled))
                .dueDate(p.getDueDate())
                .expenseKind(p.getExpenseKind().name())
                .status(p.getStatus().name())
                .build();
    }
}