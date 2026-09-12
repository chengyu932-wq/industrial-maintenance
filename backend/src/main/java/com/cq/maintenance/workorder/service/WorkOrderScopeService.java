package com.cq.maintenance.workorder.service;

import com.cq.maintenance.common.exception.*;
import com.cq.maintenance.security.*;
import com.cq.maintenance.workorder.entity.WorkOrder;
import com.cq.maintenance.workorder.mapper.WorkOrderMapper;
import com.cq.maintenance.workorder.mapper.WorkOrderMapper.WorkOrderDataScope;
import com.cq.maintenance.workorder.vo.WorkOrderListVO;
import org.springframework.stereotype.Service;

@Service
public class WorkOrderScopeService {
    private final WorkOrderMapper mapper;
    public WorkOrderScopeService(WorkOrderMapper mapper){this.mapper=mapper;}
    public WorkOrderDataScope current(){
        LoginUser user=SecurityUtils.currentUser();
        if(user.roleCodes().contains("ADMIN"))return new WorkOrderDataScope(true,"ALL",user.userId(),user.workshopId(),user.teamIds());
        if(user.roleCodes().contains("MAINTENANCE_SUPERVISOR"))return new WorkOrderDataScope(false,"SUPERVISOR",user.userId(),user.workshopId(),user.teamIds());
        if(user.roleCodes().contains("ENGINEER"))return new WorkOrderDataScope(false,"ENGINEER",user.userId(),user.workshopId(),user.teamIds());
        if(user.roleCodes().contains("REPORTER"))return new WorkOrderDataScope(false,"REPORTER",user.userId(),user.workshopId(),user.teamIds());
        return new WorkOrderDataScope(false,"NONE",user.userId(),user.workshopId(),user.teamIds());
    }
    public void assertVisible(Long id){if(mapper.countVisible(id,current())==0)throw new BusinessException(ErrorCode.DATA_FORBIDDEN,"无权访问该工单");}
    public void assertCanAssign(Long id){LoginUser u=SecurityUtils.currentUser();if(!u.roleCodes().contains("ADMIN")&&!u.roleCodes().contains("MAINTENANCE_SUPERVISOR"))throw new BusinessException(ErrorCode.DATA_FORBIDDEN,"仅管理员或运维主管可以派单");assertVisible(id);}
    public void assertAssignedEngineer(WorkOrder order){LoginUser u=SecurityUtils.currentUser();if(!u.roleCodes().contains("ENGINEER")||!u.userId().equals(order.getAssignedEngineerId()))throw new BusinessException(ErrorCode.DATA_FORBIDDEN,"工单未分配给当前工程师");}
    public void assertCanAccept(WorkOrderListVO summary){LoginUser u=SecurityUtils.currentUser();if(u.roleCodes().contains("ADMIN")){return;}if(u.roleCodes().contains("MAINTENANCE_SUPERVISOR")){assertVisible(summary.id());return;}if(u.roleCodes().contains("REPORTER")&&u.userId().equals(summary.reporterId()))return;throw new BusinessException(ErrorCode.DATA_FORBIDDEN,"当前用户无验收权限");}
    public void assertReporter(WorkOrderListVO summary){LoginUser u=SecurityUtils.currentUser();if(!u.roleCodes().contains("REPORTER")||!u.userId().equals(summary.reporterId()))throw new BusinessException(ErrorCode.DATA_FORBIDDEN,"只能取消本人提交的报修");}
}
