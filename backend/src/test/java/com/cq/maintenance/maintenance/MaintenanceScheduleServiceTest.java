package com.cq.maintenance.maintenance;

import static org.junit.jupiter.api.Assertions.*;
import com.cq.maintenance.common.exception.BusinessException;
import com.cq.maintenance.maintenance.entity.MaintenanceCycleType;
import com.cq.maintenance.maintenance.service.MaintenanceScheduleService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class MaintenanceScheduleServiceTest {
    final MaintenanceScheduleService service=new MaintenanceScheduleService();
    @Test void weeklyCycleAddsExactWeeks(){assertEquals(LocalDate.of(2026,9,20),service.calculateNextExecutionDate(LocalDate.of(2026,9,13),MaintenanceCycleType.WEEK,1));}
    @Test void monthlyCycleUsesCalendarMonth(){assertEquals(LocalDate.of(2026,2,28),service.calculateNextExecutionDate(LocalDate.of(2026,1,31),MaintenanceCycleType.MONTH,1));}
    @Test void quarterlyCycleAddsThreeMonths(){assertEquals(LocalDate.of(2026,4,30),service.calculateNextExecutionDate(LocalDate.of(2026,1,31),MaintenanceCycleType.QUARTER,1));}
    @Test void weeklyCycleCrossesYear(){assertEquals(LocalDate.of(2027,1,5),service.calculateNextExecutionDate(LocalDate.of(2026,12,29),MaintenanceCycleType.WEEK,1));}
    @Test void monthlyCycleCrossesLeapDay(){assertEquals(LocalDate.of(2024,2,29),service.calculateNextExecutionDate(LocalDate.of(2024,1,31),MaintenanceCycleType.MONTH,1));}
    @Test void quarterlyMultiplierIsApplied(){assertEquals(LocalDate.of(2027,1,13),service.calculateNextExecutionDate(LocalDate.of(2026,7,13),MaintenanceCycleType.QUARTER,2));}
    @Test void overdueScheduleAdvancesToFirstFuturePeriod(){assertEquals(LocalDate.of(2026,10,1),service.calculateNextFutureDate(LocalDate.of(2026,6,1),MaintenanceCycleType.MONTH,1,LocalDate.of(2026,9,13)));}
    @Test void invalidCycleValueIsRejected(){assertThrows(BusinessException.class,()->service.calculateNextExecutionDate(LocalDate.now(),MaintenanceCycleType.MONTH,0));}
}
