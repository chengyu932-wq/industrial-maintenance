package com.cq.maintenance.maintenance.controller;

import com.cq.maintenance.common.response.ApiResponse;
import com.cq.maintenance.maintenance.dto.MaintenanceExecutionItemRequest;
import com.cq.maintenance.maintenance.service.MaintenanceExecutionService;
import com.cq.maintenance.maintenance.vo.MaintenanceWorkOrderVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/work-orders/{workOrderId}/maintenance-items")
public class MaintenanceExecutionController {
    private final MaintenanceExecutionService service;public MaintenanceExecutionController(MaintenanceExecutionService service){this.service=service;}
    @GetMapping @PreAuthorize("hasAuthority('workorder:view')") public ApiResponse<MaintenanceWorkOrderVO> detail(@PathVariable Long workOrderId){return ApiResponse.success(service.detail(workOrderId));}
    @PutMapping @PreAuthorize("hasAuthority('workorder:process')") public ApiResponse<Void> save(@PathVariable Long workOrderId,@Valid @RequestBody List<@Valid MaintenanceExecutionItemRequest> input){service.save(workOrderId,input);return ApiResponse.success(null);}
}
