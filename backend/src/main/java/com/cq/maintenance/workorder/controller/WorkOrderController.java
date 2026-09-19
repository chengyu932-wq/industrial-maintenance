package com.cq.maintenance.workorder.controller;

import com.cq.maintenance.common.response.*;
import com.cq.maintenance.common.storage.*;
import com.cq.maintenance.workorder.dto.*;
import com.cq.maintenance.workorder.service.WorkOrderService;
import com.cq.maintenance.workorder.service.WorkOrderAttachmentService;
import com.cq.maintenance.workorder.vo.*;
import com.cq.maintenance.inventory.dto.*;
import com.cq.maintenance.inventory.service.InventoryService;
import com.cq.maintenance.inventory.vo.WorkOrderSpareVO;
import com.cq.maintenance.dispatch.service.DispatchRecommendationService;
import com.cq.maintenance.dispatch.vo.DispatchRecommendationVO;
import com.cq.maintenance.workorder.service.WorkOrderPdfService;
import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Validated @RestController @RequestMapping("/api/work-orders")
public class WorkOrderController {
    private final WorkOrderService service;private final InventoryService inventory;private final DispatchRecommendationService recommendations;private final WorkOrderPdfService pdf;private final WorkOrderAttachmentService files;public WorkOrderController(WorkOrderService service,InventoryService inventory,DispatchRecommendationService recommendations,WorkOrderPdfService pdf,WorkOrderAttachmentService files){this.service=service;this.inventory=inventory;this.recommendations=recommendations;this.pdf=pdf;this.files=files;}
    @GetMapping @PreAuthorize("hasAuthority('workorder:list')") public ApiResponse<PageResult<WorkOrderListVO>> page(@Valid WorkOrderQuery query){return ApiResponse.success(service.page(query));}
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('workorder:view')") public ApiResponse<WorkOrderDetailVO> detail(@PathVariable Long id){return ApiResponse.success(service.detail(id));}
    @GetMapping("/{id}/flows") @PreAuthorize("hasAuthority('workorder:view')") public ApiResponse<List<WorkOrderFlowVO>> flows(@PathVariable Long id){return ApiResponse.success(service.flows(id));}
    @GetMapping("/{id}/engineers") @PreAuthorize("hasAuthority('workorder:assign')") public ApiResponse<List<EngineerOptionVO>> engineers(@PathVariable Long id){return ApiResponse.success(service.engineers(id));}
    @GetMapping("/{id}/dispatch-candidates") @PreAuthorize("hasAuthority('workorder:assign')") public ApiResponse<List<DispatchRecommendationVO>> dispatchCandidates(@PathVariable Long id){return ApiResponse.success(recommendations.recommend(id));}
    @PostMapping("/{id}/assign") @PreAuthorize("hasAuthority('workorder:assign')") public ApiResponse<Void> assign(@PathVariable Long id,@Valid @RequestBody DispatchRequest input){service.assign(id,input);return ApiResponse.success(null);}
    @PostMapping("/{id}/accept") @PreAuthorize("hasAuthority('workorder:process')") public ApiResponse<Void> acceptResponse(@PathVariable Long id){service.acceptResponse(id);return ApiResponse.success(null);}
    @PostMapping("/{id}/start") @PreAuthorize("hasAuthority('workorder:process')") public ApiResponse<Void> start(@PathVariable Long id){service.start(id);return ApiResponse.success(null);}
    @PostMapping("/{id}/suspend") @PreAuthorize("hasAuthority('workorder:process')") public ApiResponse<Void> suspend(@PathVariable Long id,@Valid @RequestBody ReasonRequest input){service.suspend(id,input);return ApiResponse.success(null);}
    @PostMapping("/{id}/resume") @PreAuthorize("hasAuthority('workorder:process')") public ApiResponse<Void> resume(@PathVariable Long id,@Valid @RequestBody ReasonRequest input){service.resume(id,input);return ApiResponse.success(null);}
    @PutMapping("/{id}/repair-record") @PreAuthorize("hasAuthority('workorder:process')") public ApiResponse<Void> repairRecord(@PathVariable Long id,@Valid @RequestBody RepairRecordRequest input){service.saveRepairRecord(id,input);return ApiResponse.success(null);}
    @PostMapping("/{id}/submit-acceptance") @PreAuthorize("hasAuthority('workorder:process')") public ApiResponse<Void> submit(@PathVariable Long id){service.submitAcceptance(id);return ApiResponse.success(null);}
    @PostMapping("/{id}/acceptance/pass") @PreAuthorize("hasAuthority('workorder:accept')") public ApiResponse<Void> pass(@PathVariable Long id,@Valid @RequestBody AcceptanceRequest input){service.pass(id,input);return ApiResponse.success(null);}
    @PostMapping("/{id}/acceptance/reject") @PreAuthorize("hasAuthority('workorder:accept')") public ApiResponse<Void> reject(@PathVariable Long id,@Valid @RequestBody ReasonRequest input){service.reject(id,input);return ApiResponse.success(null);}
    @PostMapping("/{id}/cancel") @PreAuthorize("hasAuthority('workorder:cancel')") public ApiResponse<Void> cancel(@PathVariable Long id,@Valid @RequestBody CancelWorkOrderRequest input){service.cancel(id,input);return ApiResponse.success(null);}
    @GetMapping("/{id}/spares") @PreAuthorize("hasAuthority('workorder:view')") public ApiResponse<List<WorkOrderSpareVO>> spares(@PathVariable Long id){return ApiResponse.success(inventory.workOrderSpares(id));}
    @GetMapping(value="/{id}/pdf",produces=MediaType.APPLICATION_PDF_VALUE) @PreAuthorize("hasAuthority('workorder:view')") public ResponseEntity<byte[]> pdf(@PathVariable Long id){WorkOrderDetailVO detail=service.detail(id);String name=URLEncoder.encode(detail.workOrder().workOrderNo()+".pdf",StandardCharsets.UTF_8).replace("+","%20");return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename*=UTF-8''"+name).contentType(MediaType.APPLICATION_PDF).body(pdf.export(detail));}
    @GetMapping("/{id}/attachments") @PreAuthorize("hasAuthority('workorder:view')") public ApiResponse<List<AttachmentVO>> attachments(@PathVariable Long id){return ApiResponse.success(files.list(id));}
    @PostMapping(value="/{id}/attachments",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @PreAuthorize("hasAnyAuthority('workorder:process','workorder:accept')") public ApiResponse<Long> uploadAttachment(@PathVariable Long id,@RequestParam String type,@RequestPart("file") MultipartFile file){return ApiResponse.success(files.upload(id,type,file));}
    @GetMapping("/{id}/attachments/{attachmentId}") @PreAuthorize("hasAuthority('workorder:view')") public ResponseEntity<byte[]> downloadAttachment(@PathVariable Long id,@PathVariable Long attachmentId){AttachmentDownload file=files.download(id,attachmentId);String name=URLEncoder.encode(file.fileName(),StandardCharsets.UTF_8).replace("+","%20");MediaType type;try{type=MediaType.parseMediaType(file.contentType());}catch(Exception ignored){type=MediaType.APPLICATION_OCTET_STREAM;}return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename*=UTF-8''"+name).contentType(type).body(file.content());}
    @PostMapping("/{id}/spares") @PreAuthorize("hasAuthority('inventory:issue')") public ApiResponse<Long> issueSpare(@PathVariable Long id,@Valid @RequestBody WorkOrderSpareRequest input){return ApiResponse.success(inventory.issueForWorkOrder(id,input));}
    @PostMapping("/{id}/spares/{issueId}/return") @PreAuthorize("hasAuthority('inventory:return')") public ApiResponse<Void> returnSpare(@PathVariable Long id,@PathVariable Long issueId,@Valid @RequestBody ReturnSpareRequest input){inventory.returnForWorkOrder(id,issueId,input);return ApiResponse.success(null);}
}
