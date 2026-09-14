package com.cq.maintenance.dispatch.service;

import com.cq.maintenance.common.exception.*;
import com.cq.maintenance.dispatch.DispatchProperties;
import com.cq.maintenance.dispatch.mapper.DispatchMapper;
import com.cq.maintenance.dispatch.vo.*;
import com.cq.maintenance.security.LoginUser;
import com.cq.maintenance.security.SecurityUtils;
import com.cq.maintenance.workorder.entity.*;
import com.cq.maintenance.workorder.service.WorkOrderScopeService;
import java.math.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DispatchRecommendationService {
    private static final BigDecimal ZERO=BigDecimal.ZERO.setScale(4);
    private static final BigDecimal ONE=BigDecimal.ONE.setScale(4);
    private final DispatchMapper mapper; private final WorkOrderScopeService scopes; private final DispatchProperties properties;
    public DispatchRecommendationService(DispatchMapper mapper,WorkOrderScopeService scopes,DispatchProperties properties){this.mapper=mapper;this.scopes=scopes;this.properties=properties;}

    @Transactional(readOnly=true)
    public List<DispatchRecommendationVO> recommend(Long workOrderId){
        scopes.assertCanAssign(workOrderId);properties.validate();DispatchContext context=mapper.findContext(workOrderId);
        if(context==null)throw new BusinessException(ErrorCode.NOT_FOUND,"工单不存在");
        if(context.workOrderType()!=WorkOrderType.REPAIR)throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"智能推荐仅适用于维修工单");
        if(context.status()!=WorkOrderStatus.PENDING_ASSIGN)throw new BusinessException(ErrorCode.ILLEGAL_STATE_TRANSITION,"仅待派单维修工单可以生成推荐");
        LoginUser operator=SecurityUtils.currentUser();List<SkillMatchRow> required=mapper.findRequiredSkills(context.equipmentTypeId());
        Set<Long> requiredIds=required.stream().map(SkillMatchRow::skillId).collect(Collectors.toSet());
        List<String> requiredNames=required.stream().map(SkillMatchRow::skillName).toList();
        return mapper.findEligibleEngineers().stream().filter(e->allowed(operator,e)).map(e->score(context,e,requiredIds,requiredNames))
            .sorted(Comparator.comparing(DispatchRecommendationVO::totalScore).reversed()
                .thenComparing(DispatchRecommendationVO::skillScore,Comparator.reverseOrder())
                .thenComparingLong(DispatchRecommendationVO::currentActiveOrders)
                .thenComparing(DispatchRecommendationVO::teamScore,Comparator.reverseOrder())
                .thenComparing(DispatchRecommendationVO::engineerId)).limit(3).toList();
    }

    private boolean allowed(LoginUser operator,DispatchEngineerRow e){
        if(operator.roleCodes().contains("ADMIN"))return true;
        return (operator.workshopId()!=null&&operator.workshopId().equals(e.workshopId()))
            ||(e.teamId()!=null&&operator.teamIds().contains(e.teamId()));
    }
    private DispatchRecommendationVO score(DispatchContext c,DispatchEngineerRow e,Set<Long> requiredIds,List<String> requiredNames){
        List<SkillMatchRow> engineerSkills=mapper.findEngineerSkills(e.engineerId());
        List<String> matched=engineerSkills.stream().filter(s->requiredIds.contains(s.skillId())).map(SkillMatchRow::skillName).toList();
        boolean skillApplicable=!requiredIds.isEmpty();
        BigDecimal skill=skillApplicable?ratio(matched.size(),requiredIds.size()):ZERO;
        BigDecimal load=BigDecimal.ONE.divide(BigDecimal.valueOf(1+e.activeWorkOrderCount()),4,RoundingMode.HALF_UP);
        BigDecimal team=Objects.equals(e.teamId(),c.responsibleTeamId())&&e.teamId()!=null?ONE:ZERO;
        BigDecimal area=Objects.equals(e.workshopId(),c.workshopId())&&e.workshopId()!=null?ONE:ZERO;
        BigDecimal denominator=properties.getLoadWeight().add(properties.getTeamWeight()).add(properties.getAreaWeight())
            .add(skillApplicable?properties.getSkillWeight():BigDecimal.ZERO);
        BigDecimal weighted=load.multiply(properties.getLoadWeight()).add(team.multiply(properties.getTeamWeight())).add(area.multiply(properties.getAreaWeight()));
        if(skillApplicable)weighted=weighted.add(skill.multiply(properties.getSkillWeight()));
        BigDecimal total=weighted.divide(denominator,4,RoundingMode.HALF_UP);
        List<String> reasons=new ArrayList<>();
        reasons.add(skillApplicable?"匹配"+matched.size()+"/"+requiredIds.size()+"项技能":"设备类型未配置所需技能，本次不计技能维度");
        reasons.add("当前"+e.activeWorkOrderCount()+"张在处工单");
        if(team.signum()>0)reasons.add("与设备属于同一负责班组");
        if(area.signum()>0)reasons.add("负责当前车间");
        return new DispatchRecommendationVO(e.engineerId(),e.engineerName(),e.teamId(),e.teamName(),total,skill,load,team,area,e.activeWorkOrderCount(),matched,requiredNames,reasons);
    }
    private BigDecimal ratio(long numerator,long denominator){return BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator),4,RoundingMode.HALF_UP);}
}
