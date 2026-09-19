package com.cq.maintenance.workorder;
import static org.junit.jupiter.api.Assertions.*;
import com.cq.maintenance.repair.entity.*;
import com.cq.maintenance.repair.vo.RepairRequestVO;
import com.cq.maintenance.workorder.entity.*;
import com.cq.maintenance.workorder.service.WorkOrderPdfService;
import com.cq.maintenance.workorder.vo.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
class WorkOrderPdfServiceTest {
 @Test void exportsReadablePdfWithChineseFont(){LocalDateTime now=LocalDateTime.of(2026,9,18,9,0);WorkOrderListVO wo=new WorkOrderListVO(1L,"WO-TEST",WorkOrderType.REPAIR,2L,"RR-TEST",3L,"EQ-1","数控机床",4L,"一车间",RepairPriority.IMPORTANT,WorkOrderStatus.COMPLETED,5L,"报修人",6L,"工程师",7L,"维修班",now,now,now,now,now,now,0,null,1L,now.plusHours(2),now.plusHours(8));RepairRequestVO rr=new RepairRequestVO(2L,"RR-TEST",3L,"EQ-1","数控机床",5L,"报修人",RepairSource.PC,RepairPriority.IMPORTANT,"主轴异响",now,RepairRequestStatus.CONVERTED,1L,"WO-TEST",WorkOrderStatus.COMPLETED);RepairRecordVO rec=new RepairRecordVO(1L,1L,"检查轴承","轴承磨损","更换轴承","试运行正常",new BigDecimal("2.0"),60,true,6L,"工程师",now,now);byte[] pdf=new WorkOrderPdfService("C:/Windows/Fonts/Noto Sans SC (TrueType).otf").export(new WorkOrderDetailVO(wo,rr,rec,List.of(),List.of()));assertTrue(pdf.length>1000);assertArrayEquals(new byte[]{'%', 'P','D','F'},java.util.Arrays.copyOf(pdf,4));}
}
