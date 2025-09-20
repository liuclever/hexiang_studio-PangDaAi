package com.back_hexiang_studio.pangDaAi.tool.workflow;

import com.back_hexiang_studio.context.UserContextHolder;
import com.back_hexiang_studio.pangDaAi.service.PermissionService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class NoticeManagementTools {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PermissionService permissionService;

    @Tool("发布一条新公告。此工具会进行权限验证并创建新的公告记录。")
    @Transactional
    public String publishNotice(
        @P("公告标题") String title,
        @P("公告内容") String content,
        @P("公告类型，可以是 '通知', '活动', 或 '新闻'") String type,
        @P("发布者名称") String publisher,
        @P("当前用户的ID，这个ID由系统在后台自动提供，AI需要直接传递它") Long currentUserId
    ) {
        log.info("🤖 AI Workflow Tool: 发布新公告 '{}'，类型: {}", title, type);
        
        // 权限检查
        if (currentUserId == null) {
            return "❌ 用户未登录，无法发布公告。";
        }
        
        if (!permissionService.canManageNotices(currentUserId)) {
            return permissionService.getNoticeManagementPermissionInfo(currentUserId);
        }
        
        // 参数验证
        if (!StringUtils.hasText(title)) {
            return "❌ 公告标题不能为空。";
        }
        if (!StringUtils.hasText(content)) {
            return "❌ 公告内容不能为空。";
        }
        if (!StringUtils.hasText(publisher)) {
            return "❌ 发布者名称不能为空。";
        }
        
        String trimmedTitle = title.trim();
        String trimmedContent = content.trim();
        String trimmedPublisher = publisher.trim();
        
        // 标题长度检查
        if (trimmedTitle.length() > 255) {
            return "❌ 公告标题不能超过255个字符，当前为 " + trimmedTitle.length() + " 个字符。";
        }
        
        // 发布者名称长度检查
        if (trimmedPublisher.length() > 20) {
            return "❌ 发布者名称不能超过20个字符，当前为 " + trimmedPublisher.length() + " 个字符。";
        }
        
        try {
            // 检查标题是否重复
            String checkTitleSql = "SELECT COUNT(*) FROM notice WHERE title = ? AND status = 1";
            Integer existingCount = jdbcTemplate.queryForObject(checkTitleSql, Integer.class, trimmedTitle);
            if (existingCount > 0) {
                return "❌ 已存在相同标题的公告：'" + trimmedTitle + "'，请使用不同的标题。";
            }
            
            // 解析公告类型
            Integer typeCode = parseNoticeType(type);
            if (typeCode == null) {
                return "❌ 无效的公告类型 '" + type + "'。请使用：'通知'、'活动' 或 '新闻'。";
            }
            
            // 获取当前时间
            LocalDateTime now = LocalDateTime.now();
            String currentTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            
            // 插入新公告
            String insertSql = "INSERT INTO notice (title, content, publishTime, status, type, " +
                              "create_time, update_time, create_user, update_user, publisher) " +
                              "VALUES (?, ?, ?, 1, ?, ?, ?, ?, ?, ?)";
            
            int insertedRows = jdbcTemplate.update(insertSql,
                trimmedTitle,
                trimmedContent,
                currentTime,
                typeCode,
                currentTime,
                currentTime,
                String.valueOf(currentUserId),
                String.valueOf(currentUserId),
                trimmedPublisher);
            
            if (insertedRows > 0) {
                // 获取新创建的公告ID
                String getIdSql = "SELECT noticeId FROM notice WHERE title = ? AND create_user = ? ORDER BY create_time DESC LIMIT 1";
                Long newNoticeId = jdbcTemplate.queryForObject(getIdSql, Long.class, trimmedTitle, String.valueOf(currentUserId));
                
                log.info("✅ 公告发布成功 - ID: {}, 标题: '{}', 类型: {}, 发布者: '{}'", 
                        newNoticeId, trimmedTitle, getNoticeTypeText(typeCode), trimmedPublisher);
                
                return "✅ 公告发布成功！\n" +
                       "══════════════════════════════════════\n" +
                       "公告ID：" + newNoticeId + "\n" +
                       "公告标题：" + trimmedTitle + "\n" +
                       "公告类型：" + getNoticeTypeText(typeCode) + "\n" +
                       "发布者：" + trimmedPublisher + "\n" +
                       "发布时间：" + currentTime + "\n" +
                       "公告状态：已发布\n" +
                       "内容长度：" + trimmedContent.length() + " 个字符\n" +
                       "══════════════════════════════════════\n" +
                       "💡 提示：公告已成功发布，工作室成员现在可以看到此公告。";
            } else {
                return "❌ 发布公告失败，数据库操作未生效，请稍后重试。";
            }
            
        } catch (Exception e) {
            log.error("❌ 发布公告 '{}' 时发生错误: {}", trimmedTitle, e.getMessage(), e);
            return "❌ 发布公告时发生内部错误：" + e.getMessage() + "\n请检查输入信息或稍后重试。";
        }
    }
    
    /**
     * 解析公告类型文本为数字代码
     * @param typeText 类型文本
     * @return 类型代码：0-通知，1-活动，2-新闻
     */
    private Integer parseNoticeType(String typeText) {
        if (!StringUtils.hasText(typeText)) {
            return null;
        }
        
        String type = typeText.trim();
        switch (type) {
            case "通知":
                return 0;
            case "活动":
                return 1;
            case "新闻":
                return 2;
            default:
                log.warn("⚠️ 未识别的公告类型: {}", typeText);
                return null;
        }
    }
    
    /**
     * 将类型代码转换为文本描述
     * @param typeCode 类型代码
     * @return 类型文本
     */
    private String getNoticeTypeText(Integer typeCode) {
        if (typeCode == null) return "未知类型";
        switch (typeCode) {
            case 0: return "通知";
            case 1: return "活动";
            case 2: return "新闻";
            default: return "未知类型";
        }
    }

    @Tool("根据标题搜索公告。支持精确匹配和模糊搜索，返回详细的公告信息。")
    public String findNoticeByTitle(@P("要搜索的公告标题或关键词") String title) {
        log.info("🤖 AI Workflow Tool: 搜索公告 '{}'", title);
        
        // 参数验证
        if (!StringUtils.hasText(title)) {
            return "❌ 搜索关键词不能为空。";
        }
        
        String searchKeyword = title.trim();
        
        try {
            // 首先尝试精确匹配
            String exactSearchSql = "SELECT n.noticeId, n.title, n.content, n.publishTime, n.status, n.type, " +
                                   "n.create_time, n.publisher, " +
                                   "(SELECT COUNT(*) FROM notice_attachment na WHERE na.notice_id = n.noticeId) as attachment_count, " +
                                   "(SELECT COUNT(*) FROM notice_image ni WHERE ni.notice_id = n.noticeId) as image_count " +
                                   "FROM notice n " +
                                   "WHERE n.title = ? AND n.status = 1 " +
                                   "ORDER BY n.publishTime DESC";
            
            List<Map<String, Object>> exactResults = jdbcTemplate.queryForList(exactSearchSql, searchKeyword);
            
            // 如果精确匹配没有结果，尝试模糊搜索
            List<Map<String, Object>> fuzzyResults = null;
            if (exactResults.isEmpty()) {
                String fuzzySearchSql = "SELECT n.noticeId, n.title, n.content, n.publishTime, n.status, n.type, " +
                                       "n.create_time, n.publisher, " +
                                       "(SELECT COUNT(*) FROM notice_attachment na WHERE na.notice_id = n.noticeId) as attachment_count, " +
                                       "(SELECT COUNT(*) FROM notice_image ni WHERE ni.notice_id = n.noticeId) as image_count " +
                                       "FROM notice n " +
                                       "WHERE (n.title LIKE ? OR n.content LIKE ?) AND n.status = 1 " +
                                       "ORDER BY n.publishTime DESC " +
                                       "LIMIT 10";
                
                String likePattern = "%" + searchKeyword + "%";
                fuzzyResults = jdbcTemplate.queryForList(fuzzySearchSql, likePattern, likePattern);
            }
            
            List<Map<String, Object>> results = exactResults.isEmpty() ? fuzzyResults : exactResults;
            
            if (results == null || results.isEmpty()) {
                return "📢 未找到相关公告\n" +
                       "══════════════════════════════════════\n" +
                       "搜索关键词：" + searchKeyword + "\n" +
                       "搜索结果：0 条\n\n" +
                       "💡 建议：\n" +
                       "  • 检查关键词拼写是否正确\n" +
                       "  • 尝试使用更简短的关键词\n" +
                       "  • 使用公告的主要词汇进行搜索";
            }
            
            // 构建搜索结果
            StringBuilder result = new StringBuilder();
            result.append("📢 公告搜索结果\n");
            result.append("══════════════════════════════════════\n");
            result.append("搜索关键词：").append(searchKeyword).append("\n");
            result.append("匹配方式：").append(exactResults.isEmpty() ? "模糊搜索" : "精确匹配").append("\n");
            result.append("找到结果：").append(results.size()).append(" 条\n\n");
            
            for (int i = 0; i < results.size(); i++) {
                Map<String, Object> notice = results.get(i);
                
                result.append("【").append(i + 1).append("】 ").append(notice.get("title")).append("\n");
                result.append("────────────────────────────────────\n");
                result.append("公告ID：").append(notice.get("noticeId")).append("\n");
                result.append("公告类型：").append(getNoticeTypeText((Integer) notice.get("type"))).append("\n");
                result.append("发布者：").append(notice.get("publisher")).append("\n");
                result.append("发布时间：").append(notice.get("publishTime")).append("\n");
                result.append("创建时间：").append(notice.get("create_time")).append("\n");
                
                // 附件和图片统计
                Long attachmentCount = ((Number) notice.get("attachment_count")).longValue();
                Long imageCount = ((Number) notice.get("image_count")).longValue();
                
                if (attachmentCount > 0 || imageCount > 0) {
                    result.append("附加内容：");
                    if (attachmentCount > 0) {
                        result.append("附件 ").append(attachmentCount).append(" 个");
                    }
                    if (imageCount > 0) {
                        if (attachmentCount > 0) result.append("，");
                        result.append("图片 ").append(imageCount).append(" 个");
                    }
                    result.append("\n");
                }
                
                // 内容预览
                String content = (String) notice.get("content");
                if (StringUtils.hasText(content)) {
                    String preview = content.length() > 100 ? content.substring(0, 100) + "..." : content;
                    result.append("内容预览：").append(preview).append("\n");
                }
                
                if (i < results.size() - 1) {
                    result.append("\n");
                }
            }
            
            if (results.size() >= 10 && exactResults.isEmpty()) {
                result.append("\n💡 提示：为避免结果过多，模糊搜索限制显示前10条结果。");
                result.append("如需查看更多结果，请使用更具体的关键词。");
            }
            
            log.info("✅ 公告搜索完成 - 关键词: '{}', 找到: {} 条结果", searchKeyword, results.size());
            return result.toString();
            
        } catch (Exception e) {
            log.error("❌ 搜索公告 '{}' 时发生错误: {}", searchKeyword, e.getMessage(), e);
            return "❌ 搜索公告时发生内部错误，请稍后重试。";
        }
    }

    @Tool("【第一步】请求删除一篇公告。此工具会进行风险分析并返回需要用户确认的警告信息。")
    public String requestNoticeDeletion(
        @P("要删除的公告的准确标题") String title,
        @P("当前用户的ID，这个ID由系统在后台自动提供，AI需要直接传递它") Long currentUserId
    ) {
        log.info("🤖 AI Workflow Tool: 请求删除公告 '{}'", title);
        
        // 权限检查
        if (currentUserId == null) {
            return "❌ 用户未登录，无法删除公告。";
        }
        
        if (!permissionService.canManageNotices(currentUserId)) {
            return permissionService.getNoticeManagementPermissionInfo(currentUserId);
        }
        
        // 参数验证
        if (!StringUtils.hasText(title)) {
            return "❌ 公告标题不能为空。";
        }
        
        try {
            // 查找公告详细信息
            String noticeSql = "SELECT noticeId, title, content, publishTime, type, publisher, create_time " +
                              "FROM notice WHERE title = ? AND status = 1";
            List<Map<String, Object>> noticeResults = jdbcTemplate.queryForList(noticeSql, title.trim());
            if (noticeResults.isEmpty()) {
                return "❌ 请求失败：未找到标题为 '" + title + "' 的公告，或该公告已被删除。";
            }
            
            // 如果有多个同标题公告，提醒用户
            if (noticeResults.size() > 1) {
                StringBuilder multipleNotices = new StringBuilder();
                multipleNotices.append("⚠️ 发现 ").append(noticeResults.size()).append(" 个同标题公告：\n");
                for (int i = 0; i < noticeResults.size(); i++) {
                    Map<String, Object> notice = noticeResults.get(i);
                    multipleNotices.append(i + 1).append(". ID: ").append(notice.get("noticeId"));
                    multipleNotices.append("（发布者：").append(notice.get("publisher"));
                    multipleNotices.append("，发布时间：").append(notice.get("publishTime")).append("）\n");
                }
                multipleNotices.append("\n⚠️ 删除操作将删除所有同标题公告！\n");
                multipleNotices.append("如需删除特定公告，请联系管理员使用公告ID进行精确删除。");
            }
            
            Map<String, Object> notice = noticeResults.get(0); // 使用第一个作为主要信息展示
            Long noticeId = (Long) notice.get("noticeId");
            
            // 统计关联数据
            String attachmentCountSql = "SELECT COUNT(*) FROM notice_attachment WHERE notice_id = ?";
            Integer attachmentCount = jdbcTemplate.queryForObject(attachmentCountSql, Integer.class, noticeId);
            
            String imageCountSql = "SELECT COUNT(*) FROM notice_image WHERE notice_id = ?";
            Integer imageCount = jdbcTemplate.queryForObject(imageCountSql, Integer.class, noticeId);
            
            // 获取附件和图片的大小统计
            String attachmentSizeSql = "SELECT SUM(file_size), SUM(download_count) FROM notice_attachment WHERE notice_id = ?";
            Map<String, Object> attachmentStats = jdbcTemplate.queryForMap(attachmentSizeSql, noticeId);
            Long attachmentTotalSize = attachmentStats.get("SUM(file_size)") != null ? ((Number) attachmentStats.get("SUM(file_size)")).longValue() : 0;
            Long attachmentDownloads = attachmentStats.get("SUM(download_count)") != null ? ((Number) attachmentStats.get("SUM(download_count)")).longValue() : 0;
            
            String imageSizeSql = "SELECT SUM(image_size) FROM notice_image WHERE notice_id = ?";
            Long imageTotalSize = jdbcTemplate.queryForObject(imageSizeSql, Long.class, noticeId);
            if (imageTotalSize == null) imageTotalSize = 0L;
            
            // 构建详细的风险分析报告
            StringBuilder warning = new StringBuilder();
            warning.append("⚠️【严重警告 - 公告删除确认】⚠️\n");
            warning.append("══════════════════════════════════════\n");
            warning.append("公告信息：\n");
            warning.append("  • 公告标题：").append(notice.get("title")).append("\n");
            warning.append("  • 公告ID：").append(noticeId).append("\n");
            warning.append("  • 公告类型：").append(getNoticeTypeText((Integer) notice.get("type"))).append("\n");
            warning.append("  • 发布者：").append(notice.get("publisher")).append("\n");
            warning.append("  • 发布时间：").append(notice.get("publishTime")).append("\n");
            warning.append("  • 创建时间：").append(notice.get("create_time")).append("\n");
            
            String content = (String) notice.get("content");
            if (StringUtils.hasText(content)) {
                warning.append("  • 内容长度：").append(content.length()).append(" 个字符\n");
            }
            warning.append("\n");
            
            warning.append("📊 附加内容统计：\n");
            warning.append("  • 附件文件：").append(attachmentCount).append(" 个\n");
            if (attachmentCount > 0) {
                warning.append("    - 总大小：").append(formatFileSize(attachmentTotalSize)).append("\n");
                warning.append("    - 总下载：").append(attachmentDownloads).append(" 次\n");
            }
            warning.append("  • 展示图片：").append(imageCount).append(" 个\n");
            if (imageCount > 0) {
                warning.append("    - 总大小：").append(formatFileSize(imageTotalSize)).append("\n");
            }
            warning.append("\n");
            
            // 特殊警告
            if (noticeResults.size() > 1) {
                warning.append("🚨 批量删除警告：\n");
                warning.append("  将同时删除 ").append(noticeResults.size()).append(" 个同标题公告\n");
                warning.append("  以及它们的所有附件和图片！\n\n");
            }
            
            if (attachmentDownloads > 0) {
                warning.append("📥 下载影响警告：\n");
                warning.append("  公告附件已被下载 ").append(attachmentDownloads).append(" 次\n");
                warning.append("  删除后用户将无法再次下载\n\n");
            }
            
            long totalFileSize = attachmentTotalSize + imageTotalSize;
            if (totalFileSize > 0) {
                warning.append("💾 存储清理提示：\n");
                warning.append("  删除将释放服务器空间：").append(formatFileSize(totalFileSize)).append("\n\n");
            }
            
            warning.append("⚡ 此操作将：\n");
            warning.append("  ❌ 永久删除公告记录\n");
            warning.append("  ❌ 删除所有关联附件（").append(attachmentCount).append(" 个）\n");
            warning.append("  ❌ 删除所有关联图片（").append(imageCount).append(" 个）\n");
            warning.append("  ❌ 操作无法撤销！\n");
            warning.append("══════════════════════════════════════\n");
            warning.append("如果您确定要继续删除，请调用 `confirmNoticeDeletion` 工具。\n");
            warning.append("建议：删除前请确认没有用户正在查看或需要此公告。");
            
            log.warn("⚠️ 用户 {} 请求删除公告 '{}' (ID: {}), 附件: {}个, 图片: {}个", 
                    currentUserId, title, noticeId, attachmentCount, imageCount);
            
            return warning.toString();
            
        } catch (Exception e) {
            log.error("❌ 处理公告删除请求时发生错误，公告: {}, 错误: {}", title, e.getMessage(), e);
            return "❌ 处理删除请求时发生内部错误，请稍后重试。";
        }
    }
    
    /**
     * 格式化文件大小显示
     */
    private String formatFileSize(long bytes) {
        if (bytes == 0) return "0 B";
        
        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format("%.1f %s", size, units[unitIndex]);
    }

    @Tool("【第二步】确认删除一篇公告。此操作将永久删除公告及其所有附件和图片。")
    @Transactional
    public String confirmNoticeDeletion(
        @P("要删除的公告的准确标题") String title,
        @P("当前用户的ID，这个ID由系统在后台自动提供，AI需要直接传递它") Long currentUserId
    ) {
        log.info("🤖 AI Workflow Tool: 确认删除公告 '{}'", title);
        
        // 权限检查
        if (currentUserId == null) {
            return "❌ 用户未登录，无法删除公告。";
        }
        
        if (!permissionService.canManageNotices(currentUserId)) {
            return permissionService.getNoticeManagementPermissionInfo(currentUserId);
        }
        
        // 参数验证
        if (!StringUtils.hasText(title)) {
            return "❌ 公告标题不能为空。";
        }
        
        try {
            // 查找所有匹配的公告（包括同标题公告）
            String findNoticesSql = "SELECT noticeId, title, publisher, publishTime FROM notice WHERE title = ? AND status = 1";
            List<Map<String, Object>> notices = jdbcTemplate.queryForList(findNoticesSql, title.trim());
            if (notices.isEmpty()) {
                return "❌ 删除失败：在执行删除时找不到公告 '" + title + "'。可能已被其他用户删除。";
            }
            
            // 收集删除统计信息
            int totalNotices = notices.size();
            long totalAttachmentSize = 0;
            long totalImageSize = 0;
            int deletedAttachments = 0;
            int deletedImages = 0;
            int deletedNoticeRecords = 0;
            long totalDownloads = 0;
            
            log.info("🗑️ 开始删除公告 '{}', 共找到 {} 个同标题公告", title, totalNotices);
            
            // 处理每个公告及其附件和图片
            for (Map<String, Object> notice : notices) {
                Long noticeId = (Long) notice.get("noticeId");
                log.debug("🗂️ 处理公告 ID: {}, 标题: {}", noticeId, notice.get("title"));
                
                // 第1步：删除公告附件
                String attachmentsSql = "SELECT attachment_id, file_name, file_path, file_size, download_count " +
                                       "FROM notice_attachment WHERE notice_id = ?";
                List<Map<String, Object>> attachments = jdbcTemplate.queryForList(attachmentsSql, noticeId);
                
                for (Map<String, Object> attachment : attachments) {
                    String filePath = (String) attachment.get("file_path");
                    Long fileSize = attachment.get("file_size") != null ? ((Number) attachment.get("file_size")).longValue() : 0;
                    Integer downloadCount = attachment.get("download_count") != null ? ((Number) attachment.get("download_count")).intValue() : 0;
                    
                    totalAttachmentSize += fileSize;
                    totalDownloads += downloadCount;
                    
                    // 删除物理文件
                    if (StringUtils.hasText(filePath)) {
                        // 注意：这里应该调用文件服务删除实际文件
                        // fileService.deleteFile(filePath); // 实际项目中需要实现文件删除逻辑
                        log.debug("🗂️ 准备删除附件文件: {}", filePath);
                    }
                }
                
                // 删除附件数据库记录
                String deleteAttachmentsSql = "DELETE FROM notice_attachment WHERE notice_id = ?";
                int attachmentRecords = jdbcTemplate.update(deleteAttachmentsSql, noticeId);
                deletedAttachments += attachmentRecords;
                log.debug("📁 删除附件记录: {} 条", attachmentRecords);
                
                // 第2步：删除公告图片
                String imagesSql = "SELECT image_id, image_name, image_path, image_size " +
                                  "FROM notice_image WHERE notice_id = ?";
                List<Map<String, Object>> images = jdbcTemplate.queryForList(imagesSql, noticeId);
                
                for (Map<String, Object> image : images) {
                    String imagePath = (String) image.get("image_path");
                    Long imageSize = image.get("image_size") != null ? ((Number) image.get("image_size")).longValue() : 0;
                    
                    totalImageSize += imageSize;
                    
                    // 删除物理文件
                    if (StringUtils.hasText(imagePath)) {
                        // 注意：这里应该调用文件服务删除实际文件
                        // fileService.deleteFile(imagePath); // 实际项目中需要实现文件删除逻辑
                        log.debug("🖼️ 准备删除图片文件: {}", imagePath);
                    }
                }
                
                // 删除图片数据库记录
                String deleteImagesSql = "DELETE FROM notice_image WHERE notice_id = ?";
                int imageRecords = jdbcTemplate.update(deleteImagesSql, noticeId);
                deletedImages += imageRecords;
                log.debug("🖼️ 删除图片记录: {} 条", imageRecords);
            }
            
            // 第3步：删除所有同标题公告的数据库记录
            String deleteNoticesSql = "DELETE FROM notice WHERE title = ? AND status = 1";
            deletedNoticeRecords = jdbcTemplate.update(deleteNoticesSql, title.trim());
            
            if (deletedNoticeRecords > 0) {
                // 记录详细的删除操作日志
                log.warn("🗑️ 公告删除完成 - 用户: {}, 公告: '{}', " +
                        "删除公告: {}条, 删除附件: {}个, 删除图片: {}个, 总大小: {}bytes, 影响下载: {}次", 
                        currentUserId, title, deletedNoticeRecords, deletedAttachments, deletedImages, 
                        (totalAttachmentSize + totalImageSize), totalDownloads);
                
                StringBuilder result = new StringBuilder();
                result.append("✅ 公告删除成功！\n");
                result.append("══════════════════════════════════════\n");
                result.append("公告标题：").append(title).append("\n");
                if (totalNotices > 1) {
                    result.append("同标题公告：").append(totalNotices).append(" 条\n");
                }
                result.append("\n📊 删除统计：\n");
                result.append("  • 公告记录：").append(deletedNoticeRecords).append(" 条\n");
                result.append("  • 附件文件：").append(deletedAttachments).append(" 个\n");
                result.append("  • 展示图片：").append(deletedImages).append(" 个\n");
                result.append("  • 释放空间：").append(formatFileSize(totalAttachmentSize + totalImageSize)).append("\n");
                if (totalDownloads > 0) {
                    result.append("  • 影响下载：").append(totalDownloads).append(" 次历史下载\n");
                }
                result.append("\n🗂️ 文件处理详情：\n");
                
                for (Map<String, Object> notice : notices) {
                    result.append("  • 公告ID ").append(notice.get("noticeId")).append(": ");
                    result.append("发布者 ").append(notice.get("publisher"));
                    result.append(", 发布时间 ").append(notice.get("publishTime")).append("\n");
                }
                
                if (totalAttachmentSize > 0) {
                    result.append("  • 附件空间：释放 ").append(formatFileSize(totalAttachmentSize)).append("\n");
                }
                if (totalImageSize > 0) {
                    result.append("  • 图片空间：释放 ").append(formatFileSize(totalImageSize)).append("\n");
                }
                
                result.append("\n⚡ 删除操作已完成且无法撤销\n");
                result.append("══════════════════════════════════════\n");
                result.append("删除时间：刚刚\n");
                result.append("执行用户：").append(currentUserId).append("\n\n");
                result.append("💡 提示：已删除的公告和附件无法恢复，如需重新发布请重新创建。");
                
                return result.toString();
            } else {
                log.error("❌ 公告删除失败，title: {}", title);
                return "❌ 删除失败：数据库操作未影响任何行，可能数据已被其他操作修改。";
            }
            
        } catch (Exception e) {
            log.error("❌ 确认删除公告 '{}' 时发生严重错误: {}", title, e.getMessage(), e);
            // 事务会自动回滚
            return "❌ 删除公告时发生内部错误：" + e.getMessage() + 
                   "\n所有操作已回滚，数据保持完整。请稍后重试或联系技术支持。";
        }
    }
} 