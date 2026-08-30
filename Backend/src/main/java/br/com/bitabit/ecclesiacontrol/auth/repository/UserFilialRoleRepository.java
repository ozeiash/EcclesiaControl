package br.com.bitabit.ecclesiacontrol.auth.repository;

import br.com.bitabit.ecclesiacontrol.auth.domain.User;
import br.com.bitabit.ecclesiacontrol.auth.domain.UserFilialRole;
import br.com.bitabit.ecclesiacontrol.auth.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserFilialRoleRepository extends JpaRepository<UserFilialRole, UUID> {

    /**
     * Buscar todos os papéis de um usuário (em todas as filiais)
     */
    List<UserFilialRole> findByUser(User user);

    /**
     * Buscar papéis de um usuário em uma filial específica
     */
    @Query("SELECT ufr FROM UserFilialRole ufr " +
            "WHERE ufr.user = :user AND ufr.filial.id = :filialId")
    List<UserFilialRole> findByUserAndFilial(@Param("user") User user,
                                             @Param("filialId") UUID filialId);

    /**
     * Buscar um papel específico do usuário em uma filial
     */
    @Query("SELECT ufr FROM UserFilialRole ufr " +
            "WHERE ufr.user = :user AND ufr.filial.id = :filialId AND ufr.role = :role")
    Optional<UserFilialRole> findByUserAndFilialAndRole(@Param("user") User user,
                                                        @Param("filialId") UUID filialId,
                                                        @Param("role") Role role);

    /**
     * Buscar todos os papéis GLOBAL de um usuário
     */
    @Query("SELECT ufr FROM UserFilialRole ufr " +
            "WHERE ufr.user = :user AND ufr.role.scope = 'GLOBAL'")
    List<UserFilialRole> findGlobalRolesByUser(@Param("user") User user);

    /**
     * Buscar todos os papéis LOCAL de um usuário em uma filial
     */
    @Query("SELECT ufr FROM UserFilialRole ufr " +
            "WHERE ufr.user = :user AND ufr.filial.id = :filialId AND ufr.role.scope = 'LOCAL'")
    List<UserFilialRole> findLocalRolesByUserAndFilial(@Param("user") User user,
                                                       @Param("filialId") UUID filialId);

    /**
     * Verificar se usuário tem um papel específico em uma filial
     */
    @Query("SELECT CASE WHEN COUNT(ufr) > 0 THEN true ELSE false END " +
            "FROM UserFilialRole ufr " +
            "WHERE ufr.user = :user AND ufr.filial.id = :filialId AND ufr.role.name = :roleName")
    boolean hasRole(@Param("user") User user,
                    @Param("filialId") UUID filialId,
                    @Param("roleName") Role.RoleName roleName);

    /**
     * Buscar todas as filiais onde um usuário tem acesso
     */
    @Query("SELECT DISTINCT ufr.filial.id FROM UserFilialRole ufr WHERE ufr.user = :user")
    List<UUID> findAllAccessibleFilials(@Param("user") User user);

    /**
     * Contar usuários com um papel específico em uma filial
     */
    @Query("SELECT COUNT(ufr) FROM UserFilialRole ufr " +
            "WHERE ufr.filial.id = :filialId AND ufr.role.name = :roleName")
    long countByFilialAndRole(@Param("filialId") UUID filialId,
                              @Param("roleName") Role.RoleName roleName);

    /**
     * Listar todos os usuários com um papel em uma filial
     */
    @Query("SELECT ufr.user FROM UserFilialRole ufr " +
            "WHERE ufr.filial.id = :filialId AND ufr.role.name = :roleName")
    List<User> findUsersByFilialAndRole(@Param("filialId") UUID filialId,
                                        @Param("roleName") Role.RoleName roleName);

    /**
     * Deletar papéis de um usuário em uma filial específica
     * CORRIGIDO: Usar query explícita ao invés de method name
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM UserFilialRole ufr " +
            "WHERE ufr.user = :user AND ufr.filial.id = :filialId")
    void deleteByUserAndFilial(@Param("user") User user,
                               @Param("filialId") UUID filialId);

    /**
     * Deletar um papel específico de um usuário em uma filial
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM UserFilialRole ufr " +
            "WHERE ufr.user = :user AND ufr.filial.id = :filialId AND ufr.role = :role")
    void deleteByUserAndFilialAndRole(@Param("user") User user,
                                      @Param("filialId") UUID filialId,
                                      @Param("role") Role role);
}