package com.cq.maintenance.common;
import static org.junit.jupiter.api.Assertions.*;
import com.cq.maintenance.common.exception.BusinessException;
import com.cq.maintenance.common.storage.AttachmentStorageService;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
class AttachmentStorageServiceTest {
 @TempDir Path root;
 @Test void storesAndReadsAllowedFileInsideRoot()throws Exception{var service=new AttachmentStorageService(root.toString());var file=new MockMultipartFile("file","manual.pdf","application/pdf",new byte[]{1,2,3});var stored=service.store("equipment/1",file);assertEquals("manual.pdf",stored.fileName());assertArrayEquals(file.getBytes(),service.read(stored.relativePath()));assertTrue(root.resolve(stored.relativePath()).normalize().startsWith(root));}
 @Test void rejectsUnsupportedExtensionAndTraversalRead(){var service=new AttachmentStorageService(root.toString());assertThrows(BusinessException.class,()->service.store("equipment/1",new MockMultipartFile("file","payload.exe","application/octet-stream",new byte[]{1})));assertThrows(BusinessException.class,()->service.read("../secret.txt"));}
}
