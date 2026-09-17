package com.cq.maintenance.statistics;

import static org.junit.jupiter.api.Assertions.*;
import com.cq.maintenance.statistics.controller.StatisticsController;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class StatisticsControllerSecurityTest {
    @Test void everyStatisticsEndpointRequiresStatisticsViewPermission() {
        for (String name : new String[]{"overview","kpis","charts"}) {
            Method method = java.util.Arrays.stream(StatisticsController.class.getDeclaredMethods()).filter(it -> it.getName().equals(name)).findFirst().orElseThrow();
            PreAuthorize permission = method.getAnnotation(PreAuthorize.class);
            assertNotNull(permission);
            assertEquals("hasAuthority('statistics:view')", permission.value());
        }
    }
}
