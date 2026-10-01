package com.cq.maintenance;

import static org.junit.jupiter.api.Assertions.*;
import com.cq.maintenance.common.exception.BusinessException;
import com.cq.maintenance.equipment.entity.EquipmentStatus;
import com.cq.maintenance.equipment.service.EquipmentStateService;
import com.cq.maintenance.inventory.dto.*;
import com.cq.maintenance.inventory.service.InventoryService;
import com.cq.maintenance.repair.dto.RepairRequestCreateRequest;
import com.cq.maintenance.repair.entity.*;
import com.cq.maintenance.repair.service.RepairRequestService;
import com.cq.maintenance.security.LoginUser;
import com.cq.maintenance.workorder.dto.*;
import com.cq.maintenance.workorder.service.WorkOrderService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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

@SpringBootTest(properties="app.auth.jwt-secret=test-only-secret-that-is-at-least-32-bytes-long")
@ActiveProfiles("local") @EnabledIfSystemProperty(named="stage13.db-tests",matches="true")
class Stage13DatabaseIntegrationTest {
    @Autowired JdbcTemplate jdbc;@Autowired RepairRequestService repairs;@Autowired WorkOrderService orders;
    @Autowired InventoryService inventory;@Autowired EquipmentStateService equipmentStates;
    @BeforeAll static void seed(@Autowired DataSource ds)throws Exception{try(var c=ds.getConnection()){for(String path:List.of("../sql/11_dev_auth_seed.sql","../sql/12_stage4_permissions.sql","../sql/13_stage4_demo_data.sql","../sql/14_stage5_permissions.sql","../sql/15_stage7_permissions.sql","../sql/16_stage7_demo_data.sql","../sql/26_unrepairable_closure.sql"))ScriptUtils.executeSqlScript(c,new EncodedResource(new FileSystemResource(path),StandardCharsets.UTF_8));}}
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    Long user(String name){return jdbc.queryForObject("SELECT id FROM sys_user WHERE username=?",Long.class,name);}
    Long team(){return jdbc.queryForObject("SELECT id FROM org_team WHERE team_no='TEAM-01'",Long.class);}
    Long workshop(){return jdbc.queryForObject("SELECT id FROM org_workshop WHERE workshop_no='WS-01'",Long.class);}
    void login(String name,String role){Long id=user(name);Long teamId="engineer".equals(name)||"supervisor".equals(name)?team():null;Long workshopId="supervisor".equals(name)?workshop():null;List<Long> warehouses=jdbc.query("SELECT warehouse_id FROM inv_warehouse_user WHERE user_id=?",(rs,n)->rs.getLong(1),id);LoginUser u=new LoginUser(id,name,name,"ENABLED",List.of(role),List.of(),teamId==null?List.of():List.of(teamId),workshopId,warehouses);SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(u,null,List.of()));}
    Long equipment(){String no="IT13-EQP-"+System.nanoTime();jdbc.update("INSERT INTO eqp_equipment(equipment_no,equipment_name,type_id,responsible_user_id,responsible_team_id,station_id,status,running_hours,qr_code) SELECT ?,?,t.id,u.id,tm.id,s.id,'RUNNING',0,? FROM eqp_type t JOIN sys_user u ON u.username='engineer' JOIN org_team tm ON tm.team_no='TEAM-01' JOIN org_station s ON s.station_no='ST-01' WHERE t.type_code='CNC'",no,"无法修复处置测试设备",UUID.randomUUID().toString());return jdbc.queryForObject("SELECT id FROM eqp_equipment WHERE equipment_no=?",Long.class,no);}
    long started(Long equipmentId){login("reporter","REPORTER");var created=repairs.create(new RepairRequestCreateRequest(equipmentId,RepairPriority.IMPORTANT,"主轴严重损坏",RepairSource.PC));login("supervisor","MAINTENANCE_SUPERVISOR");orders.assign(created.workOrderId(),new DispatchRequest(user("engineer"),team(),"维修派单"));login("engineer","ENGINEER");assertThrows(BusinessException.class,()->orders.start(created.workOrderId()));orders.acceptResponse(created.workOrderId());orders.start(created.workOrderId());return created.workOrderId();}
    @Test void unrepairableClosureAndEquipmentLedgerPersist(){Long eid=equipment();long id=started(eid);login("admin","ADMIN");Long warehouse=jdbc.queryForObject("SELECT id FROM inv_warehouse WHERE warehouse_no='WH-DEMO-01'",Long.class);Long spare=inventory.createSparePart(new SparePartRequest("IT13-SP-"+System.nanoTime(),"处置测试备件",null,null,"件",BigDecimal.TEN,null,null,1,1,"ENABLED"));inventory.inbound(new InventoryOperationRequest(warehouse,spare,new BigDecimal("3"),null,"测试入库"));login("engineer","ENGINEER");Long issue=inventory.issueForWorkOrder(id,new WorkOrderSpareRequest(warehouse,spare,new BigDecimal("2"),"尝试更换"));inventory.returnForWorkOrder(id,issue,new ReturnSpareRequest(BigDecimal.ONE,"未使用"));orders.saveRepairRecord(id,new RepairRecordRequest("检查主轴","主轴断裂","尝试更换","仍无法恢复",BigDecimal.ONE,90,false));assertThrows(BusinessException.class,()->orders.submitAcceptance(id));login("supervisor","MAINTENANCE_SUPERVISOR");assertThrows(BusinessException.class,()->equipmentStates.scrap(eid,"绕过工单直接报废"));orders.closeUnrepairable(id,new UnrepairableCloseRequest("维修后仍存在安全隐患",EquipmentStatus.STOPPED));assertEquals("UNREPAIRABLE",jdbc.queryForObject("SELECT status FROM mnt_work_order WHERE id=?",String.class,id));assertNotNull(jdbc.queryForObject("SELECT completed_at FROM mnt_work_order WHERE id=?",java.sql.Timestamp.class,id));assertEquals("STOPPED",jdbc.queryForObject("SELECT status FROM eqp_equipment WHERE id=?",String.class,eid));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM mnt_work_order_flow WHERE work_order_id=? AND action='CLOSE_UNREPAIRABLE'",Integer.class,id));WorkOrderQuery q=new WorkOrderQuery();q.setEquipmentId(eid);assertEquals(id,orders.page(q).records().get(0).id());var usage=orders.equipmentSpareUsage(eid,1,20);assertEquals(1,usage.total());assertEquals(0,usage.records().get(0).usedQty().compareTo(BigDecimal.ONE));login("reporter","REPORTER");assertEquals(1,orders.equipmentSpareUsage(eid,1,20).total());login("admin","REPORTER");assertEquals(0,orders.equipmentSpareUsage(eid,1,20).total());assertEquals(0,orders.page(q).total());}
    @Test void scrapChoiceClosesRepairOrder(){Long eid=equipment();long id=started(eid);orders.saveRepairRecord(id,new RepairRecordRequest("检查电机","不可修复","排除外部故障","建议报废",BigDecimal.ONE,60,false));login("supervisor","MAINTENANCE_SUPERVISOR");orders.closeUnrepairable(id,new UnrepairableCloseRequest("设备达到报废条件",EquipmentStatus.SCRAPPED));assertEquals("SCRAPPED",jdbc.queryForObject("SELECT status FROM eqp_equipment WHERE id=?",String.class,eid));assertEquals("UNREPAIRABLE",jdbc.queryForObject("SELECT status FROM mnt_work_order WHERE id=?",String.class,id));}
}
