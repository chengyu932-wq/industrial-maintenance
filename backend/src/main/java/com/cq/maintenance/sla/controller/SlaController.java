package com.cq.maintenance.sla.controller;
import com.cq.maintenance.common.response.*;
import com.cq.maintenance.sla.dto.*;
import com.cq.maintenance.sla.service.SlaService;
import com.cq.maintenance.sla.vo.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/sla")
public class SlaController {
    private final SlaService service;public SlaController(SlaService service){this.service=service;}
    @GetMapping("/rules") @PreAuthorize("hasAuthority('sla:rule:list')") public ApiResponse<List<SlaRuleVO>> rules(){return ApiResponse.success(service.rules());}
    @PutMapping("/rules/{id}") @PreAuthorize("hasAuthority('sla:rule:update')") public ApiResponse<Void> update(@PathVariable Long id,@Valid @RequestBody SlaRuleUpdateRequest input){service.updateRule(id,input);return ApiResponse.success(null);}
    @GetMapping("/events") @PreAuthorize("hasAuthority('sla:event:list')") public ApiResponse<PageResult<SlaEventVO>> events(@Valid SlaEventQuery query){return ApiResponse.success(service.events(query));}
    @PostMapping("/events/{id}/handle") @PreAuthorize("hasAuthority('sla:event:handle')") public ApiResponse<Void> handle(@PathVariable Long id){service.handle(id);return ApiResponse.success(null);}
}
