package br.com.bitabit.ecclesiacontrol.member.repository;

import br.com.bitabit.ecclesiacontrol.core.repository.TenantScopedRepository;
import br.com.bitabit.ecclesiacontrol.member.domain.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MemberRepository extends TenantScopedRepository<Member, UUID> {

    Page<Member> findByMembershipStatus(Member.MembershipStatus status, Pageable pageable);

}