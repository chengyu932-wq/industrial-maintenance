package com.cq.maintenance.repair.service;
import com.cq.maintenance.common.exception.*;
import com.cq.maintenance.common.storage.*;
import com.cq.maintenance.repair.mapper.RepairAttachmentMapper;
import com.cq.maintenance.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
@Service public class RepairAttachmentService {
 @org.springframework.beans.factory.annotation.Autowired private com.cq.maintenance.workorder.mapper.WorkOrderMapper workOrders;
 private final RepairRequestService requests;private final org.springframework.beans.factory.ObjectProvider<RepairAttachmentMapper> mapperProvider;private final AttachmentStorageService storage;
 public RepairAttachmentService(RepairRequestService requests,org.springframework.beans.factory.ObjectProvider<RepairAttachmentMapper> mapperProvider,AttachmentStorageService storage){this.requests=requests;this.mapperProvider=mapperProvider;this.storage=storage;}
 private RepairAttachmentMapper mapper(){var mapper=mapperProvider.getIfAvailable();if(mapper==null)throw new IllegalStateException("报修附件存储未启用");return mapper;}
 public List<AttachmentVO> list(Long id){requests.detail(id);return mapper().list(id);}
 @Transactional public Long upload(Long id,MultipartFile file){
  workOrders.lockRepairRequest(id);var request=requests.detail(id);var user=SecurityUtils.currentUser();
  if(request.status()!=com.cq.maintenance.repair.entity.RepairRequestStatus.SUBMITTED)throw new BusinessException(ErrorCode.ILLEGAL_STATE_TRANSITION,"仅待确认报修可新增现场附件");
  if(!user.userId().equals(request.reporterId())&&!user.roleCodes().contains("ADMIN")&&!user.roleCodes().contains("MAINTENANCE_SUPERVISOR"))throw new BusinessException(ErrorCode.DATA_FORBIDDEN,"只能上传本人或负责范围的报修附件");
  var stored=storage.store("repair-request/"+id,file);
  try{var value=new RepairAttachmentMapper.AttachmentRow(null,id,stored.fileName(),stored.relativePath(),user.userId());mapper().insert(value);return mapper().lastId();}catch(RuntimeException e){storage.deleteQuietly(stored.relativePath());throw e;}
 }
 public AttachmentDownload download(Long id,Long attachmentId){requests.detail(id);var row=mapper().find(id,attachmentId);if(row==null)throw new BusinessException(ErrorCode.NOT_FOUND,"附件不存在");return new AttachmentDownload(row.fileName(),org.springframework.http.MediaTypeFactory.getMediaType(row.fileName()).map(Object::toString).orElse("application/octet-stream"),storage.read(row.filePath()));}
}