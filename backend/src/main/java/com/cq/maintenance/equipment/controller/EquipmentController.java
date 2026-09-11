package com.cq.maintenance.equipment.controller;

import com.cq.maintenance.common.response.ApiResponse;
import com.cq.maintenance.common.response.PageResult;
import com.cq.maintenance.equipment.dto.*;
import com.cq.maintenance.equipment.service.*;
import com.cq.maintenance.equipment.vo.*;
import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Validated @RestController @RequestMapping("/api/equipment")
public class EquipmentController {
    private final EquipmentService service;private final EquipmentStateService states;private final EquipmentQrCodeService qr;private final EquipmentExcelService excel;
    public EquipmentController(EquipmentService service,EquipmentStateService states,EquipmentQrCodeService qr,EquipmentExcelService excel){this.service=service;this.states=states;this.qr=qr;this.excel=excel;}
    @GetMapping @PreAuthorize("hasAuthority('equipment:list')") public ApiResponse<PageResult<EquipmentListVO>> page(@Valid EquipmentQuery q){return ApiResponse.success(service.page(q));}
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('equipment:view')") public ApiResponse<EquipmentDetailVO> detail(@PathVariable Long id){return ApiResponse.success(service.detail(id));}
    @PostMapping @PreAuthorize("hasAuthority('equipment:add')") public ApiResponse<Long> create(@Valid @RequestBody EquipmentCreateRequest r){return ApiResponse.success(service.create(r));}
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('equipment:update')") public ApiResponse<Void> update(@PathVariable Long id,@Valid @RequestBody EquipmentUpdateRequest r){service.update(id,r);return ApiResponse.success(null);}
    @PostMapping("/{id}/status") @PreAuthorize("hasAuthority('equipment:status')") public ApiResponse<Void> state(@PathVariable Long id,@Valid @RequestBody EquipmentStateRequest r){states.manual(id,r.targetStatus(),r.reason());return ApiResponse.success(null);}
    @PostMapping("/{id}/scrap") @PreAuthorize("hasAuthority('equipment:scrap')") public ApiResponse<Void> scrap(@PathVariable Long id,@Valid @RequestBody ScrapRequest r){states.scrap(id,r.reason());return ApiResponse.success(null);}
    @GetMapping("/{id}/history") @PreAuthorize("hasAuthority('equipment:view')") public ApiResponse<List<EquipmentStatusLogVO>> history(@PathVariable Long id){return ApiResponse.success(service.history(id));}
    @GetMapping(value="/{id}/qrcode",produces=MediaType.IMAGE_PNG_VALUE) @PreAuthorize("hasAuthority('equipment:view')") public ResponseEntity<byte[]> qrcode(@PathVariable Long id){return ResponseEntity.ok().cacheControl(CacheControl.noCache()).body(qr.png(id));}
    @GetMapping("/export") @PreAuthorize("hasAuthority('equipment:export')") public ResponseEntity<byte[]> export(@Valid EquipmentQuery q){return attachment("设备台账.xlsx",excel.export(q));}
    @GetMapping("/import-template") @PreAuthorize("hasAuthority('equipment:import')") public ResponseEntity<byte[]> template(){return attachment("设备导入模板.xlsx",excel.template());}
    @PostMapping(value="/import",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @PreAuthorize("hasAuthority('equipment:import')") public ApiResponse<ImportResultVO> importFile(@RequestPart("file") MultipartFile file){return ApiResponse.success(excel.importFile(file));}
    private ResponseEntity<byte[]> attachment(String name,byte[] body){String encoded=URLEncoder.encode(name,StandardCharsets.UTF_8).replace("+","%20");return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename*=UTF-8''"+encoded).contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).body(body);}
}
