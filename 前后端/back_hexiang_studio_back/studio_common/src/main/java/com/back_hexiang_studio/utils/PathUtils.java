package com.back_hexiang_studio.utils;

import com.back_hexiang_studio.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 路径处理工具类
 * <p>
 * 提供路径处理相关的静态工具方法。
 * </p>
 *
 * @author Gemini
 * @since 2024-07-20
 */
public class PathUtils {
    private static final Logger log = LoggerFactory.getLogger(PathUtils.class);
    
    /**
     * 文件类型与其存储路径前缀的映射
     */
    private static final Map<String, String> PATH_PREFIX_MAP = new HashMap<>();
    static {
        PATH_PREFIX_MAP.put("material", "material/");
        PATH_PREFIX_MAP.put("honor", "user/honor/");
        PATH_PREFIX_MAP.put("certificate", "user/certificate/");
        PATH_PREFIX_MAP.put("avatar", "avatar/");
        PATH_PREFIX_MAP.put("temp", "temp/");
        PATH_PREFIX_MAP.put("task", ""); // 任务附件不添加前缀
        PATH_PREFIX_MAP.put("notice", "notice/"); // 公告文件前缀
    }
    
    /**
     * 允许上传的安全文件扩展名集合
     */
    private static final Set<String> SAFE_FILE_EXTENSIONS = new HashSet<>();
    static {
        // 图片
        SAFE_FILE_EXTENSIONS.addAll(Arrays.asList("png", "jpg", "jpeg", "gif", "bmp", "webp", "svg"));
        // 文档
        SAFE_FILE_EXTENSIONS.addAll(Arrays.asList("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md"));
        // 视频
        SAFE_FILE_EXTENSIONS.addAll(Arrays.asList("mp4", "avi", "mov", "wmv", "flv", "webm"));
        // 音频
        SAFE_FILE_EXTENSIONS.addAll(Arrays.asList("mp3", "wav", "ogg", "flac", "aac"));
        // 压缩包
        SAFE_FILE_EXTENSIONS.addAll(Arrays.asList("zip", "rar", "7z", "tar", "gz"));
    }
    
    /**
     * 危险文件扩展名集合（这些文件类型可能包含可执行代码，存在安全风险）
     */
    public static final Set<String> DANGEROUS_FILE_EXTENSIONS = new HashSet<>();
    static {
        // 可执行文件和脚本
        DANGEROUS_FILE_EXTENSIONS.addAll(Arrays.asList(
            "exe", "bat", "cmd", "sh", "ps1", "vbs", "js", "py", "php", "asp", "aspx", "jsp", 
            "cgi", "pl", "dll", "so", "dylib", "jar", "war", "msi", "com", "scr", "gadget"
        ));
    }





    /**
     * 智能添加路径前缀
     * <p>
     * 根据文件类型和路径特征，智能判断是否需要添加路径前缀。
     * </p>
     * 
     * @param filePath 原始文件路径
     * @param fileType 文件类型参数（可选）
     * @return 处理后的文件路径
     */
    public static String addPathPrefixIfNeeded(String filePath, String fileType) {
        if (filePath == null || filePath.isEmpty()) {
            return filePath;
        }
        
        // 如果已经包含了已知的路径前缀，则不需要添加
        for (String prefix : PATH_PREFIX_MAP.values()) {
            if (filePath.startsWith(prefix)) {
                return filePath;
            }
        }
        
        // 如果指定了文件类型，且该类型在映射表中存在，则添加对应前缀
        if (fileType != null && PATH_PREFIX_MAP.containsKey(fileType)) {
            return PATH_PREFIX_MAP.get(fileType) + filePath;
        }
        
        // 对于符合日期格式的路径（资料文件的特征），添加material前缀
        if (filePath.matches("\\d{4}/\\d{2}/\\d{2}/.*")) {
            return PATH_PREFIX_MAP.get("material") + filePath;
        }
        
        // 其他情况保持原路径不变
        return filePath;
    }
    
    /**
     * 检查文件扩展名是否安全
     * <p>
     * 验证给定的文件名是否具有安全的扩展名，防止上传可能包含恶意代码的文件。
     * </p>
     * 
     * @param filename 要检查的文件名
     * @return 如果文件扩展名在安全列表中返回true，否则返回false
     */
    public static boolean isFilenameExtensionSafe(String filename) {
        if (filename == null || filename.isEmpty() || !filename.contains(".")) {
            return false;
        }
        
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        
        // 首先检查是否是危险文件类型
        if (DANGEROUS_FILE_EXTENSIONS.contains(extension)) {
            log.warn("检测到危险文件类型: {}", extension);
            return false;
        }
        
        // 然后检查是否在安全文件类型列表中
        boolean isSafe = SAFE_FILE_EXTENSIONS.contains(extension);
        if (!isSafe) {
            log.warn("未知的文件类型: {}", extension);
        }
        
        return isSafe;
    }


    /**
     * 验证图片文件类型
     * @param originalFilename 原始文件名
     * @return 验证结果
     */
    public static Result<?> validateImageFile(String originalFilename) {
        if (originalFilename == null) {
            return Result.error("文件名不能为空");
        }

        // 获取文件扩展名
        String extension = PathUtils.getFileExtension(originalFilename);
        if (extension == null) {
            return Result.error("无法识别文件类型");
        }


        extension = extension.toLowerCase();

        // 检查是否是危险文件类型
        if (DANGEROUS_FILE_EXTENSIONS.contains(extension)) {
            log.warn("尝试上传不安全的图片文件类型: {}", extension);
            return Result.error("不允许上传" + extension + "类型的文件，该类型可能存在安全风险");
        }

        // 严格限制只允许图片文件类型
        Set<String> ALLOWED_IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp"));
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            log.warn("尝试上传非图片文件类型作为展示图片: {}", extension);
            return Result.error("展示图片只允许上传JPG、PNG、GIF、BMP、WEBP等图片格式，不允许上传" + extension + "类型的文件");
        }

        return Result.success();
    }





    
    /**
     * 获取文件扩展名
     * 
     * @param filename 文件名
     * @return 文件扩展名（不包含点），如果没有扩展名则返回null
     */
    public static String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return null;
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }





} 