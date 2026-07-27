package com.kb.error.dto;

import com.kb.error.entity.ErrorRecord;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 智能日志匹配结果
 *
 * @author kb
 */
@Data
public class MatchResult {

    /** 从日志中识别出的报错特征（错误码、异常类名等） */
    private List<String> signals = new ArrayList<>();

    /** 判定的根因行（用于展示与未命中时预填标题） */
    private String rootCause;

    /** 匹配到的记录（按分数降序） */
    private List<MatchItem> matches = new ArrayList<>();

    /**
     * 单条匹配项
     */
    @Data
    public static class MatchItem {

        /** 匹配到的报错记录 */
        private ErrorRecord record;

        /** 匹配得分 */
        private int score;

        /** 匹配度：HIGH / MEDIUM / LOW */
        private String level;

        /** 命中的特征串（前端高亮展示） */
        private List<String> matchedTerms = new ArrayList<>();
    }
}
