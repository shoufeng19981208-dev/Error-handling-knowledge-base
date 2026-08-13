package com.kb.error.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 所属分类配置
 *
 * @author kb
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "category_config")
public class CategoryConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 分类名称（唯一） */
    @Column(name = "name", length = 100, nullable = false, unique = true)
    private String name;

    /** 分类描述 */
    @Column(name = "description", length = 500)
    private String description;

    /** 排序号（越小越靠前） */
    @Column(name = "sort_order")
    private Integer sortOrder;

    /** 是否启用 */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
