package com.cq.maintenance.knowledge.controller;

import com.cq.maintenance.common.response.ApiResponse;
import com.cq.maintenance.knowledge.dto.SimilarWorkOrderQuery;
import com.cq.maintenance.knowledge.service.SimilarWorkOrderService;
import com.cq.maintenance.knowledge.vo.SimilarWorkOrderVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@ConditionalOnExpression("'${spring.autoconfigure.exclude:}'.indexOf('DataSourceAutoConfiguration') < 0")
@RequestMapping("/api/work-orders")
public class SimilarWorkOrderController {
    private final SimilarWorkOrderService service;public SimilarWorkOrderController(SimilarWorkOrderService service){this.service=service;}
    @GetMapping("/similar") @PreAuthorize("hasAuthority('workorder:similar')") public ApiResponse<List<SimilarWorkOrderVO>> similar(@Valid SimilarWorkOrderQuery query){return ApiResponse.success(service.byDescription(query.description()));}
    @GetMapping("/{id}/similar") @PreAuthorize("hasAuthority('workorder:similar')") public ApiResponse<List<SimilarWorkOrderVO>> similar(@PathVariable Long id){return ApiResponse.success(service.byWorkOrder(id));}
}
