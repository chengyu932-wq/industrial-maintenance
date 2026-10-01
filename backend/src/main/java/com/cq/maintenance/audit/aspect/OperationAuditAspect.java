package com.cq.maintenance.audit.aspect;

import com.cq.maintenance.audit.mapper.OperationLogMapper;
import com.cq.maintenance.security.LoginUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

@Aspect
@Component
public class OperationAuditAspect {
    private static final Logger log = LoggerFactory.getLogger(OperationAuditAspect.class);
    private static final Set<String> READ = Set.of("GET", "HEAD", "OPTIONS");
    // Only stable identifiers and low-risk state fields are retained. Free text and credentials are excluded.
    private static final Set<String> AUDIT_FIELDS = Set.of("id", "equipmentId", "equipmentTypeId",
        "workOrderId", "warehouseId", "sparePartId", "planId", "userId", "engineerId",
        "assignedEngineerId", "teamId", "workshopId", "status", "priority", "quantity", "qty");
    private final ObjectProvider<OperationLogMapper> mapperProvider;
    private final ObjectMapper objectMapper;

    public OperationAuditAspect(ObjectProvider<OperationLogMapper> mapperProvider, ObjectMapper objectMapper) {
        this.mapperProvider = mapperProvider;
        this.objectMapper = objectMapper;
    }

    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object audit(ProceedingJoinPoint point) throws Throwable {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return point.proceed();
        HttpServletRequest request = attrs.getRequest();
        if (READ.contains(request.getMethod())) return point.proceed();
        long started = System.nanoTime();
        try {
            Object result = point.proceed();
            write(request, point, "SUCCESS", null, started);
            return result;
        } catch (Throwable ex) {
            write(request, point, "FAIL", safe(ex.getMessage(), 500), started);
            throw ex;
        }
    }

    private void write(HttpServletRequest request, ProceedingJoinPoint point, String result, String error, long started) {
        try {
            OperationLogMapper mapper = mapperProvider.getIfAvailable();
            if (mapper == null) return;
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Long userId = auth != null && auth.getPrincipal() instanceof LoginUser user ? user.userId() : null;
            String uri = safe(request.getRequestURI(), 255);
            String[] parts = uri.split("/");
            String module = parts.length > 2 ? safe(parts[2], 50) : "system";
            String operation = safe(point.getSignature().getDeclaringType().getSimpleName() + "." + point.getSignature().getName(), 100);
            String summary = "path=" + uri + auditedFields(request, point);
            mapper.insert(userId, module, operation, uri, request.getMethod(), safe(clientIp(request), 64),
                safe(summary, 2000), result, error, Math.max(0, (System.nanoTime() - started) / 1_000_000));
        } catch (Exception auditError) {
            log.warn("Operation audit persistence failed: {}", auditError.getMessage());
        }
    }

    private String auditedFields(HttpServletRequest request, ProceedingJoinPoint point) {
        List<String> fields = new ArrayList<>();
        Object path = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (path instanceof Map<?, ?> values) addAllowed(fields, values);
        request.getParameterMap().forEach((key, values) -> {
            if (AUDIT_FIELDS.contains(key) && values.length == 1) addField(fields, key, values[0]);
        });
        if (point.getSignature() instanceof MethodSignature signature) {
            Method method = signature.getMethod();
            Annotation[][] annotations = method.getParameterAnnotations();
            Object[] arguments = point.getArgs();
            for (int i = 0; i < Math.min(annotations.length, arguments.length); i++) {
                boolean body = false;
                for (Annotation annotation : annotations[i]) if (annotation instanceof RequestBody) body = true;
                if (body && arguments[i] != null) {
                    try { addAllowed(fields, objectMapper.convertValue(arguments[i], Map.class)); }
                    catch (IllegalArgumentException ignored) { /* Unconvertible bodies are omitted. */ }
                }
            }
        }
        return fields.isEmpty() ? "" : "; fields=" + String.join(",", fields.stream().distinct().limit(20).toList());
    }

    private void addAllowed(List<String> fields, Map<?, ?> values) {
        values.forEach((key, value) -> {
            if (key instanceof String name && AUDIT_FIELDS.contains(name)) addField(fields, name, value);
        });
    }

    private void addField(List<String> fields, String name, Object value) {
        if (value instanceof Number || value instanceof Boolean || value instanceof Enum<?> || value instanceof String) {
            fields.add(name + "=" + safe(String.valueOf(value), 80));
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null || forwarded.isBlank() ? request.getRemoteAddr() : forwarded.split(",", 2)[0].trim();
    }

    private String safe(String value, int max) {
        if (value == null) return null;
        String normalized = value.replaceAll("[\\r\\n\\t,;]", " ");
        return normalized.length() <= max ? normalized : normalized.substring(0, max);
    }
}
