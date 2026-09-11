package com.cq.maintenance.organization.controller;

import com.cq.maintenance.common.response.ApiResponse;
import com.cq.maintenance.organization.dto.LineRequest;
import com.cq.maintenance.organization.dto.StationRequest;
import com.cq.maintenance.organization.dto.WorkshopRequest;
import com.cq.maintenance.organization.service.OrganizationService;
import com.cq.maintenance.organization.vo.LineVO;
import com.cq.maintenance.organization.vo.OrganizationTreeVO;
import com.cq.maintenance.organization.vo.StationVO;
import com.cq.maintenance.organization.vo.WorkshopVO;
import com.cq.maintenance.organization.vo.TeamVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {
    private final OrganizationService service;
    public OrganizationController(OrganizationService service){this.service=service;}
    @GetMapping("/tree") @PreAuthorize("hasAuthority('organization:list')") public ApiResponse<List<OrganizationTreeVO>> tree(@RequestParam(required=false) String status){return ApiResponse.success(service.tree(status));}
    @GetMapping("/workshops") @PreAuthorize("hasAuthority('organization:list')") public ApiResponse<List<WorkshopVO>> workshops(@RequestParam(required=false) String status){return ApiResponse.success(service.workshops(status));}
    @PostMapping("/workshops") @PreAuthorize("hasAuthority('organization:add')") public ApiResponse<Void> createWorkshop(@Valid @RequestBody WorkshopRequest r){service.createWorkshop(r);return ApiResponse.success(null);}
    @PutMapping("/workshops/{id}") @PreAuthorize("hasAuthority('organization:update')") public ApiResponse<Void> updateWorkshop(@PathVariable Long id,@Valid @RequestBody WorkshopRequest r){service.updateWorkshop(id,r);return ApiResponse.success(null);}
    @DeleteMapping("/workshops/{id}") @PreAuthorize("hasAuthority('organization:delete')") public ApiResponse<Void> deleteWorkshop(@PathVariable Long id){service.deleteWorkshop(id);return ApiResponse.success(null);}
    @GetMapping("/lines") @PreAuthorize("hasAuthority('organization:list')") public ApiResponse<List<LineVO>> lines(@RequestParam(required=false) Long workshopId,@RequestParam(required=false) String status){return ApiResponse.success(service.lines(workshopId,status));}
    @PostMapping("/lines") @PreAuthorize("hasAuthority('organization:add')") public ApiResponse<Void> createLine(@Valid @RequestBody LineRequest r){service.createLine(r);return ApiResponse.success(null);}
    @PutMapping("/lines/{id}") @PreAuthorize("hasAuthority('organization:update')") public ApiResponse<Void> updateLine(@PathVariable Long id,@Valid @RequestBody LineRequest r){service.updateLine(id,r);return ApiResponse.success(null);}
    @DeleteMapping("/lines/{id}") @PreAuthorize("hasAuthority('organization:delete')") public ApiResponse<Void> deleteLine(@PathVariable Long id){service.deleteLine(id);return ApiResponse.success(null);}
    @GetMapping("/stations") @PreAuthorize("hasAuthority('organization:list')") public ApiResponse<List<StationVO>> stations(@RequestParam(required=false) Long lineId,@RequestParam(required=false) String status){return ApiResponse.success(service.stations(lineId,status));}
    @PostMapping("/stations") @PreAuthorize("hasAuthority('organization:add')") public ApiResponse<Void> createStation(@Valid @RequestBody StationRequest r){service.createStation(r);return ApiResponse.success(null);}
    @PutMapping("/stations/{id}") @PreAuthorize("hasAuthority('organization:update')") public ApiResponse<Void> updateStation(@PathVariable Long id,@Valid @RequestBody StationRequest r){service.updateStation(id,r);return ApiResponse.success(null);}
    @DeleteMapping("/stations/{id}") @PreAuthorize("hasAuthority('organization:delete')") public ApiResponse<Void> deleteStation(@PathVariable Long id){service.deleteStation(id);return ApiResponse.success(null);}
    @GetMapping("/teams") @PreAuthorize("hasAnyAuthority('organization:list','equipment:add','equipment:update')") public ApiResponse<List<TeamVO>> teams(@RequestParam(required=false) Long workshopId,@RequestParam(required=false) String status){return ApiResponse.success(service.teams(workshopId,status));}
}
