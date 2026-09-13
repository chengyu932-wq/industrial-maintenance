package com.cq.maintenance;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cq.maintenance.security.AuthMapper;
import com.cq.maintenance.equipment.mapper.EquipmentMapper;
import com.cq.maintenance.organization.mapper.OrganizationMapper;
import com.cq.maintenance.workorder.mapper.WorkOrderMapper;
import com.cq.maintenance.maintenance.mapper.MaintenanceMapper;
import com.cq.maintenance.system.entity.SysUser;
import com.cq.maintenance.system.vo.MenuVO;
import com.cq.maintenance.system.vo.UserSummaryVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.cq.maintenance.inventory.mapper.InventoryMapper;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "app.auth.jwt-secret=test-only-secret-that-is-at-least-32-bytes-long",
    "app.auth.access-token-ttl=30m",
    "app.auth.refresh-token-ttl=7d",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration"
})
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {
    private static final String HASH = "$2a$10$WBfN3eOHsc4FnJbeloIeq.XVYauRoVrliSjiy9mD9P4.EVplGzcMq";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private StringRedisTemplate redis;
    @MockitoBean private AuthMapper authMapper;
    @MockitoBean private EquipmentMapper equipmentMapper;
    @MockitoBean private OrganizationMapper organizationMapper;
    @MockitoBean private WorkOrderMapper workOrderMapper;
    @MockitoBean private InventoryMapper inventoryMapper;
    @MockitoBean private MaintenanceMapper maintenanceMapper;

    @BeforeEach
    void setUp() {
        var keys = redis.keys("auth:*");
        if (keys != null && !keys.isEmpty()) redis.delete(keys);
        SysUser admin = user(1L, "admin", "ENABLED");
        SysUser engineer = user(2L, "engineer", "ENABLED");
        SysUser disabled = user(3L, "disabled_user", "DISABLED");
        when(authMapper.findByUsername("admin")).thenReturn(admin);
        when(authMapper.findByUsername("engineer")).thenReturn(engineer);
        when(authMapper.findByUsername("disabled_user")).thenReturn(disabled);
        when(authMapper.selectById(1L)).thenReturn(admin);
        when(authMapper.selectById(2L)).thenReturn(engineer);
        when(authMapper.selectById(3L)).thenReturn(disabled);
        when(authMapper.findRoleCodes(1L)).thenReturn(List.of("ADMIN"));
        when(authMapper.findRoleCodes(2L)).thenReturn(List.of("ENGINEER"));
        when(authMapper.findPermissions(1L)).thenReturn(List.of("system:user:list", "system:user:add", "equipment:add"));
        when(authMapper.findPermissions(2L)).thenReturn(List.of("workspace:engineer:view"));
        when(authMapper.findMenus(anyLong())).thenReturn(List.of(new MenuVO(1L, 0L, "MENU", "工作台", "/", "HomeView", null, 1)));
        when(authMapper.findWarehouseIds(anyLong())).thenReturn(List.of());
        when(authMapper.findUsers()).thenReturn(List.of(new UserSummaryVO(1L, "admin", "管理员", "系统管理员", null, null, "ENABLED")));
    }

    @Test
    void captchaShouldRejectWrongExpiredAndReusedValues() throws Exception {
        redis.opsForValue().set("auth:captcha:wrong", "ABCD");
        login("admin", "DevOnly@123", "wrong", "WXYZ").andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(40010));
        login("admin", "DevOnly@123", "wrong", "ABCD").andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(40011));
        login("admin", "DevOnly@123", "missing", "ABCD").andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(40011));
    }

    @Test
    void loginRefreshMeAndLogoutShouldFormClosedLoop() throws Exception {
        Tokens tokens = successfulLogin("admin");
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tokens.access()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.roleCodes[0]").value("ADMIN"));
        String refreshJson = mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + tokens.refresh() + "\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode refreshed = objectMapper.readTree(refreshJson).path("data");
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + tokens.refresh() + "\"}"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(40102));
        String newAccess = refreshed.path("accessToken").asText();
        String newRefresh = refreshed.path("refreshToken").asText();
        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + newAccess)
                .contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\"" + newRefresh + "\"}"))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + newAccess))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(40101));
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + newRefresh + "\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenTypeAndInvalidTokenShouldBeRejected() throws Exception {
        Tokens tokens = successfulLogin("admin");
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + tokens.access() + "\"}"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(40102));
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer broken.token.value"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(40101));
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void rbacShouldReturn200ForGrantedAnd403ForDenied() throws Exception {
        Tokens admin = successfulLogin("admin");
        Tokens engineer = successfulLogin("engineer");
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + admin.access()))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + engineer.access()))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(40300));
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + engineer.access()))
            .andExpect(status().isOk());
    }

    @Test
    void equipmentCreateShouldReturn403WithoutPermission() throws Exception {
        Tokens engineer = successfulLogin("engineer");
        mockMvc.perform(post("/api/equipment").header("Authorization", "Bearer " + engineer.access())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"equipmentNo\":\"EQ-403\",\"equipmentName\":\"权限测试设备\",\"typeId\":1,\"stationId\":1}"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(40300));
    }

    @Test
    void disabledUserShouldNotLogin() throws Exception {
        putCaptcha("disabled");
        login("disabled_user", "DevOnly@123", "disabled", "ABCD")
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(40310));
    }

    @Test
    void fifthPasswordFailureShouldLockButCaptchaFailureShouldNotCount() throws Exception {
        putCaptcha("captcha-only");
        login("admin", "bad", "captcha-only", "ZZZZ").andExpect(jsonPath("$.code").value(40010));
        for (int i = 1; i <= 4; i++) {
            putCaptcha("fail-" + i);
            login("admin", "bad", "fail-" + i, "ABCD").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40012));
        }
        putCaptcha("fail-5");
        login("admin", "bad", "fail-5", "ABCD").andExpect(status().isLocked())
            .andExpect(jsonPath("$.code").value(42300));
        putCaptcha("locked");
        login("admin", "DevOnly@123", "locked", "ABCD").andExpect(status().isLocked());
    }

    private Tokens successfulLogin(String username) throws Exception {
        String captchaId = "captcha-" + username;
        putCaptcha(captchaId);
        String json = login(username, "DevOnly@123", captchaId, "ABCD")
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode data = objectMapper.readTree(json).path("data");
        return new Tokens(data.path("accessToken").asText(), data.path("refreshToken").asText());
    }

    private org.springframework.test.web.servlet.ResultActions login(String username, String password,
                                                                      String captchaId, String captchaCode) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("username", username, "password", password,
            "captchaId", captchaId, "captchaCode", captchaCode));
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private void putCaptcha(String id) { redis.opsForValue().set("auth:captcha:" + id, "ABCD"); }

    private SysUser user(Long id, String username, String status) {
        SysUser user = new SysUser();
        user.setId(id); user.setUsername(username); user.setPasswordHash(HASH);
        user.setRealName(username); user.setStatus(status);
        return user;
    }

    private record Tokens(String access, String refresh) {}
}
