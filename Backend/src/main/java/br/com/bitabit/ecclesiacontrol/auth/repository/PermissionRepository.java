package br.com.bitabit.ecclesiacontrol.auth.repository;

import br.com.bitabit.ecclesiacontrol.auth.domain.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {
    Optional<Permission> findByCode(String code);
}