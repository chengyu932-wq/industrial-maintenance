package com.cq.maintenance.audit.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data public class OperationLogQuery {private String module;private String result;private Long userId;@Min(value=1,message="页码必须大于0") private long page=1;@Min(value=1,message="每页数量必须大于0") @Max(value=100,message="每页最多100条") private long size=20;public long getOffset(){return(page-1)*size;}}
