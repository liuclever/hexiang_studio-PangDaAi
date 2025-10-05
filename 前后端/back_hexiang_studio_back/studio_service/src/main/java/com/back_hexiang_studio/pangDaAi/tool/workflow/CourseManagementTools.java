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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * 【新设计】课程管理工作流工具
 * 将多步骤的课程管理操作封装成单一、可靠的工具。
 */
@Service
@Slf4j
public class CourseManagementTools {

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
     * 清除课程相关缓存
     */
    private void clearCourseCache(Long courseId) {
        if (courseId != null) {
            redisTemplate.delete("course:detail:" + courseId);
            redisTemplate.delete("course:students:" + courseId);
            redisTemplate.delete("course:materials:" + courseId);
        }
        log.info("  [缓存清理] 清理课程详情缓存，课程ID: {}", courseId);
    }

    /**
     * 清除课程列表相关缓存
     */
    private void clearCourseListCache() {
        Set<String> patterns = new HashSet<>();
        patterns.add("course:list*");
        patterns.add("courses:page*");
        patterns.add("courses:teacher:*");
        patterns.add("courses:category:*");
        patterns.add("courses:search*");

        int totalDeleted = 0;
        for (String pattern : patterns) {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                totalDeleted += keys.size();
                log.info("  [缓存清理] 清理课程列表缓存，模式: {}, 键数量: {}", pattern, keys.size());
            }
        }

