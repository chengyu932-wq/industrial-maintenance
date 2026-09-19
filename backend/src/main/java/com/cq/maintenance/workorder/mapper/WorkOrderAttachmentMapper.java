package com.cq.maintenance.workorder.mapper;
import com.cq.maintenance.common.storage.AttachmentVO;
import com.cq.maintenance.workorder.entity.WorkOrderAttachment;
import java.util.List;
import org.apache.ibatis.annotations.*;
@Mapper public interface WorkOrderAttachmentMapper {
 @Insert("INSERT INTO mnt_work_order_attachment(work_order_id,attachment_type,file_name,file_path,uploaded_by) VALUES(#{workOrderId},#{attachmentType},#{fileName},#{filePath},#{uploadedBy})") @Options(useGeneratedKeys=true,keyProperty="id") int insert(WorkOrderAttachment value);
 @Select("SELECT a.id,a.attachment_type category,a.file_name,NULL file_type,a.uploaded_by,u.real_name uploader_name,a.created_at FROM mnt_work_order_attachment a LEFT JOIN sys_user u ON u.id=a.uploaded_by WHERE a.work_order_id=#{workOrderId} ORDER BY a.created_at DESC,a.id DESC") List<AttachmentVO> list(Long workOrderId);
 @Select("SELECT id,work_order_id,attachment_type,file_name,file_path,uploaded_by FROM mnt_work_order_attachment WHERE id=#{attachmentId} AND work_order_id=#{workOrderId}") WorkOrderAttachment find(@Param("workOrderId")Long workOrderId,@Param("attachmentId")Long attachmentId);
}
