package com.cq.maintenance.knowledge.service;

import static org.junit.jupiter.api.Assertions.*;import static org.mockito.ArgumentMatchers.*;import static org.mockito.Mockito.*;
import com.cq.maintenance.common.exception.BusinessException;import com.cq.maintenance.knowledge.dto.*;import com.cq.maintenance.knowledge.entity.*;import com.cq.maintenance.knowledge.mapper.KnowledgeMapper;import com.cq.maintenance.knowledge.vo.*;import com.cq.maintenance.security.LoginUser;import com.cq.maintenance.workorder.service.WorkOrderScopeService;import java.util.List;import org.junit.jupiter.api.*;import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;import org.springframework.security.core.context.SecurityContextHolder;

class KnowledgeServiceTest {
    KnowledgeMapper mapper;WorkOrderScopeService scopes;KnowledgeService service;
    @BeforeEach void setup(){mapper=mock(KnowledgeMapper.class);scopes=mock(WorkOrderScopeService.class);service=new KnowledgeService(mapper,scopes);login(7L,"ENGINEER");}
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    void login(long id,String role){LoginUser user=new LoginUser(id,"u","用户","ENABLED",List.of(role),List.of(),List.of(3L),4L,List.of());SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user,null,List.of()));}
    KnowledgeSourceRow source(){return new KnowledgeSourceRow(9L,"WO9",1L,"E1","机床",2L,"数控机床","主轴轴承异响","检查","轴承磨损","更换轴承","试运行正常","轴承 × 1 个");}
    KnowledgeArticle article(KnowledgeStatus status,long owner){KnowledgeArticle a=new KnowledgeArticle();a.setId(5L);a.setSourceWorkOrderId(9L);a.setSubmittedBy(owner);a.setStatus(status);return a;}
    KnowledgeArticleVO vo(KnowledgeStatus status,long owner){return new KnowledgeArticleVO(5L,"标题",9L,"WO9",1L,"E1","机床",2L,"数控机床","现象","原因","方案","结果",null,status,owner,"用户",null,null,null,null,null,null);}
    KnowledgeArticleRequest request(){return new KnowledgeArticleRequest("标题","现象","原因","方案","结果");}
    @Test void completedRepairCreatesPrefilledDraft(){when(mapper.source(9L)).thenReturn(source());doAnswer(i->{((KnowledgeArticle)i.getArgument(0)).setId(5L);return 1;}).when(mapper).insertArticle(any());assertEquals(5L,service.createFromWorkOrder(9L));verify(mapper).insertArticle(argThat(a->a.getStatus()==KnowledgeStatus.DRAFT&&a.getSubmittedBy()==7L&&a.getSpareSummary().contains("轴承")));}
    @Test void unfinishedOrMaintenanceOrderCannotCreate(){assertThrows(BusinessException.class,()->service.createFromWorkOrder(9L));}
    @Test void duplicateSourceIsRejected(){when(mapper.source(9L)).thenReturn(source());when(mapper.countBySource(9L)).thenReturn(1L);assertThrows(BusinessException.class,()->service.createFromWorkOrder(9L));}
    @Test void ownerCanEditDraft(){when(mapper.lock(5L)).thenReturn(article(KnowledgeStatus.DRAFT,7L));service.update(5L,request());verify(mapper).updateDraft(any());}
    @Test void rejectedEditReturnsToDraft(){when(mapper.lock(5L)).thenReturn(article(KnowledgeStatus.REJECTED,7L));service.update(5L,request());verify(mapper).updateDraft(any());}
    @Test void pendingCannotBeEdited(){when(mapper.lock(5L)).thenReturn(article(KnowledgeStatus.PENDING,7L));assertThrows(BusinessException.class,()->service.update(5L,request()));}
    @Test void ownerCanSubmitDraft(){when(mapper.lock(5L)).thenReturn(article(KnowledgeStatus.DRAFT,7L));when(mapper.submit(5L)).thenReturn(1);service.submit(5L);verify(mapper).submit(5L);}
    @Test void engineerCannotAudit(){when(mapper.lock(5L)).thenReturn(article(KnowledgeStatus.PENDING,7L));assertThrows(BusinessException.class,()->service.review(5L,new KnowledgeReviewRequest(true,"完整")));}
    @Test void supervisorCanPublishWithinScope(){login(8L,"MAINTENANCE_SUPERVISOR");when(mapper.lock(5L)).thenReturn(article(KnowledgeStatus.PENDING,7L));when(mapper.review(anyLong(),any(),anyLong(),any())).thenReturn(1);service.review(5L,new KnowledgeReviewRequest(true,"完整"));verify(scopes).assertVisible(9L);verify(mapper).review(5L,KnowledgeStatus.PUBLISHED,8L,"完整");}
    @Test void rejectionRequiresRemark(){login(8L,"MAINTENANCE_SUPERVISOR");when(mapper.lock(5L)).thenReturn(article(KnowledgeStatus.PENDING,7L));assertThrows(BusinessException.class,()->service.review(5L,new KnowledgeReviewRequest(false," ")));}
    @Test void publishedKnowledgeIsReadable(){when(mapper.detail(5L)).thenReturn(vo(KnowledgeStatus.PUBLISHED,99L));assertEquals(5L,service.detail(5L).id());}
    @Test void engineerCannotReadOthersDraft(){when(mapper.detail(5L)).thenReturn(vo(KnowledgeStatus.DRAFT,99L));assertThrows(BusinessException.class,()->service.detail(5L));}
}
