package br.com.bitabit.ecclesiacontrol.finance.repository;

import br.com.bitabit.ecclesiacontrol.finance.domain.FinancialTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, UUID> {
    Page<FinancialTransaction> findByTransactionDateBetween(LocalDate start, LocalDate end, Pageable pageable);
}