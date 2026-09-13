package com.cq.maintenance;

import static org.junit.jupiter.api.Assertions.*;
import com.cq.maintenance.common.exception.BusinessException;
import com.cq.maintenance.maintenance.dto.*;
import com.cq.maintenance.maintenance.entity.*;
import com.cq.maintenance.maintenance.service.*;
import com.cq.maintenance.repair.entity.RepairPriority;
import com.cq.maintenance.security.LoginUser;
import com.cq.maintenance.workorder.dto.*;
import com.cq.maintenance.workorder.service.WorkOrderService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
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

@SpringBootTest(properties={"app.auth.jwt-secret=test-only-secret-that-is-at-least-32-bytes-long","app.maintenance.scan-cron=-"})
@ActiveProfiles("local") @EnabledIfSystemProperty(named="stage8.db-tests",matches="true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class) @TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Stage8DatabaseIntegrationTest {
    @Autowired DataSource dataSource;@Autowired JdbcTemplate jdbc;@Autowired MaintenancePlanService plans;@Autowired MaintenanceScanService scanner;@Autowired MaintenanceExecutionService execution;@Autowired WorkOrderService orders;
    String suffix;Long equipmentId,planId,workOrderId;
    @BeforeAll void seed()throws Exception{try(var c=dataSource.getConnection()){for(String path:List.of("../sql/11_dev_auth_seed.sql","../sql/12_stage4_permissions.sql","../sql/13_stage4_demo_data.sql","../sql/14_stage5_permissions.sql","../sql/15_stage7_permissions.sql","../sql/16_stage7_demo_data.sql","../sql/17_stage8_permissions.sql"))ScriptUtils.executeSqlScript(c,new EncodedResource(new FileSystemResource(path),StandardCharsets.UTF_8));}suffix=String.valueOf(System.nanoTime());}
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    Long user(String username){return jdbc.queryForObject("SELECT id FROM sys_user WHERE username=?",Long.class,username);}
    Long team(){return jdbc.queryForObject("SELECT id FROM org_team WHERE team_no='TEAM-01'",Long.class);}
    Long workshop(){return jdbc.queryForObject("SELECT id FROM org_workshop WHERE workshop_no='WS-01'",Long.class);}
    void login(String username,String role){Long id=user(username);Long teamId="engineer".equals(username)||"supervisor".equals(username)?team():null;Long workshopId="supervisor".equals(username)?workshop():null;LoginUser u=new LoginUser(id,username,username,"ENABLED",List.of(role),List.of(),teamId==null?List.of():List.of(teamId),workshopId,List.of());SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(u,null,List.of()));}
    Long createEquipment(String status){String no="IT8-EQP-"+status+"-"+suffix;jdbc.update("INSERT INTO eqp_equipment(equipment_no,equipment_name,type_id,responsible_user_id,responsible_team_id,station_id,status,running_hours,qr_code) SELECT ?,?,t.id,u.id,tm.id,s.id,?,0,? FROM eqp_type t JOIN sys_user u ON u.username='engineer' JOIN org_team tm ON tm.team_no='TEAM-01' JOIN org_station s ON s.station_no='ST-01' WHERE t.type_code='CNC'",no,"第8阶段保养设备",status,UUID.randomUUID().toString());return jdbc.queryForObject("SELECT id FROM eqp_equipment WHERE equipment_no=?",Long.class,no);}

    @Test @Order(1) void databaseContainsFrozenMaintenanceModelAndPermissions(){assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name LIKE 'pm_%'",Integer.class));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='mnt_work_order' AND column_name='pm_plan_id'",Integer.class));assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM sys_menu WHERE permission_code LIKE 'maintenance:%'",Integer.class)>=7);}
    @Test @Order(2) void supervisorCreatesPlanAndItems(){equipmentId=createEquipment("RUNNING");login("supervisor","MAINTENANCE_SUPERVISOR");planId=plans.create(new MaintenancePlanRequest("IT8-PM-"+suffix,"月度润滑保养",equipmentId,MaintenanceCycleType.MONTH,1,LocalDate.now(),null));plans.replaceItems(planId,List.of(new MaintenancePlanItemRequest("主轴润滑","油位及油质正常",1,true),new MaintenancePlanItemRequest("防护罩检查","紧固无破损",2,true)));assertEquals(2,plans.items(planId).size());MaintenancePlanQuery query=new MaintenancePlanQuery();query.setKeyword("IT8-PM-"+suffix);assertEquals(1,plans.page(query).total());assertEquals(2,plans.detail(planId).items().size());}
    @Test @Order(3) void duplicatePlanNumberFailsAsBusinessException(){login("supervisor","MAINTENANCE_SUPERVISOR");assertThrows(BusinessException.class,()->plans.create(new MaintenancePlanRequest("IT8-PM-"+suffix,"重复计划",equipmentId,MaintenanceCycleType.WEEK,1,LocalDate.now(),null)));}
    @Test @Order(4) void duePlanGeneratesExactlyOneMaintenanceOrder(){login("admin","ADMIN");var first=scanner.scanDuePlans();var second=scanner.scanDuePlans();assertTrue(first.generated()>=1);assertEquals(0,second.generated());assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM mnt_work_order WHERE pm_plan_id=?",Integer.class,planId));workOrderId=jdbc.queryForObject("SELECT id FROM mnt_work_order WHERE pm_plan_id=?",Long.class,planId);assertEquals("MAINTENANCE",jdbc.queryForObject("SELECT work_order_type FROM mnt_work_order WHERE id=?",String.class,workOrderId));assertTrue(jdbc.queryForObject("SELECT next_execute_date FROM pm_plan WHERE id=?",LocalDate.class,planId).isAfter(LocalDate.now()));}
    @Test @Order(5) void disabledAndFuturePlansDoNotGenerate(){Long future=jdbc.queryForObject("SELECT id FROM pm_plan WHERE id=?",Long.class,planId);jdbc.update("UPDATE pm_plan SET status='DISABLED',next_execute_date=? WHERE id=?",LocalDate.now().minusDays(1),future);assertEquals(0,scanner.scanDuePlans().generated());jdbc.update("UPDATE pm_plan SET status='ENABLED',next_execute_date=? WHERE id=?",LocalDate.now().plusDays(10),future);assertEquals(0,scanner.scanDuePlans().generated());}
    @Test @Order(6) void maintenanceOrderCompletesThroughUnifiedWorkflowWithoutFaultState(){jdbc.update("UPDATE pm_plan SET status='ENABLED' WHERE id=?",planId);login("supervisor","MAINTENANCE_SUPERVISOR");orders.assign(workOrderId,new DispatchRequest(user("engineer"),team(),"保养派单"));List<Long> itemIds=jdbc.query("SELECT id FROM pm_plan_item WHERE plan_id=? ORDER BY sort_no",(rs,n)->rs.getLong(1),planId);login("engineer","ENGINEER");orders.acceptResponse(workOrderId);orders.start(workOrderId);assertEquals("RUNNING",jdbc.queryForObject("SELECT status FROM eqp_equipment WHERE id=?",String.class,equipmentId));execution.save(workOrderId,itemIds.stream().map(id->new MaintenanceExecutionItemRequest(id,MaintenanceResult.NORMAL,null,"检查正常")).toList());orders.submitAcceptance(workOrderId);login("supervisor","MAINTENANCE_SUPERVISOR");orders.pass(workOrderId,new AcceptanceRequest("保养验收通过"));assertEquals("COMPLETED",jdbc.queryForObject("SELECT status FROM mnt_work_order WHERE id=?",String.class,workOrderId));assertEquals("RUNNING",jdbc.queryForObject("SELECT status FROM eqp_equipment WHERE id=?",String.class,equipmentId));}
    @Test @Order(7) void completedMaintenanceAppearsInEquipmentHistory(){login("supervisor","MAINTENANCE_SUPERVISOR");var history=plans.equipmentHistory(equipmentId);assertTrue(history.stream().anyMatch(x->x.workOrderId().equals(workOrderId)&&x.completedItems()==2&&x.abnormalItems()==0));}
    @Test @Order(8) void generatedPlanInspectionItemsCannotBeRewritten(){login("supervisor","MAINTENANCE_SUPERVISOR");assertThrows(BusinessException.class,()->plans.replaceItems(planId,List.of(new MaintenancePlanItemRequest("篡改历史",null,1,true))));}
    @Test @Order(9) void scrappedEquipmentCannotReceiveEnabledPlan(){Long id=createEquipment("SCRAPPED");login("admin","ADMIN");assertThrows(BusinessException.class,()->plans.create(new MaintenancePlanRequest("IT8-SCRAP-"+suffix,"报废设备计划",id,MaintenanceCycleType.QUARTER,1,LocalDate.now(),null)));}
}