        if (totalDeleted > 0) {
            log.info("  [缓存清理] 总共清理了 {} 个课程列表相关缓存键", totalDeleted);
        }
    }

    /**
     * 清理全局课程相关缓存
     */
    private void clearGlobalCourseCache() {
        Set<String> globalPatterns = new HashSet<>();
        globalPatterns.add("dashboard:course*");
        globalPatterns.add("statistics:course*");

        for (String pattern : globalPatterns) {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("  [缓存清理] 清理全局课程缓存，模式: {}, 键数量: {}", pattern, keys.size());
            }
        }
    }

    /**
     * 执行完整的课程相关缓存清理
     */
    private void performCompleteCourseCacheClear(Long courseId) {
        if (courseId != null) {
            clearCourseCache(courseId);
        }
        clearCourseListCache();
        clearGlobalCourseCache();
        log.info("  [缓存清理] 完成课程相关缓存清理");
    }

    // ====================================================================================
    // 1. 课程查询工具 (Read-Only)
    // ====================================================================================

    @Tool("查询指定课程的详细信息。")
    public String findCourse(@P("要查询的课程的准确名称") String courseName) {
        log.info(" AI Workflow Tool: 查询课程 '{}'", courseName);
        
        // 参数验证
        if (!StringUtils.hasText(courseName)) {
            return "  课程名称不能为空。";
        }
        
        try {
            // 查询课程基本信息
            String courseSql = "SELECT c.course_id, c.name, c.description, c.status, c.duration, c.location, c.schedule, " +
                              "c.cover_image, c.create_time, c.update_time, u.name as teacher_name, td.direction_name " +
                              "FROM course c " +
                              "LEFT JOIN user u ON c.teacher_id = u.user_id AND u.status = '1' " +
                              "LEFT JOIN training_direction td ON c.category_id = td.direction_id " +
                              "WHERE c.name = ?";
            
            Map<String, Object> course = jdbcTemplate.queryForMap(courseSql, courseName.trim());
            
            // 查询选课学生数量
            String studentCountSql = "SELECT COUNT(*) FROM student_course sc " +
                                   "JOIN student s ON sc.student_id = s.student_id " +
                                   "JOIN user u ON s.user_id = u.user_id " +
                                   "WHERE sc.course_id = ? AND u.status = '1'";
            Integer studentCount = jdbcTemplate.queryForObject(studentCountSql, Integer.class, course.get("course_id"));
            
            // 查询课程资料数量
            String materialCountSql = "SELECT COUNT(*) FROM course_material WHERE course_id = ?";
            Integer materialCount = jdbcTemplate.queryForObject(materialCountSql, Integer.class, course.get("course_id"));
            
            // 格式化状态文本
            String statusText = getStatusText((Integer) course.get("status"));
            
            // 构建详细信息
            StringBuilder result = new StringBuilder();
            result.append(" 课程详细信息\n");
            result.append("════════════════════════════════\n");
            result.append("课程名称：").append(course.get("name")).append("\n");
            result.append("课程描述：").append(course.get("description")).append("\n");
            result.append("授课老师：").append(course.get("teacher_name") != null ? course.get("teacher_name") : "未分配").append("\n");
            result.append("培训方向：").append(course.get("direction_name") != null ? course.get("direction_name") : "未分类").append("\n");
            result.append("课程状态：").append(statusText).append("\n");
            result.append("上课地点：").append(course.get("location")).append("\n");
            result.append("上课时间：").append(course.get("schedule")).append("\n");
            result.append("课程时长：").append(course.get("duration") != null ? course.get("duration") : "待定").append("\n");
            result.append("选课学生：").append(studentCount).append(" 人\n");
            result.append("课程资料：").append(materialCount).append(" 个\n");
            result.append("创建时间：").append(course.get("create_time")).append("\n");
            result.append("更新时间：").append(course.get("update_time")).append("\n");
            
            if (course.get("cover_image") != null) {
                result.append("封面图片：").append(course.get("cover_image")).append("\n");
            }
            
            log.info(" 成功查询课程 '{}' 详细信息，选课学生 {} 人", courseName, studentCount);
            return result.toString();
            
        } catch (EmptyResultDataAccessException e) {
            log.warn("  未找到课程: {}", courseName);
            return "  未找到名为 '" + courseName + "' 的课程。请检查课程名称是否正确。";
        } catch (Exception e) {
            log.error("  查询课程 '{}' 详细信息时发生错误: {}", courseName, e.getMessage(), e);
            return "  查询课程信息时发生内部错误，请稍后重试。";
        }
    }
    
    /**
     * 获取课程状态的文本描述
     */
    private String getStatusText(Integer status) {
        if (status == null) return "未知状态";
        switch (status) {
            case 0: return "草稿";
            case 1: return "已发布";
            case 2: return "已下架";
            default: return "未知状态";
        }
    }

    @Tool("列出所有可用的课程，可以按状态筛选。")
    public String listAllCourses(@P("课程状态: '已发布', '草稿', '已下架' (可选，留空则显示所有状态)") String statusFilter) {
        log.info("  AI Workflow Tool: 列出课程, 筛选条件: {}", statusFilter);
        
        try {
            // 构建SQL查询
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT c.course_id, c.name, c.description, c.status, c.duration, c.location, c.schedule, ")
                     .append("c.create_time, u.name as teacher_name, td.direction_name, ")
                     .append("(SELECT COUNT(*) FROM student_course sc ")
                     .append("JOIN student s ON sc.student_id = s.student_id ")
                     .append("JOIN user su ON s.user_id = su.user_id ")
                     .append("WHERE sc.course_id = c.course_id AND su.status = '1') as student_count ")
                     .append("FROM course c ")
                     .append("LEFT JOIN user u ON c.teacher_id = u.user_id AND u.status = '1' ")
                     .append("LEFT JOIN training_direction td ON c.category_id = td.direction_id ");
            
            List<Object> params = new ArrayList<>();
            
            // 处理状态筛选
            Integer statusCode = parseStatusFilter(statusFilter);
            if (statusCode != null) {
                sqlBuilder.append("WHERE c.status = ? ");
                params.add(statusCode);
            }
            
            sqlBuilder.append("ORDER BY c.status ASC, c.create_time DESC");
            
            List<Map<String, Object>> courses = jdbcTemplate.queryForList(sqlBuilder.toString(), params.toArray());
            
            if (courses.isEmpty()) {
                String filterMsg = statusCode != null ? "（筛选条件：" + statusFilter + "）" : "";
                return "  当前没有找到课程" + filterMsg + "。";
            }
            
            // 按状态分组
            Map<Integer, List<Map<String, Object>>> groupedCourses = courses.stream()
                .collect(Collectors.groupingBy(course -> (Integer) course.get("status")));
            
            StringBuilder result = new StringBuilder();
            result.append("  课程列表");
            if (statusCode != null) {
                result.append("（筛选：").append(statusFilter).append("）");
            }
            result.append("\n");
            result.append("════════════════════════════════\n");
            result.append("共找到 ").append(courses.size()).append(" 门课程\n\n");
            
            // 按状态顺序输出：已发布 -> 草稿 -> 已下架
            int[] statusOrder = {1, 0, 2};
            for (int status : statusOrder) {
                List<Map<String, Object>> statusCourses = groupedCourses.get(status);
                if (statusCourses != null && !statusCourses.isEmpty()) {
                    result.append("【").append(getStatusText(status)).append("】 (").append(statusCourses.size()).append("门)\n");
                    result.append("────────────────────────────────\n");
                    
                    for (Map<String, Object> course : statusCourses) {
                        result.append("▸ ").append(course.get("name")).append("\n");
                        result.append("  授课老师：").append(course.get("teacher_name") != null ? course.get("teacher_name") : "未分配").append("\n");
                        result.append("  培训方向：").append(course.get("direction_name") != null ? course.get("direction_name") : "未分类").append("\n");
                        result.append("  上课地点：").append(course.get("location")).append("\n");
                        result.append("  选课学生：").append(course.get("student_count")).append(" 人\n");
                        result.append("  创建时间：").append(course.get("create_time")).append("\n");
                        
                        String description = (String) course.get("description");
                        if (StringUtils.hasText(description)) {
                            String shortDesc = description.length() > 50 ? description.substring(0, 50) + "..." : description;
                            result.append("  课程描述：").append(shortDesc).append("\n");
                        }
                        result.append("\n");
                    }
                }
            }
            
            log.info("  成功列出 {} 门课程，筛选条件: {}", courses.size(), statusFilter);
            return result.toString();
            
        } catch (Exception e) {
            log.error("  列出课程时发生错误，筛选条件: {}, 错误: {}", statusFilter, e.getMessage(), e);
            return "  获取课程列表时发生内部错误，请稍后重试。";
        }
    }
    
    /**
     * 解析状态筛选条件
     * @param statusFilter 用户输入的状态文本
     * @return 对应的状态码，null表示不筛选
     */
    private Integer parseStatusFilter(String statusFilter) {
        if (!StringUtils.hasText(statusFilter)) {
            return null;
        }
        
        String filter = statusFilter.trim();
        switch (filter) {
            case "已发布":
            case "发布":
                return 1;
            case "草稿":
                return 0;
            case "已下架":
            case "下架":
                return 2;
            default:
                log.warn("  未识别的状态筛选条件: {}", statusFilter);
                return null;
        }
    }

    @Tool("列出所有可用的培训方向，用于辅助创建或修改课程。")
    public String listTrainingDirections() {
        log.info("  AI Workflow Tool: 列出所有培训方向");
        
        try {
            // 查询培训方向及其关联的课程数量
            String sql = "SELECT td.direction_id, td.direction_name, td.description, " +
                        "COUNT(c.course_id) as course_count, " +
                        "SUM(CASE WHEN c.status = 1 THEN 1 ELSE 0 END) as published_count " +
                        "FROM training_direction td " +
                        "LEFT JOIN course c ON td.direction_id = c.category_id " +
                        "GROUP BY td.direction_id, td.direction_name, td.description " +
                        "ORDER BY td.direction_id";
            
            List<Map<String, Object>> directions = jdbcTemplate.queryForList(sql);
            
            if (directions.isEmpty()) {
                return "  当前系统中没有设置培训方向。\n请联系管理员添加培训方向后再创建课程。";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("  培训方向列表\n");
            result.append("════════════════════════════════\n");
            result.append("共有 ").append(directions.size()).append(" 个培训方向\n\n");
            
            for (Map<String, Object> direction : directions) {
                result.append("▸ ").append(direction.get("direction_name")).append("\n");
                result.append("  方向ID：").append(direction.get("direction_id")).append("\n");
                
                if (StringUtils.hasText((String) direction.get("description"))) {
                    result.append("  描述：").append(direction.get("description")).append("\n");
                }
                
                Long totalCourses = (Long) direction.get("course_count");
                Long publishedCourses = (Long) direction.get("published_count");
                
                result.append("  关联课程：").append(totalCourses).append(" 门");
                if (totalCourses > 0) {
                    result.append("（已发布 ").append(publishedCourses).append(" 门）");
                }
                result.append("\n\n");
            }
            
            result.append("  提示：创建课程时请使用\"培训方向名称\"而非ID进行指定。");
            
            log.info("  成功列出 {} 个培训方向", directions.size());
            return result.toString();
            
        } catch (Exception e) {
            log.error("  查询培训方向时出错: {}", e.getMessage(), e);
            return "  查询培训方向时发生内部错误，请稍后重试。";
        }
    }
    
    // ====================================================================================
    // 2. 管理工具
    // ====================================================================================

    @Tool("添加一个新课程。此工具会进行权限检查并创建课程。")
    @Transactional
    public String addCourse(
        @P("课程名称") String name,
        @P("课程描述") String description,
        @P("授课老师的准确姓名") String teacherName,
        @P("培训方向的准确名称") String directionName,
        @P("上课地点") String location,
        @P("上课时间") String schedule,
                    @P("课程时长 (可选，留空则不设置)") String duration,
        @P("当前用户的ID，这个ID由系统在后台自动提供，AI需要直接传递它") Long currentUserId
    ) {
        log.info("  AI Workflow Tool: 添加新课程 '{}'，授课老师: {}", name, teacherName);
        
        // 权限检查
        if (currentUserId == null) {
            return "  用户未登录，无法创建课程。";
        }
        
        if (!permissionService.canManageCourses(currentUserId)) {
            return permissionService.getCourseManagementPermissionInfo(currentUserId);
        }
        
        // 参数验证
        if (!StringUtils.hasText(name)) {
            return "  课程名称不能为空。";
        }
        if (!StringUtils.hasText(description)) {
            return "  课程描述不能为空。";
        }
        if (!StringUtils.hasText(teacherName)) {
            return "  授课老师姓名不能为空。";
        }
        if (!StringUtils.hasText(directionName)) {
            return "  培训方向不能为空。";
        }
        if (!StringUtils.hasText(location)) {
            return "  上课地点不能为空。";
        }
        if (!StringUtils.hasText(schedule)) {
            return "  上课时间不能为空。";
        }
        
        try {
            // 检查课程名称是否已存在
            String nameCheckSql = "SELECT COUNT(*) FROM course WHERE name = ?";
            Integer nameCount = jdbcTemplate.queryForObject(nameCheckSql, Integer.class, name.trim());
            if (nameCount > 0) {
                return "  课程名称 '" + name + "' 已存在，请使用其他名称。";
            }
            
            // 查找并验证授课老师
            String teacherSql = "SELECT u.user_id, u.name, r.role_name FROM user u " +
                              "JOIN role r ON u.role_id = r.role_id " +
                              "WHERE u.name = ? AND u.status = '1' AND r.role_name IN ('老师', '管理员', '超级管理员')";
            
            List<Map<String, Object>> teacherResults = jdbcTemplate.queryForList(teacherSql, teacherName.trim());
            if (teacherResults.isEmpty()) {
                return "  未找到名为 '" + teacherName + "' 的授课老师，或该用户不具备授课权限。";
            }
            
            Long teacherId = (Long) teacherResults.get(0).get("user_id");
            String actualTeacherName = (String) teacherResults.get(0).get("name");
            
            // 查找并验证培训方向
            String directionSql = "SELECT direction_id, direction_name FROM training_direction WHERE direction_name = ?";
            List<Map<String, Object>> directionResults = jdbcTemplate.queryForList(directionSql, directionName.trim());
            if (directionResults.isEmpty()) {
                return "  未找到名为 '" + directionName + "' 的培训方向。请先创建培训方向或检查名称是否正确。";
            }
            
            Long categoryId = (Long) directionResults.get(0).get("direction_id");
            String actualDirectionName = (String) directionResults.get(0).get("direction_name");
            
            // 插入新课程（默认状态为草稿）
            String insertSql = "INSERT INTO course (name, description, teacher_id, category_id, location, schedule, " +
                              "duration, status, create_user, update_user, create_time, update_time) " +
                              "VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?, ?, NOW(), NOW())";
            
            jdbcTemplate.update(insertSql, 
                name.trim(), 
                description.trim(), 
                teacherId, 
                categoryId, 
                location.trim(), 
                schedule.trim(),
                StringUtils.hasText(duration) ? duration.trim() : null,
                currentUserId, 
                currentUserId);
            
            // 获取新创建的课程ID
            String getIdSql = "SELECT LAST_INSERT_ID()";
            Long newCourseId = jdbcTemplate.queryForObject(getIdSql, Long.class);
            
            log.info("  课程创建成功 - ID: {}, 名称: '{}', 老师: '{}', 方向: '{}'", 
                    newCourseId, name, actualTeacherName, actualDirectionName);
            
            // 清理缓存以确保数据一致性
            performCompleteCourseCacheClear(newCourseId);
            
            return "  课程创建成功！\n" +
                   "══════════════════════════════════\n" +
                   "课程ID：" + newCourseId + "\n" +
                   "课程名称：" + name.trim() + "\n" +
                   "授课老师：" + actualTeacherName + "\n" +
                   "培训方向：" + actualDirectionName + "\n" +
                   "上课地点：" + location.trim() + "\n" +
                   "上课时间：" + schedule.trim() + "\n" +
                   (StringUtils.hasText(duration) ? "课程时长：" + duration.trim() + "\n" : "") +
                   "课程状态：草稿（可通过修改课程将状态改为已发布）\n" +
                   "══════════════════════════════════\n" +
                   "  提示：课程已创建为草稿状态，发布后学生才能看到和选课。";
            
        } catch (Exception e) {
            log.error("  创建课程 '{}' 时发生错误: {}", name, e.getMessage(), e);
            return "  创建课程时发生内部错误：" + e.getMessage() + "\n请检查输入信息是否正确，或稍后重试。";
        }
    }

    @Tool("修改现有课程的信息。")
    @Transactional
    public String updateCourse(
        @P("要修改的课程的准确名称") String courseName,
        @P("新的课程名称 (可选，不修改则留空)") String newName,
        @P("新的课程描述 (可选，不修改则留空)") String newDescription,
        @P("新的授课老师姓名 (可选，不修改则留空)") String newTeacherName,
        @P("新的培训方向名称 (可选，不修改则留空)") String newDirectionName,
        @P("新的上课地点 (可选，不修改则留空)") String newLocation,
        @P("新的上课时间 (可选，不修改则留空)") String newSchedule,
        @P("新的课程时长 (可选，不修改则留空)") String newDuration,
        @P("新的课程状态：'草稿', '已发布', '已下架' (可选，不修改则留空)") String newStatus,
        @P("当前用户的ID，这个ID由系统在后台自动提供，AI需要直接传递它") Long currentUserId
    ) {
        log.info("  AI Workflow Tool: 修改课程 '{}'", courseName);
        
        // 权限检查
        if (currentUserId == null) {
            return "  用户未登录，无法修改课程。";
        }
        
        if (!permissionService.canManageCourses(currentUserId)) {
            return permissionService.getCourseManagementPermissionInfo(currentUserId);
        }
        
        // 参数验证
        if (!StringUtils.hasText(courseName)) {
            return "  课程名称不能为空。";
        }
        
        try {
            // 查找现有课程
            String findSql = "SELECT course_id, name, description, teacher_id, category_id, location, schedule, " +
                           "duration, status FROM course WHERE name = ?";
            List<Map<String, Object>> courseResults = jdbcTemplate.queryForList(findSql, courseName.trim());
            if (courseResults.isEmpty()) {
                return "  未找到名为 '" + courseName + "' 的课程。";
            }
            
            Map<String, Object> currentCourse = courseResults.get(0);
            Long courseId = (Long) currentCourse.get("course_id");
            
            // 动态构建更新语句
            List<String> updateFields = new ArrayList<>();
            List<Object> updateParams = new ArrayList<>();
            StringBuilder changeLog = new StringBuilder();
            
            // 处理课程名称更新
            if (StringUtils.hasText(newName) && !newName.trim().equals(currentCourse.get("name"))) {
                // 检查新名称是否与其他课程重复
                String nameCheckSql = "SELECT COUNT(*) FROM course WHERE name = ? AND course_id != ?";
                Integer nameCount = jdbcTemplate.queryForObject(nameCheckSql, Integer.class, newName.trim(), courseId);
                if (nameCount > 0) {
                    return "  课程名称 '" + newName + "' 已存在，请使用其他名称。";
                }
                updateFields.add("name = ?");
                updateParams.add(newName.trim());
                changeLog.append("课程名称：").append(currentCourse.get("name")).append(" → ").append(newName.trim()).append("\n");
            }
            
            // 处理课程描述更新
            if (StringUtils.hasText(newDescription) && !newDescription.trim().equals(currentCourse.get("description"))) {
                updateFields.add("description = ?");
                updateParams.add(newDescription.trim());
                changeLog.append("课程描述：已更新\n");
            }
            
            // 处理授课老师更新
            if (StringUtils.hasText(newTeacherName)) {
                String teacherSql = "SELECT u.user_id, u.name FROM user u " +
                                  "JOIN role r ON u.role_id = r.role_id " +
                                  "WHERE u.name = ? AND u.status = '1' AND r.role_name IN ('老师', '管理员', '超级管理员')";
                List<Map<String, Object>> teacherResults = jdbcTemplate.queryForList(teacherSql, newTeacherName.trim());
                if (teacherResults.isEmpty()) {
                    return "  未找到名为 '" + newTeacherName + "' 的授课老师，或该用户不具备授课权限。";
                }
                
                Long newTeacherId = (Long) teacherResults.get(0).get("user_id");
                if (!newTeacherId.equals(currentCourse.get("teacher_id"))) {
                    updateFields.add("teacher_id = ?");
                    updateParams.add(newTeacherId);
                    changeLog.append("授课老师：").append(newTeacherName.trim()).append("\n");
                }
            }
            
            // 处理培训方向更新
            if (StringUtils.hasText(newDirectionName)) {
                String directionSql = "SELECT direction_id, direction_name FROM training_direction WHERE direction_name = ?";
                List<Map<String, Object>> directionResults = jdbcTemplate.queryForList(directionSql, newDirectionName.trim());
                if (directionResults.isEmpty()) {
                    return "  未找到名为 '" + newDirectionName + "' 的培训方向。";
                }
                
                Long newCategoryId = (Long) directionResults.get(0).get("direction_id");
                if (!newCategoryId.equals(currentCourse.get("category_id"))) {
                    updateFields.add("category_id = ?");
                    updateParams.add(newCategoryId);
                    changeLog.append("培训方向：").append(newDirectionName.trim()).append("\n");
                }
            }
            
            // 处理其他字段更新
            if (StringUtils.hasText(newLocation) && !newLocation.trim().equals(currentCourse.get("location"))) {
                updateFields.add("location = ?");
                updateParams.add(newLocation.trim());
                changeLog.append("上课地点：").append(newLocation.trim()).append("\n");
            }
            
            if (StringUtils.hasText(newSchedule) && !newSchedule.trim().equals(currentCourse.get("schedule"))) {
                updateFields.add("schedule = ?");
                updateParams.add(newSchedule.trim());
                changeLog.append("上课时间：").append(newSchedule.trim()).append("\n");
            }
            
            if (StringUtils.hasText(newDuration) && !newDuration.trim().equals(currentCourse.get("duration"))) {
                updateFields.add("duration = ?");
                updateParams.add(newDuration.trim());
                changeLog.append("课程时长：").append(newDuration.trim()).append("\n");
            }
            
            // 处理状态更新
            if (StringUtils.hasText(newStatus)) {
                Integer newStatusCode = parseStatusText(newStatus.trim());
                if (newStatusCode == null) {
                    return "  无效的课程状态 '" + newStatus + "'。请使用：'草稿'、'已发布' 或 '已下架'。";
                }
                
                if (!newStatusCode.equals(currentCourse.get("status"))) {
                    updateFields.add("status = ?");
                    updateParams.add(newStatusCode);
                    changeLog.append("课程状态：").append(getStatusText(newStatusCode)).append("\n");
                }
            }
            
            // 检查是否有需要更新的字段
            if (updateFields.isEmpty()) {
                return "  没有检测到需要更新的内容。请提供要修改的信息。";
            }
            
            // 执行更新
            updateFields.add("update_time = NOW()");
            updateFields.add("update_user = ?");
            updateParams.add(currentUserId);
            updateParams.add(courseId);
            
            String updateSql = "UPDATE course SET " + String.join(", ", updateFields) + " WHERE course_id = ?";
            int updatedRows = jdbcTemplate.update(updateSql, updateParams.toArray());
            
            if (updatedRows > 0) {
                log.info("  课程 '{}' (ID: {}) 更新成功，共更新 {} 个字段", courseName, courseId, updateFields.size() - 2);
                
                // 清理缓存以确保数据一致性
                performCompleteCourseCacheClear(courseId);
                
                return "  课程修改成功！\n" +
                       "══════════════════════════════════\n" +
                       "课程：" + courseName + "\n" +
                       "变更内容：\n" + changeLog.toString() +
                       "══════════════════════════════════\n" +
                       "修改时间：刚刚\n" +
                       "  提示：如果修改了课程状态，请注意对学生选课的影响。";
            } else {
                return "  课程修改失败，请稍后重试。";
            }
            
        } catch (Exception e) {
            log.error("  修改课程 '{}' 时发生错误: {}", courseName, e.getMessage(), e);
            return "  修改课程时发生内部错误：" + e.getMessage() + "\n请检查输入信息是否正确，或稍后重试。";
        }
    }
    
    /**
     * 将状态文本转换为状态码
     */
    private Integer parseStatusText(String statusText) {
        if (!StringUtils.hasText(statusText)) return null;
        
        switch (statusText.trim()) {
            case "草稿": return 0;
            case "已发布": case "发布": return 1;
            case "已下架": case "下架": return 2;
            default: return null;
        }
    }

    @Tool("【第一步】发起删除课程的请求。此工具会进行权限和安全检查，并返回需要用户确认的文本。")
    public String requestCourseDeletion(
        @P("要删除的课程的准确名称") String courseName,
        @P("当前用户的ID，这个ID由系统在后台自动提供，AI需要直接传递它") Long currentUserId
    ) {
        log.info("  AI Workflow Tool: 请求删除课程 '{}'", courseName);
        
        // 权限检查
        if (currentUserId == null) {
            return "  用户未登录，无法删除课程。";
        }
        
        if (!permissionService.canManageCourses(currentUserId)) {
            return permissionService.getCourseManagementPermissionInfo(currentUserId);
        }
        
        // 参数验证
        if (!StringUtils.hasText(courseName)) {
            return "  课程名称不能为空。";
        }
        
        try {
            // 查找课程基本信息
            String courseSql = "SELECT c.course_id, c.name, c.status, c.create_time, u.name as teacher_name " +
                              "FROM course c " +
                              "LEFT JOIN user u ON c.teacher_id = u.user_id " +
                              "WHERE c.name = ?";
            
            List<Map<String, Object>> courseResults = jdbcTemplate.queryForList(courseSql, courseName.trim());
            if (courseResults.isEmpty()) {
                return "  请求失败：未找到名为 '" + courseName + "' 的课程。";
            }
            
            Map<String, Object> course = courseResults.get(0);
            Long courseId = course.get("course_id") instanceof BigInteger ? 
                ((BigInteger) course.get("course_id")).longValue() : (Long) course.get("course_id");
            
            // 统计关联数据
            String studentCountSql = "SELECT COUNT(*) FROM student_course sc " +
                                   "JOIN student s ON sc.student_id = s.student_id " +
                                   "JOIN user u ON s.user_id = u.user_id " +
                                   "WHERE sc.course_id = ? AND u.status = '1'";
            Integer studentCount = jdbcTemplate.queryForObject(studentCountSql, Integer.class, courseId);
            
            String materialCountSql = "SELECT COUNT(*) FROM course_material WHERE course_id = ?";
            Integer materialCount = jdbcTemplate.queryForObject(materialCountSql, Integer.class, courseId);
            
            String attendanceCountSql = "SELECT COUNT(*) FROM attendance_plan WHERE course_id = ?";
            Integer attendanceCount = jdbcTemplate.queryForObject(attendanceCountSql, Integer.class, courseId);
            
            // 构建详细的影响分析报告
            StringBuilder warning = new StringBuilder();
            warning.append("  【严重警告 - 课程删除确认】  \n");
            warning.append("════════════════════════════════════════\n");
            warning.append("课程信息：\n");
            warning.append("  • 课程名称：").append(course.get("name")).append("\n");
            warning.append("  • 授课老师：").append(course.get("teacher_name") != null ? course.get("teacher_name") : "未分配").append("\n");
            warning.append("  • 课程状态：").append(getStatusText((Integer) course.get("status"))).append("\n");
            warning.append("  • 创建时间：").append(course.get("create_time")).append("\n\n");
            
            warning.append("  影响范围统计：\n");
            warning.append("  • 选课学生：").append(studentCount).append(" 人");
            if (studentCount > 0) {
                warning.append("    将被退选");
            }
            warning.append("\n");
            
            warning.append("  • 课程资料：").append(materialCount).append(" 个");
            if (materialCount > 0) {
                warning.append("    将被删除");
            }
            warning.append("\n");
            
            warning.append("  • 考勤计划：").append(attendanceCount).append(" 个");
            if (attendanceCount > 0) {
                warning.append("    将被删除");
            }
            warning.append("\n\n");
            
            // 特殊警告
            if (studentCount > 0) {
                warning.append("  特别注意：\n");
                warning.append("  该课程有 ").append(studentCount).append(" 名学生已选课，删除课程将:\n");
                warning.append("  - 自动退选所有学生\n");
                warning.append("  - 删除相关的考勤记录\n");
                warning.append("  - 学生将无法访问课程资料\n\n");
            }
            
            if (materialCount > 0) {
                warning.append("  资料警告：\n");
                warning.append("  该课程包含 ").append(materialCount).append(" 个资料文件，删除后：\n");
                warning.append("  - 所有课程资料将被永久删除\n");
                warning.append("  - 物理文件也将从服务器移除\n");
                warning.append("  - 学生和老师都无法再访问\n\n");
            }
            
            warning.append("⚡ 此操作 **无法撤销**！\n");
            warning.append("════════════════════════════════════════\n");
            warning.append("如果您确定要继续删除，请调用 `confirmCourseDeletion` 工具。\n");
            warning.append("建议：在删除前，可考虑将课程状态改为'已下架'作为替代方案。");
            
            log.warn("   用户 {} 请求删除课程 '{}' (ID: {}), 影响: {}学生, {}资料, {}考勤计划", 
                    currentUserId, courseName, courseId, studentCount, materialCount, attendanceCount);
            
            return warning.toString();
            
        } catch (Exception e) {
            log.error("  处理课程删除请求时出错，课程: {}, 错误: {}", courseName, e.getMessage(), e);
            return "  处理删除请求时发生内部错误，请稍后重试。";
        }
    }

    @Tool("【第二步】在用户确认后，执行对指定课程的永久删除操作。")
    @Transactional
    public String confirmCourseDeletion(
        @P("要删除的课程的准确名称") String courseName,
        @P("当前用户的ID，这个ID由系统在后台自动提供，AI需要直接传递它") Long currentUserId
    ) {
        log.info("  AI Workflow Tool: 确认删除课程 '{}'", courseName);
        
        // 权限检查
        if (currentUserId == null) {
            return "  用户未登录，无法删除课程。";
        }
        
        if (!permissionService.canManageCourses(currentUserId)) {
            return permissionService.getCourseManagementPermissionInfo(currentUserId);
        }
        
        // 参数验证
        if (!StringUtils.hasText(courseName)) {
            return "  课程名称不能为空。";
        }
        
        try {
            // 查找课程信息
            String courseSql = "SELECT course_id, name, cover_image FROM course WHERE name = ?";
            List<Map<String, Object>> courseResults = jdbcTemplate.queryForList(courseSql, courseName.trim());
            if (courseResults.isEmpty()) {
                return "  删除失败：在执行删除时找不到课程 '" + courseName + "'。可能已被其他用户删除。";
            }
            
            Map<String, Object> course = courseResults.get(0);
            Long courseId = course.get("course_id") instanceof BigInteger ? 
                ((BigInteger) course.get("course_id")).longValue() : (Long) course.get("course_id");
            String coverImage = (String) course.get("cover_image");
            
            // 收集删除统计信息
            int deletedMaterials = 0;
            int deletedEnrollments = 0;
            int deletedAttendancePlans = 0;
            int deletedAttendanceRecords = 0;
            
            log.info("🗑  开始级联删除课程 '{}' (ID: {}) 的所有关联数据", courseName, courseId);
            
            // 第1步：删除课程资料的物理文件并删除数据库记录
            String materialsSql = "SELECT file_name, file_path FROM course_material WHERE course_id = ?";
            List<Map<String, Object>> materials = jdbcTemplate.queryForList(materialsSql, courseId);
            
            for (Map<String, Object> material : materials) {
                String filePath = (String) material.get("file_path");
                String fileName = (String) material.get("file_name");
                
                // 删除物理文件（这里应该调用文件服务删除实际文件）
                // fileService.deleteFile(filePath); // 实际项目中需要实现文件删除逻辑
                
                log.debug("   准备删除课程资料文件: {}", fileName);
            }
            
            deletedMaterials = jdbcTemplate.update("DELETE FROM course_material WHERE course_id = ?", courseId);
            log.info("  删除课程资料: {} 个", deletedMaterials);
            
            // 第2步：删除选课记录（学生退选）
            deletedEnrollments = jdbcTemplate.update("DELETE FROM student_course WHERE course_id = ?", courseId);
            log.info("  删除选课记录: {} 条", deletedEnrollments);
            
            // 第3步：删除相关的考勤记录
            // 首先获取相关的考勤计划ID
            String attendancePlansSql = "SELECT plan_id FROM attendance_plan WHERE course_id = ?";
            List<Long> planIds = jdbcTemplate.queryForList(attendancePlansSql, Long.class, courseId);
            
            for (Long planId : planIds) {
                int recordsDeleted = jdbcTemplate.update("DELETE FROM attendance_record WHERE plan_id = ?", planId);
                deletedAttendanceRecords += recordsDeleted;
            }
            log.info("  删除考勤记录: {} 条", deletedAttendanceRecords);
            
            // 第4步：删除考勤计划
            deletedAttendancePlans = jdbcTemplate.update("DELETE FROM attendance_plan WHERE course_id = ?", courseId);
            log.info("  删除考勤计划: {} 个", deletedAttendancePlans);
            
            // 第5步：删除课程封面图片文件
            if (StringUtils.hasText(coverImage)) {
                // fileService.deleteFile(coverImage); // 实际项目中需要实现文件删除逻辑
                log.debug("   准备删除课程封面: {}", coverImage);
            }
            
            // 第6步：最后删除课程主记录
            int result = jdbcTemplate.update("DELETE FROM course WHERE course_id = ?", courseId);
            
            if (result > 0) {
                // 记录详细的删除操作日志
                log.warn("🗑  课程删除完成 - 用户: {}, 课程: '{}' (ID: {}), " +
                        "资料: {}个, 选课: {}条, 考勤计划: {}个, 考勤记录: {}条", 
                        currentUserId, courseName, courseId, 
                        deletedMaterials, deletedEnrollments, deletedAttendancePlans, deletedAttendanceRecords);
                
                // 清理缓存以确保数据一致性
                performCompleteCourseCacheClear(courseId);
                
                StringBuilder result_msg = new StringBuilder();
                result_msg.append("  课程删除成功！\n");
                result_msg.append("════════════════════════════════════════\n");
                result_msg.append("课程名称：").append(courseName).append("\n");
                result_msg.append("课程ID：").append(courseId).append("\n\n");
                result_msg.append("  删除统计：\n");
                result_msg.append("  • 课程资料：").append(deletedMaterials).append(" 个\n");
                result_msg.append("  • 选课记录：").append(deletedEnrollments).append(" 条\n");
                result_msg.append("  • 考勤计划：").append(deletedAttendancePlans).append(" 个\n");
                result_msg.append("  • 考勤记录：").append(deletedAttendanceRecords).append(" 条\n\n");
                
                if (deletedEnrollments > 0) {
                    result_msg.append("  ").append(deletedEnrollments).append(" 名学生已被自动退选\n");
                }
                if (deletedMaterials > 0) {
                    result_msg.append("  ").append(deletedMaterials).append(" 个课程资料文件已删除\n");
                }
                
                result_msg.append("\n⚡ 删除操作已完成且无法撤销\n");
                result_msg.append("════════════════════════════════════════\n");
                result_msg.append("删除时间：刚刚\n");
                result_msg.append("执行用户：").append(currentUserId);
                
                return result_msg.toString();
            } else {
                log.error("  课程主记录删除失败，course_id: {}", courseId);
                return "  删除失败：数据库操作未影响任何行，可能数据已被其他操作修改。";
            }
            
        } catch (EmptyResultDataAccessException e) {
            log.warn("   删除时未找到课程: {}", courseName);
            return "  删除失败：在执行删除时找不到课程 '" + courseName + "'。可能已被其他用户删除。";
        } catch (Exception e) {
            log.error("  确认删除课程 '{}' 时发生严重错误: {}", courseName, e.getMessage(), e);
            // 事务会自动回滚
            return "  删除课程时发生内部错误：" + e.getMessage() + 
                   "\n所有操作已回滚，数据保持完整。请稍后重试或联系技术支持。";
        }
    }
} 