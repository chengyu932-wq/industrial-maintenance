package com.cq.maintenance.equipment.service;

import com.cq.maintenance.equipment.dto.EquipmentCreateRequest;
import com.cq.maintenance.equipment.dto.EquipmentQuery;
import com.cq.maintenance.equipment.entity.EquipmentType;
import com.cq.maintenance.equipment.mapper.EquipmentMapper;
import com.cq.maintenance.equipment.vo.EquipmentListVO;
import com.cq.maintenance.equipment.vo.ImportResultVO;
import com.cq.maintenance.equipment.vo.ImportResultVO.RowError;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EquipmentExcelService {
    private static final String[] HEADERS={"设备编号","设备名称","设备类型编码","型号","厂商","车间编号","产线编号","工位编号","出厂日期","启用日期","保修到期日期","责任人账号","班组编号","规格参数"};
    private final EquipmentService equipmentService; private final EquipmentMapper mapper;
    public EquipmentExcelService(EquipmentService equipmentService,EquipmentMapper mapper){this.equipmentService=equipmentService;this.mapper=mapper;}

    public byte[] template(){return workbook(List.of());}
    public byte[] export(EquipmentQuery query){query.setPage(1);query.setSize(10000);return workbook(equipmentService.page(query).records());}
    private byte[] workbook(List<EquipmentListVO> rows){
        try(XSSFWorkbook wb=new XSSFWorkbook();ByteArrayOutputStream out=new ByteArrayOutputStream()){
            Sheet sheet=wb.createSheet("设备台账");Row h=sheet.createRow(0);for(int i=0;i<HEADERS.length;i++)h.createCell(i).setCellValue(HEADERS[i]);
            int n=1;for(EquipmentListVO e:rows){Row r=sheet.createRow(n++);r.createCell(0).setCellValue(e.equipmentNo());r.createCell(1).setCellValue(e.equipmentName());r.createCell(2).setCellValue(e.typeCode());r.createCell(3).setCellValue(value(e.model()));r.createCell(4).setCellValue(value(e.manufacturer()));r.createCell(5).setCellValue(value(e.workshopNo()));r.createCell(6).setCellValue(value(e.lineNo()));r.createCell(7).setCellValue(value(e.stationNo()));r.createCell(8).setCellValue(e.manufactureDate()==null?"":e.manufactureDate().toString());r.createCell(9).setCellValue(e.commissioningDate()==null?"":e.commissioningDate().toString());r.createCell(10).setCellValue(e.warrantyExpireDate()==null?"":e.warrantyExpireDate().toString());r.createCell(11).setCellValue(value(e.responsibleUsername()));r.createCell(12).setCellValue(value(e.responsibleTeamNo()));r.createCell(13).setCellValue(value(e.specifications()));}
            for(int i=0;i<HEADERS.length;i++)sheet.setColumnWidth(i,Math.min(30,Math.max(12,HEADERS[i].length()+4))*256);wb.write(out);return out.toByteArray();
        }catch(Exception ex){throw new IllegalStateException("Excel生成失败",ex);}
    }
    @Transactional public ImportResultVO importFile(MultipartFile file){
        List<RowError> errors=new ArrayList<>();int total=0,success=0;
        if(file==null||file.isEmpty())return new ImportResultVO(0,0,1,List.of(new RowError(0,"","请选择Excel文件")));
        try(Workbook wb=WorkbookFactory.create(new ByteArrayInputStream(file.getBytes()))){Sheet sheet=wb.getSheetAt(0);DataFormatter f=new DataFormatter();
            for(int i=1;i<=sheet.getLastRowNum();i++){Row row=sheet.getRow(i);if(row==null||blank(row,f))continue;total++;String no=text(row,0,f);
                try{String name=required(text(row,1,f),"设备名称");String typeCode=required(text(row,2,f),"设备类型编码");EquipmentType type=mapper.findTypeByCode(typeCode);if(type==null)throw new IllegalArgumentException("设备类型不存在或已停用");
                    Long station=mapper.findStationIdByCodes(required(text(row,5,f),"车间编号"),required(text(row,6,f),"产线编号"),required(text(row,7,f),"工位编号"));if(station==null)throw new IllegalArgumentException("车间/产线/工位关系不存在或已停用");
                    Long user=optionalId(text(row,11,f),mapper::findUserIdByUsername,"责任人不存在或已禁用");Long team=optionalId(text(row,12,f),mapper::findTeamIdByCode,"班组不存在或已停用");
                    equipmentService.create(new EquipmentCreateRequest(required(no,"设备编号"),name,type.getId(),text(row,3,f),text(row,4,f),text(row,13,f),date(text(row,8,f)),date(text(row,9,f)),user,team,station,date(text(row,10,f))));success++;
                }catch(Exception ex){errors.add(new RowError(i+1,no,rootMessage(ex)));}}
        }catch(Exception ex){return new ImportResultVO(total,success,errors.size()+1,List.of(new RowError(0,"","文件格式错误："+rootMessage(ex))));}
        return new ImportResultVO(total,success,errors.size(),List.copyOf(errors));
    }
    private static String text(Row r,int i,DataFormatter f){Cell c=r.getCell(i);return c==null?"":f.formatCellValue(c).trim();}
    private static boolean blank(Row r,DataFormatter f){for(int i=0;i<HEADERS.length;i++)if(!text(r,i,f).isBlank())return false;return true;}
    private static String required(String v,String n){if(v.isBlank())throw new IllegalArgumentException(n+"不能为空");return v;}
    private static LocalDate date(String v){if(v.isBlank())return null;try{return LocalDate.parse(v);}catch(DateTimeParseException e){throw new IllegalArgumentException("日期格式应为yyyy-MM-dd");}}
    private static Long optionalId(String v,java.util.function.Function<String,Long> finder,String message){if(v.isBlank())return null;Long id=finder.apply(v);if(id==null)throw new IllegalArgumentException(message);return id;}
    private static String rootMessage(Throwable e){Throwable x=e;while(x.getCause()!=null)x=x.getCause();return x.getMessage()==null?e.getClass().getSimpleName():x.getMessage();}
    private static String value(String v){return v==null?"":v;}
}
