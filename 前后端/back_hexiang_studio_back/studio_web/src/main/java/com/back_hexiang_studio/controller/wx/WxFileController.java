package com.back_hexiang_studio.controller.wx;

import com.back_hexiang_studio.context.UserContextHolder;
import com.back_hexiang_studio.enumeration.FileType;
import com.back_hexiang_studio.result.Result;
import com.back_hexiang_studio.utils.FileUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;


import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import com.back_hexiang_studio.service.MaterialService;
import com.back_hexiang_studio.utils.PathUtils;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.Set;

/**
 * 微信端文件控制器
 * <p>
 * 提供统一的文件上传和访问接口，不与任何具体业务耦合。
 * </p>
 *
 * @author Gemini
 * @since 2024-07-12
 */
@RestController
@RequestMapping("/wx/file")
@Slf4j
public class WxFileController {

    @Value("${file.upload.path}")
    private String uploadPath;

    @Autowired
    private MaterialService materialService;

    /**
     * MIME类型映射表 (作为我们的"字典")。
     * 使用静态代码块来初始化，确保只在类加载时执行一次。
     */
    private static final Map<String, String> MIME_TYPE_MAP = new HashMap<>();
    static {
        // 图片
        MIME_TYPE_MAP.put("png", "image/png");
        MIME_TYPE_MAP.put("jpg", "image/jpeg");
        MIME_TYPE_MAP.put("jpeg", "image/jpeg");
        MIME_TYPE_MAP.put("gif", "image/gif");
        MIME_TYPE_MAP.put("bmp", "image/bmp");
        MIME_TYPE_MAP.put("webp", "image/webp");
        MIME_TYPE_MAP.put("svg", "image/svg+xml");
        // 文档
        MIME_TYPE_MAP.put("pdf", "application/pdf");
        MIME_TYPE_MAP.put("doc", "application/msword");
        MIME_TYPE_MAP.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        MIME_TYPE_MAP.put("xls", "application/vnd.ms-excel");
        MIME_TYPE_MAP.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        MIME_TYPE_MAP.put("ppt", "application/vnd.ms-powerpoint");
        MIME_TYPE_MAP.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        MIME_TYPE_MAP.put("txt", "text/plain");
        MIME_TYPE_MAP.put("md", "text/markdown");
        // 视频
        MIME_TYPE_MAP.put("mp4", "video/mp4");
        MIME_TYPE_MAP.put("avi", "video/x-msvideo");
        MIME_TYPE_MAP.put("mov", "video/quicktime");
        MIME_TYPE_MAP.put("wmv", "video/x-ms-wmv");
        MIME_TYPE_MAP.put("flv", "video/x-flv");
        MIME_TYPE_MAP.put("webm", "video/webm");
        // 音频
        MIME_TYPE_MAP.put("mp3", "audio/mpeg");
        MIME_TYPE_MAP.put("wav", "audio/wav");
        MIME_TYPE_MAP.put("ogg", "audio/ogg");
        MIME_TYPE_MAP.put("flac", "audio/flac");
        MIME_TYPE_MAP.put("aac", "audio/aac");
        // 压缩包
        MIME_TYPE_MAP.put("zip", "application/zip");
        MIME_TYPE_MAP.put("rar", "application/x-rar-compressed");
        MIME_TYPE_MAP.put("7z", "application/x-7z-compressed");
        MIME_TYPE_MAP.put("tar", "application/x-tar");
        MIME_TYPE_MAP.put("gz", "application/gzip");
    }
    
    // 注：文件扩展名安全性检查已移至PathUtils工具类

