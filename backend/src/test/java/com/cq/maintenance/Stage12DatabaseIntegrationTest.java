package com.cq.maintenance;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.cq.maintenance.security.LoginUser;
import java.util.List;
import java.nio.file.*;
import java.util.Comparator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.*;
import org.springframework.test.context.jdbc.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties={"app.auth.jwt-secret=test-only-secret-that-is-at-least-32-bytes-long","app.maintenance.scan-cron=-","app.sla.scan-cron=-","app.storage.root=target/stage12-test-uploads"})
@AutoConfigureMockMvc @ActiveProfiles("local") @TestPropertySource(properties="stage12.db-tests=true")
@EnabledIfSystemProperty(named="stage12.db-tests",matches="true")
@Sql(scripts="file:../sql/25_stage12_support.sql") @SqlConfig(encoding="UTF-8")
@Transactional
class Stage12DatabaseIntegrationTest {
    @Autowired MockMvc mvc;@Autowired JdbcTemplate jdbc;
    @AfterEach void cleanFiles()throws Exception{Path root=Path.of("target/stage12-test-uploads");if(Files.exists(root))try(var paths=Files.walk(root)){paths.sorted(Comparator.reverseOrder()).forEach(path->{try{Files.deleteIfExists(path);}catch(Exception ignored){}});}}
    UsernamePasswordAuthenticationToken admin(String permission){Long id=jdbc.queryForObject("SELECT id FROM sys_user WHERE username='admin'",Long.class);LoginUser user=new LoginUser(id,"admin","管理员","ENABLED",List.of("ADMIN"),List.of(permission),List.of(),null,List.of());return UsernamePasswordAuthenticationToken.authenticated(user,null,List.of(new SimpleGrantedAuthority(permission)));}
    @Test void supportPermissionIsInstalled(){assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM sys_role_menu rm JOIN sys_role r ON r.id=rm.role_id JOIN sys_menu m ON m.id=rm.menu_id WHERE m.permission_code='system:operation-log:list' AND r.role_code IN ('ADMIN','MAINTENANCE_SUPERVISOR')",Integer.class));}
    @Test void runtimeWriteIsTransactionalAndAudited()throws Exception{Long equipment=jdbc.queryForObject("SELECT id FROM eqp_equipment WHERE status<>'SCRAPPED' ORDER BY id LIMIT 1",Long.class);var before=jdbc.queryForObject("SELECT running_hours FROM eqp_equipment WHERE id=?",java.math.BigDecimal.class,equipment);mvc.perform(post("/api/equipment/{id}/runtime-hours",equipment).with(authentication(admin("equipment:update"))).contentType(MediaType.APPLICATION_JSON).content("{\"recordDate\":\"2026-09-18\",\"increment\":\"1.25\",\"remark\":\"第12阶段验证\"}")).andExpect(status().isOk());assertEquals(0,before.add(new java.math.BigDecimal("1.25")).compareTo(jdbc.queryForObject("SELECT running_hours FROM eqp_equipment WHERE id=?",java.math.BigDecimal.class,equipment)));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM eqp_runtime_record WHERE equipment_id=? AND remark='第12阶段验证'",Integer.class,equipment));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM sys_operation_log WHERE request_uri=? AND result='SUCCESS'",Integer.class,"/api/equipment/"+equipment+"/runtime-hours"));}
    @Test void pdfEndpointReturnsRealPdf()throws Exception{Long order=jdbc.queryForObject("SELECT id FROM mnt_work_order ORDER BY id LIMIT 1",Long.class);byte[] body=mvc.perform(get("/api/work-orders/{id}/pdf",order).with(authentication(admin("workorder:view")))).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_PDF)).andReturn().getResponse().getContentAsByteArray();assertTrue(body.length>1000);assertEquals("%PDF",new String(body,0,4,java.nio.charset.StandardCharsets.US_ASCII));}
    @Test void equipmentAndWorkOrderAttachmentsRoundTripWithScopeChecks()throws Exception{MockMultipartFile file=new MockMultipartFile("file","现场照片.png","image/png",new byte[]{(byte)137,80,78,71,1,2,3});Long equipment=jdbc.queryForObject("SELECT id FROM eqp_equipment ORDER BY id LIMIT 1",Long.class);mvc.perform(multipart("/api/equipment/{id}/attachments",equipment).file(file).with(authentication(admin("equipment:view")))).andExpect(status().isForbidden());String equipmentResponse=mvc.perform(multipart("/api/equipment/{id}/attachments",equipment).file(file).with(authentication(admin("equipment:update")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();Long equipmentAttachment=Long.valueOf(com.jayway.jsonpath.JsonPath.read(equipmentResponse,"$.data").toString());mvc.perform(get("/api/equipment/{id}/attachments/{attachmentId}",equipment,equipmentAttachment).with(authentication(admin("equipment:view")))).andExpect(status().isOk()).andExpect(content().bytes(file.getBytes()));Long order=jdbc.queryForObject("SELECT id FROM mnt_work_order ORDER BY id LIMIT 1",Long.class);mvc.perform(multipart("/api/work-orders/{id}/attachments",order).file(file).param("type","REPAIR").with(authentication(admin("workorder:view")))).andExpect(status().isForbidden());String orderResponse=mvc.perform(multipart("/api/work-orders/{id}/attachments",order).file(file).param("type","REPAIR").with(authentication(admin("workorder:process")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();Long orderAttachment=Long.valueOf(com.jayway.jsonpath.JsonPath.read(orderResponse,"$.data").toString());mvc.perform(get("/api/work-orders/{id}/attachments/{attachmentId}",order,orderAttachment).with(authentication(admin("workorder:view")))).andExpect(status().isOk()).andExpect(content().bytes(file.getBytes()));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM eqp_attachment WHERE id=?",Integer.class,equipmentAttachment));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM mnt_work_order_attachment WHERE id=? AND attachment_type='REPAIR'",Integer.class,orderAttachment));}
}
