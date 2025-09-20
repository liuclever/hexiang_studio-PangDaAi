package com.back_hexiang_studio.pangDaAi.tool.workflow;

import com.back_hexiang_studio.context.UserContextHolder;
import com.back_hexiang_studio.pangDaAi.service.PermissionService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.HashSet;

/**
 * 【最终版】用户管理工作流工具
 * 将多步骤的用户管理操作封装成单一、可靠、角色感知的工具。
 * AI只需选择正确的工具，无需关心内部执行流程。
 */
@Service
@Slf4j
public class UserManagementTools {

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
     * 清除单个用户的缓存
     */
    private void clearUserCache(Long userId) {
        if (userId == null) return;
        Set<String> keysToDelete = new HashSet<>();
        keysToDelete.add("user:" + userId);
        keysToDelete.add("user:info:" + userId);
        keysToDelete.add("user:details:" + userId);
        keysToDelete.add("user:profile:" + userId);
        keysToDelete.add("user:permissions:" + userId);
        keysToDelete.add("user:roles:" + userId);
        keysToDelete.add("user:honors:" + userId);
        keysToDelete.add("user:certificates:" + userId);
        keysToDelete.add("user:activities:" + userId);
        keysToDelete.add("login:token:" + userId);
        keysToDelete.add("login:user:" + userId);

        redisTemplate.delete(keysToDelete);
        log.info("🔄 [缓存清理] 清理用户缓存，用户ID: {}, 键数量: {}", userId, keysToDelete.size());
    }

    /**
     * 清除所有与用户列表相关的缓存
     */
    private void clearUserListCache() {
        Set<String> patterns = new HashSet<>();
        patterns.add("user:list*");
        patterns.add("users:page*");
        patterns.add("users:filter*");
        patterns.add("users:search*");
        patterns.add("users:role:*");
        patterns.add("users:status:*");

        int totalDeleted = 0;
        for (String pattern : patterns) {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                totalDeleted += keys.size();
                log.info("🔄 [缓存清理] 清理用户列表缓存，模式: {}, 键数量: {}", pattern, keys.size());
            }
        }

