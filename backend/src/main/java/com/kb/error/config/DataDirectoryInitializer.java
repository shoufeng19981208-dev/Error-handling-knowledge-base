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
 * 解决 Linux 离线部署时相对路径目录不存在导致 H2 报 Could not open file 的问题。
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
}
