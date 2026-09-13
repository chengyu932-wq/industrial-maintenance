package com.cq.maintenance.maintenance.scheduler;

import com.cq.maintenance.maintenance.service.MaintenanceScanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j @Component
public class MaintenanceScheduler {
    private final MaintenanceScanService service;public MaintenanceScheduler(MaintenanceScanService service){this.service=service;}
    @Scheduled(cron="${app.maintenance.scan-cron:0 0 * * * *}") public void scan(){var result=service.scanDuePlans();if(result.due()>0)log.info("保养计划扫描完成 due={}, generated={}, skipped={}, failed={}",result.due(),result.generated(),result.skipped(),result.failed());}
}
