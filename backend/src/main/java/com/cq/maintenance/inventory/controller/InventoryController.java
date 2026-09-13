package com.cq.maintenance.inventory.controller;

import com.cq.maintenance.common.response.*;
import com.cq.maintenance.inventory.dto.*;
import com.cq.maintenance.inventory.service.InventoryService;
import com.cq.maintenance.inventory.vo.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService service;public InventoryController(InventoryService service){this.service=service;}
    @GetMapping("/stocks") @PreAuthorize("hasAnyAuthority('inventory:stock:list','inventory:issue')") public ApiResponse<PageResult<StockVO>> stocks(@Valid StockQuery query){return ApiResponse.success(service.stocks(query));}
    @GetMapping("/transactions") @PreAuthorize("hasAuthority('inventory:transaction:list')") public ApiResponse<PageResult<TransactionVO>> transactions(@Valid TransactionQuery query){return ApiResponse.success(service.transactions(query));}
    @PostMapping("/inbound") @PreAuthorize("hasAuthority('inventory:inbound')") public ApiResponse<Void> inbound(@Valid @RequestBody InventoryOperationRequest input){service.inbound(input);return ApiResponse.success(null);}
    @PostMapping("/outbound") @PreAuthorize("hasAuthority('inventory:outbound')") public ApiResponse<Void> outbound(@Valid @RequestBody InventoryOperationRequest input){service.outbound(input);return ApiResponse.success(null);}
    @PostMapping("/scrap") @PreAuthorize("hasAuthority('inventory:scrap')") public ApiResponse<Void> scrap(@Valid @RequestBody InventoryOperationRequest input){service.scrap(input);return ApiResponse.success(null);}
    @PostMapping("/transfer") @PreAuthorize("hasAuthority('inventory:transfer')") public ApiResponse<Void> transfer(@Valid @RequestBody TransferRequest input){service.transfer(input);return ApiResponse.success(null);}
    @PostMapping("/stocktake") @PreAuthorize("hasAuthority('inventory:stocktake')") public ApiResponse<Void> stocktake(@Valid @RequestBody StocktakeRequest input){service.stocktake(input);return ApiResponse.success(null);}
    @GetMapping("/safety-stock/suggestions") @PreAuthorize("hasAuthority('inventory:stock:list')") public ApiResponse<SafetyStockVO> safetyStock(@RequestParam Long warehouseId,@RequestParam Long sparePartId){return ApiResponse.success(service.safetyStock(warehouseId,sparePartId));}
    @GetMapping("/warnings") @PreAuthorize("hasAuthority('inventory:warning:list')") public ApiResponse<List<WarningVO>> warnings(){return ApiResponse.success(service.warnings());}
}
