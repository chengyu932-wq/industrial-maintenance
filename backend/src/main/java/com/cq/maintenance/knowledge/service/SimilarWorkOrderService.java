package com.cq.maintenance.knowledge.service;

import com.cq.maintenance.common.exception.BusinessException;
import com.cq.maintenance.common.exception.ErrorCode;
import com.cq.maintenance.knowledge.mapper.KnowledgeMapper;
import com.cq.maintenance.knowledge.similarity.TextPreprocessor;
import com.cq.maintenance.knowledge.similarity.TfIdfCalculator;
import com.cq.maintenance.knowledge.vo.SimilarWorkOrderRow;
import com.cq.maintenance.knowledge.vo.SimilarWorkOrderVO;
import com.cq.maintenance.workorder.service.WorkOrderScopeService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnExpression("'${spring.autoconfigure.exclude:}'.indexOf('DataSourceAutoConfiguration') < 0")
public class SimilarWorkOrderService {
    private static final int LIMIT=5;
    private final KnowledgeMapper mapper;private final WorkOrderScopeService scopes;private final TextPreprocessor preprocessor;private final TfIdfCalculator calculator;
    public SimilarWorkOrderService(KnowledgeMapper mapper,WorkOrderScopeService scopes,TextPreprocessor preprocessor,TfIdfCalculator calculator){this.mapper=mapper;this.scopes=scopes;this.preprocessor=preprocessor;this.calculator=calculator;}

    @Transactional(readOnly=true)
    public List<SimilarWorkOrderVO> byWorkOrder(Long workOrderId){
        scopes.assertVisible(workOrderId);String description=mapper.faultDescription(workOrderId);
        if(description==null)throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"仅维修工单可以查询历史相似工单");
        return rank(description,workOrderId);
    }
    @Transactional(readOnly=true)
    public List<SimilarWorkOrderVO> byDescription(String description){return rank(description,null);}

    List<SimilarWorkOrderVO> rank(String description,Long excludeId){
        List<String> queryTokens=preprocessor.tokens(description);
        if(queryTokens.isEmpty())return List.of();
        List<SimilarWorkOrderRow> corpus=mapper.similarityCorpus(scopes.current(),excludeId);
        if(corpus.isEmpty())return List.of();
        List<List<String>> documentTokens=corpus.stream().map(row->preprocessor.tokens(row.faultDescription())).toList();
        Map<String,Integer> frequencies=calculator.documentFrequency(documentTokens);int documentCount=corpus.size();
        Map<String,Double> queryVector=calculator.vector(queryTokens,frequencies,documentCount);if(queryVector.isEmpty())return List.of();
        List<SimilarWorkOrderVO> result=new ArrayList<>();
        for(int i=0;i<corpus.size();i++){
            SimilarWorkOrderRow row=corpus.get(i);double similarity=calculator.cosine(queryVector,calculator.vector(documentTokens.get(i),frequencies,documentCount));
            if(similarity>0D)result.add(new SimilarWorkOrderVO(row.workOrderId(),row.workOrderNo(),row.equipmentId(),row.equipmentNo(),row.equipmentName(),row.equipmentTypeId(),row.equipmentTypeName(),row.faultDescription(),similarity,row.rootCause(),row.repairAction(),row.repairResult(),row.spareSummary(),row.completedAt()));
        }
        return result.stream().sorted(Comparator.comparingDouble(SimilarWorkOrderVO::similarity).reversed()
            .thenComparing(SimilarWorkOrderVO::completedAt,Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparingLong(SimilarWorkOrderVO::workOrderId)).limit(LIMIT).toList();
    }
}
