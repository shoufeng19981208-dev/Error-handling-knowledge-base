package com.kb.error.controller;

import com.kb.error.dto.MatchResult;
import com.kb.error.entity.CategoryConfig;
import com.kb.error.entity.ErrorRecord;
import com.kb.error.repository.CategoryConfigRepository;
import com.kb.error.service.ErrorRecordService;
import com.kb.error.service.ExcelImportService;
import com.kb.error.service.SignatureExtractor;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/error-record")
public class ErrorRecordController {

    @Autowired
    private ErrorRecordService errorRecordService;

    @Autowired
    private CategoryConfigRepository categoryConfigRepository;

    @Autowired
    private ExcelImportService excelImportService;

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

    /**
     * 智能日志匹配：粘贴整段报错日志，反向匹配知识库中的解决方案
     */
    @PostMapping("/match")
    public MatchResult match(@RequestBody Map<String, String> body) {
        return errorRecordService.matchByLog(body.get("logText"));
    }

    /**
     * 从报错文本中提取特征关键字（录入页"自动提取"用）
     */
    @PostMapping("/extract-keywords")
    public Map<String, Object> extractKeywords(@RequestBody Map<String, String> body) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("keywords", SignatureExtractor.extract(body.get("text")));
        return result;
    }

    /**
     * xlsx 批量导入（收集模板回收入库）
     */
    @PostMapping("/import")
    public Map<String, Object> importExcel(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (file.isEmpty()) {
            result.put("success", false);
            result.put("message", "文件为空");
            return result;
        }
        String originalName = file.getOriginalFilename();
        if (originalName != null && !originalName.toLowerCase().endsWith(".xlsx")) {
            result.put("success", false);
            result.put("message", "仅支持 .xlsx 文件，请使用收集模板");
            return result;
        }
        try (java.io.InputStream in = file.getInputStream()) {
            return excelImportService.importExcel(in);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "导入失败: " + e.getMessage());
            return result;
        }
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
        // 合并：配置中启用的分类 + 报错记录中实际存在但未配置的分类（保证历史数据可选）
        Set<String> names = new LinkedHashSet<>();
        for (CategoryConfig config : categoryConfigRepository.findByEnabledTrueOrderBySortOrderAscIdAsc()) {
            names.add(config.getName());
        }
        names.addAll(errorRecordService.getAllCategories());
        return new java.util.ArrayList<>(names);
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
            // 必须转绝对路径：transferTo 底层的 Part.write 会把相对路径解析到 Tomcat 临时工作目录，
            // 与 createDirectories 基于 JVM 工作目录创建的位置不一致，导致 FileNotFoundException
            Path uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
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
