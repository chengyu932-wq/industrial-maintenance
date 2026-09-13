package com.cq.maintenance.maintenance.controller;

import com.cq.maintenance.common.response.*;
import com.cq.maintenance.maintenance.dto.*;
import com.cq.maintenance.maintenance.entity.MaintenancePlanStatus;
import com.cq.maintenance.maintenance.service.*;
import com.cq.maintenance.maintenance.vo.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated @RestController @RequestMapping("/api/maintenance-plans")
public class MaintenancePlanController {
    private final MaintenancePlanService service;private final MaintenanceScanService scanner;
    public MaintenancePlanController(MaintenancePlanService service,MaintenanceScanService scanner){this.service=service;this.scanner=scanner;}
    @GetMapping @PreAuthorize("hasAuthority('maintenance:plan:list')") public ApiResponse<PageResult<MaintenancePlanVO>> page(@Valid MaintenancePlanQuery q){return ApiResponse.success(service.page(q));}
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('maintenance:plan:view')") public ApiResponse<MaintenancePlanVO> detail(@PathVariable Long id){return ApiResponse.success(service.detail(id));}
    @PostMapping @PreAuthorize("hasAuthority('maintenance:plan:add')") public ApiResponse<Long> create(@Valid @RequestBody MaintenancePlanRequest r){return ApiResponse.success(service.create(r));}
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('maintenance:plan:update')") public ApiResponse<Void> update(@PathVariable Long id,@Valid @RequestBody MaintenancePlanRequest r){service.update(id,r);return ApiResponse.success(null);}
    @PostMapping("/{id}/enable") @PreAuthorize("hasAuthority('maintenance:plan:status')") public ApiResponse<Void> enable(@PathVariable Long id){service.setStatus(id,MaintenancePlanStatus.ENABLED);return ApiResponse.success(null);}
    @PostMapping("/{id}/disable") @PreAuthorize("hasAuthority('maintenance:plan:status')") public ApiResponse<Void> disable(@PathVariable Long id){service.setStatus(id,MaintenancePlanStatus.DISABLED);return ApiResponse.success(null);}
    @GetMapping("/{id}/items") @PreAuthorize("hasAuthority('maintenance:plan:view')") public ApiResponse<List<MaintenancePlanItemVO>> items(@PathVariable Long id){return ApiResponse.success(service.items(id));}
    @PutMapping("/{id}/items") @PreAuthorize("hasAuthority('maintenance:plan:update')") public ApiResponse<Void> items(@PathVariable Long id,@Valid @RequestBody List<@Valid MaintenancePlanItemRequest> items){service.replaceItems(id,items);return ApiResponse.success(null);}
    @GetMapping("/{id}/history") @PreAuthorize("hasAuthority('maintenance:history:view')") public ApiResponse<List<MaintenanceHistoryVO>> history(@PathVariable Long id){return ApiResponse.success(service.history(id));}
    @PostMapping("/scan") @PreAuthorize("hasAuthority('maintenance:scan')") public ApiResponse<MaintenanceScanResultVO> scan(){return ApiResponse.success(scanner.scanDuePlans());}
}
