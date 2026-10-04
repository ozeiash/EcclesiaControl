package br.com.bitabit.ecclesiacontrol.finance.repository;

import br.com.bitabit.ecclesiacontrol.core.repository.TenantScopedRepository;
import br.com.bitabit.ecclesiacontrol.finance.domain.TitheContribution;
import br.com.bitabit.ecclesiacontrol.member.domain.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TitheContributionRepository extends TenantScopedRepository<TitheContribution, UUID> {
    Page<TitheContribution> findAll(Pageable pageable);
    List<TitheContribution> findByMember(Member member);
}