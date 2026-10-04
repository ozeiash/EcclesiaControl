package br.com.bitabit.ecclesiacontrol.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

@NoRepositoryBean
public interface TenantScopedRepository<T, ID> extends JpaRepository<T, ID> {

    // findById padrão usa em.find (busca por PK), que ignora @Filter do Hibernate.
    // Como query HQL, o filtro de tenant passa a ser aplicado.
    @Override
    @Query("select e from #{#entityName} e where e.id = :id")
    Optional<T> findById(@Param("id") ID id);
}