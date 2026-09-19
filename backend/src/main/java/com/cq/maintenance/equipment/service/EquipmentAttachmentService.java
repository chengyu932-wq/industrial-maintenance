package com.cq.maintenance.equipment.service;
import com.cq.maintenance.common.exception.*;
import com.cq.maintenance.common.storage.*;
import com.cq.maintenance.equipment.entity.EquipmentAttachment;
import com.cq.maintenance.equipment.mapper.EquipmentAttachmentMapper;
import com.cq.maintenance.security.SecurityUtils;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
@Service public class EquipmentAttachmentService {
 private final EquipmentService equipment;private final ObjectProvider<EquipmentAttachmentMapper> mapperProvider;private final AttachmentStorageService storage;
 public EquipmentAttachmentService(EquipmentService equipment,ObjectProvider<EquipmentAttachmentMapper> mapperProvider,AttachmentStorageService storage){this.equipment=equipment;this.mapperProvider=mapperProvider;this.storage=storage;}
 private EquipmentAttachmentMapper mapper(){EquipmentAttachmentMapper mapper=mapperProvider.getIfAvailable();if(mapper==null)throw new IllegalStateException("设备附件存储未启用");return mapper;}
 public List<AttachmentVO> list(Long equipmentId){equipment.detail(equipmentId);return mapper().list(equipmentId);}
 public Long upload(Long equipmentId,MultipartFile file){equipment.detail(equipmentId);var stored=storage.store("equipment/"+equipmentId,file);try{EquipmentAttachment value=new EquipmentAttachment();value.setEquipmentId(equipmentId);value.setFileName(stored.fileName());value.setFilePath(stored.relativePath());value.setFileType(stored.contentType());value.setUploadedBy(SecurityUtils.currentUser().userId());mapper().insert(value);return value.getId();}catch(RuntimeException exception){storage.deleteQuietly(stored.relativePath());throw exception;}}
 public AttachmentDownload download(Long equipmentId,Long attachmentId){equipment.detail(equipmentId);EquipmentAttachment value=mapper().find(equipmentId,attachmentId);if(value==null)throw new BusinessException(ErrorCode.NOT_FOUND,"设备附件不存在");return new AttachmentDownload(value.getFileName(),value.getFileType(),storage.read(value.getFilePath()));}
}
