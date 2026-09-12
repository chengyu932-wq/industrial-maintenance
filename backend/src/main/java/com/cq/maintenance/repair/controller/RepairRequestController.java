package com.cq.maintenance.repair.controller;

import com.cq.maintenance.common.response.*;
import com.cq.maintenance.repair.dto.*;
import com.cq.maintenance.repair.service.RepairRequestService;
import com.cq.maintenance.repair.vo.*;
import com.cq.maintenance.workorder.dto.CancelWorkOrderRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated @RestController @RequestMapping("/api/repair-requests")
public class RepairRequestController {
    private final RepairRequestService service;public RepairRequestController(RepairRequestService service){this.service=service;}
    @PostMapping @PreAuthorize("hasAuthority('repair:create')") public ApiResponse<RepairCreateVO> create(@Valid @RequestBody RepairRequestCreateRequest input){return ApiResponse.success(service.create(input));}
    @GetMapping @PreAuthorize("hasAuthority('repair:list')") public ApiResponse<PageResult<RepairRequestVO>> page(@Valid RepairRequestQuery query){return ApiResponse.success(service.page(query));}
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('repair:view')") public ApiResponse<RepairRequestVO> detail(@PathVariable Long id){return ApiResponse.success(service.detail(id));}
    @PostMapping("/{id}/cancel") @PreAuthorize("hasAuthority('repair:cancel')") public ApiResponse<Void> cancel(@PathVariable Long id,@Valid @RequestBody CancelWorkOrderRequest input){service.cancel(id,input);return ApiResponse.success(null);}
}
