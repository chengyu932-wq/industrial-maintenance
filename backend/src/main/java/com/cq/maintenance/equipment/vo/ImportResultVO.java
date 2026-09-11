package com.cq.maintenance.equipment.vo;

import java.util.List;
public record ImportResultVO(int totalRows,int successRows,int failedRows,List<RowError> errors) {
    public record RowError(int row,String equipmentNo,String message) {}
}
