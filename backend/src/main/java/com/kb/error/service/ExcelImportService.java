package com.kb.error.service;

import com.kb.error.entity.ErrorRecord;
import com.kb.error.repository.ErrorRecordRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * xlsx 批量导入服务
 *
 * <p>解析「报错处理知识库-收集模板.xlsx」的「报错记录」工作表，按表头文字识别列，
 * 逐行复用 {@link ErrorRecordService#create}（自动继承"无处理步骤 → 待更新"机制）。
 * 标题以【示例】开头的行、与库中或文件内重复标题的行自动跳过。
 *
 * @author kb
 */
@Service
public class ExcelImportService {

    /** 数据工作表名（找不到时回退第一个工作表） */
    private static final String SHEET_NAME = "报错记录";

    /** 示例行标题前缀（自动跳过） */
    private static final String EXAMPLE_PREFIX = "【示例】";

    /** 单次导入最大数据行数 */
    private static final int MAX_ROWS = 1000;

    @Autowired
    private ErrorRecordService errorRecordService;

    @Autowired
    private ErrorRecordRepository errorRecordRepository;

    /**
     * 解析并导入 xlsx
     *
     * @param in xlsx 输入流
     * @return 结果：success、total、imported、skipped、failed、errors（逐行原因）
     */
    public Map<String, Object> importExcel(InputStream in) throws IOException {
        Map<String, Object> result = new LinkedHashMap<>();
        try (Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }
            Map<String, Integer> columnIndex = resolveHeader(sheet.getRow(sheet.getFirstRowNum()));
            if (!columnIndex.containsKey("title") || !columnIndex.containsKey("category")) {
                result.put("success", false);
                result.put("message", "表头不符合模板：未找到「报错标题」或「所属分类」列，请使用收集模板填写");
                return result;
            }

            DataFormatter formatter = new DataFormatter();
            List<Map<String, Object>> errors = new ArrayList<>();
            Set<String> seenTitles = new HashSet<>();
            int imported = 0;
            int skipped = 0;
            int failed = 0;
            int total = 0;

            int headerRow = sheet.getFirstRowNum();
            int lastRow = Math.min(sheet.getLastRowNum(), headerRow + MAX_ROWS);
            for (int rowNum = headerRow + 1; rowNum <= lastRow; rowNum++) {
                Row row = sheet.getRow(rowNum);
                String title = cellText(row, columnIndex.get("title"), formatter);
                String category = cellText(row, columnIndex.get("category"), formatter);
                String content = cellText(row, columnIndex.get("content"), formatter);
                String steps = cellText(row, columnIndex.get("steps"), formatter);
                String keywords = cellText(row, columnIndex.get("keywords"), formatter);
                String registrar = cellText(row, columnIndex.get("registrar"), formatter);

                // 整行为空：不计数直接跳过
                if (!StringUtils.hasText(title) && !StringUtils.hasText(category)
                        && !StringUtils.hasText(content) && !StringUtils.hasText(steps)
                        && !StringUtils.hasText(keywords)) {
                    continue;
                }
                total++;
                int displayRow = rowNum + 1;

                if (StringUtils.hasText(title) && title.startsWith(EXAMPLE_PREFIX)) {
                    skipped++;
                    errors.add(rowError(displayRow, title, "示例行，自动忽略"));
                    continue;
                }
                if (!StringUtils.hasText(title)) {
                    failed++;
                    errors.add(rowError(displayRow, title, "缺少必填项（报错标题）"));
                    continue;
                }
                String titleKey = title.trim().toLowerCase();
                if (!seenTitles.add(titleKey)) {
                    skipped++;
                    errors.add(rowError(displayRow, title, "文件内标题重复"));
                    continue;
                }
                if (errorRecordRepository.existsByErrorTitle(title.trim())) {
                    skipped++;
                    errors.add(rowError(displayRow, title, "库中已存在同名记录"));
                    continue;
                }

                try {
                    // 兼容模板中未填写所属分类的行：自动归入「其他」，避免整行导入失败
                    String finalCategory = StringUtils.hasText(category) ? category.trim() : "其他";
                    ErrorRecord record = ErrorRecord.builder()
                            .errorTitle(title.trim())
                            .category(finalCategory)
                            .errorContent(StringUtils.hasText(content) ? content : null)
                            .solutionSteps(StringUtils.hasText(steps) ? steps : null)
                            .keywords(StringUtils.hasText(keywords) ? keywords.trim() : null)
                            .registrar(StringUtils.hasText(registrar) ? registrar.trim() : null)
                            .build();
                    errorRecordService.create(record);
                    imported++;
                    if (!StringUtils.hasText(category)) {
                        errors.add(rowError(displayRow, title, "未填写所属分类，已自动归入「其他」"));
                    }
                } catch (Exception e) {
                    failed++;
                    errors.add(rowError(displayRow, title, "保存失败: " + e.getMessage()));
                }
            }

            result.put("success", true);
            result.put("total", total);
            result.put("imported", imported);
            result.put("skipped", skipped);
            result.put("failed", failed);
            result.put("errors", errors);
            return result;
        }
    }

    /**
     * 按表头文字识别列位置（容忍列顺序调整；表头文字不可改）
     */
    private Map<String, Integer> resolveHeader(Row headerRow) {
        Map<String, Integer> columnIndex = new HashMap<>();
        if (headerRow == null) {
            return columnIndex;
        }
        DataFormatter formatter = new DataFormatter();
        for (Cell cell : headerRow) {
            String head = formatter.formatCellValue(cell).trim();
            if (!StringUtils.hasText(head)) {
                continue;
            }
            int col = cell.getColumnIndex();
            if (head.contains("标题")) {
                columnIndex.put("title", col);
            } else if (head.contains("分类")) {
                columnIndex.put("category", col);
            } else if (head.contains("内容") || head.contains("日志")) {
                columnIndex.put("content", col);
            } else if (head.contains("步骤") || head.contains("方案")) {
                columnIndex.put("steps", col);
            } else if (head.contains("关键字") || head.contains("关键词")) {
                columnIndex.put("keywords", col);
            } else if (head.contains("登记人") || head.contains("姓名")) {
                columnIndex.put("registrar", col);
            }
        }
        return columnIndex;
    }

    private String cellText(Row row, Integer col, DataFormatter formatter) {
        if (row == null || col == null) {
            return "";
        }
        Cell cell = row.getCell(col);
        if (cell == null) {
            return "";
        }
        return formatter.formatCellValue(cell).trim();
    }

    private Map<String, Object> rowError(int row, String title, String reason) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("row", row);
        error.put("title", title == null ? "" : title);
        error.put("reason", reason);
        return error;
    }
}
