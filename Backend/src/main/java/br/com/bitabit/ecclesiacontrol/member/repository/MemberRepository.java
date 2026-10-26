package br.com.bitabit.ecclesiacontrol.member.repository;

import br.com.bitabit.ecclesiacontrol.core.repository.TenantScopedRepository;
import br.com.bitabit.ecclesiacontrol.member.domain.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;


@Repository
public interface MemberRepository extends TenantScopedRepository<Member, UUID> {

    interface MemberNameView { UUID getId(); String getFullName(); }

    @Query("select m.id as id, m.fullName as fullName from Member m where m.id in :ids")
    List<MemberNameView> findNamesByIdIn(@Param("ids") List<UUID> ids);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.id = :id")
    java.util.Optional<Member> findForUserLink(@Param("id") UUID id);

    interface StatusCountView { Member.MembershipStatus getStatus(); Long getTotal(); }
    interface TenantCountView { UUID getTenantId(); Long getTotal(); }

    Page<Member> findByMembershipStatus(Member.MembershipStatus status, Pageable pageable);
    boolean existsByCpfHashAndMembershipStatusNot(String cpfHash, Member.MembershipStatus status);
    boolean existsByCpfHashAndMembershipStatusNotAndIdNot(String cpfHash, Member.MembershipStatus status, UUID excludedId);
    @Query("select m.membershipStatus as status, count(m) as total from Member m " +
            "where m.membershipStatus <> :excluded group by m.membershipStatus")
    List<StatusCountView> countByStatus(@Param("excluded") Member.MembershipStatus excluded);

    @Query("select m.tenantId as tenantId, count(m) as total from Member m " +
            "where m.membershipStatus = :status group by m.tenantId")
    List<TenantCountView> countByTenant(@Param("status") Member.MembershipStatus status);

    @Query("select count(m) from Member m where m.admissionDate between :start and :end " +
            "and m.membershipStatus <> :excluded")
    long countAdmissions(@Param("start") LocalDate start, @Param("end") LocalDate end,
                         @Param("excluded") Member.MembershipStatus excluded);


}