package com.cq.maintenance.workorder.service;
import com.cq.maintenance.common.exception.*;
import com.cq.maintenance.common.storage.*;
import com.cq.maintenance.security.SecurityUtils;
import com.cq.maintenance.workorder.entity.WorkOrderAttachment;
import com.cq.maintenance.workorder.mapper.WorkOrderAttachmentMapper;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
@Service public class WorkOrderAttachmentService {
 @org.springframework.beans.factory.annotation.Autowired private com.cq.maintenance.workorder.mapper.WorkOrderMapper workOrders;
 private static final Set<String> TYPES=Set.of("FAULT","REPAIR","ACCEPTANCE");private final WorkOrderService orders;private final ObjectProvider<WorkOrderAttachmentMapper> mapperProvider;private final AttachmentStorageService storage;
 public WorkOrderAttachmentService(WorkOrderService orders,ObjectProvider<WorkOrderAttachmentMapper> mapperProvider,AttachmentStorageService storage){this.orders=orders;this.mapperProvider=mapperProvider;this.storage=storage;}
 private WorkOrderAttachmentMapper mapper(){WorkOrderAttachmentMapper mapper=mapperProvider.getIfAvailable();if(mapper==null)throw new IllegalStateException("工单附件存储未启用");return mapper;}
 public List<AttachmentVO> list(Long workOrderId){orders.detail(workOrderId);return mapper().list(workOrderId);}
 @org.springframework.transaction.annotation.Transactional public Long upload(Long workOrderId,String type,MultipartFile file){workOrders.lockWorkOrder(workOrderId);var detail=orders.detail(workOrderId);String category=type==null?"":type.trim().toUpperCase(Locale.ROOT);if(!TYPES.contains(category))throw new BusinessException(ErrorCode.PARAMETER_ERROR,"附件类型必须为故障、维修或验收");var user=SecurityUtils.currentUser();var order=detail.workOrder();
 if(java.util.Set.of(com.cq.maintenance.workorder.entity.WorkOrderStatus.COMPLETED,com.cq.maintenance.workorder.entity.WorkOrderStatus.UNREPAIRABLE,com.cq.maintenance.workorder.entity.WorkOrderStatus.CANCELLED).contains(order.status()))throw new BusinessException(ErrorCode.ILLEGAL_STATE_TRANSITION,"已关闭工单不能新增附件");
 boolean manager=user.roleCodes().contains("ADMIN")||user.roleCodes().contains("MAINTENANCE_SUPERVISOR");
 boolean assigned=user.roleCodes().contains("ENGINEER")&&user.userId().equals(order.assignedEngineerId());
 boolean reporter=user.roleCodes().contains("REPORTER")&&user.userId().equals(order.reporterId());
 boolean allowed=switch(category){case "FAULT" -> (manager||reporter)&&(order.status()==com.cq.maintenance.workorder.entity.WorkOrderStatus.PENDING_ASSIGN||order.status()==com.cq.maintenance.workorder.entity.WorkOrderStatus.ASSIGNED);case "REPAIR" -> assigned&&order.status()==com.cq.maintenance.workorder.entity.WorkOrderStatus.PROCESSING;case "ACCEPTANCE" -> (manager||reporter)&&order.status()==com.cq.maintenance.workorder.entity.WorkOrderStatus.PENDING_ACCEPT;default -> false;};
 if(!allowed)throw new BusinessException(ErrorCode.DATA_FORBIDDEN,"当前人员或工单阶段不能上传此类附件");
 var stored=storage.store("work-order/"+workOrderId,file);try{WorkOrderAttachment value=new WorkOrderAttachment();value.setWorkOrderId(workOrderId);value.setAttachmentType(category);value.setFileName(stored.fileName());value.setFilePath(stored.relativePath());value.setUploadedBy(SecurityUtils.currentUser().userId());mapper().insert(value);return value.getId();}catch(RuntimeException exception){storage.deleteQuietly(stored.relativePath());throw exception;}}
 public AttachmentDownload download(Long workOrderId,Long attachmentId){orders.detail(workOrderId);WorkOrderAttachment value=mapper().find(workOrderId,attachmentId);if(value==null)throw new BusinessException(ErrorCode.NOT_FOUND,"工单附件不存在");String type=MediaTypeFactory.getMediaType(value.getFileName()).map(Object::toString).orElse("application/octet-stream");return new AttachmentDownload(value.getFileName(),type,storage.read(value.getFilePath()));}
}
