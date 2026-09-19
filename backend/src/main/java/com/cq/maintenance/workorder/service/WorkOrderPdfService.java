package com.cq.maintenance.workorder.service;

import com.cq.maintenance.inventory.vo.WorkOrderSpareVO;
import com.cq.maintenance.workorder.vo.WorkOrderDetailVO;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WorkOrderPdfService {
    private static final DateTimeFormatter TIME=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final String fontPath;
    public WorkOrderPdfService(@Value("${app.pdf.font-path}") String fontPath){this.fontPath=fontPath;}

    public byte[] export(WorkOrderDetailVO detail){
        if(!Files.isRegularFile(Path.of(fontPath)))throw new IllegalStateException("PDF中文字体不存在，请配置 PDF_FONT_PATH");
        try(ByteArrayOutputStream out=new ByteArrayOutputStream()){
            Document document=new Document(PageSize.A4,36,36,40,40);PdfWriter.getInstance(document,out);document.open();
            BaseFont base=BaseFont.createFont(fontPath,BaseFont.IDENTITY_H,BaseFont.EMBEDDED);Font title=new Font(base,18,Font.BOLD);Font heading=new Font(base,12,Font.BOLD);Font text=new Font(base,9);
            Paragraph name=new Paragraph("工业设备运维工单",title);name.setAlignment(Element.ALIGN_CENTER);name.setSpacingAfter(14);document.add(name);
            var wo=detail.workOrder();PdfPTable basics=table();row(basics,text,"工单编号",wo.workOrderNo(),"工单类型",value(wo.workOrderType()));row(basics,text,"设备",wo.equipmentNo()+" / "+wo.equipmentName(),"状态",value(wo.status()));row(basics,text,"优先级",value(wo.priority()),"维修人员",value(wo.assignedEngineerName()));row(basics,text,"创建时间",time(wo.createdAt()),"完成时间",time(wo.completedAt()));document.add(basics);
            section(document,heading,"故障信息");PdfPTable fault=table();var rr=detail.repairRequest();row(fault,text,"报修编号",rr==null?"-":rr.requestNo(),"报修人",rr==null?"-":value(rr.reporterName()));row(fault,text,"故障描述",rr==null?"-":value(rr.faultDescription()),"报修时间",rr==null?"-":time(rr.reportedAt()));document.add(fault);
            section(document,heading,"维修记录");PdfPTable repair=table();var rec=detail.repairRecord();row(repair,text,"排查过程",rec==null?"-":value(rec.inspectionProcess()),"根因",rec==null?"-":value(rec.rootCause()));row(repair,text,"维修措施",rec==null?"-":value(rec.repairAction()),"维修结果",rec==null?"-":value(rec.repairResult()));row(repair,text,"工时",rec==null?"-":value(rec.laborHours()),"停机分钟",rec==null?"-":value(rec.downtimeMinutes()));document.add(repair);
            section(document,heading,"备件使用");PdfPTable spares=new PdfPTable(new float[]{2,3,2,2});spares.setWidthPercentage(100);for(String h:new String[]{"备件编号","备件名称","实际使用","单位"})cell(spares,h,text,true);if(detail.spares().isEmpty()){PdfPCell empty=cell(spares,"无",text,false);empty.setColspan(4);}else for(WorkOrderSpareVO s:detail.spares()){cell(spares,s.spareNo(),text,false);cell(spares,s.spareName(),text,false);cell(spares,value(s.usedQty()),text,false);cell(spares,value(s.unit()),text,false);}document.add(spares);
            section(document,heading,"验收与关键时间");PdfPTable acceptance=table();row(acceptance,text,"提交验收",time(wo.submittedAt()),"验收完成",time(wo.completedAt()));row(acceptance,text,"接单时间",time(wo.acceptedAt()),"开始维修",time(wo.startedAt()));row(acceptance,text,"验收退回次数",value(wo.acceptanceReturnCount()),"维修结论",rec==null?"-":value(rec.repairResult()));document.add(acceptance);
            document.close();return out.toByteArray();
        }catch(Exception ex){throw new IllegalStateException("工单PDF生成失败",ex);}
    }
    private PdfPTable table(){PdfPTable t=new PdfPTable(new float[]{1.2f,2.8f,1.2f,2.8f});t.setWidthPercentage(100);t.setSpacingAfter(8);return t;}
    private void row(PdfPTable t,Font f,String a,String b,String c,String d){cell(t,a,f,true);cell(t,b,f,false);cell(t,c,f,true);cell(t,d,f,false);}
    private PdfPCell cell(PdfPTable t,String v,Font f,boolean header){PdfPCell c=new PdfPCell(new Phrase(value(v),f));c.setPadding(6);c.setVerticalAlignment(Element.ALIGN_MIDDLE);c.setBackgroundColor(header?new Color(235,240,246):Color.WHITE);t.addCell(c);return c;}
    private void section(Document d,Font f,String value)throws DocumentException{Paragraph p=new Paragraph(value,f);p.setSpacingBefore(7);p.setSpacingAfter(5);d.add(p);}
    private String time(java.time.LocalDateTime t){return t==null?"-":TIME.format(t);}private String value(Object v){return v==null||v.toString().isBlank()?"-":v.toString();}
}
