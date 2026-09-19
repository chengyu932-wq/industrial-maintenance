package com.cq.maintenance.audit.service;
import com.cq.maintenance.audit.dto.OperationLogQuery;
import com.cq.maintenance.audit.entity.OperationLog;
import com.cq.maintenance.audit.mapper.OperationLogMapper;
import com.cq.maintenance.common.response.PageResult;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
@Service public class OperationLogService {private final ObjectProvider<OperationLogMapper> mapperProvider;public OperationLogService(ObjectProvider<OperationLogMapper> mapperProvider){this.mapperProvider=mapperProvider;}public PageResult<OperationLog> page(OperationLogQuery q){OperationLogMapper mapper=mapperProvider.getIfAvailable();if(mapper==null)throw new IllegalStateException("操作日志存储未启用");return PageResult.of(mapper.page(q),mapper.count(q),q.getPage(),q.getSize());}}
