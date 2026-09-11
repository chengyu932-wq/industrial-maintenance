package com.cq.maintenance.equipment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.cq.maintenance.common.response.PageResult;
import com.cq.maintenance.equipment.dto.EquipmentQuery;
import com.cq.maintenance.equipment.entity.EquipmentType;
import com.cq.maintenance.equipment.mapper.EquipmentMapper;
import com.cq.maintenance.equipment.service.EquipmentExcelService;
import com.cq.maintenance.equipment.service.EquipmentQrCodeService;
import com.cq.maintenance.equipment.service.EquipmentService;
import com.cq.maintenance.equipment.vo.EquipmentDetailVO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class EquipmentArtifactsServiceTest {
    @Test void qrCodeShouldBeValidPng() {
        EquipmentService equipment = mock(EquipmentService.class);
        EquipmentDetailVO detail = mock(EquipmentDetailVO.class);
        when(detail.qrCode()).thenReturn("public-random-code");
        when(equipment.detail(8L)).thenReturn(detail);
        byte[] png = new EquipmentQrCodeService(equipment,"http://localhost:5173/").png(8L);
        assertArrayEquals(new byte[]{(byte)0x89,0x50,0x4e,0x47,0x0d,0x0a,0x1a,0x0a},java.util.Arrays.copyOf(png,8));
        assertTrue(png.length>100);
    }

    @Test void templateShouldContainStableHeaders() throws Exception {
        EquipmentExcelService excel = new EquipmentExcelService(mock(EquipmentService.class),mock(EquipmentMapper.class));
        try(var workbook=new XSSFWorkbook(new ByteArrayInputStream(excel.template()))) {
            var header=workbook.getSheetAt(0).getRow(0);
            assertEquals("设备编号",header.getCell(0).getStringCellValue());
            assertEquals("规格参数",header.getCell(13).getStringCellValue());
        }
    }

    @Test void exportShouldUseBackendQueryAndProduceWorkbook() throws Exception {
        EquipmentService equipment=mock(EquipmentService.class);EquipmentQuery query=new EquipmentQuery();
        when(equipment.page(query)).thenReturn(new PageResult<>(List.of(),0,1,10000));
        byte[] bytes=new EquipmentExcelService(equipment,mock(EquipmentMapper.class)).export(query);
        try(var workbook=new XSSFWorkbook(new ByteArrayInputStream(bytes))){assertEquals("设备台账",workbook.getSheetAt(0).getSheetName());}
        verify(equipment).page(query);assertEquals(10000,query.getSize());
    }

    @Test void malformedImportShouldReturnRowLevelErrorInsteadOfThrowing() {
        EquipmentExcelService excel=new EquipmentExcelService(mock(EquipmentService.class),mock(EquipmentMapper.class));
        var file=new MockMultipartFile("file","bad.xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",new byte[]{1,2,3});
        var result=excel.importFile(file);
        assertEquals(0,result.successRows());assertEquals(1,result.failedRows());assertTrue(result.errors().get(0).message().startsWith("文件格式错误"));
    }

    @Test void validImportRowShouldResolveReferencesAndCreateEquipment() throws Exception {
        EquipmentService equipment=mock(EquipmentService.class);EquipmentMapper mapper=mock(EquipmentMapper.class);
        EquipmentType type=new EquipmentType();type.setId(2L);when(mapper.findTypeByCode("CNC")).thenReturn(type);
        when(mapper.findStationIdByCodes("WS-01","LINE-A","ST-01")).thenReturn(3L);when(equipment.create(any())).thenReturn(9L);
        byte[] bytes;
        try(var workbook=new XSSFWorkbook(new ByteArrayInputStream(new EquipmentExcelService(equipment,mapper).template()));var out=new ByteArrayOutputStream()){
            var row=workbook.getSheetAt(0).createRow(1);row.createCell(0).setCellValue("IMPORT-001");row.createCell(1).setCellValue("导入设备");row.createCell(2).setCellValue("CNC");row.createCell(5).setCellValue("WS-01");row.createCell(6).setCellValue("LINE-A");row.createCell(7).setCellValue("ST-01");row.createCell(8).setCellValue("2026-09-01");workbook.write(out);bytes=out.toByteArray();
        }
        var file=new MockMultipartFile("file","valid.xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",bytes);
        var result=new EquipmentExcelService(equipment,mapper).importFile(file);
        assertEquals(1,result.totalRows());assertEquals(1,result.successRows());assertEquals(0,result.failedRows());verify(equipment).create(any());
    }
}
