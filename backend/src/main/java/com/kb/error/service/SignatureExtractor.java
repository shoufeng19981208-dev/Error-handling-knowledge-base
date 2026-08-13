package com.kb.error.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 报错特征提取工具
 *
 * <p>从原始报错日志/文本中提取错误码、异常类名、脚本文件名、标识符、根因行等特征串。
 * 供智能匹配打分（{@link ErrorRecordService#matchByLog}）与录入页关键字建议复用。
 *
 * @author kb
 */
public final class SignatureExtractor {

    /** 错误码：ORA-12505、EM50004E、TNS-12541 等 */
    private static final Pattern CODE_PATTERN = Pattern.compile("\\b[A-Z]{2,10}-\\d{3,6}\\b");

    /** errno 数值：errno=[-6261]、errno: -6261 */
    private static final Pattern ERRNO_PATTERN = Pattern.compile("(?i)errno\\s*[=:\\[]+\\s*(-?\\d+)");

    /** SQLSTATE 代码 */
    private static final Pattern SQLSTATE_PATTERN = Pattern.compile("(?i)SQLSTATE\\s*[=:]?\\s*([0-9A-Z]{5})");

    /** Java 异常类名（提取简单类名） */
    private static final Pattern EXCEPTION_PATTERN =
            Pattern.compile("\\b(?:[a-zA-Z_$][\\w$]*\\.)*([A-Z][\\w$]{2,}(?:Exception|Error))\\b");

    /** 脚本/配置等文件名：get_ready_oss.sh、krb5.conf 等 */
    private static final Pattern FILE_PATTERN =
            Pattern.compile("\\b[\\w.-]{2,60}\\.(?:sh|log|dat|conf|cfg|xml|jar|py|sql|csv|properties|ya?ml)\\b");

    /** 下划线标识符：gtp_stat、dtes_usr、BDIP_PUB_FILE_INFO 等 */
    private static final Pattern IDENT_PATTERN =
            Pattern.compile("\\b[A-Za-z][A-Za-z0-9]*(?:_[A-Za-z0-9]+)+\\b");

    /** ERROR/FATAL 日志级别 */
    private static final Pattern LEVEL_PATTERN = Pattern.compile("\\b(?:ERROR|FATAL)\\b");

    /** 中文失败语义 */
    private static final Pattern CN_FAIL_PATTERN = Pattern.compile("失败|错误|异常|超时|拒绝|无法");

    /** 中文报错短语：如 连接失败、表空间不足、数据不存在（上下文词 + 失败语义词，用于关键字提取） */
    private static final Pattern CN_PHRASE_PATTERN = Pattern.compile(
            "(?:连接|登录|读取|写入|解析|加载|转换|查询|同步|校验|导入|导出|上传|下载|删除|更新|访问|获取|发送|接收|创建|执行|调用|采集|计算|合并|分割|替换|匹配|过滤|传输|拷贝|解压|压缩|启动|停止|初始化|配置|注册|注销|验证|提交|回滚|重试|连接池|数据库|表空间|快照|数据|文件|任务|作业|作业组|调度|脚本|索引|分区|权限|服务|进程|线程|内存|磁盘|网络|超时|超限|越界|溢出|冲突|丢失|缺少|重复|无效|非法|过期|未找到|不存在|不支持|不允许|不匹配|不一致|不足|失败|为空|为空值|为空字符串)\\s*(?:失败|错误|异常|超时|超限|越界|溢出|冲突|丢失|缺少|重复|无效|非法|过期|拒绝|无法|不存在|未找到|不允许|不匹配|不一致|不足|为空|为空值|为空字符串)");

    /** 单条文本最多提取的特征数 */
    private static final int MAX_SIGNALS = 15;

    /** 伪特征停用集（小写）：如 java.sql 会被文件名模式误判为 .sql 文件，且任何 Java 堆栈都含有，无区分度 */
    private static final Set<String> STOP_SIGNALS = new HashSet<>(java.util.Arrays.asList(
            "java.sql", "javax.sql", "java.util", "java.lang", "java.io"));

    /** 根因行展示截断长度 */
    private static final int ROOT_CAUSE_MAX_LEN = 120;

    private SignatureExtractor() {
    }

    /**
     * 提取特征串（有序去重，大小写不敏感去重）
     * <p>优先级：错误码 &gt; errno &gt; SQLSTATE &gt; 异常类名 &gt; 文件名 &gt; 标识符 &gt; 中文报错短语
     */
    public static List<String> extract(String text) {
        List<String> signals = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return signals;
        }
        Set<String> seen = new HashSet<>();
        collect(signals, seen, text, CODE_PATTERN, 0, null);
        collect(signals, seen, text, ERRNO_PATTERN, 1, "errno=");
        collect(signals, seen, text, SQLSTATE_PATTERN, 1, "SQLSTATE=");
        collect(signals, seen, text, EXCEPTION_PATTERN, 1, null);
        collect(signals, seen, text, FILE_PATTERN, 0, null);
        collect(signals, seen, text, IDENT_PATTERN, 0, null);
        collect(signals, seen, text, CN_PHRASE_PATTERN, 0, null);
        return signals;
    }

    /**
     * 判定根因行：最后一个 Caused by 行 &gt; 首个 ERROR/FATAL 行 &gt; 首个异常行 &gt; 首个中文失败行 &gt; 首个非空行
     */
    public static String rootCause(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String lastCausedBy = null;
        String firstLevelLine = null;
        String firstExceptionLine = null;
        String firstCnFailLine = null;
        String firstNonEmpty = null;
        for (String raw : text.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (firstNonEmpty == null) {
                firstNonEmpty = line;
            }
            if (line.toLowerCase().startsWith("caused by")) {
                lastCausedBy = line;
            }
            if (firstLevelLine == null && LEVEL_PATTERN.matcher(line).find()) {
                firstLevelLine = line;
            }
            if (firstExceptionLine == null && EXCEPTION_PATTERN.matcher(line).find()) {
                firstExceptionLine = line;
            }
            if (firstCnFailLine == null && CN_FAIL_PATTERN.matcher(line).find()) {
                firstCnFailLine = line;
            }
        }
        String picked = lastCausedBy != null ? lastCausedBy
                : firstLevelLine != null ? firstLevelLine
                : firstExceptionLine != null ? firstExceptionLine
                : firstCnFailLine != null ? firstCnFailLine
                : firstNonEmpty;
        return truncate(picked, ROOT_CAUSE_MAX_LEN);
    }

    /**
     * 提取关键行（Caused by / ERROR / FATAL / 含异常类名的行），返回小写形式，用于匹配加权
     */
    public static List<String> keyLines(String text) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return lines;
        }
        for (String raw : text.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            String lower = line.toLowerCase();
            if (lower.startsWith("caused by")
                    || LEVEL_PATTERN.matcher(line).find()
                    || EXCEPTION_PATTERN.matcher(line).find()) {
                lines.add(lower);
            }
        }
        return lines;
    }

    private static void collect(List<String> signals, Set<String> seen, String text,
                                Pattern pattern, int group, String prefix) {
        if (signals.size() >= MAX_SIGNALS) {
            return;
        }
        Matcher matcher = pattern.matcher(text);
        while (matcher.find() && signals.size() < MAX_SIGNALS) {
            String value = matcher.group(group);
            if (value == null || value.trim().isEmpty()) {
                continue;
            }
            String signal = (prefix == null ? "" : prefix) + value.trim();
            String signalLower = signal.toLowerCase();
            if (STOP_SIGNALS.contains(signalLower)) {
                continue;
            }
            if (seen.add(signalLower)) {
                signals.add(signal);
            }
        }
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) {
            return null;
        }
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
