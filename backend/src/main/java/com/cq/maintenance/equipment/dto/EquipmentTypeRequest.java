package com.cq.maintenance.equipment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
public record EquipmentTypeRequest(
    @NotBlank(message="类型编码不能为空") @Size(max=50,message="类型编码不能超过50个字符") String typeCode,
    @NotBlank(message="类型名称不能为空") @Size(max=100,message="类型名称不能超过100个字符") String typeName,
    @Size(max=500,message="说明不能超过500个字符") String description,
    @Pattern(regexp="ENABLED|DISABLED",message="类型状态不合法") String status) {}
