package br.com.bitabit.ecclesiacontrol.member.service;

import br.com.bitabit.ecclesiacontrol.auth.domain.Role;
import br.com.bitabit.ecclesiacontrol.auth.domain.User;
import br.com.bitabit.ecclesiacontrol.auth.domain.UserFilialRole;
import br.com.bitabit.ecclesiacontrol.auth.repository.RoleRepository;
import br.com.bitabit.ecclesiacontrol.auth.repository.UserFilialRoleRepository;
import br.com.bitabit.ecclesiacontrol.auth.repository.UserRepository;
import br.com.bitabit.ecclesiacontrol.auth.service.RefreshTokenService;
import br.com.bitabit.ecclesiacontrol.core.exception.AccessForbiddenException;
import br.com.bitabit.ecclesiacontrol.core.exception.BusinessRuleException;
import br.com.bitabit.ecclesiacontrol.core.exception.ResourceNotFoundException;
import br.com.bitabit.ecclesiacontrol.core.security.AuthenticatedUser;
import br.com.bitabit.ecclesiacontrol.core.service.AuditActor;
import br.com.bitabit.ecclesiacontrol.core.service.AuditService;
import br.com.bitabit.ecclesiacontrol.member.domain.Member;
import br.com.bitabit.ecclesiacontrol.member.dto.ChangePasswordRequest;
import br.com.bitabit.ecclesiacontrol.member.dto.MyProfileResponse;
import br.com.bitabit.ecclesiacontrol.member.dto.PortalAccessResponse;
import br.com.bitabit.ecclesiacontrol.member.dto.UpdateMyProfileRequest;
import br.com.bitabit.ecclesiacontrol.member.repository.MemberRepository;
import br.com.bitabit.ecclesiacontrol.tenant.domain.Tenant;
import br.com.bitabit.ecclesiacontrol.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Portal do membro.
 *
 * Regra de projeto: os endpoints /me NUNCA recebem o id do membro. O membro é descoberto pela conta
 * autenticada (users.member_id), então não existe como pedir o registro de outra pessoa.
 */
@Service
@RequiredArgsConstructor
public class MemberPortalService {

    private static final int MIN_AGE = 18;
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final UserFilialRoleRepository assignments;
    private final RoleRepository roleRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;

    // ------------------------------------------------------------ pela secretaria

    @Transactional
    public PortalAccessResponse grantAccess(UUID memberId, AuthenticatedUser principal) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Membro não encontrado"));

