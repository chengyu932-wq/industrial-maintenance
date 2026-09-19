package com.cq.maintenance.equipment.mapper;
import com.cq.maintenance.common.storage.AttachmentVO;
import com.cq.maintenance.equipment.entity.EquipmentAttachment;
import java.util.List;
import org.apache.ibatis.annotations.*;
@Mapper public interface EquipmentAttachmentMapper {
 @Insert("INSERT INTO eqp_attachment(equipment_id,file_name,file_path,file_type,uploaded_by) VALUES(#{equipmentId},#{fileName},#{filePath},#{fileType},#{uploadedBy})") @Options(useGeneratedKeys=true,keyProperty="id") int insert(EquipmentAttachment value);
 @Select("SELECT a.id,NULL category,a.file_name,a.file_type,a.uploaded_by,u.real_name uploader_name,a.created_at FROM eqp_attachment a LEFT JOIN sys_user u ON u.id=a.uploaded_by WHERE a.equipment_id=#{equipmentId} ORDER BY a.created_at DESC,a.id DESC") List<AttachmentVO> list(Long equipmentId);
 @Select("SELECT id,equipment_id,file_name,file_path,file_type,uploaded_by FROM eqp_attachment WHERE id=#{attachmentId} AND equipment_id=#{equipmentId}") EquipmentAttachment find(@Param("equipmentId")Long equipmentId,@Param("attachmentId")Long attachmentId);
}
