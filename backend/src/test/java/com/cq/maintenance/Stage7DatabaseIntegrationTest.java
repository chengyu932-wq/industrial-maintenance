package com.cq.maintenance;

import static org.junit.jupiter.api.Assertions.*;
import com.cq.maintenance.common.exception.*;
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
@ActiveProfiles("local") @EnabledIfSystemProperty(named="stage7.db-tests",matches="true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Stage7DatabaseIntegrationTest {
    @Autowired DataSource dataSource;@Autowired JdbcTemplate jdbc;@Autowired InventoryService inventory;@Autowired RepairRequestService repairs;@Autowired WorkOrderService orders;
    @BeforeAll static void seed(@Autowired DataSource ds)throws Exception{try(var c=ds.getConnection()){for(String path:List.of("../sql/11_dev_auth_seed.sql","../sql/12_stage4_permissions.sql","../sql/13_stage4_demo_data.sql","../sql/14_stage5_permissions.sql","../sql/15_stage7_permissions.sql","../sql/16_stage7_demo_data.sql"))ScriptUtils.executeSqlScript(c,new EncodedResource(new FileSystemResource(path),StandardCharsets.UTF_8));}}
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    Long user(String username){return jdbc.queryForObject("SELECT id FROM sys_user WHERE username=?",Long.class,username);}
    Long warehouse(String no){return jdbc.queryForObject("SELECT id FROM inv_warehouse WHERE warehouse_no=?",Long.class,no);}
    Long spare(String no){return jdbc.queryForObject("SELECT id FROM inv_spare_part WHERE spare_no=?",Long.class,no);}
    Long team(){return jdbc.queryForObject("SELECT id FROM org_team WHERE team_no='TEAM-01'",Long.class);}
    Long workshop(){return jdbc.queryForObject("SELECT id FROM org_workshop WHERE workshop_no='WS-01'",Long.class);}
    void login(String username,String role){Long id=user(username);List<Long> warehouseIds=jdbc.query("SELECT warehouse_id FROM inv_warehouse_user WHERE user_id=?",(rs,n)->rs.getLong(1),id);Long teamId="engineer".equals(username)||"supervisor".equals(username)?team():null;Long workshopId="supervisor".equals(username)?workshop():null;LoginUser u=new LoginUser(id,username,username,"ENABLED",List.of(role),List.of(),teamId==null?List.of():List.of(teamId),workshopId,warehouseIds);SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(u,null,List.of()));}
    BigDecimal qty(Long warehouseId,Long spareId){return jdbc.queryForObject("SELECT current_qty FROM inv_stock WHERE warehouse_id=? AND spare_part_id=?",BigDecimal.class,warehouseId,spareId);}
    Long createRunningEquipment(){String no="IT7-EQP-"+System.nanoTime();jdbc.update("INSERT INTO eqp_equipment(equipment_no,equipment_name,type_id,responsible_user_id,responsible_team_id,station_id,status,running_hours,qr_code) SELECT ?,?,t.id,u.id,tm.id,s.id,'RUNNING',0,? FROM eqp_type t JOIN sys_user u ON u.username='engineer' JOIN org_team tm ON tm.team_no='TEAM-01' JOIN org_station s ON s.station_no='ST-01' WHERE t.type_code='CNC'",no,"第7阶段闭环测试设备",UUID.randomUUID().toString());return jdbc.queryForObject("SELECT id FROM eqp_equipment WHERE equipment_no=?",Long.class,no);}

    @Test @Order(1) void databaseContainsAllInventoryTablesConstraintsAndPermissions(){assertEquals(8,jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name LIKE 'inv_%'",Integer.class));assertEquals(1,jdbc.queryForObject("SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='inv_stock' AND index_name='uk_inv_stock_warehouse_spare' AND non_unique=0",Integer.class));assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM sys_menu WHERE permission_code IN ('inventory:issue','inventory:transfer','inventory:stocktake')",Integer.class)>=3);}
    @Test @Order(2) void warehouseAndSpareCreationEnforceUniqueBusinessCodes(){login("admin","ADMIN");String suffix=String.valueOf(System.nanoTime());Long wid=inventory.createWarehouse(new WarehouseRequest("IT7-WH-"+suffix,"集成测试仓",null,user("warehouse"),"ENABLED"));assertNotNull(wid);assertThrows(BusinessException.class,()->inventory.createWarehouse(new WarehouseRequest("IT7-WH-"+suffix,"重复仓",null,null,"ENABLED")));Long pid=inventory.createSparePart(new SparePartRequest("IT7-SP-"+suffix,"集成测试备件","规格A","品牌A","件",new BigDecimal("18.60"),null,"CNC",3,2,"ENABLED"));assertNotNull(pid);assertThrows(BusinessException.class,()->inventory.createSparePart(new SparePartRequest("IT7-SP-"+suffix,"重复备件",null,null,"件",BigDecimal.ONE,null,null,1,1,"ENABLED")));}
    @Test @Order(3) void unauthorizedWarehouseOperationIsRejectedByBackend(){login("admin","ADMIN");Long wid=inventory.createWarehouse(new WarehouseRequest("IT7-PRIVATE-"+System.nanoTime(),"未授权仓",null,null,"ENABLED"));login("warehouse","WAREHOUSE_ADMIN");BusinessException e=assertThrows(BusinessException.class,()->inventory.inbound(new InventoryOperationRequest(wid,spare("SP-DEMO-001"),BigDecimal.ONE,null,"越权测试")));assertEquals(ErrorCode.DATA_FORBIDDEN,e.getErrorCode());assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM inv_stock WHERE warehouse_id=?",Integer.class,wid));}
    @Test @Order(4) void inboundOutboundAndInsufficientRollbackAreConsistent(){login("warehouse","WAREHOUSE_ADMIN");Long wid=warehouse("WH-DEMO-01"),pid=spare("SP-DEMO-001");String trace="第7阶段入出库-"+System.nanoTime(),failureTrace=trace+"-库存不足";BigDecimal before=qty(wid,pid);inventory.inbound(new InventoryOperationRequest(wid,pid,new BigDecimal("3"),new BigDecimal("85.50"),trace));assertEquals(0,qty(wid,pid).compareTo(before.add(new BigDecimal("3"))));inventory.outbound(new InventoryOperationRequest(wid,pid,new BigDecimal("2"),null,trace));BigDecimal after=qty(wid,pid);assertEquals(0,after.compareTo(before.add(BigDecimal.ONE)));assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM inv_transaction WHERE warehouse_id=? AND spare_part_id=? AND remark=?",Integer.class,wid,pid,trace));assertThrows(BusinessException.class,()->inventory.outbound(new InventoryOperationRequest(wid,pid,after.add(BigDecimal.ONE),null,failureTrace)));assertEquals(0,qty(wid,pid).compareTo(after));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM inv_transaction WHERE remark=?",Integer.class,failureTrace));}
    @Test @Order(5) void transferUpdatesBothWarehousesAndCreatesPairedLedger(){login("admin","ADMIN");Long source=warehouse("WH-DEMO-01"),target=warehouse("WH-DEMO-02");String suffix=String.valueOf(System.nanoTime());Long pid=inventory.createSparePart(new SparePartRequest("IT7-TRANSFER-"+suffix,"调拨专用备件",null,null,"件",BigDecimal.TEN,null,null,1,1,"ENABLED"));inventory.inbound(new InventoryOperationRequest(source,pid,new BigDecimal("5"),null,"调拨独立期初库存"));inventory.inbound(new InventoryOperationRequest(target,pid,new BigDecimal("2"),null,"调拨独立期初库存"));login("warehouse","WAREHOUSE_ADMIN");String trace="第7阶段调拨-"+suffix;BigDecimal s=qty(source,pid),t=qty(target,pid);inventory.transfer(new TransferRequest(source,target,pid,new BigDecimal("1.25"),trace));assertEquals(0,qty(source,pid).compareTo(s.subtract(new BigDecimal("1.25"))));assertEquals(0,qty(target,pid).compareTo(t.add(new BigDecimal("1.25"))));assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM inv_transaction WHERE remark=? AND transaction_type IN ('TRANSFER_OUT','TRANSFER_IN') AND related_transaction_id IS NOT NULL",Integer.class,trace));}
    @Test @Order(6) void stocktakeGainLossAndWarningLifecycleArePersisted(){login("admin","ADMIN");String suffix=String.valueOf(System.nanoTime());Long wid=warehouse("WH-DEMO-02");Long pid=inventory.createSparePart(new SparePartRequest("IT7-WARN-"+suffix,"预警测试备件",null,null,"件",BigDecimal.TEN,null,null,2,30,"ENABLED"));inventory.inbound(new InventoryOperationRequest(wid,pid,new BigDecimal("5"),null,"预警测试入库"));inventory.outbound(new InventoryOperationRequest(wid,pid,new BigDecimal("4"),null,"预警测试消耗"));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM inv_warning WHERE warehouse_id=? AND spare_part_id=? AND status='OPEN'",Integer.class,wid,pid));inventory.inbound(new InventoryOperationRequest(wid,pid,new BigDecimal("3"),null,"预警补货"));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM inv_warning WHERE warehouse_id=? AND spare_part_id=? AND status='CLOSED'",Integer.class,wid,pid));BigDecimal current=qty(wid,pid);inventory.stocktake(new StocktakeRequest(wid,pid,current.add(BigDecimal.ONE),"真实盘盈"));inventory.stocktake(new StocktakeRequest(wid,pid,current,"真实盘亏"));assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM inv_transaction WHERE warehouse_id=? AND spare_part_id=? AND transaction_type='STOCKTAKE'",Integer.class,wid,pid));}
    @Test @Order(7) void completeRepairInventoryWorkflowKeepsHistoryAfterClose(){
        login("admin","ADMIN");
        String suffix=String.valueOf(System.nanoTime());
        Long wid=warehouse("WH-DEMO-01");
        Long pid=inventory.createSparePart(new SparePartRequest("IT7-FLOW-"+suffix,"闭环专用备件",null,null,"件",BigDecimal.TEN,null,null,1,1,"ENABLED"));
        inventory.inbound(new InventoryOperationRequest(wid,pid,new BigDecimal("5"),null,"闭环测试独立期初库存"));
        Long equipmentId=createRunningEquipment();
        login("reporter","REPORTER");
        var created=repairs.create(new RepairRequestCreateRequest(equipmentId,RepairPriority.IMPORTANT,"主轴轴承异响",RepairSource.PC));
        login("supervisor","MAINTENANCE_SUPERVISOR");
        orders.assign(created.workOrderId(),new DispatchRequest(user("engineer"),team(),"第7阶段闭环派单"));
        login("engineer","ENGINEER");
        orders.acceptResponse(created.workOrderId());orders.start(created.workOrderId());
        BigDecimal before=qty(wid,pid);
        Long issueId=inventory.issueForWorkOrder(created.workOrderId(),new WorkOrderSpareRequest(wid,pid,new BigDecimal("1.00"),"更换主轴轴承"));
        assertEquals(0,qty(wid,pid).compareTo(before.subtract(BigDecimal.ONE)));
        inventory.returnForWorkOrder(created.workOrderId(),issueId,new ReturnSpareRequest(new BigDecimal("0.25"),"未使用部分退回"));
        assertEquals(0,qty(wid,pid).compareTo(before.subtract(new BigDecimal("0.75"))));
        orders.saveRepairRecord(created.workOrderId(),new RepairRecordRequest("检查主轴","轴承磨损","更换并校准","试运行正常",new BigDecimal("1.50"),90,true));
        orders.submitAcceptance(created.workOrderId());login("reporter","REPORTER");orders.pass(created.workOrderId(),new AcceptanceRequest("验收通过"));
        assertEquals("COMPLETED",jdbc.queryForObject("SELECT status FROM mnt_work_order WHERE id=?",String.class,created.workOrderId()));
        assertEquals("RUNNING",jdbc.queryForObject("SELECT status FROM eqp_equipment WHERE id=?",String.class,equipmentId));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM inv_work_order_spare WHERE id=? AND status='PARTIAL_RETURN'",Integer.class,issueId));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM inv_transaction WHERE work_order_id=? AND transaction_type IN ('ISSUE','RETURN')",Integer.class,created.workOrderId()));
        login("engineer","ENGINEER");
        assertThrows(BusinessException.class,()->inventory.issueForWorkOrder(created.workOrderId(),new WorkOrderSpareRequest(wid,pid,BigDecimal.ONE,"关单后非法领用")));
        assertEquals(1,inventory.workOrderSpares(created.workOrderId()).size());
    }
}
