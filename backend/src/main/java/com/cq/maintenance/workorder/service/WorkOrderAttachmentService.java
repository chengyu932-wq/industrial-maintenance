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
 private static final Set<String> TYPES=Set.of("FAULT","REPAIR","ACCEPTANCE");private final WorkOrderService orders;private final ObjectProvider<WorkOrderAttachmentMapper> mapperProvider;private final AttachmentStorageService storage;
 public WorkOrderAttachmentService(WorkOrderService orders,ObjectProvider<WorkOrderAttachmentMapper> mapperProvider,AttachmentStorageService storage){this.orders=orders;this.mapperProvider=mapperProvider;this.storage=storage;}
 private WorkOrderAttachmentMapper mapper(){WorkOrderAttachmentMapper mapper=mapperProvider.getIfAvailable();if(mapper==null)throw new IllegalStateException("工单附件存储未启用");return mapper;}
 public List<AttachmentVO> list(Long workOrderId){orders.detail(workOrderId);return mapper().list(workOrderId);}
 public Long upload(Long workOrderId,String type,MultipartFile file){orders.detail(workOrderId);String category=type==null?"":type.trim().toUpperCase(Locale.ROOT);if(!TYPES.contains(category))throw new BusinessException(ErrorCode.PARAMETER_ERROR,"附件类型必须为故障、维修或验收");var stored=storage.store("work-order/"+workOrderId,file);try{WorkOrderAttachment value=new WorkOrderAttachment();value.setWorkOrderId(workOrderId);value.setAttachmentType(category);value.setFileName(stored.fileName());value.setFilePath(stored.relativePath());value.setUploadedBy(SecurityUtils.currentUser().userId());mapper().insert(value);return value.getId();}catch(RuntimeException exception){storage.deleteQuietly(stored.relativePath());throw exception;}}
 public AttachmentDownload download(Long workOrderId,Long attachmentId){orders.detail(workOrderId);WorkOrderAttachment value=mapper().find(workOrderId,attachmentId);if(value==null)throw new BusinessException(ErrorCode.NOT_FOUND,"工单附件不存在");String type=MediaTypeFactory.getMediaType(value.getFileName()).map(Object::toString).orElse("application/octet-stream");return new AttachmentDownload(value.getFileName(),type,storage.read(value.getFilePath()));}
}
