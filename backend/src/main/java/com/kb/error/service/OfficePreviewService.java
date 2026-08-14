package com.kb.error.service;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.converter.WordToHtmlConverter;
import org.apache.poi.hwpf.converter.PicturesManager;
import org.apache.poi.hwpf.usermodel.PictureType;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Office 文档服务端预览转换
 *
 * <p>基于 Apache POI 将旧版/新版办公文档转换为 HTML 片段，保留常用格式：
 * <ul>
 *   <li>.doc（OLE）→ WordToHtmlConverter 输出完整 HTML；</li>
 *   <li>.xlsx / .xls → 逐 sheet 转为 HTML 表格（保留字体、填充、边框、合并单元格、列宽、数字格式）；</li>
 *   <li>.ppt（OLE）→ 提取纯文本。</li>
 * </ul>
 * 纯 OOXML（.docx/.pptx 等）不在此转换，仍由前端预览库处理。
 *
 * @author kb
 */
@Service
public class OfficePreviewService {

    private static final Logger log = LoggerFactory.getLogger(OfficePreviewService.class);

    /** 返回给前端的类型标记 */
    private static final String KIND_CLIENT = "client";
    private static final String KIND_DOC = "doc";
    private static final String KIND_EXCEL = "excel";

    @Value("${preview.cache.path:./preview-cache}")
    private String previewCachePath;

