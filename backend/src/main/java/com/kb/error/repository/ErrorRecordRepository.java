package com.kb.error.repository;

import com.kb.error.entity.ErrorRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 报错记录数据访问层
 *
 * @author kb
 */
@Repository
public interface ErrorRecordRepository extends JpaRepository<ErrorRecord, Long> {

    /**
     * 模糊搜索：匹配标题、内容、关键字、分类
     * 原生 SQL：使用 CONCAT 拼接通配符；排序已在 SQL 内完成（MySQL 列名 update_time）
     */
    @Query(value = "SELECT * FROM error_record e WHERE " +
            "e.error_title LIKE '%' || :keyword || '%' OR " +
            "e.error_content LIKE '%' || :keyword || '%' OR " +
            "e.keywords LIKE '%' || :keyword || '%' OR " +
            "e.solution_steps LIKE '%' || :keyword || '%' OR " +
            "e.category LIKE '%' || :keyword || '%' " +
            "ORDER BY e.update_time DESC",
            countQuery = "SELECT COUNT(*) FROM error_record e WHERE " +
                    "e.error_title LIKE '%' || :keyword || '%' OR " +
                    "e.error_content LIKE '%' || :keyword || '%' OR " +
                    "e.keywords LIKE '%' || :keyword || '%' OR " +
                    "e.solution_steps LIKE '%' || :keyword || '%' OR " +
                    "e.category LIKE '%' || :keyword || '%'",
            nativeQuery = true)
    Page<ErrorRecord> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 按分类查询
     */
    List<ErrorRecord> findByCategory(String category);

    /**
     * 按状态查询
     */
    List<ErrorRecord> findByStatus(ErrorRecord.RecordStatus status);

    /**
     * 获取所有去重分类
     */
    @Query("SELECT DISTINCT e.category FROM ErrorRecord e ORDER BY e.category")
    List<String> findDistinctCategories();

    /**
     * 标题是否已存在（批量导入去重用）
     */
    boolean existsByErrorTitle(String errorTitle);
}
