package com.cq.maintenance.audit.controller;
import com.cq.maintenance.audit.dto.OperationLogQuery;
import com.cq.maintenance.audit.entity.OperationLog;
import com.cq.maintenance.audit.service.OperationLogService;
import com.cq.maintenance.common.response.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/operation-logs") public class OperationLogController {private final OperationLogService service;public OperationLogController(OperationLogService service){this.service=service;}@GetMapping @PreAuthorize("hasAuthority('system:operation-log:list')") public ApiResponse<PageResult<OperationLog>> page(@Valid OperationLogQuery query){return ApiResponse.success(service.page(query));}}
