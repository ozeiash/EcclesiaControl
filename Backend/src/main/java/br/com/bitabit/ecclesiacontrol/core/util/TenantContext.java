package br.com.bitabit.ecclesiacontrol.core.util;

import java.util.UUID;

public class TenantContext {
    private static final ThreadLocal<UUID> tenantId = new ThreadLocal<>();

    public static void setTenantId(UUID id) {
        tenantId.set(id);
    }

    public static UUID getTenantId() {
        UUID id = tenantId.get();
        if (id == null) {
            throw new IllegalStateException("Tenant ID not set in context");
        }
        return id;
    }

    public static void clear() {
        tenantId.remove();
    }

    public static boolean isTenantSet() {
        return tenantId.get() != null;
    }
}