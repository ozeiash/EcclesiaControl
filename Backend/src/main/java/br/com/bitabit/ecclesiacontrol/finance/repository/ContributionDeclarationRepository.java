package br.com.bitabit.ecclesiacontrol.finance.repository;

import br.com.bitabit.ecclesiacontrol.finance.domain.ContributionDeclaration;
import br.com.bitabit.ecclesiacontrol.finance.domain.DeclarationStatus;
import br.com.bitabit.ecclesiacontrol.member.domain.Member;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ContributionDeclarationRepository extends JpaRepository<ContributionDeclaration, UUID> {

    /**
     * JPQL (respeita o filtro de filial) com trava de escrita: dois tesoureiros confirmando a mesma
     * declaração ao mesmo tempo viram uma fila, e o segundo encontra a declaração já decidida.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from ContributionDeclaration d where d.id = :id")
    Optional<ContributionDeclaration> findForUpdate(@Param("id") UUID id);

    Page<ContributionDeclaration> findByStatus(DeclarationStatus status, Pageable pageable);

    Page<ContributionDeclaration> findByMember(Member member, Pageable pageable);

    long countByMemberAndStatus(Member member, DeclarationStatus status);

    boolean existsByPixReferenceAndStatusIn(String pixReference, Collection<DeclarationStatus> statuses);
}
