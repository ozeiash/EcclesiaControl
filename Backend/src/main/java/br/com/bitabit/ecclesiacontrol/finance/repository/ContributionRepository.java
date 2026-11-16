package br.com.bitabit.ecclesiacontrol.finance.repository;

import br.com.bitabit.ecclesiacontrol.core.repository.TenantScopedRepository;
import br.com.bitabit.ecclesiacontrol.finance.domain.Contribution;
import br.com.bitabit.ecclesiacontrol.member.domain.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContributionRepository extends TenantScopedRepository<Contribution, UUID> {
    List<Contribution> findByMember(Member member);

    @Query(value = "select c from Contribution c join fetch c.transaction t join fetch t.account "
            + "where c.member = :member order by t.transactionDate desc, c.createdAt desc",
            countQuery = "select count(c) from Contribution c where c.member = :member")
    Page<Contribution> findPageByMember(@Param("member") Member member, Pageable pageable);
}