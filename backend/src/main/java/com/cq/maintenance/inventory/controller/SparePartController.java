package com.cq.maintenance.inventory.controller;

import com.cq.maintenance.common.response.*;
import com.cq.maintenance.inventory.dto.*;
import com.cq.maintenance.inventory.service.InventoryService;
import com.cq.maintenance.inventory.vo.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/spare-parts")
public class SparePartController {
    private final InventoryService service;public SparePartController(InventoryService service){this.service=service;}
    @GetMapping @PreAuthorize("hasAnyAuthority('spare:list','inventory:issue')") public ApiResponse<PageResult<SparePartVO>> page(@Valid SparePartQuery query){return ApiResponse.success(service.spareParts(query));}
    @GetMapping("/{id}") @PreAuthorize("hasAnyAuthority('spare:list','inventory:issue')") public ApiResponse<SparePartVO> detail(@PathVariable Long id){return ApiResponse.success(service.sparePartDetail(id));}
    @PostMapping @PreAuthorize("hasAuthority('spare:manage')") public ApiResponse<Long> create(@Valid @RequestBody SparePartRequest input){return ApiResponse.success(service.createSparePart(input));}
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('spare:manage')") public ApiResponse<Void> update(@PathVariable Long id,@Valid @RequestBody SparePartRequest input){service.updateSparePart(id,input);return ApiResponse.success(null);}
    @GetMapping("/suppliers/options") @PreAuthorize("hasAuthority('spare:list')") public ApiResponse<List<SupplierVO>> suppliers(){return ApiResponse.success(service.suppliers());}
}
