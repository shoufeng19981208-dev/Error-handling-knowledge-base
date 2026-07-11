package com.kb.error.controller;

import com.kb.error.entity.ErrorRecord;
import com.kb.error.service.ErrorRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/error-record")
public class ErrorRecordController {

    @Autowired
    private ErrorRecordService errorRecordService;

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    @GetMapping("/search")
    public Map<String, Object> search(@RequestParam(required = false) String keyword,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        Page<ErrorRecord> pageResult = errorRecordService.search(keyword, page, size);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", pageResult.getContent());
        result.put("totalElements", pageResult.getTotalElements());
        result.put("totalPages", pageResult.getTotalPages());
        result.put("currentPage", pageResult.getNumber());
        return result;
    }

    @GetMapping("/{id}")
    public ErrorRecord getById(@PathVariable Long id) {
        return errorRecordService.getById(id);
    }

    @PostMapping
    public ErrorRecord create(@RequestBody ErrorRecord record) {
        return errorRecordService.create(record);
    }

    @PutMapping("/{id}")
    public ErrorRecord update(@PathVariable Long id, @RequestBody ErrorRecord record) {
        return errorRecordService.update(id, record);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        errorRecordService.delete(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", "\u5220\u9664\u6210\u529f");
        return result;
    }

    @GetMapping("/pending")
    public List<ErrorRecord> getPendingList() {
        return errorRecordService.getPendingList();
    }

    @GetMapping("/categories")
    public List<String> getCategories() {
        return errorRecordService.getAllCategories();
    }

    @PostMapping("/upload-screenshot")
    public Map<String, Object> uploadScreenshot(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (file.isEmpty()) {
            result.put("success", false);
            result.put("message", "\u6587\u4ef6\u4e3a\u7a7a");
            return result;
        }
        try {
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            String originalName = file.getOriginalFilename();
            String suffix = "";
            if (originalName != null && originalName.contains(".")) {
                suffix = originalName.substring(originalName.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString() + suffix;
            Path targetPath = uploadDir.resolve(fileName);
            file.transferTo(targetPath.toFile());
            result.put("success", true);
            result.put("url", "/uploads/" + fileName);
            result.put("fileName", fileName);
        } catch (IOException e) {
            result.put("success", false);
            result.put("message", "\u4e0a\u4f20\u5931\u8d25: " + e.getMessage());
        }
        return result;
    }
}
