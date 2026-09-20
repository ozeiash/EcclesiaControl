package br.com.bitabit.ecclesiacontrol.member.service;

import br.com.bitabit.ecclesiacontrol.core.exception.BusinessRuleException;
import br.com.bitabit.ecclesiacontrol.core.exception.ResourceNotFoundException;
import br.com.bitabit.ecclesiacontrol.core.service.AuditActor;
import br.com.bitabit.ecclesiacontrol.core.service.AuditService;
import br.com.bitabit.ecclesiacontrol.core.util.TenantContext;
import br.com.bitabit.ecclesiacontrol.member.domain.Member;
import br.com.bitabit.ecclesiacontrol.member.dto.*;
import br.com.bitabit.ecclesiacontrol.member.mapper.MemberMapper;
import br.com.bitabit.ecclesiacontrol.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberMapper mapper;
    private final AuditService auditService;

    @Transactional
    public MemberResponse create(MemberRequest request, AuditActor actor) {
        Member member = new Member();
        mapper.applyRequest(member, request);
        member.setTenantId(TenantContext.getTenantId());
        member.setMembershipStatus(Member.MembershipStatus.ATIVO);

        Member saved = memberRepository.save(member);

        auditService.log(actor, TenantContext.getTenantId(), "CREATE_MEMBER", "Member",
                saved.getId(), null, mapper.toResponse(saved));

        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public MemberResponse findById(UUID id) {
        return mapper.toResponse(findEntityOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<MemberSummaryResponse> findAll(Pageable pageable) {
        return memberRepository.findAll(pageable).map(mapper::toSummary);
    }

    @Transactional
    public MemberResponse update(UUID id, MemberRequest request, AuditActor actor) {
        Member member = findEntityOrThrow(id);
        MemberResponse before = mapper.toResponse(member);

        mapper.applyRequest(member, request);
        Member saved = memberRepository.save(member);

        auditService.log(actor, TenantContext.getTenantId(), "UPDATE_MEMBER", "Member",
                saved.getId(), before, mapper.toResponse(saved));

        return mapper.toResponse(saved);
    }

    @Transactional
    public MemberResponse updateStatus(UUID id, MemberStatusUpdateRequest request, AuditActor actor) {
        Member member = findEntityOrThrow(id);

        if (member.getMembershipStatus() == Member.MembershipStatus.FALECIDO) {
            throw new BusinessRuleException("Não é possível alterar o status de um membro falecido");
        }

        Member.MembershipStatus before = member.getMembershipStatus();
        member.setMembershipStatus(request.getMembershipStatus());
        member.setExitDate(request.getExitDate());
        member.setExitWay(request.getExitWay());

        Member saved = memberRepository.save(member);

        auditService.log(actor, TenantContext.getTenantId(), "UPDATE_MEMBER_STATUS", "Member",
                saved.getId(), before, saved.getMembershipStatus());

        return mapper.toResponse(saved);
    }

    private Member findEntityOrThrow(UUID id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Membro não encontrado"));
    }
}