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
import com.cq.maintenance.system.entity.SysUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "app.auth.jwt-secret=test-only-secret-that-is-at-least-32-bytes-long",
    "app.auth.access-token-ttl=100ms",
    "app.auth.login-lock-ttl=200ms",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration"
})
@AutoConfigureMockMvc
class AuthExpiryIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private StringRedisTemplate redis;
    @MockitoBean private AuthMapper authMapper;
    @MockitoBean private EquipmentMapper equipmentMapper;
    @MockitoBean private OrganizationMapper organizationMapper;

    @BeforeEach
    void setUp() {
        var keys = redis.keys("auth:*");
        if (keys != null && !keys.isEmpty()) redis.delete(keys);
        SysUser user = new SysUser();
        user.setId(10L); user.setUsername("expiry_user"); user.setRealName("时效测试"); user.setStatus("ENABLED");
        user.setPasswordHash("$2a$10$WBfN3eOHsc4FnJbeloIeq.XVYauRoVrliSjiy9mD9P4.EVplGzcMq");
        when(authMapper.findByUsername("expiry_user")).thenReturn(user);
        when(authMapper.selectById(10L)).thenReturn(user);
        when(authMapper.findRoleCodes(10L)).thenReturn(List.of("ENGINEER"));
        when(authMapper.findPermissions(10L)).thenReturn(List.of());
        when(authMapper.findMenus(anyLong())).thenReturn(List.of());
        when(authMapper.findWarehouseIds(anyLong())).thenReturn(List.of());
    }

    @Test
    void expiredAccessTokenShouldReturnDedicated401() throws Exception {
        String access = login("ok", "DevOnly@123").path("accessToken").asText();
        Thread.sleep(1200);
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + access))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(40103));
    }

    @Test
    void userShouldLoginAfterTemporaryLockExpires() throws Exception {
        for (int i = 0; i < 5; i++) login("bad-" + i, "wrong");
        Thread.sleep(350);
        JsonNode data = login("after-lock", "DevOnly@123");
        org.junit.jupiter.api.Assertions.assertFalse(data.path("accessToken").asText().isBlank());
    }

    private JsonNode login(String captchaId, String password) throws Exception {
        redis.opsForValue().set("auth:captcha:" + captchaId, "ABCD");
        String body = objectMapper.writeValueAsString(Map.of("username", "expiry_user", "password", password,
            "captchaId", captchaId, "captchaCode", "ABCD"));
        String json = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).path("data");
    }
}
