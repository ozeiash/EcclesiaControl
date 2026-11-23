package br.com.bitabit.ecclesiacontrol.member.service;

import br.com.bitabit.ecclesiacontrol.core.exception.BusinessRuleException;
import br.com.bitabit.ecclesiacontrol.member.domain.Member;
import java.time.LocalDate;
import java.time.Period;

public final class PortalMemberEligibility {
    private PortalMemberEligibility() {}

    public static void requireEligible(Member member) {
        if (member.getMembershipStatus() != Member.MembershipStatus.ATIVO)
            throw new BusinessRuleException("Só membros ativos podem ter acesso ao portal");
        if (member.getBirthDate() == null)
            throw new BusinessRuleException("Informe a data de nascimento do membro antes de liberar o acesso");
        if (Period.between(member.getBirthDate(), LocalDate.now()).getYears() < 18)
            throw new BusinessRuleException("O acesso ao portal é liberado apenas para maiores de 18 anos");
        if (member.getEmail() == null || member.getEmail().isBlank())
            throw new BusinessRuleException("Cadastre um e-mail para o membro antes de liberar o acesso");
    }
}
