package com.kb.error.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 文档管理实体（任意格式上传，记录元数据，文件本体存放在 uploads/documents/）
 *
 * @author kb
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "document_file")
public class DocumentFile {

    /** 主键ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 原始文件名 */
    @Column(name = "original_name", length = 500, nullable = false)
    private String originalName;

    /** 存储文件名（UUID + 原扩展名） */
    @Column(name = "stored_name", length = 200, nullable = false, unique = true)
    private String storedName;

    /** 文件 MIME 类型 */
    @Column(name = "content_type", length = 200)
    private String contentType;

    /** 文件大小（字节） */
    @Column(name = "file_size", nullable = false)
    private Long size;

    /** 上传时间 */
    @Column(name = "upload_time", nullable = false)
    private LocalDateTime uploadTime;
}
