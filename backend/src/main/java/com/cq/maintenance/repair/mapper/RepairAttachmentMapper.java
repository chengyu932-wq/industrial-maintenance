package com.cq.maintenance.repair.mapper;
import com.cq.maintenance.common.storage.AttachmentVO;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper public interface RepairAttachmentMapper {
 @Insert("INSERT INTO mnt_repair_request_attachment(repair_request_id,file_name,file_path,uploaded_by) VALUES(#{repairRequestId},#{fileName},#{filePath},#{uploadedBy})") int insert(AttachmentRow row);
 @Select("SELECT LAST_INSERT_ID()") Long lastId();
 @Select("SELECT a.id,'FAULT' category,a.file_name,NULL file_type,a.uploaded_by,u.real_name uploader_name,a.created_at FROM mnt_repair_request_attachment a LEFT JOIN sys_user u ON u.id=a.uploaded_by WHERE a.repair_request_id=#{id} ORDER BY a.id DESC") List<AttachmentVO> list(Long id);
 @Select("SELECT id,repair_request_id,file_name,file_path,uploaded_by FROM mnt_repair_request_attachment WHERE repair_request_id=#{requestId} AND id=#{id}") AttachmentRow find(@Param("requestId") Long requestId,@Param("id") Long id);
 record AttachmentRow(Long id,Long repairRequestId,String fileName,String filePath,Long uploadedBy){}
}