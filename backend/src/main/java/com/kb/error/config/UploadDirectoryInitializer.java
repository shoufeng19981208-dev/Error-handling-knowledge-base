package com.kb.error.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

/**
 * 上传目录初始化与权限适配
 *
 * <p>在应用启动时确保上传目录存在且可写：
 * <ul>
 *   <li>目录不存在时自动创建（含父目录）；</li>
 *   <li>目录存在但缺少写权限时，尝试补齐 owner 读写执行权限，
 *       并对同组用户开放读写执行（适配部署目录属主变更后应用用户不在属主/属组的情况）；</li>
 *   <li>补齐后仍不可写时仅告警，避免拖垮应用启动，具体报错由上传接口反馈。</li>
 * </ul>
 * 仅当 JVM 对部署目录本身有写权限时才能生效；若部署目录对运行用户只读，
 * 需要将应用切换为部署目录属主用户运行（或调整目录属主/权限）。
 *
 * @author kb
 */
@Component
@Order(Integer.MIN_VALUE + 1)
public class UploadDirectoryInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UploadDirectoryInitializer.class);

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    @Override
    public void run(ApplicationArguments args) {
        try {
            Path uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                log.info("创建上传目录: {}", uploadDir);
            }
            fixPermissions(uploadDir);
            if (Files.isWritable(uploadDir)) {
                log.info("上传目录就绪且可写: {}", uploadDir);
            } else {
                log.warn("上传目录不可写: {}（当前用户 {}），请将应用切换到目录属主用户运行或调整目录权限",
                        uploadDir, System.getProperty("user.name"));
            }
        } catch (Exception e) {
            log.error("初始化上传目录失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 尝试修正目录权限：补齐 owner 与 group 的读写执行位（POSIX 文件系统）。
     * 非 POSIX 文件系统（如 Windows）直接跳过，不影响启动。
     */
    private void fixPermissions(Path dir) {
        try {
            if (!File.separator.equals("/")) {
                return;
            }
            Set<PosixFilePermission> perms = new HashSet<>(Files.getPosixFilePermissions(dir));
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_WRITE);
            perms.add(PosixFilePermission.OWNER_EXECUTE);
            perms.add(PosixFilePermission.GROUP_READ);
            perms.add(PosixFilePermission.GROUP_WRITE);
            perms.add(PosixFilePermission.GROUP_EXECUTE);
            Files.setPosixFilePermissions(dir, perms);
            log.info("已修正上传目录权限为可读写: {}", dir);
        } catch (UnsupportedOperationException e) {
            // 非 POSIX 文件系统，忽略
        } catch (Exception e) {
            log.warn("修正上传目录权限失败（不影响启动）: {}", e.getMessage());
        }
    }
}
