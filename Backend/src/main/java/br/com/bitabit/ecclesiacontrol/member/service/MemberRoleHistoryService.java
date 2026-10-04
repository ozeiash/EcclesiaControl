package br.com.bitabit.ecclesiacontrol.member.service;

import br.com.bitabit.ecclesiacontrol.core.exception.ResourceNotFoundException;
import br.com.bitabit.ecclesiacontrol.core.service.AuditActor;
import br.com.bitabit.ecclesiacontrol.core.service.AuditService;
import br.com.bitabit.ecclesiacontrol.core.util.TenantContext;
import br.com.bitabit.ecclesiacontrol.member.domain.Member;
import br.com.bitabit.ecclesiacontrol.member.domain.MemberRoleHistory;
import br.com.bitabit.ecclesiacontrol.member.dto.MemberRoleHistoryRequest;
import br.com.bitabit.ecclesiacontrol.member.dto.MemberRoleHistoryResponse;
import br.com.bitabit.ecclesiacontrol.member.repository.MemberRepository;
import br.com.bitabit.ecclesiacontrol.member.repository.MemberRoleHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberRoleHistoryService {

    private final MemberRoleHistoryRepository roleHistoryRepository;
    private final MemberRepository memberRepository;
    private final AuditService auditService;

    @Transactional
    public MemberRoleHistoryResponse addEntry(UUID memberId, MemberRoleHistoryRequest request, AuditActor actor) {
        Member member = findMemberOrThrow(memberId);

        MemberRoleHistory entry = MemberRoleHistory.builder()
                .member(member)
                .role(request.getRole())
                .startedAt(request.getStartedAt())
                .endedAt(request.getEndedAt())
                .build();
        entry.setTenantId(member.getTenantId());

        MemberRoleHistory saved = roleHistoryRepository.save(entry);

        member.setCurrentRole(request.getRole());
        memberRepository.save(member);

        auditService.log(actor, TenantContext.getTenantId(), "ADD_MEMBER_ROLE_HISTORY", "Member",
                member.getId(), null, toResponse(saved));

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<MemberRoleHistoryResponse> findByMember(UUID memberId) {
        Member member = findMemberOrThrow(memberId);
        return roleHistoryRepository.findByMemberOrderByStartedAtDesc(member)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public MemberRoleHistoryResponse closeEntry(UUID memberId, UUID historyId, LocalDate endedAt, AuditActor actor) {
        Member member = findMemberOrThrow(memberId);
        MemberRoleHistory entry = roleHistoryRepository.findById(historyId)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de histórico não encontrado"));

        if (!entry.getMember().getId().equals(member.getId())) {
            throw new ResourceNotFoundException("Registro de histórico não pertence a esse membro");
        }

        entry.setEndedAt(endedAt != null ? endedAt : LocalDate.now());
        MemberRoleHistory saved = roleHistoryRepository.save(entry);

        auditService.log(actor, TenantContext.getTenantId(), "CLOSE_MEMBER_ROLE_HISTORY", "Member",
                member.getId(), null, toResponse(saved));

        return toResponse(saved);
    }

    private Member findMemberOrThrow(UUID id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Membro não encontrado"));
    }

    private MemberRoleHistoryResponse toResponse(MemberRoleHistory h) {
        return MemberRoleHistoryResponse.builder()
                .id(h.getId())
                .role(h.getRole())
                .startedAt(h.getStartedAt())
                .endedAt(h.getEndedAt())
                .build();
    }
}