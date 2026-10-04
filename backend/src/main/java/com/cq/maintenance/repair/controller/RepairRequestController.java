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
    @org.springframework.beans.factory.annotation.Autowired private com.cq.maintenance.repair.service.RepairAttachmentService files;
    @GetMapping("/{id}/attachments") @PreAuthorize("hasAuthority('repair:view')") public ApiResponse<java.util.List<com.cq.maintenance.common.storage.AttachmentVO>> attachments(@PathVariable Long id){return ApiResponse.success(files.list(id));}
    @PostMapping(value="/{id}/attachments",consumes="multipart/form-data") @PreAuthorize("hasAuthority('repair:create')") public ApiResponse<Long> upload(@PathVariable Long id,@RequestPart("file") org.springframework.web.multipart.MultipartFile file){return ApiResponse.success(files.upload(id,file));}
    @GetMapping("/{id}/attachments/{attachmentId}") @PreAuthorize("hasAuthority('repair:view')") public org.springframework.http.ResponseEntity<byte[]> download(@PathVariable Long id,@PathVariable Long attachmentId){var file=files.download(id,attachmentId);String name=java.net.URLEncoder.encode(file.fileName(),java.nio.charset.StandardCharsets.UTF_8).replace("+","%20");return org.springframework.http.ResponseEntity.ok().header("Content-Disposition","attachment; filename*=UTF-8''"+name).contentType(org.springframework.http.MediaType.parseMediaType(file.contentType())).body(file.content());}
    @PostMapping @PreAuthorize("hasAuthority('repair:create')") public ApiResponse<RepairCreateVO> create(@Valid @RequestBody RepairRequestCreateRequest input){return ApiResponse.success(service.create(input));}
    @PostMapping("/{id}/confirm") @PreAuthorize("hasAuthority('repair:confirm')") public ApiResponse<RepairCreateVO> confirm(@PathVariable Long id){return ApiResponse.success(service.confirm(id));}
    @GetMapping @PreAuthorize("hasAuthority('repair:list')") public ApiResponse<PageResult<RepairRequestVO>> page(@Valid RepairRequestQuery query){return ApiResponse.success(service.page(query));}
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('repair:view')") public ApiResponse<RepairRequestVO> detail(@PathVariable Long id){return ApiResponse.success(service.detail(id));}
    @PostMapping("/{id}/cancel") @PreAuthorize("hasAuthority('repair:cancel')") public ApiResponse<Void> cancel(@PathVariable Long id,@Valid @RequestBody CancelWorkOrderRequest input){service.cancel(id,input);return ApiResponse.success(null);}
}
