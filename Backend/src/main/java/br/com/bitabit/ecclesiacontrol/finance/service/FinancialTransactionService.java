package br.com.bitabit.ecclesiacontrol.finance.service;

import br.com.bitabit.ecclesiacontrol.core.exception.ResourceNotFoundException;
import br.com.bitabit.ecclesiacontrol.core.security.AuthenticatedUser;
import br.com.bitabit.ecclesiacontrol.core.service.AuditActor;
import br.com.bitabit.ecclesiacontrol.core.service.AuditService;
import br.com.bitabit.ecclesiacontrol.core.util.TenantContext;
import br.com.bitabit.ecclesiacontrol.finance.domain.ChartOfAccount;
import br.com.bitabit.ecclesiacontrol.finance.domain.FinancialTransaction;
import br.com.bitabit.ecclesiacontrol.finance.domain.TransactionType;
import br.com.bitabit.ecclesiacontrol.finance.dto.FinancialTransactionRequest;
import br.com.bitabit.ecclesiacontrol.finance.dto.FinancialTransactionResponse;
import br.com.bitabit.ecclesiacontrol.finance.repository.ChartOfAccountRepository;
import br.com.bitabit.ecclesiacontrol.finance.repository.FinancialTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FinancialTransactionService {

    private final FinancialTransactionRepository transactionRepository;
    private final ChartOfAccountRepository accountRepository;
    private final AuditService auditService;

    @Transactional
    public FinancialTransactionResponse create(FinancialTransactionRequest request, AuditActor actor) {
        ChartOfAccount account = findAccountOrThrow(request.getAccountId());

        FinancialTransaction transaction = FinancialTransaction.builder()
                .account(account)
                .type(account.getType())
                .amount(request.getAmount())
                .transactionDate(request.getTransactionDate())
                .description(request.getDescription())
                .attachmentUrl(request.getAttachmentUrl())
                .createdBy(actor != null ? actor.id() : null)
                .build();
        transaction.setTenantId(TenantContext.getTenantId());

        FinancialTransaction saved = transactionRepository.save(transaction);

        auditService.log(actor, TenantContext.getTenantId(), "CREATE_FINANCIAL_TRANSACTION",
                "FinancialTransaction", saved.getId(), null, toResponse(saved));

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<FinancialTransactionResponse> findAll(Pageable pageable) {
        return transactionRepository.findAll(pageable).map(this::toResponse);
    }

    ChartOfAccount findAccountOrThrow(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conta contábil não encontrada"));
    }

    FinancialTransactionResponse toResponse(FinancialTransaction t) {
        return FinancialTransactionResponse.builder()
                .id(t.getId())
                .accountCode(t.getAccount().getCode())
                .accountName(t.getAccount().getName())
                .type(t.getType().name())
                .amount(t.getAmount())
                .transactionDate(t.getTransactionDate())
                .description(t.getDescription())
                .attachmentUrl(t.getAttachmentUrl())
                .createdAt(t.getCreatedAt())
                .build();
    }

    @Transactional
    public FinancialTransaction save(FinancialTransaction transaction) {
        return transactionRepository.save(transaction);
    }
}