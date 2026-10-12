package br.com.bitabit.ecclesiacontrol.finance.repository;

import br.com.bitabit.ecclesiacontrol.core.repository.TenantScopedRepository;
import br.com.bitabit.ecclesiacontrol.finance.domain.Contribution;
import br.com.bitabit.ecclesiacontrol.member.domain.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContributionRepository extends TenantScopedRepository<Contribution, UUID> {
    List<Contribution> findByMember(Member member);
}