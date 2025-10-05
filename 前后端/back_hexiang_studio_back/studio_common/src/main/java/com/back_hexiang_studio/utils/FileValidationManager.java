package com.back_hexiang_studio.utils;

import com.back_hexiang_studio.result.Result;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 文件验证
 * 集成了配置管理和验证功能
 */
@Component
@ConfigurationProperties(prefix = "file.size")
@Slf4j
public class FileValidationManager {
    
    // 默认文件大小限制（字节）
    private long defaultMaxSize = 10 * 1024 * 1024; // 10MB
    
    // 不同类型文件的大小限制配置
    private Map<String, Long> typeLimits = new HashMap<>();
    
    // 初始化默认配置
    public FileValidationManager() {
        // 图片文件限制
        typeLimits.put("image", 5 * 1024 * 1024L);        // 5MB
        typeLimits.put("avatar", 2 * 1024 * 1024L);       // 2MB
        typeLimits.put("cover", 2 * 1024 * 1024L);        // 2MB

        // 文档文件限制
        typeLimits.put("document", 20 * 1024 * 1024L);    // 20MB
        typeLimits.put("material", 100 * 1024 * 1024L);   // 100MB
        typeLimits.put("attachment", 10 * 1024 * 1024L);  // 10MB

        // 视频文件限制
        typeLimits.put("video", 200 * 1024 * 1024L);      // 200MB

        // 音频文件限制
        typeLimits.put("audio", 50 * 1024 * 1024L);       // 50MB

        // 压缩文件限制
        typeLimits.put("archive", 100 * 1024 * 1024L);    // 100MB

        // 荣誉证书限制
        typeLimits.put("honor", 10 * 1024 * 1024L);       // 10MB
        typeLimits.put("certificate", 10 * 1024 * 1024L); // 10MB
    }
    
    /**
     * 验证文件（类型 + 大小）
     * @param file 上传的文件
     * @param fileType 文件类型（如：image, material, attachment等）
     * @return 验证结果
     */
    public Result<?> validateFile(MultipartFile file, String fileType) {
        if (file == null || file.isEmpty()) {
            return Result.error("文件不能为空");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return Result.error("文件名不能为空");
        }
        
        // 1. 验证文件类型安全性
        if (!PathUtils.isFilenameExtensionSafe(originalFilename)) {
            String extension = PathUtils.getFileExtension(originalFilename);
            log.warn("尝试上传不安全的文件类型: {}", extension);
            return Result.error("不允许上传" + extension + "类型的文件，该类型可能存在安全风险");
        }
        
        // 2. 验证文件大小
        long fileSize = file.getSize();
        long maxSize = getMaxSize(fileType);
        
        if (fileSize > maxSize) {
            String formattedMaxSize = getFormattedMaxSize(fileType);
            String formattedFileSize = formatFileSize(fileSize);
            return Result.error(String.format("文件大小 %s 超过限制 %s", formattedFileSize, formattedMaxSize));
        }
        
        return Result.success();
    }
    
    /**
     * 验证图片文件（类型 + 大小）
     * @param file 上传的文件
     * @return 验证结果
     */
    public Result<?> validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error("文件不能为空");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return Result.error("文件名不能为空");
        }
        
        // 1. 验证图片文件类型
        Result<?> typeResult = PathUtils.validateImageFile(originalFilename);
        if (!typeResult.isSuccess()) {
            return typeResult;
        }
        
        // 2. 验证图片文件大小
        long fileSize = file.getSize();
        long maxSize = getMaxSize("image");
        
        if (fileSize > maxSize) {
            String formattedMaxSize = getFormattedMaxSize("image");
            String formattedFileSize = formatFileSize(fileSize);
            return Result.error(String.format("图片大小 %s 超过限制 %s", formattedFileSize, formattedMaxSize));
        }
        
        return Result.success();
    }
    
    /**
     * 验证荣誉证书文件（类型 + 大小）
     * @param file 上传的文件
     * @param fileType 文件类型（honor 或 certificate）
     * @return 验证结果
     */
    public Result<?> validateHonorCertificateFile(MultipartFile file, String fileType) {
        if (file == null || file.isEmpty()) {
            return Result.error("文件不能为空");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return Result.error("文件名不能为空");
        }
        
        // 1. 验证图片文件类型
        Result<?> typeResult = PathUtils.validateImageFile(originalFilename);
        if (!typeResult.isSuccess()) {
            return typeResult;
        }
        
        // 2. 验证文件大小
        long fileSize = file.getSize();
        long maxSize = getMaxSize(fileType);
        
        if (fileSize > maxSize) {
            String formattedMaxSize = getFormattedMaxSize(fileType);
            String formattedFileSize = formatFileSize(fileSize);
            return Result.error(String.format("文件大小 %s 超过限制 %s", formattedFileSize, formattedMaxSize));
        }
        
        return Result.success();
    }
    
    /**
     * 获取指定类型的文件大小限制
     * @param fileType 文件类型
     * @return 文件大小限制（字节）
     */
    public long getMaxSize(String fileType) {
        return typeLimits.getOrDefault(fileType.toLowerCase(), defaultMaxSize);
    }
    
    /**
     * 设置指定类型的文件大小限制
     * @param fileType 文件类型
     * @param maxSize 最大大小（字节）
     */
    public void setMaxSize(String fileType, long maxSize) {
        typeLimits.put(fileType.toLowerCase(), maxSize);
    }
    
    /**
     * 验证文件大小是否符合限制
     * @param fileType 文件类型
     * @param fileSize 文件大小（字节）
     * @return 是否符合限制
     */
    public boolean isValidSize(String fileType, long fileSize) {
        long maxSize = getMaxSize(fileType);
        return fileSize <= maxSize;
    }
    
    /**
     * 格式化文件大小限制为可读字符串
     * @param fileType 文件类型
     * @return 格式化的文件大小限制
     */
    public String getFormattedMaxSize(String fileType) {
        long maxSize = getMaxSize(fileType);
        return formatFileSize(maxSize);
    }
    
    /**
     * 获取所有配置的类型限制
     * @return 类型限制映射
     */
    public Map<String, Long> getAllTypeLimits() {
        return new HashMap<>(typeLimits);
    }
    
    /**
     * 格式化文件大小
     * @param size 文件大小（字节）
     * @return 格式化的文件大小字符串
     */
    public static String formatFileSize(long size) {
        if (size < 1024) {
            return size + "B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1fKB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1fMB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.1fGB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    // Getter和Setter方法
    public long getDefaultMaxSize() {
        return defaultMaxSize;
    }
    
    public void setDefaultMaxSize(long defaultMaxSize) {
        this.defaultMaxSize = defaultMaxSize;
    }
    
    public Map<String, Long> getTypeLimits() {
        return typeLimits;
    }
    
    public void setTypeLimits(Map<String, Long> typeLimits) {
        this.typeLimits = typeLimits;
    }
} 