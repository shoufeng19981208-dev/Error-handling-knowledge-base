package com.kb.error.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    @Value("${preview.cache.path:./preview-cache}")
    private String previewCachePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 与上传逻辑保持一致，统一按 JVM 工作目录解析为绝对路径
        String absoluteUploadPath = Paths.get(uploadPath).toAbsolutePath().normalize().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absoluteUploadPath + "/");

        // Office 文档转 PDF 后的预览缓存目录
        String absolutePreviewPath = Paths.get(previewCachePath).toAbsolutePath().normalize().toString();
        registry.addResourceHandler("/preview-cache/**")
                .addResourceLocations("file:" + absolutePreviewPath + "/");
    }
}
