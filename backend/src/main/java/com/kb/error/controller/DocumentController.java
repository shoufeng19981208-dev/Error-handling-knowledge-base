package com.kb.error.controller;

import com.kb.error.entity.DocumentFile;
import com.kb.error.repository.DocumentFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 文档管理接口：任意格式上传、列表、删除
 *
 * <p>文件本体存放在 {@code uploads/documents/} 下，由 {@code /uploads/**} 静态映射对外提供预览/下载；
 * 元数据（原始文件名、大小、上传时间等）持久化到 MySQL 的 document_file 表。</p>
 *
 * @author kb
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    /** 文档子目录（与截图等其他上传文件隔离） */
    private static final String DOCUMENTS_SUBDIR = "documents";

    @Autowired
    private DocumentFileRepository documentFileRepository;

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    /**
     * 文档列表，按上传时间倒序
     */
    @GetMapping
    public List<Map<String, Object>> list() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (DocumentFile doc : documentFileRepository.findAllByOrderByUploadTimeDesc()) {
            result.add(toVo(doc));
        }
        return result;
    }

    /**
     * 上传文档（格式不限）
     */
    @PostMapping
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        if (file == null || file.isEmpty()) {
            result.put("success", false);
            result.put("message", "文件为空");
            return result;
        }
        try {
            Path documentDir = getDocumentDir();
            if (!Files.exists(documentDir)) {
                Files.createDirectories(documentDir);
            }

            String originalName = sanitizeFileName(file.getOriginalFilename());
            if (originalName.isEmpty()) {
                originalName = "未命名文件";
            }
            String ext = getExtension(originalName);
            String storedName = UUID.randomUUID().toString() + ext;
            Path target = documentDir.resolve(storedName);
            file.transferTo(target.toFile());

            DocumentFile doc = DocumentFile.builder()
                    .originalName(originalName)
                    .storedName(storedName)
                    .contentType(resolveContentType(file, target, originalName))
                    .size(file.getSize())
                    .uploadTime(LocalDateTime.now())
                    .build();
            doc = documentFileRepository.save(doc);

            result.put("success", true);
            result.put("message", "上传成功");
            result.put("document", toVo(doc));
        } catch (IOException e) {
            log.error("上传文档失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "上传失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 删除文档（同时删除元数据与磁盘文件）
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        DocumentFile doc = documentFileRepository.findById(id).orElse(null);
        if (doc == null) {
            result.put("success", false);
            result.put("message", "文档不存在");
            return result;
        }
        try {
            Path file = getDocumentDir().resolve(doc.getStoredName());
            Files.deleteIfExists(file);
        } catch (IOException e) {
            // 磁盘文件删除失败不阻塞记录删除，仅告警
            log.warn("删除文档磁盘文件失败: {} ({})", doc.getStoredName(), e.getMessage());
        }
        documentFileRepository.delete(doc);
        result.put("success", true);
        result.put("message", "删除成功");
        return result;
    }

    private Map<String, Object> toVo(DocumentFile doc) {
        Map<String, Object> vo = new HashMap<>();
        vo.put("id", doc.getId());
        vo.put("originalName", doc.getOriginalName());
        vo.put("storedName", doc.getStoredName());
        vo.put("contentType", doc.getContentType());
        vo.put("size", doc.getSize());
        vo.put("uploadTime", doc.getUploadTime());
        vo.put("url", "/uploads/" + DOCUMENTS_SUBDIR + "/" + doc.getStoredName());
        return vo;
    }

    private Path getDocumentDir() {
        return Paths.get(uploadPath).toAbsolutePath().normalize().resolve(DOCUMENTS_SUBDIR);
    }

    /** 去掉路径分隔等危险字符，仅保留文件名 */
    private String sanitizeFileName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "";
        }
        String clean = Paths.get(name).getFileName().toString();
        return clean.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_");
    }

    private String getExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx < 0) {
            return "";
        }
        String ext = fileName.substring(idx);
        return ext.length() > 20 ? "" : ext;
    }

    private String resolveContentType(MultipartFile file, Path target, String originalName) {
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isEmpty() && !"application/octet-stream".equalsIgnoreCase(contentType)) {
            return contentType;
        }
        try {
            String probed = Files.probeContentType(target);
            if (probed != null && !probed.isEmpty()) {
                return probed;
            }
        } catch (IOException ignored) {
            // fall through
        }
        String lower = originalName.toLowerCase();
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (lower.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        if (lower.endsWith(".doc")) {
            return "application/msword";
        }
        if (lower.endsWith(".xlsx")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        }
        if (lower.endsWith(".xls")) {
            return "application/vnd.ms-excel";
        }
        if (lower.endsWith(".pptx")) {
            return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        }
        if (lower.endsWith(".ppt")) {
            return "application/vnd.ms-powerpoint";
        }
        if (lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".log")
                || lower.endsWith(".json") || lower.endsWith(".xml") || lower.endsWith(".yml")
                || lower.endsWith(".yaml") || lower.endsWith(".csv")) {
            return "text/plain; charset=UTF-8";
        }
        return "application/octet-stream";
    }
}
