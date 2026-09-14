package com.cq.maintenance.notification.mapper;
import com.cq.maintenance.notification.entity.Notification;
import com.cq.maintenance.notification.vo.NotificationVO;
import java.util.List;
import org.apache.ibatis.annotations.*;
@Mapper
public interface NotificationMapper {
    @Insert("INSERT INTO msg_notification(receiver_id,message_type,title,content,business_type,business_id,is_read) VALUES(#{receiverId},#{messageType},#{title},#{content},#{businessType},#{businessId},0)") @Options(useGeneratedKeys=true,keyProperty="id") int insert(Notification notification);
    @Select("SELECT id,message_type,title,content,business_type,business_id,is_read `read`,read_at,created_at FROM msg_notification WHERE receiver_id=#{userId} ORDER BY is_read,created_at DESC,id DESC LIMIT #{size} OFFSET #{offset}") List<NotificationVO> findMine(@Param("userId")Long userId,@Param("offset")long offset,@Param("size")long size);
    @Select("SELECT COUNT(*) FROM msg_notification WHERE receiver_id=#{userId}") long countMine(Long userId);
    @Select("SELECT COUNT(*) FROM msg_notification WHERE receiver_id=#{userId} AND is_read=0") long unreadCount(Long userId);
    @Update("UPDATE msg_notification SET is_read=1,read_at=NOW() WHERE id=#{id} AND receiver_id=#{userId} AND is_read=0") int markRead(@Param("id")Long id,@Param("userId")Long userId);
    @Select("SELECT COUNT(*) FROM msg_notification WHERE id=#{id} AND receiver_id=#{userId}") long countMineById(@Param("id")Long id,@Param("userId")Long userId);
    @Update("UPDATE msg_notification SET is_read=1,read_at=NOW() WHERE receiver_id=#{userId} AND is_read=0") int markAllRead(Long userId);
}
