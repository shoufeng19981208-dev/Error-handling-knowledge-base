package com.kb.error.service;

import com.kb.error.dto.MatchResult;
import com.kb.error.entity.ErrorRecord;
import com.kb.error.entity.ErrorRecord.RecordStatus;
import com.kb.error.repository.ErrorRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 报错记录业务服务层
 *
 * @author kb
 */
@Service
public class ErrorRecordService {

    /** 智能匹配返回的最大条数 */
    private static final int MATCH_TOP_N = 10;

    /** 短输入（可同时走 LIKE 搜索合并）的最大长度 */
    private static final int SHORT_INPUT_MAX_LEN = 100;

    /** LIKE 搜索命中的保底分 */
    private static final int LIKE_HIT_SCORE = 12;

    /** 纯 ASCII 词判定 */
    private static final Pattern ASCII_PATTERN = Pattern.compile("^[\\x00-\\x7F]+$");

    @Autowired
    private ErrorRecordRepository errorRecordRepository;

    /**
     * 模糊搜索报错记录
     *
     * @param keyword 搜索关键字
     * @param page    页码（从0开始）
     * @param size    每页数量
     * @return 分页结果
     */
    public Page<ErrorRecord> search(String keyword, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updateTime"));
        if (!StringUtils.hasText(keyword)) {
            return errorRecordRepository.findAll(pageRequest);
        }
        return errorRecordRepository.searchByKeyword(keyword.trim(), pageRequest);
    }

