package com.cq.maintenance.maintenance.vo;

import com.cq.maintenance.maintenance.entity.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import org.apache.ibatis.annotations.AutomapConstructor;

public record MaintenancePlanVO(Long id,String planNo,String planName,Long equipmentId,String equipmentNo,String equipmentName,
    Long workshopId,String workshopName,Long responsibleTeamId,String responsibleTeamName,MaintenanceCycleType cycleType,
    Integer cycleValue,LocalDate nextExecuteDate,BigDecimal runningHourThreshold,MaintenancePlanStatus status,
    LocalDateTime lastGeneratedAt,LocalDateTime createdAt,LocalDateTime updatedAt,List<MaintenancePlanItemVO> items) {
    public MaintenancePlanVO { items=items==null?List.of():List.copyOf(items); }

    @AutomapConstructor
    public MaintenancePlanVO(Long id,String planNo,String planName,Long equipmentId,String equipmentNo,String equipmentName,
        Long workshopId,String workshopName,Long responsibleTeamId,String responsibleTeamName,MaintenanceCycleType cycleType,
        Integer cycleValue,LocalDate nextExecuteDate,BigDecimal runningHourThreshold,MaintenancePlanStatus status,
        LocalDateTime lastGeneratedAt,LocalDateTime createdAt,LocalDateTime updatedAt) {
        this(id,planNo,planName,equipmentId,equipmentNo,equipmentName,workshopId,workshopName,responsibleTeamId,
            responsibleTeamName,cycleType,cycleValue,nextExecuteDate,runningHourThreshold,status,lastGeneratedAt,
            createdAt,updatedAt,List.of());
    }
}
