package com.cq.maintenance.maintenance.service;

import com.cq.maintenance.maintenance.mapper.MaintenanceMapper;
import com.cq.maintenance.maintenance.vo.MaintenanceScanResultVO;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j @Service
public class MaintenanceScanService {
    private final MaintenanceMapper mapper;private final MaintenanceOrderGenerator generator;
    public MaintenanceScanService(MaintenanceMapper mapper,MaintenanceOrderGenerator generator){this.mapper=mapper;this.generator=generator;}
    public MaintenanceScanResultVO scanDuePlans(){LocalDate today=LocalDate.now();var ids=mapper.findDuePlanIds(today);int generated=0,skipped=0,failed=0;for(Long id:ids){try{if(generator.generateIfDue(id,today))generated++;else skipped++;}catch(Exception e){failed++;log.error("保养计划 {} 自动生成工单失败",id,e);}}return new MaintenanceScanResultVO(ids.size(),generated,skipped,failed);}
}
