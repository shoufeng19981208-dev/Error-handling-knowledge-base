package com.kb.error.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 报错记录实体
 *
 * @author kb
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "error_record")
public class ErrorRecord {

    /** 主键ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 报错标题/关键词 */
    @Column(name = "error_title", length = 500, nullable = false)
    private String errorTitle;

    /** 报错内容（文本描述） */
    @Column(name = "error_content", columnDefinition = "CLOB")
    private String errorContent;

    /** 报错截图路径 */
    @Column(name = "error_screenshot", length = 1000)
    private String errorScreenshot;

    /** 处理步骤 */
    @Column(name = "solution_steps", columnDefinition = "CLOB")
    private String solutionSteps;

    /** 所属分类 */
    @Column(name = "category", length = 200)
    private String category;

    /** 状态：RECORDED-已记录，PENDING-待更新 */
    @Column(name = "status", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private RecordStatus status;

    /** 关键字（用于模糊搜索，逗号分隔） */
    @Column(name = "keywords", length = 1000)
    private String keywords;

    /** 登记人 */
    @Column(name = "registrar", length = 100)
    private String registrar;

    /** 登记时间 */
    @Column(name = "register_time", nullable = false)
    private LocalDateTime registerTime;

    /** 更新人 */
    @Column(name = "updater", length = 100)
    private String updater;

    /** 更新时间 */
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    /**
     * 记录状态枚举
     */
    public enum RecordStatus {
        /** 已记录 */
        RECORDED,
        /** 待更新 */
        PENDING
    }
}
