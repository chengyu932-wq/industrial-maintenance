package com.cq.maintenance;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.cq.maintenance.inventory.mapper.InventoryMapper;
import com.cq.maintenance.security.AuthMapper;
import com.cq.maintenance.equipment.mapper.EquipmentMapper;
import com.cq.maintenance.organization.mapper.OrganizationMapper;
import com.cq.maintenance.workorder.mapper.WorkOrderMapper;
import com.cq.maintenance.maintenance.mapper.MaintenanceMapper;

@SpringBootTest(properties = {
    "app.auth.jwt-secret=test-only-secret-that-is-at-least-32-bytes-long",
    "spring.autoconfigure.exclude="
        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration"
})
@AutoConfigureMockMvc
class HealthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthMapper authMapper;
    @MockitoBean private EquipmentMapper equipmentMapper;
    @MockitoBean private OrganizationMapper organizationMapper;
    @MockitoBean private WorkOrderMapper workOrderMapper;
    @MockitoBean private InventoryMapper inventoryMapper;
    @MockitoBean private MaintenanceMapper maintenanceMapper;

    @Test
    void shouldReturnPublicHealthResponse() throws Exception {
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.message").value("success"))
            .andExpect(jsonPath("$.data").value("ok"));
    }
}
