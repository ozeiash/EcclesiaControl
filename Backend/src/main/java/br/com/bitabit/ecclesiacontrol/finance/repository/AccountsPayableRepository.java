package br.com.bitabit.ecclesiacontrol.finance.repository;

import br.com.bitabit.ecclesiacontrol.finance.domain.AccountsPayable;
import br.com.bitabit.ecclesiacontrol.finance.domain.PayableStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AccountsPayableRepository extends JpaRepository<AccountsPayable, UUID> {
    Page<AccountsPayable> findByStatus(PayableStatus status, Pageable pageable);
}