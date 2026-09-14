package com.cq.maintenance.sla;
import static org.mockito.Mockito.*;import com.cq.maintenance.sla.scheduler.SlaScheduler;import com.cq.maintenance.sla.service.SlaService;import org.junit.jupiter.api.Test;
class SlaSchedulerTest {@Test void schedulerDelegatesToMonitorService(){SlaService service=mock(SlaService.class);new SlaScheduler(service).scan();verify(service).scanNow();}}
