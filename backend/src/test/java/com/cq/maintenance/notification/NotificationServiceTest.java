package com.cq.maintenance.notification;
import static org.junit.jupiter.api.Assertions.*;import static org.mockito.ArgumentMatchers.*;import static org.mockito.Mockito.*;
import com.cq.maintenance.common.exception.BusinessException;import com.cq.maintenance.notification.entity.Notification;import com.cq.maintenance.notification.mapper.NotificationMapper;import com.cq.maintenance.notification.service.NotificationService;import com.cq.maintenance.security.LoginUser;import java.util.List;import org.junit.jupiter.api.*;import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;import org.springframework.security.core.context.SecurityContextHolder;
class NotificationServiceTest {
    NotificationMapper mapper;NotificationService service;
    @BeforeEach void setup(){mapper=mock(NotificationMapper.class);service=new NotificationService(mapper);LoginUser u=new LoginUser(7L,"u","用户","ENABLED",List.of("ENGINEER"),List.of(),List.of(),null,List.of());SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(u,null,List.of()));}
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    @Test void listAlwaysUsesCurrentUser(){service.mine(1,20);verify(mapper).findMine(7L,0,20);verify(mapper).countMine(7L);}
    @Test void invalidPageIsRejected(){assertThrows(BusinessException.class,()->service.mine(0,20));}
    @Test void unreadCountUsesCurrentUser(){service.unreadCount();verify(mapper).unreadCount(7L);}
    @Test void userCanReadOwnMessage(){when(mapper.markRead(3L,7L)).thenReturn(1);service.read(3L);}
    @Test void userCannotReadOthersMessage(){assertThrows(BusinessException.class,()->service.read(3L));}
    @Test void alreadyReadOwnMessageIsIdempotent(){when(mapper.countMineById(3L,7L)).thenReturn(1L);service.read(3L);}
    @Test void readAllOnlyUsesCurrentUser(){service.readAll();verify(mapper).markAllRead(7L);}
    @Test void slaMessageLinksWorkOrder(){doAnswer(i->{((Notification)i.getArgument(0)).setId(9L);return 1;}).when(mapper).insert(any());assertEquals(9L,service.createSla(7L,12L,"超时","内容"));verify(mapper).insert(argThat(n->n.getReceiverId().equals(7L)&&n.getBusinessId().equals(12L)&&n.getMessageType().equals("SLA")));}
}