    /**
     * 根据ID查询详情
     */
    public ErrorRecord getById(Long id) {
        return errorRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("报错记录不存在，id=" + id));
    }

    /**
     * 新增报错记录（同时支持：首次遇到报错直接提交，或待补充后提交）
     * 如果未填写处理步骤，状态自动设为PENDING（待更新）
     */
    @Transactional
    public ErrorRecord create(ErrorRecord record) {
        record.setId(null);
        record.setRegisterTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        if (!StringUtils.hasText(record.getSolutionSteps())) {
            record.setStatus(RecordStatus.PENDING);
        } else {
            record.setStatus(RecordStatus.RECORDED);
        }
        if (record.getRegistrar() == null) {
            record.setRegistrar("匿名用户");
        }
        if (record.getUpdater() == null) {
            record.setUpdater(record.getRegistrar());
        }
        return errorRecordRepository.save(record);
    }

    /**
     * 更新报错记录（更新人可修改处理步骤等信息）
     */
    @Transactional
    public ErrorRecord update(Long id, ErrorRecord updateRecord) {
        ErrorRecord existing = getById(id);
        if (StringUtils.hasText(updateRecord.getErrorTitle())) {
            existing.setErrorTitle(updateRecord.getErrorTitle());
        }
        if (updateRecord.getErrorContent() != null) {
            existing.setErrorContent(updateRecord.getErrorContent());
        }
        if (updateRecord.getErrorScreenshot() != null) {
            existing.setErrorScreenshot(updateRecord.getErrorScreenshot());
        }
        if (updateRecord.getSolutionSteps() != null) {
            existing.setSolutionSteps(updateRecord.getSolutionSteps());
        }
        if (StringUtils.hasText(updateRecord.getCategory())) {
            existing.setCategory(updateRecord.getCategory());
        }
        if (StringUtils.hasText(updateRecord.getKeywords())) {
            existing.setKeywords(updateRecord.getKeywords());
        }
        if (StringUtils.hasText(updateRecord.getUpdater())) {
            existing.setUpdater(updateRecord.getUpdater());
        } else {
            existing.setUpdater("匿名用户");
        }
        existing.setUpdateTime(LocalDateTime.now());
        // 如果有处理步骤，状态改为已记录
        if (StringUtils.hasText(existing.getSolutionSteps())) {
            existing.setStatus(RecordStatus.RECORDED);
        }
        return errorRecordRepository.save(existing);
    }

    /**
     * 删除报错记录
     */
    @Transactional
    public void delete(Long id) {
        if (!errorRecordRepository.existsById(id)) {
            throw new IllegalArgumentException("报错记录不存在，id=" + id);
        }
        errorRecordRepository.deleteById(id);
    }

    /**
     * 获取所有分类
     */
    public List<String> getAllCategories() {
        return errorRecordRepository.findDistinctCategories();
    }

    /**
     * 智能日志匹配：把整段报错日志作为"草堆"，用每条记录的关键字/标题/内容特征作为"针"，
     * 反向检查特征是否出现在日志中并打分排序，用户无需自己从日志中提炼关键词。
     *
     * @param logText 用户粘贴的原始报错日志（或短关键词）
     * @return 匹配结果（识别出的特征、根因行、按得分降序的记录列表）
     */
    public MatchResult matchByLog(String logText) {
        String log = logText == null ? "" : logText.trim();
        MatchResult result = new MatchResult();
        if (!StringUtils.hasText(log)) {
            return result;
        }
        result.setSignals(SignatureExtractor.extract(log));
        result.setRootCause(SignatureExtractor.rootCause(log));

        String logLower = log.toLowerCase();
        List<String> keyLines = SignatureExtractor.keyLines(log);
        Set<String> logSignalsLower = new HashSet<>();
        for (String signal : result.getSignals()) {
            logSignalsLower.add(signal.toLowerCase());
        }

        for (ErrorRecord record : errorRecordRepository.findAll()) {
            MatchResult.MatchItem item = scoreRecord(record, logLower, keyLines, logSignalsLower);
            if (item != null) {
                result.getMatches().add(item);
            }
        }
        mergeShortInputSearch(result, log);

        result.getMatches().sort(Comparator
                .comparingInt(MatchResult.MatchItem::getScore).reversed()
                .thenComparing((MatchResult.MatchItem item) -> item.getRecord().getUpdateTime(),
                        Comparator.nullsLast(Comparator.reverseOrder())));
        if (result.getMatches().size() > MATCH_TOP_N) {
            result.setMatches(new ArrayList<>(result.getMatches().subList(0, MATCH_TOP_N)));
        }
        for (MatchResult.MatchItem item : result.getMatches()) {
            item.setLevel(levelOf(item.getScore()));
        }
        return result;
    }

    /**
     * 对单条记录打分：关键字命中 + 标题整体命中 + 内容特征交集；命中日志关键行（ERROR/Caused by）加权
     */
    private MatchResult.MatchItem scoreRecord(ErrorRecord record, String logLower,
                                              List<String> keyLines, Set<String> logSignalsLower) {
        int score = 0;
        // 泛词（低权重短词）单独累计并封顶，避免多个泛词叠加盖过精确特征命中
        int genericScore = 0;
        List<String> matchedTerms = new ArrayList<>();

        for (String term : splitTerms(record.getKeywords())) {
            String termLower = term.toLowerCase();
            if (!containsTerm(logLower, termLower) || containsIgnoreCase(matchedTerms, term)) {
                continue;
            }
            int weight = termWeight(term);
            if (hitKeyLine(keyLines, termLower)) {
                weight += 10;
            }
            if (weight <= 8) {
                genericScore += weight;
            } else {
                score += weight;
            }
            matchedTerms.add(term);
        }
        score += Math.min(genericScore, 18);

        String title = record.getErrorTitle() == null ? "" : record.getErrorTitle().trim();
        if (title.length() >= 4 && logLower.contains(title.toLowerCase())) {
            score += 40;
            matchedTerms.add(title);
        }

        for (String signal : SignatureExtractor.extract(record.getErrorContent())) {
            if (logSignalsLower.contains(signal.toLowerCase()) && !containsIgnoreCase(matchedTerms, signal)) {
                score += 20;
                matchedTerms.add(signal);
            }
        }

        if (score <= 0) {
            return null;
        }
        MatchResult.MatchItem item = new MatchResult.MatchItem();
        item.setRecord(record);
        item.setScore(score);
        item.setMatchedTerms(matchedTerms);
        return item;
    }

    /**
     * 短输入（单行且不超过 {@link #SHORT_INPUT_MAX_LEN} 字符）同时跑原有 LIKE 搜索并按 id 去重合并，
     * 保证一个输入框粘什么都行、旧的关键词搜索行为不回退。
     */
    private void mergeShortInputSearch(MatchResult result, String log) {
        if (log.contains("\n") || log.length() > SHORT_INPUT_MAX_LEN) {
            return;
        }
        Set<Long> matchedIds = new HashSet<>();
        for (MatchResult.MatchItem item : result.getMatches()) {
            matchedIds.add(item.getRecord().getId());
        }
        PageRequest pageRequest = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "updateTime"));
        for (ErrorRecord record : errorRecordRepository.searchByKeyword(log, pageRequest)) {
            if (matchedIds.contains(record.getId())) {
                continue;
            }
            MatchResult.MatchItem item = new MatchResult.MatchItem();
            item.setRecord(record);
            item.setScore(LIKE_HIT_SCORE);
            item.getMatchedTerms().add(log);
            result.getMatches().add(item);
        }
    }

    /**
     * 关键字切分：支持中英文逗号/分号，过滤单字符噪声
     */
    private List<String> splitTerms(String keywords) {
        List<String> terms = new ArrayList<>();
        if (!StringUtils.hasText(keywords)) {
            return terms;
        }
        for (String part : keywords.split("[,，;；]")) {
            String term = part.trim();
            if (term.length() >= 2) {
                terms.add(term);
            }
        }
        return terms;
    }

    /**
     * 特征词权重：含数字/连接符的"代码型"词（如 ORA-12505、errno）与长词特异性更高
     */
    private int termWeight(String term) {
        if (term.length() <= 2) {
            return 6;
        }
        int weight = 15;
        if (term.matches(".*\\d.*") || term.contains("-") || term.contains("_") || term.contains(".")) {
            weight += 10;
        }
        if (term.length() >= 6) {
            weight += 5;
        }
        return weight;
    }

    private boolean hitKeyLine(List<String> keyLines, String termLower) {
        for (String line : keyLines) {
            if (containsTerm(line, termLower)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 特征词是否出现在文本中：纯 ASCII 词要求两侧为非字母数字（避免 desc 误命中 descriptor），
     * 含中文的词直接子串匹配（中文无词边界概念）
     */
    private boolean containsTerm(String textLower, String termLower) {
        if (!ASCII_PATTERN.matcher(termLower).matches()) {
            return textLower.contains(termLower);
        }
        Pattern boundary = Pattern.compile("(?<![A-Za-z0-9])" + Pattern.quote(termLower) + "(?![A-Za-z0-9])");
        return boundary.matcher(textLower).find();
    }

    private boolean containsIgnoreCase(List<String> list, String value) {
        for (String item : list) {
            if (item.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 分数映射匹配度档位
     */
    private String levelOf(int score) {
        if (score >= 45) {
            return "HIGH";
        }
        if (score >= 22) {
            return "MEDIUM";
        }
        return "LOW";
    }

    /**
     * 获取待更新列表
     */
    public List<ErrorRecord> getPendingList() {
        return errorRecordRepository.findByStatus(RecordStatus.PENDING);
    }
}
