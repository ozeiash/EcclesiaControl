package br.com.bitabit.ecclesiacontrol.core.util;

import java.util.UUID;

public class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> GLOBAL_SCOPE = new ThreadLocal<>();

    public static void setTenantId(UUID tenantId) { CURRENT_TENANT.set(tenantId); }
    public static UUID getTenantId() { return CURRENT_TENANT.get(); }

    public static void setGlobalScope(boolean isGlobal) { GLOBAL_SCOPE.set(isGlobal); }
    public static boolean isGlobalScope() { return Boolean.TRUE.equals(GLOBAL_SCOPE.get()); }

    public static void clear() {
        CURRENT_TENANT.remove();
        GLOBAL_SCOPE.remove();
    }
}