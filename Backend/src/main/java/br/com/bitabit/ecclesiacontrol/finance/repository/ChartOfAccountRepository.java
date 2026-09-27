package br.com.bitabit.ecclesiacontrol.finance.repository;

import br.com.bitabit.ecclesiacontrol.finance.domain.ChartOfAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChartOfAccountRepository extends JpaRepository<ChartOfAccount, UUID> {
    List<ChartOfAccount> findByActiveTrue();
}