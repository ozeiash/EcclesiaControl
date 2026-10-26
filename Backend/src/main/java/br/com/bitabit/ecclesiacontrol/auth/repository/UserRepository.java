package br.com.bitabit.ecclesiacontrol.auth.repository;

import br.com.bitabit.ecclesiacontrol.auth.domain.User;
import br.com.bitabit.ecclesiacontrol.core.util.TenantContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("SELECT u FROM User u WHERE u.email = ?1")
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.tenant.id = ?1")
    List<User> findAllByTenant(UUID tenantId);

    Optional<User> findByMemberId(UUID memberId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findForMemberLink(@Param("id") UUID id);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    @Query(value = "select u from User u where (:filialId is null or u.tenant.id = :filialId or exists (select 1 from UserFilialRole r where r.user = u and r.filial.id = :filialId)) and (:status is null or u.status = :status)",
           countQuery = "select count(u) from User u where (:filialId is null or u.tenant.id = :filialId or exists (select 1 from UserFilialRole r where r.user = u and r.filial.id = :filialId)) and (:status is null or u.status = :status)")
    Page<User> findManagedUsers(@Param("filialId") UUID filialId,
            @Param("status") User.UserStatus status, Pageable pageable);

    default List<User> findAllWithTenant() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new RuntimeException("TenantContext not set");
        }
        return findAllByTenant(tenantId);
    }
}
