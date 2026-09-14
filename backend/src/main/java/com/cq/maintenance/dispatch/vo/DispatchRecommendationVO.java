package com.cq.maintenance.dispatch.vo;

import java.math.BigDecimal;
import java.util.List;

public record DispatchRecommendationVO(Long engineerId,String engineerName,Long teamId,String teamName,
    BigDecimal totalScore,BigDecimal skillScore,BigDecimal loadScore,BigDecimal teamScore,
    BigDecimal areaScore,long currentActiveOrders,List<String> matchedSkills,List<String> requiredSkills,
    List<String> recommendationReasons) {
    public DispatchRecommendationVO {
        matchedSkills=List.copyOf(matchedSkills);requiredSkills=List.copyOf(requiredSkills);
        recommendationReasons=List.copyOf(recommendationReasons);
    }
}
