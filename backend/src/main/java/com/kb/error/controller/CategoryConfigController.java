package com.kb.error.controller;

import com.kb.error.entity.CategoryConfig;
import com.kb.error.repository.CategoryConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 所属分类配置管理接口
 *
 * @author kb
 */
@RestController
@RequestMapping("/api/category-config")
public class CategoryConfigController {

    @Autowired
    private CategoryConfigRepository categoryConfigRepository;

    /**
     * 全部分类（含记录引用统计），供管理页使用
     */
    @GetMapping
    public List<Map<String, Object>> list() {
        List<CategoryConfig> configs = categoryConfigRepository.findAllByOrderBySortOrderAscIdAsc();
        List<Map<String, Object>> result = new ArrayList<>();
        for (CategoryConfig config : configs) {
            result.add(toView(config));
        }
        return result;
    }

    /**
     * 新增分类
     */
    @PostMapping
    public Map<String, Object> create(@RequestBody CategoryConfig config) {
        Map<String, Object> result = new LinkedHashMap<>();
        String name = config.getName() == null ? "" : config.getName().trim();
        if (!StringUtils.hasText(name)) {
            result.put("success", false);
            result.put("message", "分类名称不能为空");
            return result;
        }
        if (name.length() > 100) {
            result.put("success", false);
            result.put("message", "分类名称不能超过 100 个字符");
            return result;
        }
        if (categoryConfigRepository.existsByName(name)) {
            result.put("success", false);
            result.put("message", "分类「" + name + "」已存在");
            return result;
        }
        config.setId(null);
        config.setName(name);
        config.setSortOrder(config.getSortOrder() == null ? 0 : config.getSortOrder());
        config.setEnabled(config.getEnabled() == null ? Boolean.TRUE : config.getEnabled());
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        CategoryConfig saved = categoryConfigRepository.save(config);
        result.put("success", true);
        result.put("data", toView(saved));
        return result;
    }

    /**
     * 修改分类
     */
    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody CategoryConfig config) {
        Map<String, Object> result = new LinkedHashMap<>();
        CategoryConfig existing = categoryConfigRepository.findById(id).orElse(null);
        if (existing == null) {
            result.put("success", false);
            result.put("message", "分类不存在");
            return result;
        }
        String name = config.getName() == null ? "" : config.getName().trim();
        if (!StringUtils.hasText(name)) {
            result.put("success", false);
            result.put("message", "分类名称不能为空");
            return result;
        }
        if (categoryConfigRepository.existsByNameAndIdNot(name, id)) {
            result.put("success", false);
            result.put("message", "分类「" + name + "」已存在");
            return result;
        }
        existing.setName(name);
        if (config.getDescription() != null) {
            existing.setDescription(config.getDescription());
        }
        if (config.getSortOrder() != null) {
            existing.setSortOrder(config.getSortOrder());
        }
        if (config.getEnabled() != null) {
            existing.setEnabled(config.getEnabled());
        }
        existing.setUpdateTime(LocalDateTime.now());
        CategoryConfig saved = categoryConfigRepository.save(existing);
        result.put("success", true);
        result.put("data", toView(saved));
        return result;
    }

    /**
     * 删除分类（被报错记录引用时不允许删除，避免记录失去分类）
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> result = new LinkedHashMap<>();
        CategoryConfig existing = categoryConfigRepository.findById(id).orElse(null);
        if (existing == null) {
            result.put("success", false);
            result.put("message", "分类不存在");
            return result;
        }
        long refCount = categoryConfigRepository.countRecordsByCategory(existing.getName());
        if (refCount > 0) {
            result.put("success", false);
            result.put("message", "该分类下还有 " + refCount + " 条报错记录，不能删除（可先停用）");
            result.put("refCount", refCount);
            return result;
        }
        categoryConfigRepository.delete(existing);
        result.put("success", true);
        result.put("message", "删除成功");
        return result;
    }

    private Map<String, Object> toView(CategoryConfig config) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", config.getId());
        item.put("name", config.getName());
        item.put("description", config.getDescription());
        item.put("sortOrder", config.getSortOrder());
        item.put("enabled", config.getEnabled());
        item.put("createTime", config.getCreateTime());
        item.put("updateTime", config.getUpdateTime());
        item.put("recordCount", categoryConfigRepository.countRecordsByCategory(config.getName()));
        return item;
    }
}
