package br.com.bitabit.ecclesiacontrol.core.security;

import br.com.bitabit.ecclesiacontrol.core.domain.TenantAwareEntity;
import br.com.bitabit.ecclesiacontrol.core.exception.AccessForbiddenException;
import br.com.bitabit.ecclesiacontrol.core.util.TenantContext;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TenantGuard {

    public void requireSameTenant(TenantAwareEntity entity, String label) {
        UUID active = TenantContext.getTenantId();
        if (active == null || !active.equals(entity.getTenantId())) {
            throw new AccessForbiddenException(label + " não pertence à filial ativa");
        }
    }
}