        if (member.getMembershipStatus() != Member.MembershipStatus.ATIVO) {
            throw new BusinessRuleException("Só membros ativos podem ter acesso ao portal");
        }
        if (member.getBirthDate() == null) {
            throw new BusinessRuleException("Informe a data de nascimento do membro antes de liberar o acesso");
        }
        if (Period.between(member.getBirthDate(), LocalDate.now()).getYears() < MIN_AGE) {
            throw new BusinessRuleException("O acesso ao portal é liberado apenas para maiores de 18 anos");
        }
        if (member.getEmail() == null || member.getEmail().isBlank()) {
            throw new BusinessRuleException("Cadastre um e-mail para o membro antes de liberar o acesso");
        }
        String email = member.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByMemberId(memberId)) {
            throw new BusinessRuleException("Este membro já tem acesso ao portal");
        }
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new BusinessRuleException("Já existe um usuário com esse e-mail");
        }

        Tenant filial = tenantRepository.findById(member.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Filial não encontrada"));
        Role role = roleRepository.findByName(Role.RoleName.MEMBRO)
                .orElseThrow(() -> new ResourceNotFoundException("Papel não encontrado"));

        String temporary = randomPassword(12);
        User user = userRepository.save(User.builder()
                .tenant(filial).email(email).fullName(member.getFullName())
                .passwordHash(passwordEncoder.encode(temporary))
                .status(User.UserStatus.ACTIVE).memberId(memberId).build());
        assignments.save(UserFilialRole.builder().user(user).filial(filial).role(role).build());

        // A senha provisória nunca entra no audit_log.
        auditService.log(AuditActor.from(principal), member.getTenantId(), "GRANT_PORTAL_ACCESS", "Member",
                memberId, null, Map.of("userId", user.getId(), "email", email));
        return new PortalAccessResponse(user.getId(), email, temporary);
    }

    @Transactional
    public void revokeAccess(UUID memberId, AuthenticatedUser principal) {
        Member member = memberRepository.findById(memberId)   // aplica o escopo de filial antes de tudo
                .orElseThrow(() -> new ResourceNotFoundException("Membro não encontrado"));
        User user = userRepository.findByMemberId(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Este membro não tem acesso ao portal"));
        suspend(user);
        auditService.log(AuditActor.from(principal), member.getTenantId(), "REVOKE_PORTAL_ACCESS", "Member",
                memberId, Map.of("userId", user.getId()), null);
    }

    /** Chamado quando o membro deixa de ser ATIVO. */
    @Transactional
    public void suspendIfAny(UUID memberId) {
        userRepository.findByMemberId(memberId).ifPresent(this::suspend);
    }

    private void suspend(User user) {
        if (user.getStatus() != User.UserStatus.ACTIVE) return;
        user.setStatus(User.UserStatus.SUSPENDED);
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(user);
    }

    // ------------------------------------------------------------ pelo próprio membro

    @Transactional(readOnly = true)
    public MyProfileResponse myProfile(AuthenticatedUser principal) {
        return toProfile(currentMember(principal));
    }

    @Transactional
    public MyProfileResponse updateMyProfile(UpdateMyProfileRequest r, AuthenticatedUser principal) {
        Member m = currentMember(principal);
        MyProfileResponse before = toProfile(m);

        m.setEmail(blankToNull(r.getEmail() == null ? null : r.getEmail().toLowerCase(Locale.ROOT)));
        m.setPhonePrimary(blankToNull(r.getPhonePrimary()));
        m.setPhoneSecondary(blankToNull(r.getPhoneSecondary()));
        m.setPostalCode(blankToNull(r.getPostalCode()));
        m.setStreet(blankToNull(r.getStreet()));
        m.setNumber(blankToNull(r.getNumber()));
        m.setComplement(blankToNull(r.getComplement()));
        m.setNeighborhood(blankToNull(r.getNeighborhood()));
        m.setCity(blankToNull(r.getCity()));
        m.setState(r.getState() == null ? null : r.getState().toUpperCase(Locale.ROOT));
        memberRepository.save(m);

        MyProfileResponse after = toProfile(m);
        auditService.log(AuditActor.from(principal), m.getTenantId(), "UPDATE_MY_PROFILE", "Member",
                m.getId(), before, after);
        return after;
    }

    @Transactional
    public void changePassword(ChangePasswordRequest r, AuthenticatedUser principal) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new AccessForbiddenException("Conta não encontrada"));
        if (!passwordEncoder.matches(r.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("Senha atual incorreta");
        }
        if (passwordEncoder.matches(r.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("A nova senha deve ser diferente da atual");
        }
        user.setPasswordHash(passwordEncoder.encode(r.getNewPassword()));
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(user);   // encerra as outras sessões
        auditService.log(AuditActor.from(principal), user.getTenant().getId(), "CHANGE_PASSWORD", "User",
                user.getId(), null, Map.of("id", user.getId()));
    }

    // ------------------------------------------------------------ apoio

    private Member currentMember(AuthenticatedUser principal) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new AccessForbiddenException("Conta não encontrada"));
        if (user.getMemberId() == null) {
            throw new AccessForbiddenException("Esta conta não está vinculada a um membro");
        }
        return memberRepository.findById(user.getMemberId())
                .orElseThrow(() -> new AccessForbiddenException("Cadastro de membro indisponível"));
    }

    private MyProfileResponse toProfile(Member m) {
        String filialName = tenantRepository.findById(m.getTenantId()).map(Tenant::getName).orElse(null);
        return MyProfileResponse.builder()
                .id(m.getId()).filialId(m.getTenantId()).filialName(filialName)
                .fullName(m.getFullName()).socialName(m.getSocialName()).birthDate(m.getBirthDate())
                .gender(m.getGender()).maskedCpf(maskCpf(m.getCpf())).email(m.getEmail())
                .phonePrimary(m.getPhonePrimary()).phoneSecondary(m.getPhoneSecondary())
                .postalCode(m.getPostalCode()).street(m.getStreet()).number(m.getNumber())
                .complement(m.getComplement()).neighborhood(m.getNeighborhood()).city(m.getCity())
                .state(m.getState()).membershipStatus(m.getMembershipStatus().name())
                .admissionDate(m.getAdmissionDate()).baptized(m.isBaptized()).baptismDate(m.getBaptismDate())
                .maritalStatus(m.getMaritalStatus()).weddingDate(m.getWeddingDate())
                .spouseName(m.getSpouseName()).fatherName(m.getFatherName()).motherName(m.getMotherName())
                .currentRole(m.getCurrentRole()).build();
    }

    private static String maskCpf(String cpf) {
        if (cpf == null) return null;
        String d = cpf.replaceAll("\\D", "");
        return d.length() == 11 ? "***.***.***-" + d.substring(9) : "***";
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String randomPassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        return sb.toString();
    }
}
