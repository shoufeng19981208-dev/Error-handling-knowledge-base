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
     */
    @Query("SELECT e FROM ErrorRecord e WHERE " +
            "e.errorTitle LIKE %:keyword% OR " +
            "e.errorContent LIKE %:keyword% OR " +
            "e.keywords LIKE %:keyword% OR " +
            "e.solutionSteps LIKE %:keyword% OR " +
            "e.category LIKE %:keyword%")
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
