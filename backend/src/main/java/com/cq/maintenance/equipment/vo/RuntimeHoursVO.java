package com.cq.maintenance.equipment.vo;
import java.math.BigDecimal;
import java.time.*;
public record RuntimeHoursVO(Long id,Long equipmentId,LocalDate recordDate,BigDecimal runningHoursIncrement,BigDecimal totalRunningHours,String source,Long recordedBy,String recorderName,String remark,LocalDateTime createdAt){}
