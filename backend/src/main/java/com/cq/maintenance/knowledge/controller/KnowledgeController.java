package com.cq.maintenance.knowledge.controller;

import com.cq.maintenance.common.response.ApiResponse;
import com.cq.maintenance.common.response.PageResult;
import com.cq.maintenance.knowledge.dto.KnowledgeArticleRequest;
import com.cq.maintenance.knowledge.dto.KnowledgeQuery;
import com.cq.maintenance.knowledge.dto.KnowledgeReviewRequest;
import com.cq.maintenance.knowledge.service.KnowledgeService;
import com.cq.maintenance.knowledge.vo.KnowledgeArticleVO;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@ConditionalOnExpression("'${spring.autoconfigure.exclude:}'.indexOf('DataSourceAutoConfiguration') < 0")
@RequestMapping("/api/knowledge")
public class KnowledgeController {
    private final KnowledgeService service;public KnowledgeController(KnowledgeService service){this.service=service;}
    @GetMapping @PreAuthorize("hasAuthority('knowledge:list')") public ApiResponse<PageResult<KnowledgeArticleVO>> page(@Valid KnowledgeQuery query){return ApiResponse.success(service.page(query));}
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('knowledge:view')") public ApiResponse<KnowledgeArticleVO> detail(@PathVariable Long id){return ApiResponse.success(service.detail(id));}
    @PostMapping("/from-work-order/{workOrderId}") @PreAuthorize("hasAuthority('knowledge:create')") public ApiResponse<Long> create(@PathVariable Long workOrderId){return ApiResponse.success(service.createFromWorkOrder(workOrderId));}
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('knowledge:update')") public ApiResponse<Void> update(@PathVariable Long id,@Valid @RequestBody KnowledgeArticleRequest input){service.update(id,input);return ApiResponse.success(null);}
    @PostMapping("/{id}/submit") @PreAuthorize("hasAuthority('knowledge:submit')") public ApiResponse<Void> submit(@PathVariable Long id){service.submit(id);return ApiResponse.success(null);}
    @PostMapping("/{id}/review") @PreAuthorize("hasAuthority('knowledge:audit')") public ApiResponse<Void> review(@PathVariable Long id,@Valid @RequestBody KnowledgeReviewRequest input){service.review(id,input);return ApiResponse.success(null);}
}
