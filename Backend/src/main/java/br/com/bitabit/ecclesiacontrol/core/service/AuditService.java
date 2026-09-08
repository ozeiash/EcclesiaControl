package br.com.bitabit.ecclesiacontrol.core.service;

import br.com.bitabit.ecclesiacontrol.auth.domain.User;
import br.com.bitabit.ecclesiacontrol.core.domain.AuditLog;
import br.com.bitabit.ecclesiacontrol.core.repository.AuditLogRepository;
import br.com.bitabit.ecclesiacontrol.core.util.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void log(User actor, UUID tenantId, String action, String entityType, UUID entityId,
                    Object oldValue, Object newValue) {
        try {
            HttpServletRequest request = currentRequest();

            AuditLog entry = AuditLog.builder()
                    .user(actor)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .oldValue(toJsonOrNull(oldValue))
                    .newValue(toJsonOrNull(newValue))
                    .ipAddress(request != null ? request.getRemoteAddr() : null)
                    .userAgent(request != null ? request.getHeader("User-Agent") : null)
                    .build();
            entry.setTenantId(tenantId); // ← agora vem explícito do chamador, não do TenantContext

            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Falha ao registrar auditoria: action={}, entityType={}, entityId={}",
                    action, entityType, entityId, e);
        }
    }

    private String toJsonOrNull(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("Falha ao serializar valor de auditoria", e);
            return null;
        }
    }

    private HttpServletRequest currentRequest() {
        var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }
}