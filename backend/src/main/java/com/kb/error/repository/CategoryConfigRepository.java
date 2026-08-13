package com.kb.error.repository;

import com.kb.error.entity.CategoryConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 分类配置数据访问层
 *
 * @author kb
 */
@Repository
public interface CategoryConfigRepository extends JpaRepository<CategoryConfig, Long> {

    /** 按排序号升序、id 升序获取全部分类 */
    List<CategoryConfig> findAllByOrderBySortOrderAscIdAsc();

    /** 启用的分类（供记录录入下拉框使用） */
    List<CategoryConfig> findByEnabledTrueOrderBySortOrderAscIdAsc();

    Optional<CategoryConfig> findByName(String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    /** 统计某分类在报错记录中被引用的数量 */
    @Query("SELECT COUNT(e) FROM ErrorRecord e WHERE e.category = :name")
    long countRecordsByCategory(@Param("name") String name);
}
