package com.cq.maintenance;

import static org.junit.jupiter.api.Assertions.*;
import com.cq.maintenance.common.exception.BusinessException;
import com.cq.maintenance.equipment.dto.EquipmentCreateRequest;
import com.cq.maintenance.equipment.dto.EquipmentQuery;
import com.cq.maintenance.equipment.entity.EquipmentStatus;
import com.cq.maintenance.equipment.mapper.EquipmentMapper;
import com.cq.maintenance.equipment.service.EquipmentService;
import com.cq.maintenance.equipment.service.EquipmentStateService;
import com.cq.maintenance.organization.service.OrganizationService;
import com.cq.maintenance.security.LoginUser;
import java.util.List;
import java.nio.charset.StandardCharsets;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties="app.auth.jwt-secret=test-only-secret-that-is-at-least-32-bytes-long")
@ActiveProfiles("local")
@EnabledIfSystemProperty(named="stage4.db-tests",matches="true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Stage4DatabaseIntegrationTest {
    @Autowired DataSource dataSource;@Autowired JdbcTemplate jdbc;@Autowired OrganizationService organizations;
    @Autowired EquipmentService equipment;@Autowired EquipmentStateService states;@Autowired EquipmentMapper mapper;

    @BeforeAll static void seed(@Autowired DataSource ds)throws Exception{
        try(var c=ds.getConnection()){
            executeUtf8(c,"../sql/11_dev_auth_seed.sql");
            executeUtf8(c,"../sql/12_stage4_permissions.sql");
            executeUtf8(c,"../sql/13_stage4_demo_data.sql");
        }
    }
    private static void executeUtf8(java.sql.Connection connection,String path){
        ScriptUtils.executeSqlScript(connection,new EncodedResource(new FileSystemResource(path),StandardCharsets.UTF_8));
    }
    @BeforeEach void login(){Long id=jdbc.queryForObject("SELECT id FROM sys_user WHERE username='admin'",Long.class);LoginUser u=new LoginUser(id,"admin","开发管理员","ENABLED",List.of("ADMIN"),List.of("equipment:list","equipment:view","equipment:add","equipment:update","equipment:status","equipment:scrap"),List.of(),null,List.of());SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(u,null,u.authorities()));}
    @AfterEach void clear(){SecurityContextHolder.clearContext();}

    @Test @Order(1) void organizationHierarchyAndEquipmentPageUseRealSql(){assertFalse(organizations.workshops(null).isEmpty());assertFalse(organizations.lines(null,null).isEmpty());assertFalse(organizations.stations(null,null).isEmpty());assertTrue(equipment.page(new EquipmentQuery()).total()>=1);}

    @Test @Order(2) void equipmentCrudStateAndHistoryUseRealTransactions(){Long type=jdbc.queryForObject("SELECT id FROM eqp_type WHERE type_code='CNC'",Long.class);Long station=jdbc.queryForObject("SELECT id FROM org_station WHERE station_no='ST-01'",Long.class);String no="IT-EQP-"+System.nanoTime();Long id=equipment.create(new EquipmentCreateRequest(no,"事务测试设备",type,null,null,null,null,null,null,null,station,null));assertEquals(EquipmentStatus.PENDING,equipment.detail(id).status());states.manual(id,EquipmentStatus.RUNNING,"集成测试启用");assertEquals(EquipmentStatus.RUNNING,mapper.findEquipment(id).getStatus());assertEquals(2,mapper.findStatusHistory(id).size());assertThrows(BusinessException.class,()->states.manual(id,EquipmentStatus.RUNNING,"重复"));}

    @Test @Order(3) void statusUpdateRollsBackWhenHistoryInsertFails(){Long type=jdbc.queryForObject("SELECT id FROM eqp_type WHERE type_code='CNC'",Long.class);Long station=jdbc.queryForObject("SELECT id FROM org_station WHERE station_no='ST-01'",Long.class);Long operator=jdbc.queryForObject("SELECT id FROM sys_user WHERE username='admin'",Long.class);Long id=equipment.create(new EquipmentCreateRequest("IT-RB-"+System.nanoTime(),"回滚测试设备",type,null,null,null,null,null,null,null,station,null));assertThrows(Exception.class,()->states.transition(id,EquipmentStatus.RUNNING,"制造履历约束失败","INVALID_SOURCE",null,operator));assertEquals(EquipmentStatus.PENDING,mapper.findEquipment(id).getStatus());}
}
