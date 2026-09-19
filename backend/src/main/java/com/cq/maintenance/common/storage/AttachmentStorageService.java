package com.cq.maintenance.common.storage;

import com.cq.maintenance.common.exception.BusinessException;
import com.cq.maintenance.common.exception.ErrorCode;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AttachmentStorageService {
    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final Set<String> EXTENSIONS = Set.of("png","jpg","jpeg","pdf","doc","docx","xls","xlsx","txt");
    private final Path root;

    public AttachmentStorageService(@Value("${app.storage.root:./data/uploads}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    public StoredFile store(String namespace, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException(ErrorCode.PARAMETER_ERROR, "请选择附件");
        if (file.getSize() > MAX_BYTES) throw new BusinessException(ErrorCode.PARAMETER_ERROR, "附件不能超过10MB");
        String original = sanitize(file.getOriginalFilename());
        String extension = extension(original);
        if (!EXTENSIONS.contains(extension)) throw new BusinessException(ErrorCode.PARAMETER_ERROR, "仅支持图片、PDF、Office文档和文本附件");
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        String relative = namespace + "/" + storedName;
        Path target = resolve(relative);
        try {
            Files.createDirectories(target.getParent());
            try (var input = file.getInputStream()) { Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING); }
            return new StoredFile(original, relative.replace('\\','/'), normalizeType(file.getContentType(), extension));
        } catch (IOException exception) {
            throw new IllegalStateException("附件保存失败", exception);
        }
    }

    public byte[] read(String relativePath) {
        try { return Files.readAllBytes(resolve(relativePath)); }
        catch (NoSuchFileException exception) { throw new BusinessException(ErrorCode.NOT_FOUND, "附件文件不存在"); }
        catch (IOException exception) { throw new IllegalStateException("附件读取失败", exception); }
    }

    public void deleteQuietly(String relativePath) {
        try { Files.deleteIfExists(resolve(relativePath)); } catch (IOException ignored) { }
    }

    private Path resolve(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) throw new BusinessException(ErrorCode.NOT_FOUND, "附件路径不存在");
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root)) throw new BusinessException(ErrorCode.DATA_FORBIDDEN, "非法附件路径");
        return target;
    }
    private String sanitize(String value) {
        String name = value == null ? "attachment" : Path.of(value).getFileName().toString().trim();
        name = name.replaceAll("[\\r\\n\\t]", "_");
        if (name.isBlank() || name.length() > 255) throw new BusinessException(ErrorCode.PARAMETER_ERROR, "附件文件名不合法");
        return name;
    }
    private String extension(String name) {
        int index = name.lastIndexOf('.');
        return index < 0 ? "" : name.substring(index + 1).toLowerCase(Locale.ROOT);
    }
    private String normalizeType(String type, String extension) {
        if (type != null && !type.isBlank() && type.length() <= 100) return type;
        return switch (extension) { case "png" -> "image/png"; case "jpg","jpeg" -> "image/jpeg"; case "pdf" -> "application/pdf"; default -> "application/octet-stream"; };
    }
    public record StoredFile(String fileName, String relativePath, String contentType) {}
}