    /**
     * 通用文件访问接口。
     * <p>
     * 通过文件的相对路径来提供在线预览或附件下载功能。
     * 使用 `**` 通配符来捕获包含子目录的完整相对路径。
     * </p>
     *
     * @param materialId 资料ID，用于记录下载次数（可选）
     * @param download 是否作为附件下载。如果为 true，则浏览器会提示下载；否则会尝试在线打开。
     * @param originalName 原始文件名，用于下载时显示（可选）
     * @param request 用于从中提取实际的文件相对路径。
     * @return 返回文件资源(ResponseEntity<Resource>)，如果文件不存在则返回404。
     */
    @GetMapping("/view/**")
    public ResponseEntity<Resource> viewFile(
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false, defaultValue = "false") boolean download,
            @RequestParam(required = false) String originalName,
            HttpServletRequest request) {

        // 如果是下载，则记录下载次数
        if (download && materialId != null) {
            materialService.recordDownload(materialId, UserContextHolder.getCurrentId(),request.getRemoteAddr(), request.getHeader("User-Agent"));
        }

        // 从请求URI中动态提取文件路径
        String requestUri = request.getRequestURI();
        log.debug("收到文件访问请求，URI: {}", requestUri);
        
        // 定义可能的前缀列表，按优先级排序
        String[] possiblePrefixes = {
            "/api/wx/file/view/",     // 前端使用 /api 前缀的情况
            "/wx/file/view/"          // 直接访问后端的情况
        };
        
        String filePath = null;
        for (String prefix : possiblePrefixes) {
            if (requestUri.startsWith(prefix)) {
                filePath = requestUri.substring(prefix.length());
                log.debug("从路径 '{}' 提取文件路径: '{}'", prefix, filePath);
                break;
            }
        }
        
        // 如果没有匹配到任何前缀，使用兜底方案
        if (filePath == null) {
            String fallbackPrefix = "/view/";
            int viewIndex = requestUri.indexOf(fallbackPrefix);
            if (viewIndex >= 0) {
                filePath = requestUri.substring(viewIndex + fallbackPrefix.length());
                log.debug("使用兜底方案提取文件路径: '{}'", filePath);
            } else {
                log.error("无法从URI中提取文件路径: {}", requestUri);
                return ResponseEntity.badRequest().build();
            }
        }

        try {
            // 对文件路径进行URL解码，处理中文等非ASCII字符
            String decodedFilePath = URLDecoder.decode(filePath, StandardCharsets.UTF_8.name());
            log.debug("解码后的文件路径: {}", decodedFilePath);
            
            // 将相对路径与服务器上的物理根目录拼接，形成绝对路径
            Path fileStorageLocation = Paths.get(uploadPath).toAbsolutePath().normalize();
            log.debug("文件存储根目录: {}", fileStorageLocation);
            
            // 智能添加路径前缀
            decodedFilePath = PathUtils.addPathPrefixIfNeeded(decodedFilePath, request.getParameter("fileType"));
            log.debug("处理后的文件路径: {}", decodedFilePath);
            
            Path targetPath = fileStorageLocation.resolve(decodedFilePath).normalize();
            log.debug("尝试访问的完整文件路径: {}", targetPath);
            
            // 检查文件是否存在
            boolean fileExists = Files.exists(targetPath);
            log.debug("文件是否存在: {}", fileExists);
            
            // 🔧 删除冗余的调试信息：父目录检查和文件列表输出
            // 这些调试信息在生产环境中会产生大量日志，且对静态资源访问意义不大

            Resource resource = new UrlResource(targetPath.toUri());
            
            if (!resource.exists() || !resource.isReadable()) {
                log.error("请求的文件不存在或不可读: {}", targetPath);
                return ResponseEntity.notFound().build();
            }
            
            // 根据文件名猜测MIME类型
            String contentType = guessContentType(targetPath);

            HttpHeaders headers = new HttpHeaders();
            // 根据 'download' 参数决定是内联显示还是作为附件下载
            // attachment: 浏览器会提示用户下载
            // inline: 浏览器会尝试直接在页面上显示（如图片、PDF）
            if (download) {
                // 优先使用前端传递的原始文件名，否则回退到资源本身的物理文件名
                String filename = (originalName != null && !originalName.trim().isEmpty())
                        ? originalName
                        : resource.getFilename();

                // 对文件名进行URL编码，以正确处理非ASCII字符（如中文）
                assert filename != null;
                String encodedFilename = UriUtils.encode(filename, StandardCharsets.UTF_8);
                
                // 设置Content-Disposition头，使用RFC 5987标准格式确保跨浏览器兼容性
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename);
            } else {
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"");
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .headers(headers)
                    .body(resource);
                    
        } catch (MalformedURLException e) {
            log.error("构造文件URL时出错: {}", filePath, e);
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据文件路径猜测文件的MIME类型 (简洁、规范版)。
     * <p>
     * 通过查询预定义的MIME类型映射表来获取类型。
     * 这种"查表法"将数据和逻辑分离，清晰且易于扩展。
     * </p>
     *
     * @param filePath 文件的物理路径对象。
     * @return 对应的MIME类型字符串。
     */
    private String guessContentType(Path filePath) {
        String fileName = filePath.getFileName().toString();
        int lastDotIndex = fileName.lastIndexOf(".");

        // 确保文件名中有后缀
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            String extension = fileName.substring(lastDotIndex + 1).toLowerCase();
            // 从"字典"中查找，如果找不到，就使用默认值
            return MIME_TYPE_MAP.getOrDefault(extension, "application/octet-stream");
        }

        // 如果没有后缀名，也返回默认值
        return "application/octet-stream";
    }

    /**
     * 通用文件上传接口。
     *
     * @param file        前端上传的 MultipartFile 文件。
     * @param fileTypeStr 文件的业务类型字符串，必须与 {@link FileType} 中的枚举名对应。
     * @return 包含文件相对路径的 {@link Result} 对象，成功时 code 为1，失败时返回错误信息。
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam("type") String fileTypeStr) {
        if (file.isEmpty()) {
            return Result.error("上传失败，请选择文件");
        }

        // 验证文件类型
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return Result.error("文件名不能为空");
        }
        
        // 使用PathUtils工具类验证文件扩展名安全性
        if (!PathUtils.isFilenameExtensionSafe(originalFilename)) {
            String extension = PathUtils.getFileExtension(originalFilename);
            log.warn("尝试上传不安全的文件类型: {}", extension);
            return Result.error("不允许上传" + extension + "类型的文件，该类型可能存在安全风险");
        }
        
        // 验证文件内容类型
        String contentType = file.getContentType();
        if (contentType == null || !MIME_TYPE_MAP.values().contains(contentType)) {
            log.warn("文件内容类型不匹配: {}", contentType);
            // 这里只记录警告，不阻止上传，因为某些浏览器可能不准确地设置Content-Type
        }

        FileType fileType;
        try {
            // 将前端传入的字符串安全地转换为 FileType 枚举
            fileType = FileType.valueOf(fileTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("接收到无效的文件类型: {}", fileTypeStr);
            return Result.error("无效的文件类型: " + fileTypeStr);
                }

        try {
            // 调用工具类保存文件
            String relativePath = FileUtils.saveFile(file, fileType);
            // 将生成的相对路径返回给前端
            return Result.success(relativePath);
        } catch (IOException e) {
            log.error("文件上传时发生IO错误", e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }
} 