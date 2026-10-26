package br.com.bitabit.ecclesiacontrol.member.service;

import br.com.bitabit.ecclesiacontrol.core.exception.BusinessRuleException;
import br.com.bitabit.ecclesiacontrol.core.exception.ResourceNotFoundException;
import br.com.bitabit.ecclesiacontrol.core.security.CryptUtils;
import br.com.bitabit.ecclesiacontrol.core.service.AuditActor;
import br.com.bitabit.ecclesiacontrol.core.service.AuditService;
import br.com.bitabit.ecclesiacontrol.core.util.TenantContext;
import br.com.bitabit.ecclesiacontrol.member.domain.Member;
import br.com.bitabit.ecclesiacontrol.member.dto.*;
import br.com.bitabit.ecclesiacontrol.member.mapper.MemberMapper;
import br.com.bitabit.ecclesiacontrol.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import br.com.bitabit.ecclesiacontrol.auth.domain.User;
import br.com.bitabit.ecclesiacontrol.auth.repository.UserRepository;
import br.com.bitabit.ecclesiacontrol.auth.service.RefreshTokenService;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Map;
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
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public MemberResponse create(MemberRequest request, AuditActor actor) {
        String normalizedCpf = normalizeCpf(request.getCpf());
        if (normalizedCpf != null) {
            String hash = CryptUtils.hashForLookup(normalizedCpf);
            if (memberRepository.existsByCpfHashAndMembershipStatusNot(hash, Member.MembershipStatus.DUPLICATE)) {
                throw new BusinessRuleException("Já existe um membro cadastrado com esse CPF");
            }
        }

        Member member = new Member();
        mapper.applyRequest(member, request);
        member.setTenantId(TenantContext.getTenantId());
        member.setMembershipStatus(Member.MembershipStatus.ATIVO);

        Member saved = memberRepository.save(member);

        auditService.log(actor, TenantContext.getTenantId(), "CREATE_MEMBER", "Member",
                saved.getId(), null, mapper.toResponse(saved));

        return responseWithAccess(saved);
    }

    private String normalizeCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) return null;
        return cpf.replaceAll("\\D", "");
    }

    @Transactional(readOnly = true)
    public MemberResponse findById(UUID id) {
        return responseWithAccess(findEntityOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<MemberSummaryResponse> findAll(Member.MembershipStatus status, Pageable pageable) {
        Page<Member> page = (status != null)
                ? memberRepository.findByMembershipStatus(status, pageable)
                : memberRepository.findAll(pageable);
        return page.map(mapper::toSummary);
    }

    @Transactional
    public MemberResponse update(UUID id, MemberRequest request, AuditActor actor) {
        Member member = findEntityOrThrow(id);
        MemberResponse before = mapper.toResponse(member);

        String normalizedCpf = normalizeCpf(request.getCpf());
        if (normalizedCpf != null) {
            String hash = CryptUtils.hashForLookup(normalizedCpf);
            if (memberRepository.existsByCpfHashAndMembershipStatusNotAndIdNot(
                    hash, Member.MembershipStatus.DUPLICATE, id)) {
                throw new BusinessRuleException("Já existe um membro cadastrado com esse CPF");
            }
        }

        mapper.applyRequest(member, request);
        Member saved = memberRepository.save(member);

        auditService.log(actor, TenantContext.getTenantId(), "UPDATE_MEMBER", "Member",
                saved.getId(), before, mapper.toResponse(saved));

        return responseWithAccess(saved);
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
        if (saved.getMembershipStatus() == Member.MembershipStatus.FALECIDO) {
            userRepository.findByMemberId(saved.getId()).ifPresent(user -> {
                User.UserStatus previousStatus = user.getStatus();
                user.setStatus(User.UserStatus.INACTIVE);
                userRepository.save(user);
                refreshTokenService.revokeAllForUser(user);
                auditService.log(actor, saved.getTenantId(), "UPDATE_USER_STATUS", "User", user.getId(),
                        Map.of("status", previousStatus),
                        Map.of("status", user.getStatus(), "reason", "MEMBER_DECEASED"));
            });
        }

        auditService.log(actor, TenantContext.getTenantId(), "UPDATE_MEMBER_STATUS", "Member",
                saved.getId(), before, saved.getMembershipStatus());

        return responseWithAccess(saved);
    }

    private MemberResponse responseWithAccess(Member member) {
        MemberResponse response = mapper.toResponse(member);
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("PERM_USER_MANAGE"))) {
            userRepository.findByMemberId(member.getId()).ifPresent(user -> {
                response.setUserId(user.getId());
                response.setUserStatus(user.getStatus().name());
            });
        }
        return response;
    }

    private Member findEntityOrThrow(UUID id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Membro não encontrado"));
    }
}