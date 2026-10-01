package com.cq.maintenance.audit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cq.maintenance.audit.aspect.OperationAuditAspect;
import com.cq.maintenance.audit.mapper.OperationLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

class OperationAuditAspectTest {
    record AuditBody(Long equipmentId, String status, String password) {}
    public void action(@RequestBody AuditBody body) {}

    @AfterEach void clear() { RequestContextHolder.resetRequestAttributes(); }

    @Test void recordsBusinessIdentifiersWithoutCredentials() throws Throwable {
        @SuppressWarnings("unchecked") ObjectProvider<OperationLogMapper> provider = mock(ObjectProvider.class);
        OperationLogMapper mapper = mock(OperationLogMapper.class);
        when(provider.getIfAvailable()).thenReturn(mapper);
        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method method = getClass().getMethod("action", AuditBody.class);
        when(point.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getDeclaringType()).thenReturn(getClass());
        when(signature.getName()).thenReturn("action");
        when(point.getArgs()).thenReturn(new Object[]{new AuditBody(7L, "RUNNING", "Secret123")});
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/equipment/42/status");
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("id", "42"));
        request.addParameter("warehouseId", "3");
        request.addParameter("token", "hidden");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        new OperationAuditAspect(provider, new ObjectMapper()).audit(point);

        ArgumentCaptor<String> summary = ArgumentCaptor.forClass(String.class);
        verify(mapper).insert(any(), any(), any(), any(), any(), any(), summary.capture(), eq("SUCCESS"), isNull(), anyLong());
        assertTrue(summary.getValue().contains("equipmentId=7"));
        assertTrue(summary.getValue().contains("status=RUNNING"));
        assertTrue(summary.getValue().contains("warehouseId=3"));
        assertFalse(summary.getValue().contains("Secret123"));
        assertFalse(summary.getValue().contains("hidden"));
    }
}
