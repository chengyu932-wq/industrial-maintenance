package com.cq.maintenance.dispatch;

import com.cq.maintenance.common.exception.BusinessException;
import com.cq.maintenance.common.exception.ErrorCode;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.dispatch")
public class DispatchProperties {
    private BigDecimal skillWeight = new BigDecimal("0.40");
    private BigDecimal loadWeight = new BigDecimal("0.30");
    private BigDecimal teamWeight = new BigDecimal("0.20");
    private BigDecimal areaWeight = new BigDecimal("0.10");

    public void validate() {
        BigDecimal sum = skillWeight.add(loadWeight).add(teamWeight).add(areaWeight);
        if (skillWeight.signum() < 0 || loadWeight.signum() < 0 || teamWeight.signum() < 0
            || areaWeight.signum() < 0 || sum.compareTo(BigDecimal.ONE) != 0) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "智能派单权重必须非负且总和等于1");
        }
    }
    public BigDecimal getSkillWeight(){return skillWeight;} public void setSkillWeight(BigDecimal v){skillWeight=v;}
    public BigDecimal getLoadWeight(){return loadWeight;} public void setLoadWeight(BigDecimal v){loadWeight=v;}
    public BigDecimal getTeamWeight(){return teamWeight;} public void setTeamWeight(BigDecimal v){teamWeight=v;}
    public BigDecimal getAreaWeight(){return areaWeight;} public void setAreaWeight(BigDecimal v){areaWeight=v;}
}
