package br.com.bitabit.ecclesiacontrol.member.repository;

import br.com.bitabit.ecclesiacontrol.member.domain.Member;
import br.com.bitabit.ecclesiacontrol.member.domain.MemberRoleHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MemberRoleHistoryRepository extends JpaRepository<MemberRoleHistory, UUID> {
    List<MemberRoleHistory> findByMemberOrderByStartedAtDesc(Member member);
}