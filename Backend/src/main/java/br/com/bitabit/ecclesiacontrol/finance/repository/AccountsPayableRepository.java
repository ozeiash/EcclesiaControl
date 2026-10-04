package br.com.bitabit.ecclesiacontrol.finance.repository;

import br.com.bitabit.ecclesiacontrol.core.repository.TenantScopedRepository;
import br.com.bitabit.ecclesiacontrol.finance.domain.AccountsPayable;
import br.com.bitabit.ecclesiacontrol.finance.domain.PayableStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountsPayableRepository extends TenantScopedRepository<AccountsPayable, UUID> {
    Page<AccountsPayable> findByStatus(PayableStatus status, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from AccountsPayable p where p.id = :id")
    Optional<AccountsPayable> findByIdForUpdate(@Param("id") UUID id);
}