package com.cq.maintenance.knowledge.dto;

import com.cq.maintenance.knowledge.entity.KnowledgeStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import lombok.Data;

@Data
public class KnowledgeQuery {
    private String keyword;
    private Long equipmentTypeId;
    private KnowledgeStatus status;
    private String sourceWorkOrderNo;
    private LocalDate startDate;
    private LocalDate endDate;
    @Min(value=1,message="页码必须大于0") private long page=1;
    @Min(value=1,message="每页条数必须大于0") @Max(value=100,message="每页最多100条") private long size=10;
    public long getOffset(){return (page-1)*size;}
}
