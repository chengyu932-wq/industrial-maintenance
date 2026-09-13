package com.cq.maintenance.inventory.controller;

import com.cq.maintenance.common.response.*;
import com.cq.maintenance.inventory.dto.*;
import com.cq.maintenance.inventory.service.InventoryService;
import com.cq.maintenance.inventory.vo.WarehouseVO;
import com.cq.maintenance.system.vo.UserSummaryVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/warehouses")
public class WarehouseController {
    private final InventoryService service;public WarehouseController(InventoryService service){this.service=service;}
    @GetMapping @PreAuthorize("hasAnyAuthority('warehouse:list','inventory:issue')") public ApiResponse<PageResult<WarehouseVO>> page(@Valid WarehouseQuery query){return ApiResponse.success(service.warehouses(query));}
    @GetMapping("/{id}") @PreAuthorize("hasAnyAuthority('warehouse:list','inventory:issue')") public ApiResponse<WarehouseVO> detail(@PathVariable Long id){return ApiResponse.success(service.warehouseDetail(id));}
    @PostMapping @PreAuthorize("hasAuthority('warehouse:manage')") public ApiResponse<Long> create(@Valid @RequestBody WarehouseRequest input){return ApiResponse.success(service.createWarehouse(input));}
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('warehouse:manage')") public ApiResponse<Void> update(@PathVariable Long id,@Valid @RequestBody WarehouseRequest input){service.updateWarehouse(id,input);return ApiResponse.success(null);}
    @PutMapping("/{id}/authorized-users") @PreAuthorize("hasAuthority('warehouse:authorize')") public ApiResponse<Void> authorize(@PathVariable Long id,@Valid @RequestBody WarehouseAuthorizationRequest input){service.authorizeWarehouse(id,input);return ApiResponse.success(null);}
    @GetMapping("/authorized-user-options") @PreAuthorize("hasAuthority('warehouse:authorize')") public ApiResponse<List<UserSummaryVO>> authorizedUserOptions(){return ApiResponse.success(service.warehouseAssignableUsers());}
}
