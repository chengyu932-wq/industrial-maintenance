package com.cq.maintenance.notification.controller;
import com.cq.maintenance.common.response.*;
import com.cq.maintenance.notification.service.NotificationService;
import com.cq.maintenance.notification.vo.NotificationVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;public NotificationController(NotificationService service){this.service=service;}
    @GetMapping @PreAuthorize("hasAuthority('notification:list')") public ApiResponse<PageResult<NotificationVO>> mine(@RequestParam(defaultValue="1")long page,@RequestParam(defaultValue="20")long size){return ApiResponse.success(service.mine(page,size));}
    @GetMapping("/unread-count") @PreAuthorize("hasAuthority('notification:list')") public ApiResponse<Long> unread(){return ApiResponse.success(service.unreadCount());}
    @PostMapping("/{id}/read") @PreAuthorize("hasAuthority('notification:read')") public ApiResponse<Void> read(@PathVariable Long id){service.read(id);return ApiResponse.success(null);}
    @PostMapping("/read-all") @PreAuthorize("hasAuthority('notification:read')") public ApiResponse<Void> readAll(){service.readAll();return ApiResponse.success(null);}
}
