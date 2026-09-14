package com.cq.maintenance.sla.dto;

import com.cq.maintenance.sla.entity.SlaEventType;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SlaEventQuery {
    @Min(1) private long page=1; @Min(1) @Max(100) private long size=20;
    private SlaEventType eventType; private Boolean handled;
    public long getOffset(){return (page-1)*size;}
}
