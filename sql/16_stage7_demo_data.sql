USE industrial_maintenance;
SET NAMES utf8mb4;

-- 仅用于本地开发与答辩演示，不覆盖已发生库存业务。
INSERT INTO inv_supplier(supplier_no,supplier_name,contact_name,phone,address,status)
VALUES('SUP-DEMO-01','绵阳工业备件供应商','演示联系人','0816-0000000','绵阳','ENABLED')
ON DUPLICATE KEY UPDATE supplier_name=VALUES(supplier_name),status='ENABLED';

INSERT INTO inv_warehouse(warehouse_no,warehouse_name,location,manager_id,status)
SELECT 'WH-DEMO-01','中心备件仓','一号车间东侧',u.id,'ENABLED' FROM sys_user u WHERE u.username='warehouse'
ON DUPLICATE KEY UPDATE warehouse_name=VALUES(warehouse_name),location=VALUES(location),manager_id=VALUES(manager_id),status='ENABLED';
INSERT INTO inv_warehouse(warehouse_no,warehouse_name,location,manager_id,status)
SELECT 'WH-DEMO-02','产线周转仓','一号车间西侧',u.id,'ENABLED' FROM sys_user u WHERE u.username='warehouse'
ON DUPLICATE KEY UPDATE warehouse_name=VALUES(warehouse_name),location=VALUES(location),manager_id=VALUES(manager_id),status='ENABLED';

INSERT INTO inv_spare_part(spare_no,spare_name,specification,brand,unit,unit_price,supplier_id,compatible_model,lead_time_days,safety_days,status)
SELECT 'SP-DEMO-001','主轴轴承','6205-2RS','HRB','件',86.50,s.id,'CNC-850',5,3,'ENABLED' FROM inv_supplier s WHERE s.supplier_no='SUP-DEMO-01'
ON DUPLICATE KEY UPDATE spare_name=VALUES(spare_name),specification=VALUES(specification),brand=VALUES(brand),unit=VALUES(unit),unit_price=VALUES(unit_price),supplier_id=VALUES(supplier_id),compatible_model=VALUES(compatible_model),lead_time_days=VALUES(lead_time_days),safety_days=VALUES(safety_days),status='ENABLED';

INSERT IGNORE INTO inv_warehouse_user(warehouse_id,user_id)
SELECT w.id,u.id FROM inv_warehouse w JOIN sys_user u ON u.username IN ('warehouse','engineer') WHERE w.warehouse_no IN ('WH-DEMO-01','WH-DEMO-02');

INSERT INTO inv_stock(warehouse_id,spare_part_id,current_qty)
SELECT w.id,p.id,20.00 FROM inv_warehouse w JOIN inv_spare_part p
WHERE w.warehouse_no='WH-DEMO-01' AND p.spare_no='SP-DEMO-001' AND NOT EXISTS(SELECT 1 FROM inv_stock s WHERE s.warehouse_id=w.id AND s.spare_part_id=p.id);
INSERT INTO inv_stock(warehouse_id,spare_part_id,current_qty)
SELECT w.id,p.id,5.00 FROM inv_warehouse w JOIN inv_spare_part p
WHERE w.warehouse_no='WH-DEMO-02' AND p.spare_no='SP-DEMO-001' AND NOT EXISTS(SELECT 1 FROM inv_stock s WHERE s.warehouse_id=w.id AND s.spare_part_id=p.id);

INSERT INTO inv_transaction(transaction_no,warehouse_id,spare_part_id,transaction_type,qty_change,qty_before,qty_after,unit_price,operator_id,remark)
SELECT CONCAT('TX-DEMO-',w.warehouse_no),w.id,p.id,'INBOUND',s.current_qty,0,s.current_qty,p.unit_price,u.id,'第7阶段演示期初入库'
FROM inv_warehouse w JOIN inv_spare_part p JOIN inv_stock s ON s.warehouse_id=w.id AND s.spare_part_id=p.id JOIN sys_user u ON u.username='admin'
WHERE w.warehouse_no IN ('WH-DEMO-01','WH-DEMO-02') AND p.spare_no='SP-DEMO-001' AND s.current_qty>0
  AND NOT EXISTS(SELECT 1 FROM inv_transaction t WHERE t.transaction_no=CONCAT('TX-DEMO-',w.warehouse_no));
