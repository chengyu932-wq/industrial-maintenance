package com.cq.maintenance.maintenance.service;

import com.cq.maintenance.common.exception.*;
import com.cq.maintenance.maintenance.entity.MaintenanceCycleType;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class MaintenanceScheduleService {
    public LocalDate calculateNextExecutionDate(LocalDate base,MaintenanceCycleType type,int value){
        if(base==null||type==null||value<=0)throw new BusinessException(ErrorCode.PARAMETER_ERROR,"保养周期或执行日期非法");
        return switch(type){case WEEK->base.plusWeeks(value);case MONTH->base.plusMonths(value);case QUARTER->base.plusMonths(3L*value);};
    }
    public LocalDate calculateNextFutureDate(LocalDate scheduled,MaintenanceCycleType type,int value,LocalDate today){
        LocalDate next=calculateNextExecutionDate(scheduled,type,value);
        while(!next.isAfter(today))next=calculateNextExecutionDate(next,type,value);
        return next;
    }
}
