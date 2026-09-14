package com.cq.maintenance.knowledge.service;

import com.cq.maintenance.common.exception.BusinessException;
import com.cq.maintenance.common.exception.ErrorCode;
import com.cq.maintenance.common.response.PageResult;
import com.cq.maintenance.knowledge.dto.KnowledgeArticleRequest;
import com.cq.maintenance.knowledge.dto.KnowledgeQuery;
import com.cq.maintenance.knowledge.dto.KnowledgeReviewRequest;
import com.cq.maintenance.knowledge.entity.KnowledgeArticle;
import com.cq.maintenance.knowledge.entity.KnowledgeStatus;
import com.cq.maintenance.knowledge.mapper.KnowledgeMapper;
import com.cq.maintenance.knowledge.vo.KnowledgeArticleVO;
import com.cq.maintenance.knowledge.vo.KnowledgeSourceRow;
import com.cq.maintenance.security.LoginUser;
import com.cq.maintenance.security.SecurityUtils;
import com.cq.maintenance.workorder.service.WorkOrderScopeService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnExpression("'${spring.autoconfigure.exclude:}'.indexOf('DataSourceAutoConfiguration') < 0")
public class KnowledgeService {
    private final KnowledgeMapper mapper;private final WorkOrderScopeService scopes;
    public KnowledgeService(KnowledgeMapper mapper,WorkOrderScopeService scopes){this.mapper=mapper;this.scopes=scopes;}

    @Transactional(readOnly=true)
    public PageResult<KnowledgeArticleVO> page(KnowledgeQuery query){return PageResult.of(mapper.page(query,scopes.current()),mapper.countPage(query,scopes.current()),query.getPage(),query.getSize());}
    @Transactional(readOnly=true)
    public KnowledgeArticleVO detail(Long id){KnowledgeArticleVO value=requiredVO(id);assertReadable(value);return value;}

    @Transactional
    public Long createFromWorkOrder(Long workOrderId){
        scopes.assertVisible(workOrderId);KnowledgeSourceRow source=mapper.source(workOrderId);
        if(source==null)throw new BusinessException(ErrorCode.ILLEGAL_STATE_TRANSITION,"只有已完成且维修记录完整的维修工单可以沉淀知识");
        if(mapper.countBySource(workOrderId)>0)throw new BusinessException(ErrorCode.DUPLICATE_OPERATION,"该工单已经创建知识条目");
        KnowledgeArticle article=new KnowledgeArticle();article.setTitle(defaultTitle(source));article.setSourceWorkOrderId(workOrderId);article.setEquipmentTypeId(source.equipmentTypeId());article.setFaultSymptom(source.faultDescription());article.setRootCause(source.rootCause());article.setSolution(source.repairAction());article.setRepairResult(source.repairResult());article.setSpareSummary(source.spareSummary());article.setStatus(KnowledgeStatus.DRAFT);article.setSubmittedBy(current().userId());
        try{mapper.insertArticle(article);}catch(DuplicateKeyException e){throw new BusinessException(ErrorCode.DUPLICATE_OPERATION,"该工单已经创建知识条目");}return article.getId();
    }
    @Transactional
    public void update(Long id,KnowledgeArticleRequest input){KnowledgeArticle article=required(id);assertCanEdit(article);if(article.getStatus()!=KnowledgeStatus.DRAFT&&article.getStatus()!=KnowledgeStatus.REJECTED)throw new BusinessException(ErrorCode.ILLEGAL_STATE_TRANSITION,"只有草稿或已退回知识可以修改");copy(article,input);mapper.updateDraft(article);}
    @Transactional
    public void submit(Long id){KnowledgeArticle article=required(id);assertCanEdit(article);if(article.getStatus()!=KnowledgeStatus.DRAFT&&article.getStatus()!=KnowledgeStatus.REJECTED)throw new BusinessException(ErrorCode.ILLEGAL_STATE_TRANSITION,"当前知识状态不能提交审核");if(mapper.submit(id)!=1)throw new BusinessException(ErrorCode.DUPLICATE_OPERATION,"知识状态已变化，请刷新后重试");}
    @Transactional
    public void review(Long id,KnowledgeReviewRequest input){KnowledgeArticle article=required(id);assertAuditor(article);if(article.getStatus()!=KnowledgeStatus.PENDING)throw new BusinessException(ErrorCode.ILLEGAL_STATE_TRANSITION,"只有待审核知识可以审核");String remark=trim(input.remark());if(!input.approved()&&(remark==null||remark.isBlank()))throw new BusinessException(ErrorCode.PARAMETER_ERROR,"审核退回必须填写意见");KnowledgeStatus target=input.approved()?KnowledgeStatus.PUBLISHED:KnowledgeStatus.REJECTED;if(mapper.review(id,target,current().userId(),remark)!=1)throw new BusinessException(ErrorCode.DUPLICATE_OPERATION,"知识状态已变化，请刷新后重试");}

    private void assertReadable(KnowledgeArticleVO article){LoginUser user=current();if(article.status()==KnowledgeStatus.PUBLISHED||article.submittedBy().equals(user.userId())||isAdmin(user)){return;}if(isSupervisor(user)){scopes.assertVisible(article.sourceWorkOrderId());return;}throw new BusinessException(ErrorCode.DATA_FORBIDDEN,"无权查看该知识条目");}
    private void assertCanEdit(KnowledgeArticle article){LoginUser user=current();if(article.getSubmittedBy().equals(user.userId())||isAdmin(user))return;if(isSupervisor(user)){scopes.assertVisible(article.getSourceWorkOrderId());return;}throw new BusinessException(ErrorCode.DATA_FORBIDDEN,"无权修改该知识条目");}
    private void assertAuditor(KnowledgeArticle article){LoginUser user=current();if(isAdmin(user))return;if(isSupervisor(user)){scopes.assertVisible(article.getSourceWorkOrderId());return;}throw new BusinessException(ErrorCode.DATA_FORBIDDEN,"仅管理员或授权范围内的运维主管可以审核知识");}
    private KnowledgeArticle required(Long id){KnowledgeArticle value=mapper.lock(id);if(value==null)throw new BusinessException(ErrorCode.NOT_FOUND,"知识条目不存在");return value;}
    private KnowledgeArticleVO requiredVO(Long id){KnowledgeArticleVO value=mapper.detail(id);if(value==null)throw new BusinessException(ErrorCode.NOT_FOUND,"知识条目不存在");return value;}
    private static void copy(KnowledgeArticle article,KnowledgeArticleRequest input){article.setTitle(input.title().trim());article.setFaultSymptom(input.faultSymptom().trim());article.setRootCause(input.rootCause().trim());article.setSolution(input.solution().trim());article.setRepairResult(input.repairResult().trim());}
    private static String defaultTitle(KnowledgeSourceRow source){String fault=source.faultDescription().trim();if(fault.length()>70)fault=fault.substring(0,70)+"…";return source.equipmentTypeName()+" - "+fault;}
    private static String trim(String value){return value==null?null:value.trim();}
    private static boolean isAdmin(LoginUser user){return user.roleCodes().contains("ADMIN");}private static boolean isSupervisor(LoginUser user){return user.roleCodes().contains("MAINTENANCE_SUPERVISOR");}private static LoginUser current(){return SecurityUtils.currentUser();}
}
