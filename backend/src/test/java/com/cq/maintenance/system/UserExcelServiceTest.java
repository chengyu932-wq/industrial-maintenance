package com.cq.maintenance.system;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.cq.maintenance.equipment.vo.ImportResultVO;
import com.cq.maintenance.system.dto.UserCreateRequest;
import com.cq.maintenance.system.service.UserExcelService;
import com.cq.maintenance.system.service.UserManagementService;
import com.cq.maintenance.system.vo.UserMetadataVO;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class UserExcelServiceTest {
    private UserManagementService users;
    private UserExcelService service;

    @BeforeEach
    void setUp() {
        users = mock(UserManagementService.class);
        when(users.metadata()).thenReturn(new UserMetadataVO(
                List.of(new UserMetadataVO.Option(1L, "ENGINEER", "工程师")),
                List.of(new UserMetadataVO.Option(2L, "MECHANICAL", "机械")),
                List.of(), List.of()));
        service = new UserExcelService(users);
    }

    @Test
    void templateIsAValidXlsxWorkbook() throws Exception {
        byte[] bytes = service.template();
        assertTrue(bytes.length > 1000);
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            assertEquals("账号*", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
        }
    }

    @Test
    void importsTemplateSampleWithMappedRoleAndSkill() {
        byte[] bytes = service.template();
        var file = new MockMultipartFile("file", "users.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);
        ImportResultVO result = service.importFile(file);
        assertEquals(1, result.totalRows());
        assertEquals(1, result.successRows());
        assertEquals(0, result.failedRows());
        verify(users).create(argThat((UserCreateRequest request) ->
                request.roleIds().equals(List.of(1L)) && request.skillIds().equals(List.of(2L))));
    }

    @Test
    void reportsUnknownRoleWithoutCreatingUser() {
        byte[] bytes = service.template();
        when(users.metadata()).thenReturn(new UserMetadataVO(List.of(), List.of(), List.of(), List.of()));
        var file = new MockMultipartFile("file", "users.xlsx", "application/octet-stream", bytes);
        ImportResultVO result = new UserExcelService(users).importFile(file);
        assertEquals(1, result.failedRows());
        assertTrue(result.errors().get(0).message().contains("角色编码不存在"));
        verify(users, never()).create(any());
    }
}
