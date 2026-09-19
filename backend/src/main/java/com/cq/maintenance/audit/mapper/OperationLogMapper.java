package com.cq.maintenance.audit.mapper;
import com.cq.maintenance.audit.dto.OperationLogQuery;
import com.cq.maintenance.audit.entity.OperationLog;
import java.util.List;
import org.apache.ibatis.annotations.*;
@Mapper public interface OperationLogMapper {
    String FILTER="<where>1=1 <if test='q.module != null and q.module != &quot;&quot;'> AND l.module=#{q.module}</if><if test='q.result != null and q.result != &quot;&quot;'> AND l.result=#{q.result}</if><if test='q.userId != null'> AND l.user_id=#{q.userId}</if></where>";
    @Insert("INSERT INTO sys_operation_log(user_id,module,operation,request_uri,http_method,ip_address,request_summary,result,error_message,duration_ms) VALUES(#{userId},#{module},#{operation},#{requestUri},#{httpMethod},#{ipAddress},#{requestSummary},#{result},#{errorMessage},#{durationMs})") int insert(@Param("userId")Long userId,@Param("module")String module,@Param("operation")String operation,@Param("requestUri")String requestUri,@Param("httpMethod")String httpMethod,@Param("ipAddress")String ipAddress,@Param("requestSummary")String requestSummary,@Param("result")String result,@Param("errorMessage")String errorMessage,@Param("durationMs")Long durationMs);
    @Select("<script>SELECT l.id,l.user_id,u.username,l.module,l.operation,l.request_uri,l.http_method,l.ip_address,l.request_summary,l.result,l.error_message,l.duration_ms,l.created_at FROM sys_operation_log l LEFT JOIN sys_user u ON u.id=l.user_id "+FILTER+" ORDER BY l.created_at DESC,l.id DESC LIMIT #{q.size} OFFSET #{q.offset}</script>") List<OperationLog> page(@Param("q")OperationLogQuery query);
    @Select("<script>SELECT COUNT(*) FROM sys_operation_log l "+FILTER+"</script>") long count(@Param("q")OperationLogQuery query);
}
