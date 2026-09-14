package com.cq.maintenance.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cq.maintenance.knowledge.dto.KnowledgeQuery;
import com.cq.maintenance.knowledge.entity.KnowledgeArticle;
import com.cq.maintenance.knowledge.entity.KnowledgeStatus;
import com.cq.maintenance.knowledge.vo.KnowledgeArticleVO;
import com.cq.maintenance.knowledge.vo.KnowledgeSourceRow;
import com.cq.maintenance.knowledge.vo.SimilarWorkOrderRow;
import com.cq.maintenance.workorder.mapper.WorkOrderMapper.WorkOrderDataScope;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface KnowledgeMapper extends BaseMapper<KnowledgeArticle> {
    String ARTICLE_FROM=" FROM kb_article a JOIN mnt_work_order wo ON wo.id=a.source_work_order_id LEFT JOIN mnt_repair_request rr ON rr.id=wo.repair_request_id JOIN eqp_equipment e ON e.id=wo.equipment_id "+
        "JOIN eqp_type et ON et.id=a.equipment_type_id JOIN org_station st ON st.id=e.station_id JOIN org_line l ON l.id=st.line_id JOIN org_workshop w ON w.id=l.workshop_id "+
        "LEFT JOIN sys_user submitter ON submitter.id=a.submitted_by LEFT JOIN sys_user reviewer ON reviewer.id=a.reviewed_by ";
    String ARTICLE_COLUMNS="a.id,a.title,a.source_work_order_id,wo.work_order_no,wo.equipment_id,e.equipment_no,e.equipment_name,a.equipment_type_id,et.type_name equipment_type_name,"+
        "a.fault_symptom,a.root_cause,a.solution,a.repair_result,a.spare_summary,a.status,a.submitted_by,submitter.real_name submitter_name,a.reviewed_by,reviewer.real_name reviewer_name,"+
        "a.review_remark,a.reviewed_at,a.created_at,a.updated_at";
    String SOURCE_SCOPE="<if test='scope.allData == false'> AND (<choose>"+
        "<when test='scope.mode == &quot;SUPERVISOR&quot;'><if test='scope.workshopId != null'>w.id=#{scope.workshopId}</if><if test='scope.workshopId == null'>1=0</if><if test='scope.teamIds != null and scope.teamIds.size() > 0'> OR wo.assigned_team_id IN <foreach collection='scope.teamIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> OR e.responsible_team_id IN <foreach collection='scope.teamIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if></when>"+
        "<when test='scope.mode == &quot;ENGINEER&quot;'>wo.assigned_engineer_id=#{scope.userId}<if test='scope.teamIds != null and scope.teamIds.size() > 0'> OR wo.assigned_team_id IN <foreach collection='scope.teamIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if></when>"+
        "<when test='scope.mode == &quot;REPORTER&quot;'>rr.reporter_id=#{scope.userId}</when><otherwise>1=0</otherwise></choose>)</if>";
    String ARTICLE_SCOPE="<if test='scope.allData == false'> AND (a.status='PUBLISHED' OR a.submitted_by=#{scope.userId}<if test='scope.mode == &quot;SUPERVISOR&quot;'><if test='scope.workshopId != null'> OR w.id=#{scope.workshopId}</if><if test='scope.teamIds != null and scope.teamIds.size() > 0'> OR wo.assigned_team_id IN <foreach collection='scope.teamIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> OR e.responsible_team_id IN <foreach collection='scope.teamIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if></if>)</if>";
    String ARTICLE_QUERY="<if test='q.keyword != null and q.keyword != &quot;&quot;'> AND (a.title LIKE CONCAT('%',#{q.keyword},'%') OR a.fault_symptom LIKE CONCAT('%',#{q.keyword},'%') OR a.root_cause LIKE CONCAT('%',#{q.keyword},'%') OR a.solution LIKE CONCAT('%',#{q.keyword},'%'))</if>"+
        "<if test='q.equipmentTypeId != null'> AND a.equipment_type_id=#{q.equipmentTypeId}</if><if test='q.status != null'> AND a.status=#{q.status}</if>"+
        "<if test='q.sourceWorkOrderNo != null and q.sourceWorkOrderNo != &quot;&quot;'> AND wo.work_order_no LIKE CONCAT('%',#{q.sourceWorkOrderNo},'%')</if>"+
        "<if test='q.startDate != null'> AND DATE(a.created_at)&gt;=#{q.startDate}</if><if test='q.endDate != null'> AND DATE(a.created_at)&lt;=#{q.endDate}</if>";

    @Select("<script>SELECT "+ARTICLE_COLUMNS+ARTICLE_FROM+" WHERE 1=1 "+ARTICLE_QUERY+ARTICLE_SCOPE+" ORDER BY a.updated_at DESC,a.id DESC LIMIT #{q.size} OFFSET #{q.offset}</script>")
    List<KnowledgeArticleVO> page(@Param("q") KnowledgeQuery query,@Param("scope") WorkOrderDataScope scope);
    @Select("<script>SELECT COUNT(*)"+ARTICLE_FROM+" WHERE 1=1 "+ARTICLE_QUERY+ARTICLE_SCOPE+"</script>")
    long countPage(@Param("q") KnowledgeQuery query,@Param("scope") WorkOrderDataScope scope);
    @Select("SELECT "+ARTICLE_COLUMNS+ARTICLE_FROM+" WHERE a.id=#{id}") KnowledgeArticleVO detail(Long id);
    @Select("SELECT id,title,source_work_order_id,equipment_type_id,fault_symptom,root_cause,solution,repair_result,spare_summary,status,submitted_by,reviewed_by,review_remark,reviewed_at,created_at,updated_at FROM kb_article WHERE id=#{id} FOR UPDATE") KnowledgeArticle lock(Long id);
    @Select("SELECT COUNT(*) FROM kb_article WHERE source_work_order_id=#{workOrderId}") long countBySource(Long workOrderId);
    @Insert("INSERT INTO kb_article(title,source_work_order_id,equipment_type_id,fault_symptom,root_cause,solution,repair_result,spare_summary,status,submitted_by) VALUES(#{title},#{sourceWorkOrderId},#{equipmentTypeId},#{faultSymptom},#{rootCause},#{solution},#{repairResult},#{spareSummary},#{status},#{submittedBy})")
    @Options(useGeneratedKeys=true,keyProperty="id") int insertArticle(KnowledgeArticle article);
    @Update("UPDATE kb_article SET title=#{title},fault_symptom=#{faultSymptom},root_cause=#{rootCause},solution=#{solution},repair_result=#{repairResult},status='DRAFT',reviewed_by=NULL,review_remark=NULL,reviewed_at=NULL WHERE id=#{id}") int updateDraft(KnowledgeArticle article);
    @Update("UPDATE kb_article SET status='PENDING',reviewed_by=NULL,review_remark=NULL,reviewed_at=NULL WHERE id=#{id} AND status IN ('DRAFT','REJECTED')") int submit(Long id);
    @Update("UPDATE kb_article SET status=#{status},reviewed_by=#{reviewerId},review_remark=#{remark},reviewed_at=NOW() WHERE id=#{id} AND status='PENDING'") int review(@Param("id") Long id,@Param("status") KnowledgeStatus status,@Param("reviewerId") Long reviewerId,@Param("remark") String remark);

    @Select("SELECT wo.id work_order_id,wo.work_order_no,wo.equipment_id,e.equipment_no,e.equipment_name,e.type_id equipment_type_id,et.type_name equipment_type_name,rr.fault_description,r.inspection_process,r.root_cause,r.repair_action,r.repair_result,"+
        "(SELECT GROUP_CONCAT(CONCAT(p.spare_name,' × ',TRIM(TRAILING '.' FROM TRIM(TRAILING '0' FROM CAST(i.qty-COALESCE((SELECT SUM(t.qty_change) FROM inv_transaction t WHERE t.related_transaction_id=i.transaction_id AND t.transaction_type='RETURN'),0) AS CHAR))), ' ',p.unit) ORDER BY p.spare_name SEPARATOR '；') FROM inv_work_order_spare i JOIN inv_spare_part p ON p.id=i.spare_part_id WHERE i.work_order_id=wo.id AND i.qty-COALESCE((SELECT SUM(t.qty_change) FROM inv_transaction t WHERE t.related_transaction_id=i.transaction_id AND t.transaction_type='RETURN'),0)>0) spare_summary "+
        "FROM mnt_work_order wo JOIN mnt_repair_request rr ON rr.id=wo.repair_request_id JOIN mnt_repair_record r ON r.work_order_id=wo.id JOIN eqp_equipment e ON e.id=wo.equipment_id JOIN eqp_type et ON et.id=e.type_id WHERE wo.id=#{workOrderId} AND wo.work_order_type='REPAIR' AND wo.status='COMPLETED'")
    KnowledgeSourceRow source(Long workOrderId);

    @Select("<script>SELECT wo.id work_order_id,wo.work_order_no,wo.equipment_id,e.equipment_no,e.equipment_name,e.type_id equipment_type_id,et.type_name equipment_type_name,rr.fault_description,r.root_cause,r.repair_action,r.repair_result,"+
        "(SELECT GROUP_CONCAT(CONCAT(p.spare_name,' × ',TRIM(TRAILING '.' FROM TRIM(TRAILING '0' FROM CAST(i.qty-COALESCE((SELECT SUM(t.qty_change) FROM inv_transaction t WHERE t.related_transaction_id=i.transaction_id AND t.transaction_type='RETURN'),0) AS CHAR))), ' ',p.unit) ORDER BY p.spare_name SEPARATOR '；') FROM inv_work_order_spare i JOIN inv_spare_part p ON p.id=i.spare_part_id WHERE i.work_order_id=wo.id AND i.qty-COALESCE((SELECT SUM(t.qty_change) FROM inv_transaction t WHERE t.related_transaction_id=i.transaction_id AND t.transaction_type='RETURN'),0)&gt;0) spare_summary,wo.completed_at "+
        "FROM mnt_work_order wo JOIN mnt_repair_request rr ON rr.id=wo.repair_request_id JOIN mnt_repair_record r ON r.work_order_id=wo.id JOIN eqp_equipment e ON e.id=wo.equipment_id JOIN eqp_type et ON et.id=e.type_id JOIN org_station st ON st.id=e.station_id JOIN org_line l ON l.id=st.line_id JOIN org_workshop w ON w.id=l.workshop_id WHERE wo.work_order_type='REPAIR' AND wo.status='COMPLETED' AND rr.fault_description IS NOT NULL AND TRIM(rr.fault_description) != '' <if test='excludeId != null'>AND wo.id != #{excludeId}</if> "+SOURCE_SCOPE+" ORDER BY wo.completed_at DESC,wo.id ASC</script>")
    List<SimilarWorkOrderRow> similarityCorpus(@Param("scope") WorkOrderDataScope scope,@Param("excludeId") Long excludeId);
    @Select("SELECT rr.fault_description FROM mnt_work_order wo JOIN mnt_repair_request rr ON rr.id=wo.repair_request_id WHERE wo.id=#{workOrderId} AND wo.work_order_type='REPAIR'") String faultDescription(Long workOrderId);
}
