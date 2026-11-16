package br.com.bitabit.ecclesiacontrol.finance.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class ContributionDeclarationResponse {
    UUID id;
    BigDecimal amount;
    LocalDate declaredDate;
    String accountName;
    String paymentMethod;
    String pixReference;
    String note;
    String status;
    String rejectionReason;
    LocalDateTime createdAt;
    LocalDateTime decidedAt;
    /** Só preenchidos na visão do tesoureiro. */
    UUID memberId;
    String memberName;
}
