package com.kb.error.service;

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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 报错记录业务服务层
 *
 * @author kb
 */
@Service
public class ErrorRecordService {

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
        return Collections.singletonList("全部");
    }

    /**
     * 获取待更新列表
     */
    public List<ErrorRecord> getPendingList() {
        return errorRecordRepository.findByStatus(RecordStatus.PENDING);
    }
}
