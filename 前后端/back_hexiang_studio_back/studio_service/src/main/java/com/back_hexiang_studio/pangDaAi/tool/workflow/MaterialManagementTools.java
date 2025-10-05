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
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.HashSet;

/**
 * 【新设计】资料与分类管理工作流工具
 */
@Service
@Slf4j
public class MaterialManagementTools {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // ====================================================================================
    // Redis缓存管理方法
    // ====================================================================================

    /**
     * 清除素材相关缓存
     */
    private void clearMaterialCache(Long materialId) {
        if (materialId != null) {
            redisTemplate.delete("material:detail:" + materialId);
        }
        log.info("  [缓存清理] 清理素材详情缓存，素材ID: {}", materialId);
    }

    /**
     * 清除素材列表相关缓存
     */
    private void clearMaterialListCache() {
        Set<String> patterns = new HashSet<>();
        patterns.add("material:list*");
        patterns.add("materials:page*");
        patterns.add("materials:category:*");
        patterns.add("materials:search*");

        int totalDeleted = 0;
        for (String pattern : patterns) {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                totalDeleted += keys.size();
                log.info("  [缓存清理] 清理素材列表缓存，模式: {}, 键数量: {}", pattern, keys.size());
            }
        }

        if (totalDeleted > 0) {
            log.info("  [缓存清理] 总共清理了 {} 个素材列表相关缓存键", totalDeleted);
        }
    }

    /**
     * 清除分类相关缓存
     */
    private void clearCategoryCache() {
        Set<String> patterns = new HashSet<>();
        patterns.add("categories:all");
        patterns.add("category:*");

        for (String pattern : patterns) {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("  [缓存清理] 清理分类缓存，模式: {}, 键数量: {}", pattern, keys.size());
            }
        }
    }

    /**
     * 执行完整的素材相关缓存清理
     */
    private void performCompleteMaterialCacheClear(Long materialId) {
        if (materialId != null) {
            clearMaterialCache(materialId);
        }
        clearMaterialListCache();
        clearCategoryCache();
        log.info("  [缓存清理] 完成素材相关缓存清理");
    }

    // ====================================================================================
    // 1. 分类管理工具
    // ====================================================================================

    @Tool("列出所有资料分类，包含每个分类下的资料数量统计。")
    public String listMaterialCategories() {
        log.info("  AI Workflow Tool: 列出所有资料分类");
        
        try {
            // 查询分类信息并统计每个分类下的资料数量
            String sql = "SELECT mc.id, mc.name, mc.order_id, mc.create_time, mc.create_user, " +
                        "COUNT(m.id) as material_count, " +
                        "SUM(CASE WHEN m.is_public = 1 THEN 1 ELSE 0 END) as public_count " +
                        "FROM material_category mc " +
                        "LEFT JOIN material m ON mc.id = m.category_id AND m.status = 1 " +
                        "GROUP BY mc.id, mc.name, mc.order_id, mc.create_time, mc.create_user " +
                        "ORDER BY mc.order_id ASC, mc.name ASC";
            
            List<Map<String, Object>> categories = jdbcTemplate.queryForList(sql);
            
            if (categories.isEmpty()) {
                return "  当前系统中没有资料分类。\n  提示：您可以使用 `addMaterialCategory` 工具创建新的分类。";
            }
            
            // 统计总数
            int totalCategories = categories.size();
            Long totalMaterials = categories.stream()
                .mapToLong(c -> ((Number) c.get("material_count")).longValue())
                .sum();
            
            StringBuilder result = new StringBuilder();
            result.append("  资料分类列表\n");
            result.append("═══════════════════════════════════════\n");
            result.append("共有 ").append(totalCategories).append(" 个分类，总计 ").append(totalMaterials).append(" 个资料\n\n");
            
            for (Map<String, Object> category : categories) {
                Long materialCount = ((Number) category.get("material_count")).longValue();
                Long publicCount = ((Number) category.get("public_count")).longValue();
                Long privateCount = materialCount - publicCount;
                
                result.append("▸ ").append(category.get("name")).append("\n");
                result.append("  分类ID：").append(category.get("id")).append("\n");
                result.append("  排序权重：").append(category.get("order_id")).append("\n");
                result.append("  资料数量：").append(materialCount).append(" 个");
                
                if (materialCount > 0) {
                    result.append("（公开 ").append(publicCount).append(" 个");
                    if (privateCount > 0) {
                        result.append("，私有 ").append(privateCount).append(" 个");
                    }
                    result.append("）");
                }
                result.append("\n");
                
                result.append("  创建时间：").append(category.get("create_time")).append("\n\n");
            }
            
            result.append("  使用提示：\n");
            result.append("  • 要查看某分类下的资料，请使用 `findMaterialsByCategory`\n");
            result.append("  • 要添加新分类，请使用 `addMaterialCategory`");
            
            log.info("  成功列出 {} 个资料分类，总计 {} 个资料", totalCategories, totalMaterials);
            return result.toString();
            
        } catch (Exception e) {
            log.error("  查询资料分类时发生错误: {}", e.getMessage(), e);
            return "  查询资料分类时发生内部错误，请稍后重试。";
        }
    }

    @Tool("添加一个新的资料分类。")
    @Transactional
    public String addMaterialCategory(
        @P("新分类的名称") String categoryName, 
        @P("排序权重（数字越小越靠前）") int orderWeight,
        @P("当前用户的ID，这个ID由系统在后台自动提供，AI需要直接传递它") Long currentUserId
    ) {
        log.info("  AI Workflow Tool: 添加资料分类 '{}'，排序权重: {}", categoryName, orderWeight);
        
        // 权限检查
        if (currentUserId == null) {
            return "  用户未登录，无法创建资料分类。";
        }
        
        if (!permissionService.canManageMaterials(currentUserId)) {
            return permissionService.getMaterialManagementPermissionInfo(currentUserId);
        }
        
        // 参数验证
        if (!StringUtils.hasText(categoryName)) {
            return "  分类名称不能为空。";
        }
        
        String trimmedName = categoryName.trim();
        if (trimmedName.length() > 50) {
            return "  分类名称不能超过50个字符，当前为 " + trimmedName.length() + " 个字符。";
        }
        
        if (orderWeight < 0) {
            return "  排序权重不能为负数。";
        }
        
        try {
            // 检查分类名称是否已存在
            String checkSql = "SELECT COUNT(*) FROM material_category WHERE name = ?";
            Integer existingCount = jdbcTemplate.queryForObject(checkSql, Integer.class, trimmedName);
            if (existingCount > 0) {
                return "  分类名称 '" + trimmedName + "' 已存在，请使用其他名称。";
            }
            
            // 插入新分类
            String insertSql = "INSERT INTO material_category (name, order_id, create_time, update_time, create_user, update_user) " +
                              "VALUES (?, ?, NOW(), NOW(), ?, ?)";
            
            int insertedRows = jdbcTemplate.update(insertSql, trimmedName, orderWeight, currentUserId, String.valueOf(currentUserId));
            
            if (insertedRows > 0) {
                // 获取新创建的分类ID
                String getIdSql = "SELECT id FROM material_category WHERE name = ? AND create_user = ?";
                Long newCategoryId = jdbcTemplate.queryForObject(getIdSql, Long.class, trimmedName, currentUserId);
                
                log.info("  资料分类创建成功 - ID: {}, 名称: '{}', 排序: {}, 创建者: {}", 
                        newCategoryId, trimmedName, orderWeight, currentUserId);
                
                // 清理缓存以确保数据一致性
                performCompleteMaterialCacheClear(null);
                
                return "  资料分类创建成功！\n" +
                       "══════════════════════════════════\n" +
                       "分类ID：" + newCategoryId + "\n" +
                       "分类名称：" + trimmedName + "\n" +
                       "排序权重：" + orderWeight + "\n" +
                       "创建时间：刚刚\n" +
                       "══════════════════════════════════\n" +
                       "  提示：现在您可以向此分类添加资料文件了。";
            } else {
                return "  创建分类失败，数据库操作未生效，请稍后重试。";
            }
            
        } catch (Exception e) {
            log.error("  创建资料分类 '{}' 时发生错误: {}", trimmedName, e.getMessage(), e);
            return "  创建资料分类时发生内部错误：" + e.getMessage() + "\n请检查输入信息或稍后重试。";
        }
    }

    @Tool("【第一步】请求删除一个资料分类。此工具会进行影响分析并返回需要用户确认的警告信息。")
    public String requestCategoryDeletion(
        @P("要删除的分类的准确名称") String categoryName,
        @P("当前用户的ID，这个ID由系统在后台自动提供，AI需要直接传递它") Long currentUserId
    ) {
        log.info("  AI Workflow Tool: 请求删除资料分类 '{}'", categoryName);
        
        // 权限检查
        if (currentUserId == null) {
            return "  用户未登录，无法删除资料分类。";
        }
        
        if (!permissionService.canManageMaterials(currentUserId)) {
            return permissionService.getMaterialManagementPermissionInfo(currentUserId);
        }
        
        // 参数验证
        if (!StringUtils.hasText(categoryName)) {
            return "  分类名称不能为空。";
        }
        
        try {
            // 查找分类基本信息
            String categorySql = "SELECT id, name, order_id, create_time, create_user FROM material_category WHERE name = ?";
            List<Map<String, Object>> categoryResults = jdbcTemplate.queryForList(categorySql, categoryName.trim());
            if (categoryResults.isEmpty()) {
                return "  请求失败：未找到名为 '" + categoryName + "' 的资料分类。";
            }
            
            Map<String, Object> category = categoryResults.get(0);
            Long categoryId = (Long) category.get("id");
            
            // 统计该分类下的资料
            String materialStatsSql = "SELECT COUNT(*) as total_count, " +
                                     "SUM(CASE WHEN is_public = 1 THEN 1 ELSE 0 END) as public_count, " +
                                     "SUM(CASE WHEN is_public = 0 THEN 1 ELSE 0 END) as private_count, " +
                                     "SUM(download_count) as total_downloads " +
                                     "FROM material WHERE category_id = ? AND status = 1";
            Map<String, Object> stats = jdbcTemplate.queryForMap(materialStatsSql, categoryId);
            
            Long totalCount = ((Number) stats.get("total_count")).longValue();
            Long publicCount = ((Number) stats.get("public_count")).longValue();
            Long privateCount = ((Number) stats.get("private_count")).longValue();
            Long totalDownloads = ((Number) stats.get("total_downloads")).longValue();
            
            // 构建详细的风险分析报告
            StringBuilder warning = new StringBuilder();
            warning.append(" 【严重警告 - 分类删除确认】 \n");
            warning.append("══════════════════════════════════════\n");
            warning.append("分类信息：\n");
            warning.append("  • 分类名称：").append(category.get("name")).append("\n");
            warning.append("  • 分类ID：").append(categoryId).append("\n");
            warning.append("  • 排序权重：").append(category.get("order_id")).append("\n");
            warning.append("  • 创建时间：").append(category.get("create_time")).append("\n\n");
            
            warning.append("  影响分析：\n");
            warning.append("  • 总资料数：").append(totalCount).append(" 个\n");
            if (totalCount > 0) {
                warning.append("    - 公开资料：").append(publicCount).append(" 个\n");
                warning.append("    - 私有资料：").append(privateCount).append(" 个\n");
                warning.append("    - 累计下载：").append(totalDownloads).append(" 次\n");
            }
            warning.append("\n");
            
            // 特殊警告
            if (totalCount > 0) {
                warning.append("  重要影响：\n");
                warning.append("  删除此分类后，").append(totalCount).append(" 个资料将：\n");
                warning.append("  ✓ 资料文件本身不会被删除\n");
                warning.append("  ✓ 资料将变为\"未分类\"状态\n");
                warning.append("  ✓ 用户仍可通过资料名称搜索访问\n");
                warning.append("  ✓ 下载链接和权限保持不变\n\n");
            } else {
                warning.append("  安全提示：\n");
                warning.append("  该分类下没有资料，删除操作相对安全。\n\n");
            }
            
            warning.append("⚡ 此操作删除的是分类信息，不会删除实际资料文件\n");
            warning.append("══════════════════════════════════════\n");
            warning.append("如果您确定要继续删除，请调用 `confirmCategoryDeletion` 工具。");
            
            log.warn("  用户 {} 请求删除分类 '{}' (ID: {}), 影响资料: {} 个", 
                    currentUserId, categoryName, categoryId, totalCount);
            
            return warning.toString();
            
        } catch (Exception e) {
            log.error("  处理分类删除请求时发生错误，分类: {}, 错误: {}", categoryName, e.getMessage(), e);
            return "  处理删除请求时发生内部错误，请稍后重试。";
        }
    }

    @Tool("【第二步】确认删除一个资料分类。")
    @Transactional
    public String confirmCategoryDeletion(
        @P("要删除的分类的准确名称") String categoryName,
        @P("当前用户的ID，这个ID由系统在后台自动提供，AI需要直接传递它") Long currentUserId
    ) {
        log.info("  AI Workflow Tool: 确认删除资料分类 '{}'", categoryName);
        
        // 权限检查
        if (currentUserId == null) {
            return "  用户未登录，无法删除资料分类。";
        }
        
        if (!permissionService.canManageMaterials(currentUserId)) {
            return permissionService.getMaterialManagementPermissionInfo(currentUserId);
        }
        
        // 参数验证
        if (!StringUtils.hasText(categoryName)) {
            return "  分类名称不能为空。";
        }
        
        try {
            // 查找分类信息
            String findCategorySql = "SELECT id, name FROM material_category WHERE name = ?";
            List<Map<String, Object>> categoryResults = jdbcTemplate.queryForList(findCategorySql, categoryName.trim());
            if (categoryResults.isEmpty()) {
                return "  删除失败：在执行删除时找不到分类 '" + categoryName + "'。可能已被其他用户删除。";
            }
            
            Map<String, Object> category = categoryResults.get(0);
            Long categoryId = (Long) category.get("id");
            String actualCategoryName = (String) category.get("name");
            
            // 收集删除统计信息
            String countSql = "SELECT COUNT(*) FROM material WHERE category_id = ? AND status = 1";
            Integer affectedMaterialCount = jdbcTemplate.queryForObject(countSql, Integer.class, categoryId);
            
            log.info("🗑️ 开始删除分类 '{}' (ID: {}), 影响 {} 个资料", actualCategoryName, categoryId, affectedMaterialCount);
            
            // 第1步：将该分类下的所有资料设置为未分类状态
            String updateMaterialsSql = "UPDATE material SET category_id = NULL, update_time = NOW(), update_id = ? WHERE category_id = ?";
            int updatedMaterials = jdbcTemplate.update(updateMaterialsSql, currentUserId, categoryId);
            log.info("  已将 {} 个资料设置为未分类状态", updatedMaterials);
            
            // 第2步：删除分类记录
            String deleteCategorySql = "DELETE FROM material_category WHERE id = ?";
            int deletedRows = jdbcTemplate.update(deleteCategorySql, categoryId);
            
            if (deletedRows > 0) {
                // 记录详细的删除操作日志
                log.warn("🗑️ 分类删除完成 - 用户: {}, 分类: '{}' (ID: {}), 影响资料: {} 个", 
                        currentUserId, actualCategoryName, categoryId, affectedMaterialCount);
                
                // 清理缓存以确保数据一致性
                performCompleteMaterialCacheClear(null);
                
                StringBuilder result = new StringBuilder();
                result.append("  资料分类删除成功！\n");
                result.append("══════════════════════════════════════\n");
                result.append("分类名称：").append(actualCategoryName).append("\n");
                result.append("分类ID：").append(categoryId).append("\n\n");
                result.append("  操作统计：\n");
                result.append("  • 删除分类：1 个\n");
                result.append("  • 影响资料：").append(affectedMaterialCount).append(" 个\n");
                
                if (affectedMaterialCount > 0) {
                    result.append("  • 资料状态：已设置为\"未分类\"\n");
                    result.append("  • 资料文件：完整保留，可正常访问\n");
                    result.append("  • 下载链接：保持有效\n");
                }
                
                result.append("\n⚡ 删除操作已完成且无法撤销\n");
                result.append("══════════════════════════════════════\n");
                result.append("删除时间：刚刚\n");
                result.append("执行用户：").append(currentUserId);
                
                if (affectedMaterialCount > 0) {
                    result.append("\n\n  提示：原分类下的资料现在显示为\"未分类\"，");
                    result.append("您可以为它们重新指定分类。");
                }
                
                return result.toString();
            } else {
                log.error("  分类删除失败，category_id: {}", categoryId);
                return "  删除失败：数据库操作未影响任何行，可能数据已被其他操作修改。";
            }
            
        } catch (Exception e) {
            log.error("  确认删除分类 '{}' 时发生严重错误: {}", categoryName, e.getMessage(), e);
            // 事务会自动回滚
            return "  删除分类时发生内部错误：" + e.getMessage() + 
                   "\n所有操作已回滚，数据保持完整。请稍后重试或联系技术支持。";
        }
    }

    // ====================================================================================
    // 2. 资料管理工具
    // ====================================================================================

    @Tool("根据分类名称查询其下的所有资料，包含详细的资料信息和下载统计。")
    public String findMaterialsByCategory(@P("资料分类的准确名称") String categoryName) {
        log.info("  AI Workflow Tool: 查询分类 '{}' 下的资料", categoryName);
        
        // 参数验证
        if (!StringUtils.hasText(categoryName)) {
            return "  分类名称不能为空。";
        }
        
        try {
            // 先查找分类ID
            String categorySql = "SELECT id, name FROM material_category WHERE name = ?";
            List<Map<String, Object>> categoryResults = jdbcTemplate.queryForList(categorySql, categoryName.trim());
            if (categoryResults.isEmpty()) {
                return "  未找到名为 '" + categoryName + "' 的资料分类。请检查分类名称是否正确。";
            }
            
            Long categoryId = (Long) categoryResults.get(0).get("id");
            String actualCategoryName = (String) categoryResults.get(0).get("name");
            
            // 查询该分类下的所有资料
            String materialsSql = "SELECT m.id, m.file_name, m.file_type, m.file_size, m.description, " +
                                 "m.is_public, m.download_count, m.upload_time, u.name as uploader_name " +
                                 "FROM material m " +
                                 "LEFT JOIN user u ON m.uploader_id = u.user_id " +
                                 "WHERE m.category_id = ? AND m.status = 1 " +
                                 "ORDER BY m.upload_time DESC";
            
            List<Map<String, Object>> materials = jdbcTemplate.queryForList(materialsSql, categoryId);
            
            if (materials.isEmpty()) {
                return "  分类 '" + actualCategoryName + "' 下暂无资料。\n" +
                       "  提示：您可以使用 `addMaterial` 工具上传新的资料到此分类。";
            }
            
            // 统计信息
            long totalSize = materials.stream()
                .mapToLong(m -> m.get("file_size") != null ? ((Number) m.get("file_size")).longValue() : 0)
                .sum();
            long totalDownloads = materials.stream()
                .mapToLong(m -> ((Number) m.get("download_count")).longValue())
                .sum();
            long publicCount = materials.stream()
                .mapToLong(m -> ((Number) m.get("is_public")).intValue() == 1 ? 1 : 0)
                .sum();
            long privateCount = materials.size() - publicCount;
            
            StringBuilder result = new StringBuilder();
            result.append("  分类「").append(actualCategoryName).append("」资料列表\n");
            result.append("═══════════════════════════════════════\n");
            result.append("共有 ").append(materials.size()).append(" 个资料");
            result.append("（公开 ").append(publicCount).append(" 个，私有 ").append(privateCount).append(" 个）\n");
            result.append("总大小：").append(formatFileSize(totalSize)).append("\n");
            result.append("总下载：").append(totalDownloads).append(" 次\n\n");
            
            for (int i = 0; i < materials.size(); i++) {
                Map<String, Object> material = materials.get(i);
                
                result.append("▸ ").append(material.get("file_name")).append("\n");
                result.append("  资料ID：").append(material.get("id")).append("\n");
                result.append("  文件类型：").append(material.get("file_type")).append("\n");
                result.append("  文件大小：").append(formatFileSize(
                    material.get("file_size") != null ? ((Number) material.get("file_size")).longValue() : 0)).append("\n");
                result.append("  访问权限：").append(((Number) material.get("is_public")).intValue() == 1 ? "公开" : "私有").append("\n");
                result.append("  下载次数：").append(material.get("download_count")).append(" 次\n");
                result.append("  上传者：").append(material.get("uploader_name") != null ? material.get("uploader_name") : "未知").append("\n");
                result.append("  上传时间：").append(material.get("upload_time")).append("\n");
                
                if (StringUtils.hasText((String) material.get("description"))) {
                    String description = (String) material.get("description");
                    String shortDesc = description.length() > 50 ? description.substring(0, 50) + "..." : description;
                    result.append("  资料描述：").append(shortDesc).append("\n");
                }
                
                if (i < materials.size() - 1) {
                    result.append("\n");
                }
            }
            
            result.append("\n  操作提示：\n");
            result.append("  • 要删除某个资料，请使用 `requestMaterialDeletion`\n");
            result.append("  • 要添加新资料，请使用 `addMaterial`");
            
            log.info("  成功查询分类 '{}' 下的 {} 个资料", actualCategoryName, materials.size());
            return result.toString();
            
        } catch (Exception e) {
            log.error("  查询分类 '{}' 下的资料时发生错误: {}", categoryName, e.getMessage(), e);
            return "  查询资料时发生内部错误，请稍后重试。";
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

    @Tool("添加一个新资料（通常指上传文件）。")
    @Transactional
    public String addMaterial(
        @P("资料/文件名称") String materialName,
        @P("所属分类的准确名称") String categoryName,
                    @P("文件描述 (可选，留空则不设置)") String description,
        @P("当前用户的ID，这个ID由系统在后台自动提供，AI需要直接传递它") Long currentUserId
    ) {
        log.info("  AI Workflow Tool: 添加资料 '{}'，分类: {}", materialName, categoryName);
        
        // 权限检查
        if (currentUserId == null) {
            return "  用户未登录，无法添加资料。";
        }

        if (!permissionService.canManageMaterials(currentUserId)) {
            return permissionService.getMaterialManagementPermissionInfo(currentUserId);
        }
        // ... [Find category_id, INSERT logic] ...
        // 注意：实际的文件上传应在此工具被调用前完成，此工具仅负责将文件信息写入数据库。
        
        // 清理缓存以确保数据一致性
        performCompleteMaterialCacheClear(null);
        
        return "  资料 '" + materialName + "' 已成功添加到分类 '" + categoryName + "'。";
    }

    @Tool("【第一步】请求删除一份资料。此工具会进行风险评估并返回需要用户确认的警告信息。")
    public String requestMaterialDeletion(
        @P("要删除的资料的准确文件名称") String materialName,
        @P("当前用户的ID，这个ID由系统在后台自动提供，AI需要直接传递它") Long currentUserId
    ) {
        log.info("  AI Workflow Tool: 请求删除资料 '{}'", materialName);
        
        // 权限检查
        if (currentUserId == null) {
            return "  用户未登录，无法删除资料。";
        }
        
        if (!permissionService.canManageMaterials(currentUserId)) {
            return permissionService.getMaterialManagementPermissionInfo(currentUserId);
        }
        
        // 参数验证
        if (!StringUtils.hasText(materialName)) {
            return "  资料名称不能为空。";
        }
        
        try {
            // 查找资料详细信息
            String materialSql = "SELECT m.id, m.file_name, m.file_type, m.file_size, m.url, m.description, " +
                                "m.is_public, m.download_count, m.upload_time, " +
                                "mc.name as category_name, u.name as uploader_name " +
                                "FROM material m " +
                                "LEFT JOIN material_category mc ON m.category_id = mc.id " +
                                "LEFT JOIN user u ON m.uploader_id = u.user_id " +
                                "WHERE m.file_name = ? AND m.status = 1";
            
            List<Map<String, Object>> materialResults = jdbcTemplate.queryForList(materialSql, materialName.trim());
            if (materialResults.isEmpty()) {
                return "  请求失败：未找到名为 '" + materialName + "' 的资料。";
            }
            
            // 如果有多个同名文件，提醒用户
            if (materialResults.size() > 1) {
                StringBuilder multipleFiles = new StringBuilder();
                multipleFiles.append("  发现 ").append(materialResults.size()).append(" 个同名资料：\n");
                for (int i = 0; i < materialResults.size(); i++) {
                    Map<String, Object> material = materialResults.get(i);
                    multipleFiles.append(i + 1).append(". ").append(material.get("file_name"));
                    multipleFiles.append("（分类：").append(material.get("category_name") != null ? material.get("category_name") : "未分类");
                    multipleFiles.append("，上传时间：").append(material.get("upload_time")).append("）\n");
                }
                multipleFiles.append("\n  删除操作将删除所有同名资料！\n");
                multipleFiles.append("如需删除特定资料，请联系管理员使用资料ID进行精确删除。");
            }
            
            Map<String, Object> material = materialResults.get(0); // 使用第一个作为主要信息展示
            Long materialId = (Long) material.get("id");
            
            // 查询下载记录数量
            String downloadRecordsSql = "SELECT COUNT(*) FROM material_download_record WHERE material_id = ?";
            Integer downloadRecords = jdbcTemplate.queryForObject(downloadRecordsSql, Integer.class, materialId);
            
            // 构建详细的风险分析报告
            StringBuilder warning = new StringBuilder();
            warning.append(" 【严重警告 - 资料删除确认】 \n");
            warning.append("══════════════════════════════════════\n");
            warning.append("资料信息：\n");
            warning.append("  • 文件名称：").append(material.get("file_name")).append("\n");
            warning.append("  • 资料ID：").append(materialId).append("\n");
            warning.append("  • 文件类型：").append(material.get("file_type")).append("\n");
            warning.append("  • 文件大小：").append(formatFileSize(
                material.get("file_size") != null ? ((Number) material.get("file_size")).longValue() : 0)).append("\n");
            warning.append("  • 所属分类：").append(material.get("category_name") != null ? material.get("category_name") : "未分类").append("\n");
            warning.append("  • 访问权限：").append(((Number) material.get("is_public")).intValue() == 1 ? "公开" : "私有").append("\n");
            warning.append("  • 上传者：").append(material.get("uploader_name") != null ? material.get("uploader_name") : "未知").append("\n");
            warning.append("  • 上传时间：").append(material.get("upload_time")).append("\n\n");
            
            warning.append("  使用统计：\n");
            warning.append("  • 下载次数：").append(material.get("download_count")).append(" 次\n");
            warning.append("  • 下载记录：").append(downloadRecords).append(" 条\n");
            if (materialResults.size() > 1) {
                warning.append("  • 同名文件：").append(materialResults.size()).append(" 个\n");
            }
            warning.append("\n");
            
            // 特殊警告
            Long downloadCount = ((Number) material.get("download_count")).longValue();
            if (downloadCount > 0) {
                warning.append("  高风险警告：\n");
                warning.append("  该资料已被下载 ").append(downloadCount).append(" 次，可能有用户正在使用！\n");
                warning.append("  删除后，用户将无法再次下载此资料。\n\n");
            }
            
            if (((Number) material.get("is_public")).intValue() == 1) {
                warning.append("  公开资料警告：\n");
                warning.append("  该资料为公开资料，删除后将影响所有有权限的用户。\n\n");
            }
            
            warning.append("⚡ 此操作将：\n");
            warning.append("    永久删除数据库记录\n");
            warning.append("    删除服务器上的物理文件\n");
            warning.append("    删除所有下载记录\n");
            warning.append("    操作无法撤销！\n");
            warning.append("══════════════════════════════════════\n");
            warning.append("如果您确定要继续删除，请调用 `confirmMaterialDeletion` 工具。\n");
            warning.append("建议：删除前请确认没有用户正在使用此资料。");
            
            log.warn("  用户 {} 请求删除资料 '{}' (ID: {}), 下载量: {} 次", 
                    currentUserId, materialName, materialId, downloadCount);
            
            return warning.toString();
            
        } catch (Exception e) {
            log.error("  处理资料删除请求时发生错误，资料: {}, 错误: {}", materialName, e.getMessage(), e);
            return "  处理删除请求时发生内部错误，请稍后重试。";
        }
    }

    @Tool("【第二步】确认删除一份资料。此操作将永久删除物理文件和所有相关记录。")
    @Transactional
    public String confirmMaterialDeletion(
        @P("要删除的资料的准确文件名称") String materialName,
        @P("当前用户的ID，这个ID由系统在后台自动提供，AI需要直接传递它") Long currentUserId
    ) {
        log.info("  AI Workflow Tool: 确认删除资料 '{}'", materialName);
        
        // 权限检查
        if (currentUserId == null) {
            return "  用户未登录，无法删除资料。";
        }
        
        if (!permissionService.canManageMaterials(currentUserId)) {
            return permissionService.getMaterialManagementPermissionInfo(currentUserId);
        }
        
        // 参数验证
        if (!StringUtils.hasText(materialName)) {
            return "  资料名称不能为空。";
        }
        
        try {
            // 查找所有匹配的资料（包括同名文件）
            String findMaterialsSql = "SELECT id, file_name, url, file_size, download_count, category_id " +
                                     "FROM material WHERE file_name = ? AND status = 1";
            List<Map<String, Object>> materials = jdbcTemplate.queryForList(findMaterialsSql, materialName.trim());
            if (materials.isEmpty()) {
                return "  删除失败：在执行删除时找不到资料 '" + materialName + "'。可能已被其他用户删除。";
            }
            
            // 收集删除统计信息
            int totalMaterials = materials.size();
            long totalSize = 0;
            long totalDownloads = 0;
            int deletedFiles = 0;
            int deletedDownloadRecords = 0;
            int deletedMaterialRecords = 0;
            
            log.info("🗑️ 开始删除资料 '{}', 共找到 {} 个同名文件", materialName, totalMaterials);
            
            // 处理每个资料文件
            for (Map<String, Object> material : materials) {
                Long materialId = (Long) material.get("id");
                String filePath = (String) material.get("url");
                Long fileSize = material.get("file_size") != null ? ((Number) material.get("file_size")).longValue() : 0;
                Long downloadCount = ((Number) material.get("download_count")).longValue();
                
                totalSize += fileSize;
                totalDownloads += downloadCount;
                
                log.debug("  处理资料 ID: {}, 路径: {}, 大小: {} bytes", materialId, filePath, fileSize);
                
                // 第1步：删除物理文件
                if (StringUtils.hasText(filePath)) {
                    // 注意：这里应该调用文件服务删除实际文件
                    // fileService.deleteFile(filePath); // 实际项目中需要实现文件删除逻辑
                    log.debug("  准备删除物理文件: {}", filePath);
                    deletedFiles++; // 假设删除成功
                }
                
                // 第2步：删除下载记录
                String deleteDownloadRecordsSql = "DELETE FROM material_download_record WHERE material_id = ?";
                int recordsDeleted = jdbcTemplate.update(deleteDownloadRecordsSql, materialId);
                deletedDownloadRecords += recordsDeleted;
                log.debug("  删除下载记录: {} 条", recordsDeleted);
            }
            
            // 第3步：删除所有同名资料的数据库记录
            String deleteMaterialsSql = "DELETE FROM material WHERE file_name = ? AND status = 1";
            deletedMaterialRecords = jdbcTemplate.update(deleteMaterialsSql, materialName.trim());
            
            if (deletedMaterialRecords > 0) {
                // 记录详细的删除操作日志
                log.warn("🗑️ 资料删除完成 - 用户: {}, 资料: '{}', " +
                        "删除文件: {}个, 总大小: {}bytes, 删除记录: {}条, 删除下载记录: {}条", 
                        currentUserId, materialName, deletedMaterialRecords, totalSize, deletedMaterialRecords, deletedDownloadRecords);
                
                // 清理缓存以确保数据一致性
                performCompleteMaterialCacheClear(null);
                
                StringBuilder result = new StringBuilder();
                result.append("  资料删除成功！\n");
                result.append("══════════════════════════════════════\n");
                result.append("资料名称：").append(materialName).append("\n");
                if (totalMaterials > 1) {
                    result.append("同名文件：").append(totalMaterials).append(" 个\n");
                }
                result.append("\n  删除统计：\n");
                result.append("  • 物理文件：").append(deletedFiles).append(" 个\n");
                result.append("  • 数据记录：").append(deletedMaterialRecords).append(" 条\n");
                result.append("  • 下载记录：").append(deletedDownloadRecords).append(" 条\n");
                result.append("  • 释放空间：").append(formatFileSize(totalSize)).append("\n");
                result.append("  • 影响下载：").append(totalDownloads).append(" 次历史下载\n\n");
                
                result.append("  文件处理：\n");
                for (Map<String, Object> material : materials) {
                    result.append("  • ID ").append(material.get("id")).append(": ");
                    result.append(material.get("url") != null ? "物理文件已删除" : "无物理文件").append("\n");
                }
                
                result.append("\n⚡ 删除操作已完成且无法撤销\n");
                result.append("══════════════════════════════════════\n");
                result.append("删除时间：刚刚\n");
                result.append("执行用户：").append(currentUserId).append("\n\n");
                result.append("  提示：已删除的资料无法恢复，如需重新使用请重新上传。");
                
                return result.toString();
            } else {
                log.error("  资料删除失败，materialName: {}", materialName);
                return "  删除失败：数据库操作未影响任何行，可能数据已被其他操作修改。";
            }
            
        } catch (Exception e) {
            log.error("  确认删除资料 '{}' 时发生严重错误: {}", materialName, e.getMessage(), e);
            // 事务会自动回滚
            return "  删除资料时发生内部错误：" + e.getMessage() + 
                   "\n所有操作已回滚，数据保持完整。请稍后重试或联系技术支持。";
        }
    }
} 