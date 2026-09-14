package com.cq.maintenance.notification.service;
import com.cq.maintenance.common.exception.*;
import com.cq.maintenance.common.response.PageResult;
import com.cq.maintenance.notification.entity.Notification;
import com.cq.maintenance.notification.mapper.NotificationMapper;
import com.cq.maintenance.notification.vo.NotificationVO;
import com.cq.maintenance.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class NotificationService {
    private final NotificationMapper mapper;public NotificationService(NotificationMapper mapper){this.mapper=mapper;}
    @Transactional public Long createSla(Long receiverId,Long workOrderId,String title,String content){Notification n=new Notification();n.setReceiverId(receiverId);n.setMessageType("SLA");n.setTitle(title);n.setContent(content);n.setBusinessType("WORK_ORDER");n.setBusinessId(workOrderId);mapper.insert(n);return n.getId();}
    @Transactional(readOnly=true) public PageResult<NotificationVO> mine(long page,long size){validate(page,size);Long id=SecurityUtils.currentUser().userId();return PageResult.of(mapper.findMine(id,(page-1)*size,size),mapper.countMine(id),page,size);}
    @Transactional(readOnly=true) public long unreadCount(){return mapper.unreadCount(SecurityUtils.currentUser().userId());}
    @Transactional public void read(Long id){Long userId=SecurityUtils.currentUser().userId();if(mapper.markRead(id,userId)==0&&mapper.countMineById(id,userId)==0)throw new BusinessException(ErrorCode.NOT_FOUND,"消息不存在");}
    @Transactional public void readAll(){mapper.markAllRead(SecurityUtils.currentUser().userId());}
    private void validate(long page,long size){if(page<1||size<1||size>100)throw new BusinessException(ErrorCode.PARAMETER_ERROR,"分页参数不合法");}
}
