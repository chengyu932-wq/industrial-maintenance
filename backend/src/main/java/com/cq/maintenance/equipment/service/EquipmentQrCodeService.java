package com.cq.maintenance.equipment.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.ByteArrayOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EquipmentQrCodeService {
    private final EquipmentService equipmentService; private final String publicBaseUrl;
    public EquipmentQrCodeService(EquipmentService equipmentService,@Value("${app.public-base-url}") String publicBaseUrl){this.equipmentService=equipmentService;this.publicBaseUrl=publicBaseUrl.replaceAll("/+$","");}
    public byte[] png(Long id){
        var e=equipmentService.detail(id);String content=publicBaseUrl+"/repair-requests?equipmentId="+id+"&source=QR&qr="+e.qrCode();
        try{BitMatrix matrix=new QRCodeWriter().encode(content,BarcodeFormat.QR_CODE,320,320);ByteArrayOutputStream out=new ByteArrayOutputStream();MatrixToImageWriter.writeToStream(matrix,"PNG",out);return out.toByteArray();}
        catch(Exception ex){throw new IllegalStateException("二维码生成失败",ex);}
    }
}
