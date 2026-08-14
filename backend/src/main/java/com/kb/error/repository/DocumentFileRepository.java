package com.kb.error.repository;

import com.kb.error.entity.DocumentFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 文档元数据仓库
 *
 * @author kb
 */
public interface DocumentFileRepository extends JpaRepository<DocumentFile, Long> {

    /** 按上传时间倒序返回，便于前端直接展示最新文档 */
    List<DocumentFile> findAllByOrderByUploadTimeDesc();
}