    /**
     * 根据文件实际内容生成预览结果
     *
     * @return kind: client(前端渲染) / doc / excel / ppt / error
     */
    public Map<String, Object> preview(Path file, String storedName) {
        Map<String, Object> result = new HashMap<>();
        if (file == null || !Files.exists(file)) {
            result.put("kind", "error");
            result.put("message", "文件不存在");
            return result;
        }
        try {
            if (isZip(file)) {
                String ext = extensionOf(storedName);
                if ("xlsx".equals(ext) || "xls".equals(ext)) {
                    return excelPreview(file);
                }
                if ("pptx".equals(ext)) {
                    return pdfPreview(file, storedName);
                }
                result.put("kind", KIND_CLIENT);
                return result;
            }
            if (isOle(file)) {
                String ext = detectOleExtension(file, storedName);
                if ("xls".equals(ext)) {
                    return excelPreview(file);
                }
                if ("doc".equals(ext)) {
                    return docPreview(file);
                }
                if ("ppt".equals(ext)) {
                    return pdfPreview(file, storedName);
                }
            }
            result.put("kind", KIND_CLIENT);
        } catch (Exception e) {
            log.error("文档预览转换失败: {} ({})", storedName, e.getMessage(), e);
            result.put("kind", "error");
            result.put("message", "文档转换失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 上传时识别真实扩展名：很多 WPS 生成的文件扩展名与实际格式不符（如 .doc 起名 .docx）
     */
    public String sniffRealExtension(Path file, String currentExtension) {
        String current = currentExtension == null ? "" : currentExtension.toLowerCase();
        try {
            if (isOle(file)) {
                if (current.equals(".doc") || current.equals(".xls") || current.equals(".ppt")) {
                    return current;
                }
                String probed = Files.probeContentType(file);
                if (probed != null) {
                    if (probed.contains("msword")) {
                        return ".doc";
                    }
                    if (probed.contains("ms-excel") || probed.contains("spreadsheet")) {
                        return ".xls";
                    }
                    if (probed.contains("ms-powerpoint") || probed.contains("presentation")) {
                        return ".ppt";
                    }
                }
                // OLE 复合文档且无法识别子类型时，默认按 .doc 处理
                return ".doc";
            }
        } catch (IOException ignored) {
            // 读取失败时沿用原扩展名
        }
        return current.isEmpty() ? "" : current;
    }

    private Map<String, Object> docPreview(Path file) throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("kind", KIND_DOC);
        result.put("html", convertDocToHtml(file));
        return result;
    }

    /**
     * 使用 LibreOffice 将 PPT/PPTX 转换为 PDF 预览（保真且不会缺页），转换结果缓存到 preview-cache
     */
    private Map<String, Object> pdfPreview(Path file, String storedName) throws Exception {
        Path cacheDir = Paths.get(previewCachePath).toAbsolutePath().normalize();
        if (!Files.exists(cacheDir)) {
            Files.createDirectories(cacheDir);
        }
        String base = storedName;
        int dot = base.lastIndexOf('.');
        if (dot >= 0) {
            base = base.substring(0, dot);
        }
        String pdfName = base + ".pdf";
        Path pdf = cacheDir.resolve(pdfName);
        if (!Files.exists(pdf)) {
            ProcessBuilder pb = new ProcessBuilder(
                    "soffice", "--headless", "--convert-to", "pdf",
                    "-env:UserInstallation=file:///tmp/errorkb-lo-" + UUID.randomUUID(),
                    "--outdir", cacheDir.toString(),
                    file.toAbsolutePath().toString());
            pb.redirectErrorStream(true);
            // 应用可能以受限用户运行，把 HOME 指到可写目录，避免 LibreOffice 写用户配置目录失败
            pb.environment().put("HOME", "/tmp");
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("soffice: {}", line);
                }
            }
            boolean finished = process.waitFor(180, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new Exception("文档转换超时");
            }
            if (process.exitValue() != 0) {
                throw new Exception("LibreOffice 转换失败，退出码 " + process.exitValue());
            }
            if (!Files.exists(pdf)) {
                throw new Exception("未生成 PDF 文件");
            }
            log.info("PPT 已转换为 PDF: {}", pdf);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("kind", "pdf");
        result.put("url", "/preview-cache/" + pdfName);
        return result;
    }

    private Map<String, Object> excelPreview(Path file) throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("kind", KIND_EXCEL);
        List<Map<String, String>> sheets = new ArrayList<>();
        try (InputStream in = Files.newInputStream(file);
             Workbook workbook = WorkbookFactory.create(in)) {
            DataFormatter formatter = new DataFormatter();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                Map<String, String> item = new LinkedHashMap<>();
                item.put("name", sheet.getSheetName());
                item.put("html", buildSheetHtml(sheet, workbook, formatter));
                sheets.add(item);
            }
        }
        result.put("sheets", sheets);
        return result;
    }

    private String convertDocToHtml(Path file) throws Exception {
        try (InputStream in = Files.newInputStream(file)) {
            HWPFDocument document = new HWPFDocument(in);
            org.w3c.dom.Document dom = javax.xml.parsers.DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().newDocument();
            WordToHtmlConverter converter = new WordToHtmlConverter(dom);
            // 默认图片管理器不输出图片，这里把图片内嵌为 base64 data URI，保证预览时图片可见
            converter.setPicturesManager(new PicturesManager() {
                @Override
                public String savePicture(byte[] content, PictureType pictureType, String suggestedName,
                                          float widthInches, float heightInches) {
                    String mime = pictureType.getMime();
                    if (mime == null || mime.isEmpty()) {
                        mime = "image/" + pictureType.getExtension().toLowerCase();
                    }
                    return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(content);
                }
            });
            converter.processDocument(document);
            org.w3c.dom.Document htmlDocument = converter.getDocument();

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(htmlDocument), new StreamResult(writer));
            return writer.toString();
        }
    }

    private String buildSheetHtml(Sheet sheet, Workbook workbook, DataFormatter formatter) {
        StringBuilder html = new StringBuilder(4096);
        html.append("<html><head><meta charset=\"utf-8\"><style>")
                .append("body{margin:0;font-family:-apple-system,'Segoe UI','Microsoft YaHei',sans-serif;font-size:13px;}")
                .append("table{border-collapse:collapse;} td{white-space:nowrap;padding:4px 8px;}")
                .append("</style></head><body><table>");

        int maxCol = maxColumn(sheet);
        appendColGroup(sheet, maxCol, html);

        Set<String> mergedTopLeft = new HashSet<>();
        Set<String> mergedCovered = new HashSet<>();
        for (CellRangeAddress region : sheet.getMergedRegions()) {
            String key = region.getFirstRow() + "," + region.getFirstColumn();
            mergedTopLeft.add(key);
            for (int r = region.getFirstRow(); r <= region.getLastRow(); r++) {
                for (int c = region.getFirstColumn(); c <= region.getLastColumn(); c++) {
                    if (r == region.getFirstRow() && c == region.getFirstColumn()) {
                        continue;
                    }
                    mergedCovered.add(r + "," + c);
                }
            }
        }

        for (int r = sheet.getFirstRowNum(); r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            html.append("<tr");
            if (row.getHeight() > sheet.getDefaultRowHeight()) {
                html.append(" style=\"height:").append(rowHeightPx(row)).append("px\"");
            }
            html.append(">");
            int lastCell = Math.max(row.getLastCellNum() - 1, 0);
            for (int c = 0; c <= lastCell; c++) {
                String pos = r + "," + c;
                if (mergedCovered.contains(pos)) {
                    continue;
                }
                Cell cell = row.getCell(c);
                CellStyle style = cell == null ? null : cell.getCellStyle();
                String css = buildCellCss(style, workbook);
                int colSpan = 1;
                int rowSpan = 1;
                if (mergedTopLeft.contains(pos)) {
                    for (CellRangeAddress region : sheet.getMergedRegions()) {
                        if (region.getFirstRow() == r && region.getFirstColumn() == c) {
                            colSpan = region.getLastColumn() - region.getFirstColumn() + 1;
                            rowSpan = region.getLastRow() - region.getFirstRow() + 1;
                            break;
                        }
                    }
                }
                html.append("<td");
                if (colSpan > 1) {
                    html.append(" colspan=\"").append(colSpan).append("\"");
                }
                if (rowSpan > 1) {
                    html.append(" rowspan=\"").append(rowSpan).append("\"");
                }
                if (!css.isEmpty()) {
                    html.append(" style=\"").append(css).append("\"");
                }
                html.append(">").append(escapeHtml(formatCell(cell, formatter, workbook))).append("</td>");
            }
            html.append("</tr>");
        }
        html.append("</table></body></html>");
        return html.toString();
    }

    private int maxColumn(Sheet sheet) {
        int max = 0;
        for (int r = sheet.getFirstRowNum(); r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row != null) {
                max = Math.max(max, row.getLastCellNum() - 1);
            }
        }
        for (CellRangeAddress region : sheet.getMergedRegions()) {
            max = Math.max(max, region.getLastColumn());
        }
        return max;
    }

    private void appendColGroup(Sheet sheet, int maxCol, StringBuilder html) {
        html.append("<colgroup>");
        for (int c = 0; c <= maxCol; c++) {
            int charWidth = sheet.getColumnWidth(c); // 1/256 字符宽
            int px = Math.max(Math.round(charWidth / 256f * 7f), 40);
            html.append("<col style=\"width:").append(px).append("px\">");
        }
        html.append("</colgroup>");
    }

    private String formatCell(Cell cell, DataFormatter formatter, Workbook workbook) {
        if (cell == null) {
            return "";
        }
        try {
            return formatter.formatCellValue(cell, workbook.getCreationHelper().createFormulaEvaluator());
        } catch (Exception e) {
            return formatter.formatCellValue(cell);
        }
    }

    private String buildCellCss(CellStyle style, Workbook workbook) {
        if (style == null) {
            return "";
        }
        StringBuilder css = new StringBuilder();
        try {
            Font font = workbook.getFontAt(style.getFontIndex());
            if (font.getBold()) {
                css.append("font-weight:bold;");
            }
            if (font.getItalic()) {
                css.append("font-style:italic;");
            }
            if (font.getFontHeightInPoints() > 0) {
                css.append("font-size:").append(font.getFontHeightInPoints()).append("pt;");
            }
            String fontName = font.getFontName();
            if (fontName != null && !fontName.isEmpty()) {
                css.append("font-family:'").append(escapeCss(fontName)).append("';");
            }
            String fontColor = fontColor(font, workbook);
            if (fontColor != null) {
                css.append("color:").append(fontColor).append(";");
            }
        } catch (Exception ignored) {
            // 单个字体属性失败不影响整表
        }
        try {
            if (style.getFillPattern() != null && style.getFillPattern() != FillPatternType.NO_FILL) {
                String fill = fillColor(style, workbook);
                if (fill != null) {
                    css.append("background-color:").append(fill).append(";");
                }
            }
        } catch (Exception ignored) {
            // ignore
        }
        try {
            css.append(borderCss("top", style.getBorderTop(), style.getTopBorderColor(), workbook));
            css.append(borderCss("right", style.getBorderRight(), style.getRightBorderColor(), workbook));
            css.append(borderCss("bottom", style.getBorderBottom(), style.getBottomBorderColor(), workbook));
            css.append(borderCss("left", style.getBorderLeft(), style.getLeftBorderColor(), workbook));
        } catch (Exception ignored) {
            // ignore
        }
        try {
            HorizontalAlignment halign = style.getAlignment();
            if (halign == HorizontalAlignment.CENTER) {
                css.append("text-align:center;");
            } else if (halign == HorizontalAlignment.RIGHT) {
                css.append("text-align:right;");
            } else if (halign == HorizontalAlignment.LEFT) {
                css.append("text-align:left;");
            }
            VerticalAlignment valign = style.getVerticalAlignment();
            if (valign == VerticalAlignment.CENTER) {
                css.append("vertical-align:middle;");
            } else if (valign == VerticalAlignment.TOP) {
                css.append("vertical-align:top;");
            } else if (valign == VerticalAlignment.BOTTOM) {
                css.append("vertical-align:bottom;");
            }
        } catch (Exception ignored) {
            // ignore
        }
        if (style.getWrapText()) {
            css.append("white-space:normal;word-break:break-all;");
        }
        return css.toString();
    }

    private String borderCss(String side, BorderStyle border, short colorIndex, Workbook workbook) {
        if (border == null || border == BorderStyle.NONE) {
            return "";
        }
        int px = 1;
        if (border == BorderStyle.MEDIUM || border == BorderStyle.DASH_DOT_DOT || border == BorderStyle.DASH_DOT) {
            px = 2;
        } else if (border == BorderStyle.THICK) {
            px = 3;
        }
        String color = borderColor(colorIndex, workbook);
        return "border-" + side + ":" + px + "px solid " + (color == null ? "#000000" : color) + ";";
    }

    private String fontColor(Font font, Workbook workbook) {
        if (font instanceof XSSFFont) {
            XSSFColor color = ((XSSFFont) font).getXSSFColor();
            return toCssColor(color);
        }
        try {
            if (font.getColor() >= 0) {
                org.apache.poi.hssf.util.HSSFColor color =
                        ((org.apache.poi.hssf.usermodel.HSSFWorkbook) workbook).getCustomPalette().getColor(font.getColor());
                if (color != null) {
                    return "#" + color.getHexString();
                }
            }
        } catch (Exception ignored) {
            // ignore
        }
        return null;
    }

    private String fillColor(CellStyle style, Workbook workbook) {
        if (style instanceof XSSFCellStyle) {
            return toCssColor(((XSSFCellStyle) style).getFillForegroundXSSFColor());
        }
        try {
            short idx = style.getFillForegroundColor();
            if (idx >= 0) {
                org.apache.poi.hssf.util.HSSFColor color =
                        ((org.apache.poi.hssf.usermodel.HSSFWorkbook) workbook).getCustomPalette().getColor(idx);
                if (color != null) {
                    return "#" + color.getHexString();
                }
            }
        } catch (Exception ignored) {
            // ignore
        }
        return null;
    }

    private String borderColor(short index, Workbook workbook) {
        try {
            org.apache.poi.hssf.util.HSSFColor color =
                    ((org.apache.poi.hssf.usermodel.HSSFWorkbook) workbook).getCustomPalette().getColor(index);
            if (color != null) {
                return "#" + color.getHexString();
            }
        } catch (Exception ignored) {
            // ignore
        }
        return null;
    }

    private String toCssColor(XSSFColor color) {
        if (color == null) {
            return null;
        }
        try {
            String argb = color.getARGBHex();
            if (argb != null && argb.length() >= 6) {
                return "#" + argb.substring(argb.length() - 6);
            }
        } catch (Exception ignored) {
            // ignore
        }
        return null;
    }

    private float rowHeightPx(Row row) {
        return Math.round(row.getHeight() / 20f * 10f) / 10f;
    }

    private String detectOleExtension(Path file, String storedName) throws IOException {
        String stored = extensionOf(storedName);
        if (stored.equals("doc") || stored.equals("xls") || stored.equals("ppt")) {
            return stored;
        }
        String probed = Files.probeContentType(file);
        if (probed != null) {
            if (probed.contains("msword")) {
                return "doc";
            }
            if (probed.contains("ms-excel") || probed.contains("spreadsheet")) {
                return "xls";
            }
            if (probed.contains("ms-powerpoint") || probed.contains("presentation")) {
                return "ppt";
            }
        }
        return "doc";
    }

    private String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int idx = fileName.lastIndexOf('.');
        return idx < 0 ? "" : fileName.substring(idx + 1).toLowerCase();
    }

    private boolean isZip(Path file) throws IOException {
        return startsWith(file, new byte[]{0x50, 0x4B, 0x03, 0x04});
    }

    private boolean isOle(Path file) throws IOException {
        return startsWith(file, new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0});
    }

    private boolean startsWith(Path file, byte[] magic) throws IOException {
        byte[] header = new byte[magic.length];
        try (InputStream in = Files.newInputStream(file)) {
            int read = in.read(header);
            if (read < magic.length) {
                return false;
            }
        }
        for (int i = 0; i < magic.length; i++) {
            if (header[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String escapeCss(String value) {
        return value.replace("'", "\\'").replace(";", "\\;");
    }
}
