package com.kb.error.controller;

import com.kb.error.entity.CategoryConfig;
import com.kb.error.repository.CategoryConfigRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFDataValidation;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 收集模板下载（动态适配所属分类配置）
 *
 * <p>以内置静态模板为基础，按当前启用的分类配置动态写入：
 * <ul>
 *   <li>「报错记录」页：所属分类列增加下拉选项（数据验证），选项来自分类配置模块当前启用的分类；</li>
 *   <li>「填写说明」页：追加当前可用分类清单，提示用户从清单中选择，避免填写已停用/不存在的分类。</li>
 * </ul>
 * 这样模板永远与系统内分类配置保持一致，批量导入时分类列能正确读取。
 *
 * @author kb
 */
@RestController
@RequestMapping("/api/template")
public class TemplateController {

    @Autowired
    private CategoryConfigRepository categoryConfigRepository;

    @GetMapping("/error-kb-template")
    public ResponseEntity<byte[]> downloadTemplate() throws Exception {
        List<CategoryConfig> categories = categoryConfigRepository.findByEnabledTrueOrderBySortOrderAscIdAsc();

        // 完全动态生成模板，确保分类下拉与当前配置一致，且无旧静态下拉残留
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // ===== 样式 =====
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.INDIGO.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle requiredStyle = workbook.createCellStyle();
            Font requiredFont = workbook.createFont();
            requiredFont.setColor(IndexedColors.RED.getIndex());
            requiredFont.setBold(true);
            requiredStyle.setFont(requiredFont);

            CellStyle exampleStyle = workbook.createCellStyle();
            exampleStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            exampleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle exampleTitleStyle = workbook.createCellStyle();
            Font exampleFont = workbook.createFont();
            exampleFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            exampleFont.setItalic(true);
            exampleTitleStyle.setFont(exampleFont);
            exampleTitleStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            exampleTitleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle guideTitleStyle = workbook.createCellStyle();
            Font guideTitleFont = workbook.createFont();
            guideTitleFont.setBold(true);
            guideTitleFont.setFontHeightInPoints((short) 14);
            guideTitleFont.setColor(IndexedColors.INDIGO.getIndex());
            guideTitleStyle.setFont(guideTitleFont);

            CellStyle sectionTitleStyle = workbook.createCellStyle();
            Font sectionFont = workbook.createFont();
            sectionFont.setBold(true);
            sectionFont.setColor(IndexedColors.DARK_BLUE.getIndex());
            sectionTitleStyle.setFont(sectionFont);

            CellStyle categoryListStyle = workbook.createCellStyle();
            Font categoryFont = workbook.createFont();
            categoryFont.setBold(true);
            categoryFont.setColor(IndexedColors.DARK_GREEN.getIndex());
            categoryListStyle.setFont(categoryFont);

            // ===== 填写说明 =====
            Sheet guideSheet = workbook.createSheet("填写说明");
            List<String> guideLines = Arrays.asList(
                    "报错处理知识库 · 收集模板（xlsx）",
                    "每行填写一条报错记录，填写完成后在系统首页「批量导入」直接上传本文件即可入库。",
                    "",
                    "填写要求",
                    "1. 报错标题、所属分类为必填；分类请从本页下方「当前可用分类」中选择（也可在报错记录表分类列的下拉中选择）。",
                    "2. 报错内容：尽量原样粘贴报错日志原文，不要转述——系统的智能匹配依赖日志原文特征（错误码、异常类名等）。",
                    "3. 关键字：逗号分隔，优先填「日志中会原样出现的串」：错误码（ORA-12505）、errno（-6261）、异常类名（AccessControlException）、脚本名（get_ready_oss.sh）、表名/用户名等。",
                    "4. 处理步骤：按 1. 2. 3. 编号写清楚；尚未解决的报错也请登记（处理步骤留空），导入后系统自动标记为「待更新」。",
                    "5. 与库中已有记录标题完全相同的行会被自动跳过（防止重复导入）。",
                    "6. 示例行（标题以【示例】开头）导入时自动忽略，可保留或删除。",
                    "7. 报错截图无法通过 xlsx 导入，导入完成后可在系统详情页编辑记录补传截图。",
                    "8. 请勿修改表头文字，导入按表头识别列。",
                    "",
                    "列说明",
                    "报错标题*      一句话概括，例：GTP uploadDir failed errno=-6261",
                    "所属分类*      从「当前可用分类」中选择，勿填写不在列表中的分类",
                    "报错内容（原始日志）  报错日志/报错原文，可多行",
                    "处理步骤      1. 2. 3. 编号描述；未解决可留空",
                    "关键字      逗号分隔的特征串",
                    "登记人      你的姓名；留空则记为「匿名用户」",
                    "",
                    "当前可用分类（请从下列分类中选择填写「所属分类」列）:");
            for (int i = 0; i < guideLines.size(); i++) {
                Row row = guideSheet.createRow(i);
                Cell cell = row.createCell(0);
                cell.setCellValue(guideLines.get(i));
                if (i == 0) {
                    cell.setCellStyle(guideTitleStyle);
                } else if ("填写要求".equals(guideLines.get(i)) || "列说明".equals(guideLines.get(i))
                        || guideLines.get(i).startsWith("当前可用分类")) {
                    cell.setCellStyle(sectionTitleStyle);
                } else if (guideLines.get(i).startsWith("报错标题*")
                        || guideLines.get(i).startsWith("所属分类*")) {
                    cell.setCellStyle(requiredStyle);
                }
            }
            for (int i = 0; i < categories.size(); i++) {
                Cell cell = guideSheet.createRow(guideLines.size() + i).createCell(0);
                cell.setCellValue((i + 1) + ". " + categories.get(i).getName());
                cell.setCellStyle(categoryListStyle);
            }
            guideSheet.setColumnWidth(0, 90 * 256);

            // ===== 报错记录 =====
            Sheet dataSheet = workbook.createSheet("报错记录");
            String[] headers = {"报错标题*", "所属分类*", "报错内容（原始日志）", "处理步骤", "关键字", "登记人"};
            Row headerRow = dataSheet.createRow(0);
            for (int c = 0; c < headers.length; c++) {
                Cell cell = headerRow.createCell(c);
                cell.setCellValue(headers[c]);
                cell.setCellStyle(headerStyle);
            }
            dataSheet.createFreezePane(0, 1);

            // 示例行
            Row exampleRow = dataSheet.createRow(1);
            Cell exampleTitleCell = exampleRow.createCell(0);
            exampleTitleCell.setCellValue("【示例】Oracle数据源连接失败 ORA-12505");
            exampleTitleCell.setCellStyle(exampleTitleStyle);
            Cell exampleCategoryCell = exampleRow.createCell(1);
            exampleCategoryCell.setCellValue("数据交换平台");
            exampleCategoryCell.setCellStyle(exampleStyle);
            Cell exampleContentCell = exampleRow.createCell(2);
            exampleContentCell.setCellValue("Oracle数据源连接失败，报错：\nerror: ORA-12505, TNS:listener does not currently know of SID given in connect descriptor");
            exampleContentCell.setCellStyle(exampleStyle);
            Cell exampleStepsCell = exampleRow.createCell(3);
            exampleStepsCell.setCellValue("1. 确认 JDBC 连接串使用的是 SID 还是 SERVICE_NAME；\n2. 将连接地址改为 jdbc:oracle:thin:@<host>:<port>/<database> 尝试；\n3. 仍失败时联系 DBA 确认监听中注册的服务名。");
            exampleStepsCell.setCellStyle(exampleStyle);
            Cell exampleKeywordsCell = exampleRow.createCell(4);
            exampleKeywordsCell.setCellValue("ORA-12505,Oracle,数据源连接,JDBC,SID,SERVICE_NAME");
            exampleKeywordsCell.setCellStyle(exampleStyle);
            Cell exampleRegistrarCell = exampleRow.createCell(5);
            exampleRegistrarCell.setCellValue("张三");
            exampleRegistrarCell.setCellStyle(exampleStyle);

            for (int c = 0; c < headers.length; c++) {
                dataSheet.setColumnWidth(c, 28 * 256);
            }

            // 所属分类列（第 2 列）加动态下拉，数据区 2~201 行
            XSSFSheet xssfSheet = (XSSFSheet) dataSheet;
            XSSFDataValidationHelper helper = new XSSFDataValidationHelper(xssfSheet);
            CellRangeAddressList addressList = new CellRangeAddressList(2, 201, 1, 1);
            DataValidationConstraint constraint = helper.createExplicitListConstraint(
                    categories.stream().map(CategoryConfig::getName).toArray(String[]::new));
            DataValidation validation = helper.createValidation(constraint, addressList);
            validation.setSuppressDropDownArrow(true);
            validation.setShowErrorBox(true);
            xssfSheet.addValidationData(validation);

            workbook.write(out);
            byte[] bytes = out.toByteArray();
            String fileName = URLEncoder.encode("报错处理知识库-收集模板.xlsx", StandardCharsets.UTF_8.toString());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        }
    }
}
