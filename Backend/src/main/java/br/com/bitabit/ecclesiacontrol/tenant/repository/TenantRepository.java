package br.com.bitabit.ecclesiacontrol.tenant.repository;

import br.com.bitabit.ecclesiacontrol.tenant.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    /**
     * Buscar tenant por nome
     */
    Optional<Tenant> findByName(String name);

    /**
     * Buscar tenant por CNPJ
     */
    Optional<Tenant> findByCnpj(String cnpj);

    /**
     * Buscar todas as filiais de uma sede
     */
    @Query("SELECT t FROM Tenant t WHERE t.parentTenantId = :parentId AND t.isActive = true")
    List<Tenant> findChildTenants(@Param("parentId") UUID parentId);

    /**
     * Buscar todas as sedes (tenants raiz)
     */
    @Query("SELECT t FROM Tenant t WHERE t.parentTenantId IS NULL AND t.type = 'SEDE' AND t.isActive = true")
    List<Tenant> findAllHeadquarters();

    /**
     * Buscar todos os tenants ativos
     */
    @Query("SELECT t FROM Tenant t WHERE t.isActive = true ORDER BY t.type, t.name")
    List<Tenant> findAllActive();

    /**
     * Buscar tenant por email
     */
    Optional<Tenant> findByEmail(String email);

    /**
     * Contar filiais de uma sede
     */
    @Query("SELECT COUNT(t) FROM Tenant t WHERE t.parentTenantId = :parentId")
    long countChildTenants(@Param("parentId") UUID parentId);

    /**
     * Verificar se um tenant é sede
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END " +
            "FROM Tenant t WHERE t.id = :id AND t.type = 'SEDE'")
    boolean isHeadquarters(@Param("id") UUID id);

    /**
     * Buscar tenant por tipo
     */
    List<Tenant> findByType(Tenant.TenantType type);

    /**
     * Buscar descendentes de um tenant (filiais, subfiliais, etc)
     */
    @Query("SELECT t FROM Tenant t WHERE t.parentTenantId = :parentId OR t.id = :parentId")
    List<Tenant> findTenantAndDescendants(@Param("parentId") UUID parentId);
}