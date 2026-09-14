USE industrial_maintenance;
SET NAMES utf8mb4;

-- 仅用于本地开发与答辩演示：固定技能数据保证推荐与实验可复现。
INSERT INTO org_skill(skill_code,skill_name,description,status) VALUES
 ('MECHANICAL','机械检修','机械传动、轴承与装配检修','ENABLED'),
 ('ELECTRICAL','电气检修','低压电气与控制回路检修','ENABLED'),
 ('CNC','数控系统','数控系统诊断与参数维护','ENABLED')
ON DUPLICATE KEY UPDATE skill_name=VALUES(skill_name),description=VALUES(description),status='ENABLED';

INSERT IGNORE INTO eqp_type_skill(type_id,skill_id)
SELECT t.id,s.id FROM eqp_type t JOIN org_skill s
WHERE (t.type_code='CNC' AND s.skill_code IN ('MECHANICAL','CNC'))
   OR (t.type_code='ASSEMBLY' AND s.skill_code IN ('MECHANICAL','ELECTRICAL'));

INSERT IGNORE INTO org_user_skill(user_id,skill_id,skill_level)
SELECT u.id,s.id,CASE s.skill_code WHEN 'MECHANICAL' THEN 5 WHEN 'CNC' THEN 4 ELSE 3 END
FROM sys_user u JOIN org_skill s
WHERE u.username='engineer' AND s.skill_code IN ('MECHANICAL','CNC');

INSERT INTO org_team(team_no,team_name,workshop_id,leader_id,status)
SELECT 'TEAM-ALT','机加工机动组',w.id,u.id,'ENABLED' FROM org_workshop w JOIN sys_user u ON u.username='supervisor' WHERE w.workshop_no='WS-01'
ON DUPLICATE KEY UPDATE team_name=VALUES(team_name),workshop_id=VALUES(workshop_id),leader_id=VALUES(leader_id),status='ENABLED';

INSERT INTO sys_user(username,password_hash,real_name,job_title,team_id,workshop_id,status)
SELECT 'engineer2',base.password_hash,'王工','电气工程师',t.id,w.id,'ENABLED' FROM sys_user base JOIN org_team t ON t.team_no='TEAM-ALT' JOIN org_workshop w ON w.workshop_no='WS-01' WHERE base.username='engineer'
ON DUPLICATE KEY UPDATE real_name=VALUES(real_name),job_title=VALUES(job_title),team_id=VALUES(team_id),workshop_id=VALUES(workshop_id),status='ENABLED';
INSERT INTO sys_user(username,password_hash,real_name,job_title,team_id,workshop_id,status)
SELECT 'engineer3',base.password_hash,'赵工','机械工程师',t.id,w.id,'ENABLED' FROM sys_user base JOIN org_team t ON t.team_no='TEAM-01' JOIN org_workshop w ON w.workshop_no='WS-01' WHERE base.username='engineer'
ON DUPLICATE KEY UPDATE real_name=VALUES(real_name),job_title=VALUES(job_title),team_id=VALUES(team_id),workshop_id=VALUES(workshop_id),status='ENABLED';

INSERT IGNORE INTO sys_user_role(user_id,role_id)
SELECT u.id,r.id FROM sys_user u JOIN sys_role r ON r.role_code='ENGINEER' WHERE u.username IN ('engineer2','engineer3');
INSERT IGNORE INTO org_user_skill(user_id,skill_id,skill_level)
SELECT u.id,s.id,CASE WHEN u.username='engineer2' THEN 4 ELSE 5 END FROM sys_user u JOIN org_skill s
WHERE (u.username='engineer2' AND s.skill_code='ELECTRICAL') OR (u.username='engineer3' AND s.skill_code='MECHANICAL');