        if (totalDeleted > 0) {
            log.info("🔄 [缓存清理] 总共清理了 {} 个用户列表相关缓存键", totalDeleted);
        }
    }

    /**
     * 清理全局用户相关缓存
     */
    private void clearGlobalUserCache() {
        Set<String> globalPatterns = new HashSet<>();
        globalPatterns.add("user:all*");
        globalPatterns.add("users:count*");
        globalPatterns.add("users:stats*");
        globalPatterns.add("dashboard:user*");

        for (String pattern : globalPatterns) {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("🔄 [缓存清理] 清理全局缓存，模式: {}, 键数量: {}", pattern, keys.size());
            }
        }
    }

    /**
     * 执行完整的用户相关缓存清理
     */
    private void performCompleteUserCacheClear(Long userId) {
        if (userId != null) {
            clearUserCache(userId);
        }
        clearUserListCache();
        clearGlobalUserCache();
        log.info("🔄 [缓存清理] 完成用户相关缓存清理");
    }

    // ====================================================================================
    // 1. 信息收集与查询工具 (Read-Only)
    // ====================================================================================

    @Tool("获取创建一个特定角色用户所需的字段列表。当你需要添加一个新用户但不知道需要哪些信息时，首先调用此工具。")
    public String getRequiredFieldsForUser(@P("要查询的角色名称，例如 '学员', '老师', '访客'") String roleName) {
        log.info("🤖 AI Workflow Tool: 查询角色 '{}' 的必填字段", roleName);
        if (roleName == null || roleName.trim().isEmpty()) {
            return "❌ 角色名称不能为空。请输入 '学员', '老师', 或 '访客'。";
        }

        switch (roleName.trim()) {
            case "学员":
                return "📝 要创建一个新的【学员】，你需要提供以下信息：\n" +
                        "【基本信息】\n" +
                        "- 姓名 (必填)\n" +
                        "- 性别 (必填: '男' 或 '女')\n" +
                        "- 手机号 (必填)\n" +
                        "- 邮箱 (必填)\n" +
                        "【学籍信息】\n" +
                        "- 学号 (必填)\n" +
                        "- 年级 (例如 '2022')\n" +
                        "- 专业班级 (例如 '软件工程2201')\n" +
                        "- 辅导员姓名 (可选)";
            case "老师":
                return "📝 要创建一个新的【老师】，你需要提供以下信息：\n" +
                        "【基本信息】\n" +
                        "- 姓名 (必填)\n" +
                        "- 性别 (必填: '男' 或 '女')\n" +
                        "- 手机号 (必填)\n" +
                        "- 邮箱 (必填)\n" +
                        "【教师信息】\n" +
                        "- 职称 (例如 '教授', '讲师', 可选)\n" +
                        "- 研究方向 (可选)";
            case "访客":
                return "📝 要创建一个新的【访客】，你需要提供以下基本信息：\n" +
                        "【基本信息】\n" +
                        "- 姓名 (必填)\n" +
                        "- 性别 (必填: '男' 或 '女')\n" +
                        "- 手机号 (必填)\n" +
                        "- 邮箱 (必填)";
            default:
                return "❌ 未知的角色: '" + roleName + "'。目前只支持 '学员', '老师', 和 '访客'。";
        }
    }

    @Tool("查询指定用户的完整档案信息，包括基本信息、角色、职位，以及学籍或教师信息（如果适用）。")
    public String findUser(@P("要查询的用户的准确姓名") String userName) {
        log.info("🤖 AI Workflow Tool: 查询用户 '{}' 的完整档案", userName);
        String sql = "SELECT u.user_id, u.name, u.sex, u.phone, u.email, u.create_time, " +
                     "r.role_name, p.position_name, " +
                     "s.student_number, s.grade_year, s.majorClass, s.counselor, d.department_name, " +
                     "t.title " +
                     "FROM user u " +
                     "LEFT JOIN role r ON u.role_id = r.role_id " +
                     "LEFT JOIN position p ON u.position_id = p.position_id " +
                     "LEFT JOIN student s ON u.user_id = s.user_id " +
                     "LEFT JOIN department d ON s.department_id = d.department_id " +
                     "LEFT JOIN teacher t ON u.user_id = t.user_id " +
                     "WHERE u.name = ? AND u.status = '1'";
        try {
            Map<String, Object> userMap = jdbcTemplate.queryForMap(sql, userName);

            StringBuilder profile = new StringBuilder("👤 用户档案: " + userMap.get("name") + "\n");
            profile.append("------------------------\n");
            profile.append("基本信息:\n");
            profile.append("  - 性别: ").append("1".equals(userMap.get("sex").toString()) ? "男" : "女").append("\n");
            profile.append("  - 手机: ").append(userMap.get("phone")).append("\n");
            profile.append("  - 邮箱: ").append(userMap.get("email")).append("\n");
            profile.append("  - 角色: ").append(userMap.get("role_name")).append("\n");
            if (userMap.get("position_name") != null) {
                profile.append("  - 职位: ").append(userMap.get("position_name")).append("\n");
            }

            if ("学员".equals(userMap.get("role_name"))) {
                profile.append("学籍信息:\n");
                profile.append("  - 学号: ").append(userMap.get("student_number")).append("\n");
                profile.append("  - 年级: ").append(userMap.get("grade_year")).append("\n");
                profile.append("  - 班级: ").append(userMap.get("majorClass")).append("\n");
                if (userMap.get("counselor") != null) {
                    profile.append("  - 辅导员: ").append(userMap.get("counselor")).append("\n");
                }
            } else if ("老师".equals(userMap.get("role_name"))) {
                profile.append("教师信息:\n");
                if (userMap.get("title") != null) {
                    profile.append("  - 职称: ").append(userMap.get("title")).append("\n");
                }
            }
            profile.append("------------------------\n");
            profile.append("加入时间: ").append(userMap.get("create_time"));

            return profile.toString();

        } catch (EmptyResultDataAccessException e) {
            return "❌ 未找到名为 '" + userName + "' 的用户。";
        } catch (Exception e) {
            log.error("❌ 查询用户 '{}' 档案时出错: {}", userName, e.getMessage(), e);
            return "❌ 查询用户信息时发生内部错误。";
        }
    }

    @Tool("查询当前登录用户（“我”）的完整个人档案信息。")
    public String getCurrentUserProfile(@P("当前用户的ID，这个ID由系统在后台自动提供，AI需要直接传递它") Long currentUserId) {
        if (currentUserId == null) {
            log.warn("👤 getCurrentUserProfile: 调用时未提供currentUserId。");
            return "错误：调用工具时currentUserId为空。请直接回复用户：“抱歉，系统内部错误，我暂时无法获取您的信息。”";
        }

        log.info("👤 正在查询当前用户个人档案, User ID: {}", currentUserId);
        try {
            String userName = jdbcTemplate.queryForObject("SELECT name FROM user WHERE user_id = ?", String.class, currentUserId);
            // 复用findUser逻辑，保持代码 DRY (Don't Repeat Yourself)
            return findUser(userName);
        } catch (EmptyResultDataAccessException e) {
            log.warn("👤 未找到ID为 {} 的用户。", currentUserId);
            return "错误：未在数据库中找到您的用户信息。请联系管理员核实您的账户是否正确。";
        } catch (Exception e) {
            log.error("❌ 查询当前用户档案时发生未知错误, User ID: {}", currentUserId, e);
            return "错误：查询您的档案时系统出现意外，请稍后再试。";
        }
    }

    @Tool("查询并列出工作室所有成员的名单，按角色分组显示。")
    public String listAllUsers() {
        log.info("🤖 AI Workflow Tool: 列出所有工作室成员");
        String sql = "SELECT u.name, u.sex, r.role_name " +
                     "FROM user u " +
                     "JOIN role r ON u.role_id = r.role_id " +
                     "WHERE u.status = '1' " +
                     "ORDER BY r.role_id, u.name";
        try {
            List<Map<String, Object>> users = jdbcTemplate.queryForList(sql);
            if (users.isEmpty()) {
                return "工作室目前没有成员。";
            }

            Map<String, List<Map<String, Object>>> groupedByRole = users.stream()
                .collect(Collectors.groupingBy(u -> (String) u.get("role_name")));

            StringBuilder result = new StringBuilder("工作室成员列表:\n------------------------\n");
            groupedByRole.forEach((role, userList) -> {
                result.append("【").append(role).append("】 (").append(userList.size()).append("人)\n");
                userList.forEach(user -> result.append("- ").append(user.get("name")).append("\n"));
                result.append("\n");
            });

            return result.toString();
        } catch (Exception e) {
            log.error("❌ 列出所有用户时出错: {}", e.getMessage(), e);
            return "❌ 获取成员列表时发生内部错误。";
        }
    }


    // ====================================================================================
    // 2. 专门化的创建工具 (角色感知)
    // ====================================================================================

    @Tool("查询当前可用的培训方向列表，供创建学员和老师时选择")
    public String getAvailableTrainingDirections(@P("当前用户的ID") Long currentUserId) {
        log.info("🤖 AI Workflow Tool: 查询可用培训方向");
        
        try {
            String sql = "SELECT direction_id, direction_name, description FROM training_direction ORDER BY direction_id";
            List<Map<String, Object>> directions = jdbcTemplate.queryForList(sql);
            
            if (directions.isEmpty()) {
                return "❌ 当前系统中没有配置培训方向，请联系管理员添加。";
            }
            
            StringBuilder result = new StringBuilder("📚 当前可用的培训方向：\n\n");
            for (Map<String, Object> direction : directions) {
                Long directionId = direction.get("direction_id") instanceof BigInteger ?
                    ((BigInteger) direction.get("direction_id")).longValue() : (Long) direction.get("direction_id");
                String directionName = (String) direction.get("direction_name");
                String description = (String) direction.get("description");
                
                result.append("【").append(directionId).append("】")
                      .append(directionName)
                      .append(" - ").append(description != null ? description : "无描述")
                      .append("\n");
            }
            
            result.append("\n💡 在创建学员/老师时，请输入对应的数字编号（如：1）");
            return result.toString();
            
        } catch (Exception e) {
            log.error("❌ 查询培训方向失败: {}", e.getMessage(), e);
            return "❌ 查询培训方向时发生错误，请稍后重试。";
        }
    }

    @Tool("查询当前可用的职位列表，供创建用户时选择")
    public String getAvailablePositions(@P("当前用户的ID") Long currentUserId) {
        log.info("🤖 AI Workflow Tool: 查询可用职位");
        
        try {
            String sql = "SELECT position_id, role, position_name FROM position ORDER BY position_id";
            List<Map<String, Object>> positions = jdbcTemplate.queryForList(sql);
            
            if (positions.isEmpty()) {
                return "❌ 当前系统中没有配置职位，请联系管理员添加。";
            }
            
            StringBuilder result = new StringBuilder("💼 当前可用的职位：\n\n");
            
            // 按角色分组显示
            Map<String, List<Map<String, Object>>> positionsByRole = new LinkedHashMap<>();
            for (Map<String, Object> position : positions) {
                String role = (String) position.get("role");
                positionsByRole.computeIfAbsent(role, k -> new ArrayList<>()).add(position);
            }
            
            for (Map.Entry<String, List<Map<String, Object>>> entry : positionsByRole.entrySet()) {
                String role = entry.getKey();
                String roleDisplayName;
                switch (role) {
                    case "visitor": roleDisplayName = "访客"; break;
                    case "student": roleDisplayName = "学员"; break;
                    case "teacher": roleDisplayName = "老师"; break;
                    case "manager": roleDisplayName = "管理员"; break;
                    case "admin": roleDisplayName = "超级管理员"; break;
                    default: roleDisplayName = role;
                }
                
                result.append("🏷️ ").append(roleDisplayName).append("类职位：\n");
                for (Map<String, Object> position : entry.getValue()) {
                    Object positionIdObj = position.get("position_id");
                    Long positionId = positionIdObj instanceof Integer ? 
                        ((Integer) positionIdObj).longValue() : (Long) positionIdObj;
                    String positionName = (String) position.get("position_name");
                    
                    result.append("   【").append(positionId).append("】").append(positionName).append("\n");
                }
                result.append("\n");
            }
            
            result.append("💡 在创建用户时，请输入对应的数字编号（如：1）");
            return result.toString();
            
        } catch (Exception e) {
            log.error("❌ 查询职位失败: {}", e.getMessage(), e);
            return "❌ 查询职位时发生错误，请稍后重试。";
        }
    }

    @Tool("创建一个新的【学员】用户。此工具会同时在用户表和学员表中创建记录，默认密码为123456。在使用前，请先调用getAvailableTrainingDirections和getAvailablePositions查询可用选项。")
    @Transactional
    public String createStudent(
            @P("学员真实姓名") String name,
            @P("用户名（登录用户名）") String userName,
            @P("性别: '男' 或 '女'") String sex,
            @P("手机号码") String phone,
            @P("邮箱地址") String email,
            @P("学号") String studentNumber,
            @P("年级, 例如 '2022'") String gradeYear,
            @P("专业班级, 例如 '软件工程2201'") String majorClass,
            @P("培训方向ID（请先用getAvailableTrainingDirections查询可用选项）") Long directionId,
            @P("职位ID（请先用getAvailablePositions查询可用选项，建议选择学员类职位）") Long positionId,
            @P("辅导员姓名 (可选，留空则不设置)") String counselor,
            @P("当前用户的ID，这个ID由系统在后台自动提供，AI需要直接传递它") Long currentUserId
    ) {
        log.info("🤖 AI Workflow Tool: 开始执行'创建学员'工作流, 学员姓名: {}, 用户名: {}", name, userName);
        if (!permissionService.canManageUsers(currentUserId)) {
            return permissionService.getUserManagementPermissionInfo(currentUserId);
        }
        if (!StringUtils.hasText(name) || !StringUtils.hasText(userName) || 
            !StringUtils.hasText(phone) || !StringUtils.hasText(email) || !StringUtils.hasText(studentNumber) || 
            directionId == null || positionId == null) {
            return "❌ 操作失败: 姓名、用户名、手机号、邮箱、学号、培训方向和职位是必填项。";
        }
        try {
            // 🔧 验证培训方向ID是否有效
            int directionCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM training_direction WHERE direction_id = ?", Integer.class, directionId);
            if (directionCount == 0) {
                return "❌ 创建失败: 培训方向ID '" + directionId + "' 不存在。请先使用getAvailableTrainingDirections查询有效的培训方向。";
            }
            
            // 🔧 验证职位ID是否有效
            int positionCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM position WHERE position_id = ?", Integer.class, positionId);
            if (positionCount == 0) {
                return "❌ 创建失败: 职位ID '" + positionId + "' 不存在。请先使用getAvailablePositions查询有效的职位。";
            }
            
            // 🔧 检查用户名、姓名、手机号、邮箱是否已存在
            if (jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user WHERE user_name = ?", Integer.class, userName) > 0) {
                return "❌ 创建失败: 用户名 '" + userName + "' 已被占用。";
            }
            if (jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user WHERE name = ? OR phone = ? OR email = ?", Integer.class, name, phone, email) > 0) {
                return "❌ 创建失败: 姓名、手机号或邮箱已被占用。";
            }
            if (jdbcTemplate.queryForObject("SELECT COUNT(*) FROM student WHERE student_number = ?", Integer.class, studentNumber) > 0) {
                return "❌ 创建失败: 学号 '" + studentNumber + "' 已被占用。";
            }

            // 🔧 使用默认密码123456并进行MD5加密
            String defaultPassword = "123456";
            String encryptedPassword = encryptMD5(defaultPassword);
            
            // 🔧 修正SQL语句，添加完整字段
            String insertUserSql = "INSERT INTO user (user_name, name, sex, phone, email, password, role_id, position_id, status, create_time, update_time, createUser, updateUser) VALUES (?, ?, ?, ?, ?, ?, ?, ?, '1', NOW(), NOW(), ?, ?)";
            KeyHolder keyHolder = new GeneratedKeyHolder();
            final String sexValue = "男".equals(sex) ? "1" : "0";
            final Long studentRoleId = 1L;

            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, userName);           // 用户名
                ps.setString(2, name);               // 真实姓名
                ps.setString(3, sexValue);           // 性别
                ps.setString(4, phone);              // 手机号
                ps.setString(5, email);              // 邮箱
                ps.setString(6, encryptedPassword);  // 加密后的密码
                ps.setLong(7, studentRoleId);        // 角色ID
                ps.setLong(8, positionId);           // 职位ID
                ps.setLong(9, currentUserId);        // 创建人ID
                ps.setLong(10, currentUserId);       // 修改人ID
                return ps;
            }, keyHolder);

            Long newUserId = Objects.requireNonNull(keyHolder.getKey()).longValue();
            String insertStudentSql = "INSERT INTO student (user_id, student_number, grade_year, majorClass, direction_id, counselor) VALUES (?, ?, ?, ?, ?, ?)";
            jdbcTemplate.update(insertStudentSql, newUserId, studentNumber, gradeYear, majorClass, directionId, counselor);

            // 🔧 获取培训方向和职位名称用于显示
            String directionName = jdbcTemplate.queryForObject("SELECT direction_name FROM training_direction WHERE direction_id = ?", String.class, directionId);
            String positionName = jdbcTemplate.queryForObject("SELECT position_name FROM position WHERE position_id = ?", String.class, positionId);

            log.info("✅ 学员 '{}' (用户名: {}, User ID: {}) 创建成功。", name, userName, newUserId);
            
            // 清理缓存以确保数据一致性
            performCompleteUserCacheClear(newUserId);
            
            return "✅ 学员 '" + name + "' 的档案已成功创建！\n" +
                   "📋 账户信息：\n" +
                   "   • 用户名：" + userName + "\n" +
                   "   • 密码：123456（默认密码）\n" +
                   "   • 学号：" + studentNumber + "\n" +
                   "   • 培训方向：" + directionName + "\n" +
                   "   • 职位：" + positionName + "\n" +
                   "📢 请提醒学员尽快登录并修改密码。";
        } catch (Exception e) {
            log.error("❌ 执行'创建学员'工作流时发生错误: {}", e.getMessage(), e);
            return "❌ 创建学员时发生系统内部错误，操作已取消。";
        }
    }

    /**
     * MD5加密工具方法（与userServiceimpl.java保持一致）
     * @param input 需要加密的字符串
     * @return MD5加密后的字符串
     */
    private String encryptMD5(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());

            // 将字节数组转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("MD5加密失败: {}", e.getMessage());
            throw new RuntimeException("MD5加密失败", e);
        }
    }
    
    @Tool("创建一个新的【老师】用户。此工具会同时在用户表和教师表中创建记录，默认密码为123456。在使用前，请先调用getAvailableTrainingDirections和getAvailablePositions查询可用选项。")
    @Transactional
    public String createTeacher(
            @P("老师真实姓名") String name,
            @P("用户名（登录用户名）") String userName,
            @P("性别: '男' 或 '女'") String sex,
            @P("手机号码") String phone,
            @P("邮箱地址") String email,
            @P("培训方向ID（请先用getAvailableTrainingDirections查询可用选项）") Long directionId,
            @P("职位ID（请先用getAvailablePositions查询可用选项，建议选择老师类职位）") Long positionId,
            @P("职称 (可选，留空则不设置)") String title,
            @P("办公室位置 (可选，留空则不设置)") String officeLocation,
            @P("当前用户的ID，这个ID由系统在后台自动提供，AI需要直接传递它") Long currentUserId
    ) {
        log.info("🤖 AI Workflow Tool: 开始执行'创建老师'工作流, 老师姓名: {}, 用户名: {}", name, userName);
        if (!permissionService.canManageUsers(currentUserId)) {
            return permissionService.getUserManagementPermissionInfo(currentUserId);
        }
        if (!StringUtils.hasText(name) || !StringUtils.hasText(userName) || 
            !StringUtils.hasText(phone) || !StringUtils.hasText(email) || directionId == null || positionId == null) {
            return "❌ 操作失败: 姓名、用户名、手机号、邮箱、培训方向和职位是必填项。";
        }
        try {
            // 🔧 验证培训方向ID是否有效
            int directionCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM training_direction WHERE direction_id = ?", Integer.class, directionId);
            if (directionCount == 0) {
                return "❌ 创建失败: 培训方向ID '" + directionId + "' 不存在。请先使用getAvailableTrainingDirections查询有效的培训方向。";
            }
            
            // 🔧 验证职位ID是否有效
            int positionCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM position WHERE position_id = ?", Integer.class, positionId);
            if (positionCount == 0) {
                return "❌ 创建失败: 职位ID '" + positionId + "' 不存在。请先使用getAvailablePositions查询有效的职位。";
            }
            
            // 🔧 检查用户名、姓名、手机号、邮箱是否已存在
            if (jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user WHERE user_name = ?", Integer.class, userName) > 0) {
                return "❌ 创建失败: 用户名 '" + userName + "' 已被占用。";
            }
            if (jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user WHERE name = ? OR phone = ? OR email = ?", Integer.class, name, phone, email) > 0) {
                return "❌ 创建失败: 姓名、手机号或邮箱已被占用。";
            }

            // 🔧 使用默认密码123456并进行MD5加密
            String defaultPassword = "123456";
            String encryptedPassword = encryptMD5(defaultPassword);
            
            // 🔧 修正SQL语句，添加完整字段
            String insertUserSql = "INSERT INTO user (user_name, name, sex, phone, email, password, role_id, position_id, status, create_time, update_time, createUser, updateUser) VALUES (?, ?, ?, ?, ?, ?, ?, ?, '1', NOW(), NOW(), ?, ?)";
            KeyHolder keyHolder = new GeneratedKeyHolder();
            final String sexValue = "男".equals(sex) ? "1" : "0";
            final Long teacherRoleId = 2L;

            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, userName);           // 用户名
                ps.setString(2, name);               // 真实姓名
                ps.setString(3, sexValue);           // 性别
                ps.setString(4, phone);              // 手机号
                ps.setString(5, email);              // 邮箱
                ps.setString(6, encryptedPassword);  // 加密后的密码
                ps.setLong(7, teacherRoleId);        // 角色ID
                ps.setLong(8, positionId);           // 职位ID
                ps.setLong(9, currentUserId);        // 创建人ID
                ps.setLong(10, currentUserId);       // 修改人ID
                return ps;
            }, keyHolder);

            Long newUserId = Objects.requireNonNull(keyHolder.getKey()).longValue();
            String insertTeacherSql = "INSERT INTO teacher (user_id, direction_id, title, office_location) VALUES (?, ?, ?, ?)";
            jdbcTemplate.update(insertTeacherSql, newUserId, directionId, title, officeLocation);

            // 🔧 获取培训方向和职位名称用于显示
            String directionName = jdbcTemplate.queryForObject("SELECT direction_name FROM training_direction WHERE direction_id = ?", String.class, directionId);
            String positionName = jdbcTemplate.queryForObject("SELECT position_name FROM position WHERE position_id = ?", String.class, positionId);

            log.info("✅ 老师 '{}' (用户名: {}, User ID: {}) 创建成功。", name, userName, newUserId);
            
            // 清理缓存以确保数据一致性
            performCompleteUserCacheClear(newUserId);
            
            return "✅ 老师 '" + name + "' 的档案已成功创建！\n" +
                   "📋 账户信息：\n" +
                   "   • 用户名：" + userName + "\n" +
                   "   • 密码：123456（默认密码）\n" +
                   "   • 培训方向：" + directionName + "\n" +
                   "   • 职位：" + positionName + "\n" +
                   "   • 职称：" + (title != null ? title : "未设置") + "\n" +
                   "📢 请提醒老师尽快登录并修改密码。";
        } catch (Exception e) {
            log.error("❌ 执行'创建老师'工作流时发生错误: {}", e.getMessage(), e);
            return "❌ 创建老师时发生系统内部错误，操作已取消。";
        }
    }

    @Tool("创建一个新的【访客】用户。访客拥有最基础的权限，默认密码为123456。在使用前，请先调用getAvailablePositions查询可用的访客职位。")
    @Transactional
    public String createVisitor(
            @P("访客真实姓名") String name,
            @P("用户名（登录用户名）") String userName,
            @P("性别: '男' 或 '女'") String sex,
            @P("手机号码") String phone,
            @P("邮箱地址") String email,
            @P("职位ID（请先用getAvailablePositions查询可用选项，建议选择访客类职位）") Long positionId,
            @P("当前用户的ID，这个ID由系统在后台自动提供，AI需要直接传递它") Long currentUserId
    ) {
        log.info("🤖 AI Workflow Tool: 开始执行'创建访客'工作流, 访客姓名: {}, 用户名: {}", name, userName);
        if (!permissionService.canManageUsers(currentUserId)) {
            return permissionService.getUserManagementPermissionInfo(currentUserId);
        }
        if (!StringUtils.hasText(name) || !StringUtils.hasText(userName) || !StringUtils.hasText(phone) || !StringUtils.hasText(email) || positionId == null) {
            return "❌ 操作失败: 姓名、用户名、手机号、邮箱和职位是必填项。";
        }
        try {
            // 🔧 验证职位ID是否有效
            int positionCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM position WHERE position_id = ?", Integer.class, positionId);
            if (positionCount == 0) {
                return "❌ 创建失败: 职位ID '" + positionId + "' 不存在。请先使用getAvailablePositions查询有效的职位。";
            }
            
            // 🔧 检查用户名、姓名、手机号、邮箱是否已存在
            if (jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user WHERE user_name = ?", Integer.class, userName) > 0) {
                return "❌ 创建失败: 用户名 '" + userName + "' 已被占用。";
            }
            if (jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user WHERE name = ? OR phone = ? OR email = ?", Integer.class, name, phone, email) > 0) {
                return "❌ 创建失败: 姓名、手机号或邮箱已被占用。";
            }
            
            // 🔧 使用默认密码123456并进行MD5加密
            String defaultPassword = "123456";
            String encryptedPassword = encryptMD5(defaultPassword);
            
            // 🔧 修正SQL语句，添加完整字段并获取新用户ID
            String insertUserSql = "INSERT INTO user (user_name, name, sex, phone, email, password, role_id, position_id, status, create_time, update_time, createUser, updateUser) VALUES (?, ?, ?, ?, ?, ?, ?, ?, '1', NOW(), NOW(), ?, ?)";
            final String sexValue = "男".equals(sex) ? "1" : "0";
            final Long visitorRoleId = 0L; // 访客角色ID
            
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, userName);
                ps.setString(2, name);
                ps.setString(3, sexValue);
                ps.setString(4, phone);
                ps.setString(5, email);
                ps.setString(6, encryptedPassword);
                ps.setLong(7, visitorRoleId);
                ps.setLong(8, positionId);
                ps.setLong(9, currentUserId);
                ps.setLong(10, currentUserId);
                return ps;
            }, keyHolder);
            
            Long newUserId = Objects.requireNonNull(keyHolder.getKey()).longValue();

            // 🔧 获取职位名称用于显示
            String positionName = jdbcTemplate.queryForObject("SELECT position_name FROM position WHERE position_id = ?", String.class, positionId);

            log.info("✅ 访客 '{}' (用户名: {}) 创建成功。", name, userName);
            
            // 清理缓存以确保数据一致性
            performCompleteUserCacheClear(newUserId);
            
            return "✅ 访客 '" + name + "' 的账户已成功创建！\n" +
                   "📋 账户信息：\n" +
                   "   • 用户名：" + userName + "\n" +
                   "   • 密码：123456（默认密码）\n" +
                   "   • 职位：" + positionName + "\n" +
                   "📢 请提醒访客尽快登录并修改密码。";
        } catch (Exception e) {
            log.error("❌ 执行'创建访客'工作流时发生错误: {}", e.getMessage(), e);
            return "❌ 创建访客时发生系统内部错误，操作已取消。";
        }
    }

    // ====================================================================================
    // 3. 专门化的修改工具 (角色感知)
    // ====================================================================================

    @Tool("修改用户的【基本】信息，如手机号、邮箱。此工具适用于所有角色，但不能修改角色特定信息（如学号）。")
    @Transactional
    public String updateUserBaseInfo(
            @P("要修改的用户的准确姓名") String userName,
            @P("新的手机号 (可选, 不修改则留空)") String newPhone,
            @P("新的邮箱 (可选, 不修改则留空)") String newEmail,
            @P("当前用户的ID，这个ID由系统在后台自动提供，AI需要直接传递它") Long currentUserId
    ) {
        log.info("🤖 AI Workflow Tool: 修改用户 '{}' 的基本信息", userName);
        if (!permissionService.canManageUsers(currentUserId)) {
            return permissionService.getUserManagementPermissionInfo(currentUserId);
        }
        if (!StringUtils.hasText(newPhone) && !StringUtils.hasText(newEmail)) {
            return "🤔 无任何修改内容。请输入新的手机号或邮箱。";
        }
        try {
            Long userId = jdbcTemplate.queryForObject("SELECT user_id FROM user WHERE name = ?", Long.class, userName);
            
            List<String> updates = new ArrayList<>();
            List<Object> params = new ArrayList<>();
            if (StringUtils.hasText(newPhone)) {
                updates.add("phone = ?");
                params.add(newPhone);
            }
            if (StringUtils.hasText(newEmail)) {
                updates.add("email = ?");
                params.add(newEmail);
            }
            params.add(userId);

            String sql = "UPDATE user SET " + String.join(", ", updates) + " WHERE user_id = ?";
            int result = jdbcTemplate.update(sql, params.toArray());

            if (result > 0) {
                // 清理缓存以确保数据一致性
                performCompleteUserCacheClear(userId);
                return "✅ 用户 '" + userName + "' 的基本信息已更新。";
            } else {
                return "❌ 更新失败: 未找到用户或数据无变化。";
            }
        } catch (EmptyResultDataAccessException e) {
            return "❌ 更新失败: 未找到名为 '" + userName + "' 的用户。";
        } catch (Exception e) {
            log.error("❌ 更新用户 '{}' 基本信息时出错: {}", userName, e.getMessage(), e);
            return "❌ 更新用户基本信息时发生内部错误。";
        }
    }
    
    @Tool("修改【学员】的学籍信息，如年级、班级。")
    @Transactional
    public String updateStudentAcademicInfo(
            @P("要修改的学员的准确姓名") String studentName,
            @P("新的年级 (可选, 不修改则留空)") String newGradeYear,
            @P("新的专业班级 (可选, 不修改则留空)") String newMajorClass,
            @P("当前用户的ID，这个ID由系统在后台自动提供，AI需要直接传递它") Long currentUserId
    ) {
        log.info("🤖 AI Workflow Tool: 修改学员 '{}' 的学籍信息", studentName);
        if (!permissionService.canManageUsers(currentUserId)) {
            return permissionService.getUserManagementPermissionInfo(currentUserId);
        }
        if (!StringUtils.hasText(newGradeYear) && !StringUtils.hasText(newMajorClass)) {
            return "🤔 无任何修改内容。请输入新的年级或专业班级。";
        }
        try {
            Long userId = jdbcTemplate.queryForObject("SELECT user_id FROM user WHERE name = ?", Long.class, studentName);

            List<String> updates = new ArrayList<>();
            List<Object> params = new ArrayList<>();
            if (StringUtils.hasText(newGradeYear)) {
                updates.add("grade_year = ?");
                params.add(newGradeYear);
            }
            if (StringUtils.hasText(newMajorClass)) {
                updates.add("majorClass = ?");
                params.add(newMajorClass);
            }
            params.add(userId);

            String sql = "UPDATE student SET " + String.join(", ", updates) + " WHERE user_id = ?";
            int result = jdbcTemplate.update(sql, params.toArray());

            if (result > 0) {
                // 清理缓存以确保数据一致性
                performCompleteUserCacheClear(userId);
                return "✅ 学员 '" + studentName + "' 的学籍信息已更新。";
            } else {
                return "❌ 更新失败: 未找到学员或数据无变化。";
            }
        } catch (EmptyResultDataAccessException e) {
            return "❌ 更新失败: 未找到名为 '" + studentName + "' 的学员。";
        } catch (Exception e) {
            log.error("❌ 更新学员 '{}' 学籍时出错: {}", studentName, e.getMessage(), e);
            return "❌ 更新学员学籍时发生内部错误。";
        }
    }

    @Tool("修改【老师】的教师信息，如职称、研究方向。")
    @Transactional
    public String updateTeacherInfo(
            @P("要修改的老师的准确姓名") String teacherName,
            @P("新的职称 (可选, 不修改则留空)") String newTitle,
            @P("新的研究方向 (可选, 不修改则留空)") String newResearchDirection,
            @P("当前用户的ID，这个ID由系统在后台自动提供，AI需要直接传递它") Long currentUserId
    ) {
        log.info("🤖 AI Workflow Tool: 修改老师 '{}' 的信息", teacherName);
        if (!permissionService.canManageUsers(currentUserId)) {
            return permissionService.getUserManagementPermissionInfo(currentUserId);
        }
        if (!StringUtils.hasText(newTitle) && !StringUtils.hasText(newResearchDirection)) {
            return "🤔 无任何修改内容。请输入新的职称或研究方向。";
        }
        try {
            Long userId = jdbcTemplate.queryForObject("SELECT user_id FROM user WHERE name = ?", Long.class, teacherName);
            
            List<String> updates = new ArrayList<>();
            List<Object> params = new ArrayList<>();
            if (StringUtils.hasText(newTitle)) {
                updates.add("title = ?");
                params.add(newTitle);
            }
            if (StringUtils.hasText(newResearchDirection)) {
                updates.add("research_direction = ?");
                params.add(newResearchDirection);
            }
            params.add(userId);

            String sql = "UPDATE teacher SET " + String.join(", ", updates) + " WHERE user_id = ?";
            int result = jdbcTemplate.update(sql, params.toArray());

            if (result > 0) {
                // 清理缓存以确保数据一致性
                performCompleteUserCacheClear(userId);
                return "✅ 老师 '" + teacherName + "' 的信息已更新。";
            } else {
                return "❌ 更新失败: 未找到老师或数据无变化。";
            }
        } catch (EmptyResultDataAccessException e) {
            return "❌ 更新失败: 未找到名为 '" + teacherName + "' 的老师。";
        } catch (Exception e) {
            log.error("❌ 更新老师 '{}' 信息时出错: {}", teacherName, e.getMessage(), e);
            return "❌ 更新老师信息时发生内部错误。";
        }
    }

    // ====================================================================================
    // 4. 专门化的删除工具 (请求-确认模式)
    // ====================================================================================



    @Tool("【第一步】发起删除用户的请求。此工具会进行权限和安全检查，并返回一段需要用户确认的文本。")
    public String requestUserDeletion(
            @P("要删除的用户的准确姓名") String userName,
            @P("当前用户的ID，这个ID由系统在后台自动提供，AI需要直接传递它") Long currentUserId
    ) {
        log.info("🤖 AI Workflow Tool: 请求删除用户 '{}'", userName);
        if (!permissionService.canManageUsers(currentUserId)) {
            return permissionService.getUserManagementPermissionInfo(currentUserId);
        }
        try {
            String sql = "SELECT user_id, role_id FROM user WHERE name = ? AND status = '1'";
            Map<String, Object> user = jdbcTemplate.queryForMap(sql, userName);
            Long userIdToDelete = user.get("user_id") instanceof BigInteger ? 
                ((BigInteger) user.get("user_id")).longValue() : (Long) user.get("user_id");

            if (userIdToDelete.equals(currentUserId)) {
                return "❌ 操作失败：不能删除自己的账户。";
            }
            
            // 获取要删除用户的角色
            Long targetRoleId = user.get("role_id") instanceof BigInteger ? 
                ((BigInteger) user.get("role_id")).longValue() : (Long) user.get("role_id");
                
            // 不能删除超级管理员
            if (Objects.equals(targetRoleId, 4L)) {
                return "❌ 操作失败：不能删除超级管理员账户。";
            }
            
            // 🔒 安全检查：管理员只能被超级管理员删除
            if (Objects.equals(targetRoleId, 3L)) { // 目标用户是管理员
                // 获取当前用户的角色
                String currentUserRoleSql = "SELECT role_id FROM user WHERE user_id = ?";
                Map<String, Object> currentUserResult = jdbcTemplate.queryForMap(currentUserRoleSql, currentUserId);
                Long currentUserRoleId = currentUserResult.get("role_id") instanceof BigInteger ? 
                    ((BigInteger) currentUserResult.get("role_id")).longValue() : (Long) currentUserResult.get("role_id");
                    
                if (!Objects.equals(currentUserRoleId, 4L)) { // 当前用户不是超级管理员
                    return "❌ 操作失败：管理员账户只能由超级管理员删除。您当前的权限不足以执行此操作。";
                }
            }

            return "⚠️【严重警告】⚠️\n" +
                   "您确定要永久删除用户 '" + userName + "' 吗？\n" +
                   "此操作将删除该用户的所有数据（包括学籍、考勤、任务等），且 **无法撤销**。\n" +
                   "要确认删除，请调用 `confirmUserDeletion` 工具并提供用户名。";

        } catch (EmptyResultDataAccessException e) {
            return "❌ 操作失败：找不到名为 '" + userName + "' 的活跃用户。";
        } catch (Exception e) {
            log.error("❌ 请求删除用户 '{}' 时出错: {}", userName, e.getMessage(), e);
            return "❌ 系统内部错误，请联系技术支持。";
        }
    }

    @Tool("【第二步】在用户确认后，执行对指定用户的永久删除操作。")
    @Transactional
    public String confirmUserDeletion(
            @P("要删除的用户的准确姓名") String userName,
            @P("当前用户的ID，这个ID由系统在后台自动提供，AI需要直接传递它") Long currentUserId
            ) {
        log.info("🤖 AI Workflow Tool: 确认删除用户 '{}'", userName);
        if (!permissionService.canManageUsers(currentUserId)) {
            return permissionService.getUserManagementPermissionInfo(currentUserId);
        }
        try {
            // 使用安全的查询方式，避免类型转换问题
            String sql = "SELECT user_id, role_id FROM user WHERE name = ?";
            Map<String, Object> queryResult = jdbcTemplate.queryForMap(sql, userName);
            Long userIdToDelete = queryResult.get("user_id") instanceof BigInteger ? 
                ((BigInteger) queryResult.get("user_id")).longValue() : (Long) queryResult.get("user_id");
                
            // 🔒 再次安全检查：管理员只能被超级管理员删除
            Long targetRoleId = queryResult.get("role_id") instanceof BigInteger ? 
                ((BigInteger) queryResult.get("role_id")).longValue() : (Long) queryResult.get("role_id");
                
            if (Objects.equals(targetRoleId, 3L)) { // 目标用户是管理员
                // 获取当前用户的角色
                String currentUserRoleSql = "SELECT role_id FROM user WHERE user_id = ?";
                Map<String, Object> currentUserResult = jdbcTemplate.queryForMap(currentUserRoleSql, currentUserId);
                Long currentUserRoleId = currentUserResult.get("role_id") instanceof BigInteger ? 
                    ((BigInteger) currentUserResult.get("role_id")).longValue() : (Long) currentUserResult.get("role_id");
                    
                if (!Objects.equals(currentUserRoleId, 4L)) { // 当前用户不是超级管理员
                    return "❌ 删除失败：管理员账户只能由超级管理员删除。权限验证失败。";
                }
            }
            
            // 级联删除关联数据 - 确保数据完整性
            // 首先获取student_id，用于删除关联数据
            Long studentId = null;
            try {
                String getStudentIdSql = "SELECT student_id FROM student WHERE user_id = ?";
                Map<String, Object> studentResult = jdbcTemplate.queryForMap(getStudentIdSql, userIdToDelete);
                studentId = studentResult.get("student_id") instanceof BigInteger ? 
                    ((BigInteger) studentResult.get("student_id")).longValue() : (Long) studentResult.get("student_id");
            } catch (EmptyResultDataAccessException e) {
                log.debug("用户 {} 不是学员，跳过学员相关数据删除", userIdToDelete);
            }
            
            // 1. 删除学员选课记录 (必须在删除student记录之前，因为有ON DELETE RESTRICT约束)
            if (studentId != null) {
                int deletedCourseRecords = jdbcTemplate.update("DELETE FROM student_course WHERE student_id = ?", studentId);
                if (deletedCourseRecords > 0) {
                    log.debug("🗑️ 删除选课记录: {} 条", deletedCourseRecords);
                }
                
                // 2. 删除活动预约记录 (必须在删除student记录之前，因为有ON DELETE RESTRICT约束)
                int deletedReservationRecords = jdbcTemplate.update("DELETE FROM activity_reservation WHERE student_id = ?", studentId);
                if (deletedReservationRecords > 0) {
                    log.debug("🗑️ 删除活动预约记录: {} 条", deletedReservationRecords);
                }
                
                // 3. 删除值班安排关联记录
                int deletedDutyRecords = jdbcTemplate.update("DELETE FROM duty_schedule_student WHERE student_id = ?", studentId);
                if (deletedDutyRecords > 0) {
                    log.debug("🗑️ 删除值班安排记录: {} 条", deletedDutyRecords);
                }
                
                // 4. 删除请假申请记录
                int deletedLeaveRecords = jdbcTemplate.update("DELETE FROM leave_request WHERE student_id = ?", studentId);
                if (deletedLeaveRecords > 0) {
                    log.debug("🗑️ 删除请假申请记录: {} 条", deletedLeaveRecords);
                }
            }
            
            // 5. 现在可以安全地删除学员记录 (student_direction和attendance_record会自动CASCADE删除)
            int deletedStudentRecords = jdbcTemplate.update("DELETE FROM student WHERE user_id = ?", userIdToDelete);
            if (deletedStudentRecords > 0) {
                log.debug("🗑️ 删除学员记录: {} 条", deletedStudentRecords);
            }
            
            // 2. 删除教师相关数据  
            int deletedTeacherRecords = jdbcTemplate.update("DELETE FROM teacher WHERE user_id = ?", userIdToDelete);
            if (deletedTeacherRecords > 0) {
                log.debug("🗑️ 删除教师记录: {} 条", deletedTeacherRecords);
            }
            
            // 3. 删除其他关联数据 (如果有的话)
            // jdbcTemplate.update("DELETE FROM student_course WHERE user_id = ?", userIdToDelete);
            // jdbcTemplate.update("DELETE FROM attendance_record WHERE user_id = ?", userIdToDelete);
            
            // 4. 最后删除用户主记录
            int result = jdbcTemplate.update("DELETE FROM user WHERE user_id = ?", userIdToDelete);
            
            if (result > 0) {
                log.info("✅ 用户 '{}' (ID: {}) 已被用户 {} 永久删除。", userName, userIdToDelete, currentUserId);
                
                // 清理缓存以确保数据一致性
                performCompleteUserCacheClear(userIdToDelete);
                
                return "✅ 用户 '" + userName + "' 已被永久删除。";
            }
            return "❌ 删除失败：数据库操作未影响任何行。";
        } catch (EmptyResultDataAccessException e) {
            return "❌ 删除失败：在执行删除时找不到用户 '" + userName + "'。";
        } catch (Exception e) {
            log.error("❌ 确认删除用户 '{}' 时出错: {}", userName, e.getMessage(), e);
            return "❌ 删除用户时发生严重的内部错误。";
        }
    }
} 
