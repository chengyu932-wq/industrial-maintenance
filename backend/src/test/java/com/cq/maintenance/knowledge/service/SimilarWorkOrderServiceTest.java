package com.cq.maintenance.knowledge.service;

import static org.junit.jupiter.api.Assertions.*;import static org.mockito.ArgumentMatchers.*;import static org.mockito.Mockito.*;
import com.cq.maintenance.knowledge.mapper.KnowledgeMapper;import com.cq.maintenance.knowledge.similarity.*;import com.cq.maintenance.knowledge.vo.*;import com.cq.maintenance.workorder.mapper.WorkOrderMapper.WorkOrderDataScope;import com.cq.maintenance.workorder.service.WorkOrderScopeService;import java.time.LocalDateTime;import java.util.*;import org.junit.jupiter.api.BeforeEach;import org.junit.jupiter.api.Test;

class SimilarWorkOrderServiceTest {
    KnowledgeMapper mapper;WorkOrderScopeService scopes;SimilarWorkOrderService service;WorkOrderDataScope scope=new WorkOrderDataScope(true,"ALL",1L,null,List.of());
    @BeforeEach void setup(){mapper=mock(KnowledgeMapper.class);scopes=mock(WorkOrderScopeService.class);when(scopes.current()).thenReturn(scope);service=new SimilarWorkOrderService(mapper,scopes,new TextPreprocessor(),new TfIdfCalculator());}
    SimilarWorkOrderRow row(long id,String text,LocalDateTime time){return new SimilarWorkOrderRow(id,"WO"+id,1L,"E1","设备",1L,"数控机床",text,"原因","措施","结果",null,time);}
    @Test void sixPositiveCandidatesReturnFive(){when(mapper.similarityCorpus(scope,null)).thenReturn(List.of(row(1,"主轴轴承过热",LocalDateTime.now()),row(2,"主轴轴承异响",LocalDateTime.now()),row(3,"主轴振动过热",LocalDateTime.now()),row(4,"轴承振动",LocalDateTime.now()),row(5,"主轴温度过高",LocalDateTime.now()),row(6,"主轴异响",LocalDateTime.now())));assertEquals(5,service.byDescription("主轴轴承异响过热").size());}
    @Test void threeCandidatesReturnThree(){when(mapper.similarityCorpus(scope,null)).thenReturn(List.of(row(1,"主轴异响",LocalDateTime.now()),row(2,"主轴过热",LocalDateTime.now()),row(3,"主轴振动",LocalDateTime.now())));assertEquals(3,service.byDescription("主轴异常振动异响过热").size());}
    @Test void zeroSimilarityIsNotPadding(){when(mapper.similarityCorpus(scope,null)).thenReturn(List.of(row(1,"液压漏油",LocalDateTime.now())));assertTrue(service.byDescription("主轴异响").isEmpty());}
    @Test void similaritySortsDescending(){when(mapper.similarityCorpus(scope,null)).thenReturn(List.of(row(1,"主轴异响",LocalDateTime.now()),row(2,"主轴轴承严重异响",LocalDateTime.now())));var values=service.byDescription("主轴轴承严重异响");assertTrue(values.get(0).similarity()>=values.get(1).similarity());}
    @Test void equalScoreUsesRecentCompletionThenId(){LocalDateTime now=LocalDateTime.now();when(mapper.similarityCorpus(scope,null)).thenReturn(List.of(row(3,"主轴异响",now.minusDays(1)),row(2,"主轴异响",now),row(1,"主轴异响",now)));assertEquals(List.of(1L,2L,3L),service.byDescription("主轴异响").stream().map(SimilarWorkOrderVO::workOrderId).toList());}
    @Test void emptyCorpusReturnsEmpty(){when(mapper.similarityCorpus(scope,null)).thenReturn(List.of());assertTrue(service.byDescription("主轴异响").isEmpty());}
    @Test void unknownQueryVocabularyReturnsEmpty(){when(mapper.similarityCorpus(scope,null)).thenReturn(List.of(row(1,"液压漏油",LocalDateTime.now())));assertTrue(service.byDescription("abcdef").isEmpty());}
    @Test void workOrderQueryChecksVisibilityAndExcludesItself(){when(mapper.faultDescription(9L)).thenReturn("主轴异响");when(mapper.similarityCorpus(scope,9L)).thenReturn(List.of());service.byWorkOrder(9L);verify(scopes).assertVisible(9L);verify(mapper).similarityCorpus(scope,9L);}
    @Test void repeatedQueryIsDeterministic(){when(mapper.similarityCorpus(scope,null)).thenReturn(List.of(row(1,"主轴异响",LocalDateTime.now()),row(2,"主轴过热",LocalDateTime.now())));assertEquals(service.byDescription("主轴异常"),service.byDescription("主轴异常"));}
}
