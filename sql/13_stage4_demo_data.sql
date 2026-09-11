USE industrial_maintenance;
SET NAMES utf8mb4;

-- 可选：仅用于本地开发与答辩演示，依赖 11_dev_auth_seed.sql。
INSERT INTO org_workshop(workshop_no,workshop_name,manager_id,status)
SELECT 'WS-01','机加工车间',u.id,'ENABLED' FROM sys_user u WHERE u.username='supervisor'
ON DUPLICATE KEY UPDATE workshop_name=VALUES(workshop_name),manager_id=VALUES(manager_id),status=VALUES(status);
INSERT INTO org_workshop(workshop_no,workshop_name,status) VALUES('WS-02','装配车间','ENABLED')
ON DUPLICATE KEY UPDATE workshop_name=VALUES(workshop_name),status=VALUES(status);

INSERT INTO org_line(workshop_id,line_no,line_name,status)
SELECT id,'LINE-A','A生产线','ENABLED' FROM org_workshop WHERE workshop_no='WS-01'
ON DUPLICATE KEY UPDATE line_name=VALUES(line_name),status=VALUES(status);
INSERT INTO org_line(workshop_id,line_no,line_name,status)
SELECT id,'LINE-B','B装配线','ENABLED' FROM org_workshop WHERE workshop_no='WS-02'
ON DUPLICATE KEY UPDATE line_name=VALUES(line_name),status=VALUES(status);

INSERT INTO org_station(line_id,station_no,station_name,status)
SELECT id,'ST-01','数控加工工位','ENABLED' FROM org_line WHERE line_no='LINE-A'
ON DUPLICATE KEY UPDATE station_name=VALUES(station_name),status=VALUES(status);
INSERT INTO org_station(line_id,station_no,station_name,status)
SELECT id,'ST-02','总装工位','ENABLED' FROM org_line WHERE line_no='LINE-B'
ON DUPLICATE KEY UPDATE station_name=VALUES(station_name),status=VALUES(status);

INSERT INTO org_team(team_no,team_name,workshop_id,leader_id,status)
SELECT 'TEAM-01','机加工运维组',w.id,u.id,'ENABLED' FROM org_workshop w JOIN sys_user u ON u.username='supervisor' WHERE w.workshop_no='WS-01'
ON DUPLICATE KEY UPDATE team_name=VALUES(team_name),workshop_id=VALUES(workshop_id),leader_id=VALUES(leader_id),status=VALUES(status);

UPDATE sys_user u JOIN org_workshop w ON w.workshop_no='WS-01' SET u.workshop_id=w.id WHERE u.username='supervisor';
UPDATE sys_user u JOIN org_team t ON t.team_no='TEAM-01' SET u.team_id=t.id WHERE u.username IN ('supervisor','engineer');

INSERT INTO eqp_type(type_code,type_name,description,status) VALUES
 ('CNC','数控机床','机加工数控设备','ENABLED'),('ASSEMBLY','装配设备','装配线通用设备','ENABLED')
ON DUPLICATE KEY UPDATE type_name=VALUES(type_name),description=VALUES(description),status=VALUES(status);

INSERT INTO eqp_equipment(equipment_no,equipment_name,type_id,model,manufacturer,specifications,manufacture_date,commissioning_date,responsible_user_id,responsible_team_id,station_id,warranty_expire_date,status,running_hours,qr_code)
SELECT 'EQP202609110001','一号数控机床',t.id,'CK-6150','示范制造商','额定功率 15kW','2025-05-20','2026-01-10',u.id,tm.id,s.id,'2027-05-20','RUNNING',1280.00,'demo-eqp-202609110001'
FROM eqp_type t JOIN org_station s ON s.station_no='ST-01' JOIN org_team tm ON tm.team_no='TEAM-01' JOIN sys_user u ON u.username='engineer'
WHERE t.type_code='CNC'
ON DUPLICATE KEY UPDATE equipment_name=VALUES(equipment_name),type_id=VALUES(type_id),station_id=VALUES(station_id),responsible_user_id=VALUES(responsible_user_id),responsible_team_id=VALUES(responsible_team_id);

INSERT INTO eqp_status_log(equipment_id,from_status,to_status,source_type,reason,operator_id)
SELECT e.id,NULL,'RUNNING','MANUAL','演示数据初始化',u.id FROM eqp_equipment e JOIN sys_user u ON u.username='admin'
WHERE e.equipment_no='EQP202609110001' AND NOT EXISTS(SELECT 1 FROM eqp_status_log l WHERE l.equipment_id=e.id);
