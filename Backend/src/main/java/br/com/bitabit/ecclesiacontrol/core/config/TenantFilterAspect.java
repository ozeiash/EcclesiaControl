package br.com.bitabit.ecclesiacontrol.core.config;

import br.com.bitabit.ecclesiacontrol.core.util.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
public class TenantFilterAspect {

    @PersistenceContext
    private EntityManager entityManager;

    @Before("@annotation(org.springframework.transaction.annotation.Transactional)")
    public void applyTenantFilter() {
        if (TenantContext.isGlobalScope()) {
            return; // Pastor Sede em contexto GLOBAL: não filtra, vê tudo
        }
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) return; // chamada sem contexto (job interno, etc.)

        Session session = entityManager.unwrap(Session.class);
        session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);
    }
}