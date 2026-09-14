package com.cq.maintenance.sla.scheduler;
import com.cq.maintenance.sla.service.SlaService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component
public class SlaScheduler {
    private final SlaService service;public SlaScheduler(SlaService service){this.service=service;}
    @Scheduled(cron="${app.sla.scan-cron:0 */5 * * * *}") public void scan(){service.scanNow();}
}
