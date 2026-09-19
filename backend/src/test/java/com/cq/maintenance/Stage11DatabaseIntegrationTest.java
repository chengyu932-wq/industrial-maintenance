package com.cq.maintenance;

import static org.junit.jupiter.api.Assertions.*;
import com.cq.maintenance.security.LoginUser;
import com.cq.maintenance.statistics.dto.*;
import com.cq.maintenance.statistics.service.StatisticsService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

@SpringBootTest(properties={"app.auth.jwt-secret=test-only-secret-that-is-at-least-32-bytes-long","app.maintenance.scan-cron=-","app.sla.scan-cron=-"})
@ActiveProfiles("local") @TestPropertySource(properties="stage11.db-tests=true")
@EnabledIfSystemProperty(named="stage11.db-tests",matches="true")
@Sql(scripts={"file:../sql/23_stage11_statistics.sql","file:../sql/24_stage11_permissions.sql"}) @SqlConfig(encoding="UTF-8")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Stage11DatabaseIntegrationTest {
    @Autowired JdbcTemplate jdbc; @Autowired StatisticsService statistics;
    Long workshop,team,equipment1,equipment2,warehouse,spare,supervisor;
    final LocalDate start=LocalDate.of(2026,8,1),end=LocalDate.of(2026,8,3);

    @BeforeAll void seed() {
        String suffix=Long.toString(System.nanoTime());supervisor=user("supervisor");Long admin=user("admin");Long engineer=user("engineer");Long reporter=user("reporter");
        jdbc.update("INSERT INTO org_workshop(workshop_no,workshop_name,status) VALUES(?,?,'ENABLED')","S11-WS"+suffix,"第11阶段统计车间");workshop=id("SELECT id FROM org_workshop WHERE workshop_no=?","S11-WS"+suffix);
        jdbc.update("INSERT INTO org_line(workshop_id,line_no,line_name,status) VALUES(?,?,?,'ENABLED')",workshop,"S11-L"+suffix,"统计产线");Long line=id("SELECT id FROM org_line WHERE line_no=?","S11-L"+suffix);
        jdbc.update("INSERT INTO org_station(line_id,station_no,station_name,status) VALUES(?,?,?,'ENABLED')",line,"S11-ST"+suffix,"统计工位");Long station=id("SELECT id FROM org_station WHERE station_no=?","S11-ST"+suffix);
        jdbc.update("INSERT INTO org_team(team_no,team_name,workshop_id,status) VALUES(?,?,?,'ENABLED')","S11-T"+suffix,"统计班组",workshop);team=id("SELECT id FROM org_team WHERE team_no=?","S11-T"+suffix);
        Long type=id("SELECT id FROM eqp_type ORDER BY id LIMIT 1");
        equipment1=equipment("S11-E1"+suffix,"统计设备甲",type,station,team,"RUNNING");equipment2=equipment("S11-E2"+suffix,"统计设备乙",type,station,team,"REPAIRING");
        jdbc.update("INSERT INTO eqp_status_log(equipment_id,from_status,to_status,source_type,reason,operator_id,changed_at) VALUES(?,NULL,'RUNNING','MANUAL','统计起点',?,'2026-08-01 00:00:00'),(?,'RUNNING','FAULT','MANUAL','故障',?,'2026-08-02 12:00:00'),(?,'FAULT','RUNNING','MANUAL','恢复',?,'2026-08-03 00:00:00')",equipment1,admin,equipment1,admin,equipment1,admin);
        jdbc.update("INSERT INTO eqp_runtime_record(equipment_id,record_date,running_hours_increment,total_running_hours,recorded_by) VALUES(?,'2026-08-01',40,40,?),(?,'2026-08-02',20,60,?)",equipment1,engineer,equipment1,engineer);
        repair("S11-A"+suffix,equipment1,reporter,engineer,"2026-08-01 08:00:00","2026-08-01 10:00:00","2026-08-01 11:00:00",admin);
        repair("S11-B"+suffix,equipment1,reporter,engineer,"2026-08-02 08:00:00","2026-08-02 12:00:00","2026-08-02 11:00:00",admin);
        jdbc.update("INSERT INTO inv_warehouse(warehouse_no,warehouse_name,status) VALUES(?,?,'ENABLED')","S11-WH"+suffix,"统计仓库");warehouse=id("SELECT id FROM inv_warehouse WHERE warehouse_no=?","S11-WH"+suffix);
        jdbc.update("INSERT INTO inv_spare_part(spare_no,spare_name,unit,unit_price,lead_time_days,safety_days,status) VALUES(?,?, '件',10,3,2,'ENABLED')","S11-SP"+suffix,"统计轴承");spare=id("SELECT id FROM inv_spare_part WHERE spare_no=?","S11-SP"+suffix);
        jdbc.update("INSERT INTO inv_stock(warehouse_id,spare_part_id,current_qty) VALUES(?,?,80)",warehouse,spare);
        jdbc.update("INSERT INTO inv_transaction(transaction_no,warehouse_id,spare_part_id,transaction_type,qty_change,qty_before,qty_after,unit_price,operator_id,created_at) VALUES(?,?,?,'INBOUND',100,0,100,10,?,'2026-07-31 08:00:00'),(?,?,?,'ISSUE',-20,100,80,10,?,'2026-08-01 12:00:00')","S11-TX0"+suffix,warehouse,spare,admin,"S11-TX1"+suffix,warehouse,spare,engineer);
        jdbc.update("INSERT INTO inv_warning(warehouse_id,spare_part_id,threshold_qty,current_qty,status,triggered_at) VALUES(?,?,90,80,'OPEN','2026-08-01 13:00:00')",warehouse,spare);
    }
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    Long user(String name){return id("SELECT id FROM sys_user WHERE username=?",name);}Long id(String sql,Object...args){return jdbc.queryForObject(sql,Long.class,args);}
    Long equipment(String no,String name,Long type,Long station,Long teamId,String status){jdbc.update("INSERT INTO eqp_equipment(equipment_no,equipment_name,type_id,station_id,responsible_team_id,status,qr_code) VALUES(?,?,?,?,?,?,?)",no,name,type,station,teamId,status,"QR-"+no);return id("SELECT id FROM eqp_equipment WHERE equipment_no=?",no);}
    void repair(String no,Long equipment,Long reporter,Long engineer,String started,String completed,String deadline,Long admin){jdbc.update("INSERT INTO mnt_repair_request(request_no,equipment_id,reporter_id,source,priority,fault_description,reported_at,status) VALUES(?,?,?,'PC','NORMAL','阶段11轴承故障',?,'CONVERTED')","RR-"+no,equipment,reporter,started);Long request=id("SELECT id FROM mnt_repair_request WHERE request_no=?","RR-"+no);jdbc.update("INSERT INTO mnt_work_order(work_order_no,work_order_type,repair_request_id,equipment_id,priority,status,assigned_engineer_id,assigned_team_id,created_at,started_at,completed_at,sla_resolve_deadline,created_by) VALUES(?,'REPAIR',?,?,'NORMAL','COMPLETED',?,?,?, ?,?,?,?)",no,request,equipment,engineer,team,started,started,completed,deadline,admin);Long order=id("SELECT id FROM mnt_work_order WHERE work_order_no=?",no);jdbc.update("INSERT INTO mnt_repair_record(work_order_id,root_cause,repair_action,repair_result,labor_hours,downtime_minutes,repairable,created_by) VALUES(?,'轴承磨损','更换轴承','恢复运行',2,120,1,?)",order,engineer);}
    StatisticsQuery query(){StatisticsQuery q=new StatisticsQuery();q.setRange(StatisticsRange.CUSTOM);q.setStartDate(start);q.setEndDate(end);return q;}
    void supervisorLogin(){LoginUser u=new LoginUser(supervisor,"supervisor","开发主管","ENABLED",List.of("MAINTENANCE_SUPERVISOR"),List.of("statistics:view"),List.of(team),workshop,List.of(warehouse));SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(u,null,List.of()));}
    BigDecimal metric(String code){return statistics.kpis(query()).metrics().stream().filter(v->code.equals(v.code())).findFirst().orElseThrow().value();}

    @Test void schemaIndexAndRolePermissionsExist(){assertEquals(1,jdbc.queryForObject("SELECT COUNT(DISTINCT INDEX_NAME) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='eqp_runtime_record' AND INDEX_NAME='idx_runtime_date_equipment'",Integer.class));assertEquals(4,jdbc.queryForObject("SELECT COUNT(*) FROM sys_role_menu rm JOIN sys_role r ON r.id=rm.role_id JOIN sys_menu m ON m.id=rm.menu_id WHERE m.permission_code='statistics:view' AND r.role_code IN ('ADMIN','MAINTENANCE_SUPERVISOR','ENGINEER','WAREHOUSE_ADMIN')",Integer.class));}
    @Test void overviewMatchesDirectScopedSql(){supervisorLogin();var result=statistics.overview(query());Long direct=jdbc.queryForObject("SELECT COUNT(*) FROM mnt_work_order wo JOIN eqp_equipment e ON e.id=wo.equipment_id JOIN org_station st ON st.id=e.station_id JOIN org_line l ON l.id=st.line_id WHERE l.workshop_id=? AND wo.work_order_type='REPAIR' AND wo.status='COMPLETED' AND wo.completed_at>='2026-08-01' AND wo.completed_at<'2026-08-04'",Long.class,workshop);assertEquals(direct,result.completedRepairOrders());assertEquals(2,result.equipmentTotal());assertEquals(1,result.openStockWarnings());}
    @Test void mtbfAndMttrUseRealRuntimeAndRepairTimes(){supervisorLogin();assertEquals(new BigDecimal("30.00"),metric("MTBF"));assertEquals(new BigDecimal("3.00"),metric("MTTR"));}
    @Test void slaAvailabilityAndTurnoverMatchFrozenFormula(){supervisorLogin();assertEquals(new BigDecimal("50.00"),metric("ON_TIME_CLOSE_RATE"));assertEquals(new BigDecimal("83.33"),metric("EQUIPMENT_AVAILABILITY"));assertEquals(new BigDecimal("0.22"),metric("SPARE_PART_TURNOVER"));}
    @Test void firstTimeFixUsesFirstPassAcceptanceRecords(){supervisorLogin();var value=statistics.kpis(query()).metrics().stream().filter(v->"FIRST_TIME_FIX_RATE".equals(v.code())).findFirst().orElseThrow();assertTrue(value.available());assertEquals(new BigDecimal("100.00"),value.value());assertEquals(2,value.sampleSize());}
    @Test void chartsAreDatabaseAggregatesAndTrendIncludesEmptyDates(){supervisorLogin();var value=statistics.charts(query());assertTrue(value.equipmentStatus().stream().anyMatch(row->"RUNNING".equals(row.name())&&row.value().intValue()==1));assertTrue(value.faultEquipmentTypes().stream().anyMatch(row->row.value().intValue()==2));assertEquals(3,value.repairTrend().size());assertEquals(1,value.repairTrend().get(0).value());assertEquals(1,value.repairTrend().get(1).value());assertEquals(0,value.repairTrend().get(2).value());}
}
