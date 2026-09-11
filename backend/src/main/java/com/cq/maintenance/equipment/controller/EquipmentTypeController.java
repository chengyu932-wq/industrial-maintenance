package com.cq.maintenance.equipment.controller;

import com.cq.maintenance.common.response.ApiResponse;
import com.cq.maintenance.equipment.dto.EquipmentTypeRequest;
import com.cq.maintenance.equipment.entity.EquipmentType;
import com.cq.maintenance.equipment.service.EquipmentTypeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/equipment-types")
public class EquipmentTypeController {
    private final EquipmentTypeService service;public EquipmentTypeController(EquipmentTypeService service){this.service=service;}
    @GetMapping @PreAuthorize("hasAuthority('equipment:list')") public ApiResponse<List<EquipmentType>> list(){return ApiResponse.success(service.list());}
    @PostMapping @PreAuthorize("hasAuthority('equipment:type:manage')") public ApiResponse<Void> create(@Valid @RequestBody EquipmentTypeRequest r){service.create(r);return ApiResponse.success(null);}
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('equipment:type:manage')") public ApiResponse<Void> update(@PathVariable Long id,@Valid @RequestBody EquipmentTypeRequest r){service.update(id,r);return ApiResponse.success(null);}
    @DeleteMapping("/{id}") @PreAuthorize("hasAuthority('equipment:type:manage')") public ApiResponse<Void> delete(@PathVariable Long id){service.delete(id);return ApiResponse.success(null);}
}
