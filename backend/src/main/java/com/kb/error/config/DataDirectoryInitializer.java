package com.kb.error.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 数据目录初始化器：在 Spring 启动最早阶段创建 data 和 uploads 目录，
 * 并清理异常中断残留的空 .mv.db 文件，
 * 确保后续 Hibernate/H2 能正常初始化数据库。
 *
 * @author kb
 */
@Component
@Order(Integer.MIN_VALUE)
public class DataDirectoryInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataDirectoryInitializer.class);

    @Override
    public void run(ApplicationArguments args) {
        String userDir = System.getProperty("user.dir");
        ensureDir(userDir + File.separator + "data");
        ensureDir(userDir + File.separator + "uploads");

        // 清理异常中断残留的空 .mv.db 文件
        // 空文件会导致 H2 报 "Could not open file"，删除后让 H2 重建
        cleanEmptyMvDbFiles(userDir + File.separator + "data");
    }

    private void ensureDir(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                log.info("创建目录: {}", dir.getAbsolutePath());
            } else {
                log.error("无法创建目录: {}", dir.getAbsolutePath());
            }
        }
    }

    /**
     * 清理空 .mv.db 文件（异常中断残留）
     */
    private void cleanEmptyMvDbFiles(String dataDirPath) {
        File dataDir = new File(dataDirPath);
        if (!dataDir.exists() || !dataDir.isDirectory()) {
            return;
        }
        File[] files = dataDir.listFiles((dir, name) -> name.endsWith(".mv.db"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isFile() && file.length() == 0) {
                if (file.delete()) {
                    log.warn("已删除异常中断残留的空数据库文件: {} ({} 字节)", file.getAbsolutePath(), file.length());
                } else {
                    log.error("无法删除残留空文件: {}, 请手动删除后重试", file.getAbsolutePath());
                }
            }
        }
    }
}
