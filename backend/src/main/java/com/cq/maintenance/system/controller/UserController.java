package com.cq.maintenance.system.controller;

import com.cq.maintenance.common.response.ApiResponse;
import com.cq.maintenance.security.AuthMapper;
import com.cq.maintenance.system.dto.*;
import com.cq.maintenance.system.service.UserManagementService;
import com.cq.maintenance.system.vo.UserDetailVO;
import com.cq.maintenance.system.vo.UserMetadataVO;
import com.cq.maintenance.system.vo.UserSummaryVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.http.*;
import org.springframework.web.multipart.MultipartFile;
import com.cq.maintenance.equipment.vo.ImportResultVO;
import com.cq.maintenance.system.service.UserExcelService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final AuthMapper authMapper;
    private final UserManagementService service;
    private final UserExcelService excel;

    public UserController(AuthMapper authMapper,UserManagementService service,UserExcelService excel) { this.authMapper = authMapper;this.service=service;this.excel=excel; }

    @GetMapping
    @PreAuthorize("hasAuthority('system:user:list')")
    public ApiResponse<List<UserSummaryVO>> list() {
        return ApiResponse.success(authMapper.findUsers());
    }

    @GetMapping("/options")
    @PreAuthorize("hasAnyAuthority('equipment:add','equipment:update')")
    public ApiResponse<List<UserSummaryVO>> options() {
        return ApiResponse.success(authMapper.findUsers().stream().filter(u -> "ENABLED".equals(u.status())).toList());
    }
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('system:user:list')") public ApiResponse<UserDetailVO> detail(@PathVariable Long id){return ApiResponse.success(service.detail(id));}
    @GetMapping("/metadata") @PreAuthorize("hasAuthority('system:user:list')") public ApiResponse<UserMetadataVO> metadata(){return ApiResponse.success(service.metadata());}
    @PostMapping @PreAuthorize("hasAuthority('system:user:add')") public ApiResponse<Long> create(@Valid @RequestBody UserCreateRequest input){return ApiResponse.success(service.create(input));}
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('system:user:update')") public ApiResponse<Void> update(@PathVariable Long id,@Valid @RequestBody UserUpdateRequest input){service.update(id,input);return ApiResponse.success(null);}
    @PutMapping("/{id}/status") @PreAuthorize("hasAuthority('system:user:update')") public ApiResponse<Void> status(@PathVariable Long id,@Valid @RequestBody UserStatusRequest input){service.status(id,input);return ApiResponse.success(null);}
    @PutMapping("/{id}/password") @PreAuthorize("hasAuthority('system:user:update')") public ApiResponse<Void> password(@PathVariable Long id,@Valid @RequestBody PasswordResetRequest input){service.resetPassword(id,input);return ApiResponse.success(null);}
    @GetMapping("/import-template") @PreAuthorize("hasAuthority('system:user:import')") public ResponseEntity<byte[]> importTemplate(){String name=URLEncoder.encode("用户导入模板.xlsx",StandardCharsets.UTF_8).replace("+","%20");return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename*=UTF-8''"+name).contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).body(excel.template());}
    @PostMapping(value="/import",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @PreAuthorize("hasAuthority('system:user:import')") public ApiResponse<ImportResultVO> importUsers(@RequestPart("file") MultipartFile file){return ApiResponse.success(excel.importFile(file));}
}
