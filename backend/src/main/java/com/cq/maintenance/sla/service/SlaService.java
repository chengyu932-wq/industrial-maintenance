package com.cq.maintenance.sla.service;

import com.cq.maintenance.common.exception.*;
import com.cq.maintenance.common.response.PageResult;
import com.cq.maintenance.notification.service.NotificationService;
import com.cq.maintenance.repair.entity.RepairPriority;
import com.cq.maintenance.sla.dto.*;
import com.cq.maintenance.sla.entity.*;
import com.cq.maintenance.sla.mapper.SlaMapper;
import com.cq.maintenance.sla.vo.*;
import com.cq.maintenance.workorder.service.WorkOrderScopeService;
import java.time.LocalDateTime;
import java.util.*;
import org.slf4j.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SlaService {
    private static final Logger log=LoggerFactory.getLogger(SlaService.class);
    private final SlaMapper mapper;private final NotificationService notifications;private final WorkOrderScopeService scopes;
    public SlaService(SlaMapper mapper,NotificationService notifications,WorkOrderScopeService scopes){this.mapper=mapper;this.notifications=notifications;this.scopes=scopes;}
    @Transactional(readOnly=true) public List<SlaRuleVO> rules(){return mapper.findRules();}
    @Transactional public void updateRule(Long id,SlaRuleUpdateRequest input){if(input.responseMinutes()>input.resolveMinutes())throw new BusinessException(ErrorCode.PARAMETER_ERROR,"响应时限不能超过解决时限");if(mapper.updateRule(id,input.responseMinutes(),input.resolveMinutes(),input.enabled())!=1)throw new BusinessException(ErrorCode.NOT_FOUND,"SLA规则不存在");}
    @Transactional public void bindRepairOrder(Long workOrderId,RepairPriority priority){SlaRule rule=mapper.findEnabledRule(priority);if(rule==null)throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"当前故障等级没有启用的SLA规则");if(mapper.bindRule(workOrderId,rule.getId(),rule.getResponseMinutes(),rule.getResolveMinutes())!=1)throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"维修工单SLA绑定失败");}
    @Transactional public int scan(LocalDateTime now){int created=0;List<SlaMonitorRow> rows=new ArrayList<>();rows.addAll(mapper.findResponseTimeouts(now));rows.addAll(mapper.findResolveTimeouts(now));for(SlaMonitorRow row:rows){try{created+=createEvent(row,now);}catch(RuntimeException ex){log.error("SLA scan item failed, workOrderId={}, type={}",row.workOrderId(),row.eventType(),ex);}}return created;}
    public int scanNow(){return scan(LocalDateTime.now());}
    private int createEvent(SlaMonitorRow row,LocalDateTime now){SlaEvent event=new SlaEvent();event.setWorkOrderId(row.workOrderId());event.setEventType(row.eventType());event.setRuleId(row.ruleId());event.setOccurredAt(now);event.setEscalatedToUserId(row.escalationUserId());event.setHandled(false);event.setRemark(row.escalationUserId()==null?"未找到有效班组长或车间主管，事件已保留待处理":"已升级至"+row.escalationUserName());if(mapper.insertEvent(event)!=1)return 0;if(row.escalationUserId()!=null){String label=row.eventType()==SlaEventType.RESPONSE_TIMEOUT?"响应":"解决";Long messageId=notifications.createSla(row.escalationUserId(),row.workOrderId(),"工单"+label+"SLA超时",row.workOrderNo()+"已超过"+label+"截止时间 "+row.deadline());mapper.linkMessage(event.getId(),messageId);}return 1;}
    @Transactional(readOnly=true) public PageResult<SlaEventVO> events(SlaEventQuery query){var scope=scopes.current();return PageResult.of(mapper.findEventPage(query,scope),mapper.countEvents(query,scope),query.getPage(),query.getSize());}
    @Transactional public void handle(Long id){var scope=scopes.current();if(mapper.countEventVisible(id,scope)==0)throw new BusinessException(ErrorCode.DATA_FORBIDDEN,"SLA事件不存在或无权访问");if(mapper.handle(id)!=1)throw new BusinessException(ErrorCode.DUPLICATE_OPERATION,"SLA事件已处理");}
}
