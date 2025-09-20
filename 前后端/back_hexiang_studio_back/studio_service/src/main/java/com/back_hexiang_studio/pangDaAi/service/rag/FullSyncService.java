package com.back_hexiang_studio.pangDaAi.service.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 全量同步服务
 * 
 * 🔄 专门负责向量数据库的全量重建
 * 用于替代旧的DataSyncService中的全量同步功能
 * 
 * 特性：
 * - 独立于业务流程，专注于全量数据同步
 * - 支持手动触发的向量索引重建
 * - 从MySQL批量提取数据并向量化
 * - 提供详细的同步进度和统计信息
 * - 启动时自动检查并初始化向量数据库
 * 
 * @author 胖达AI助手开发团队
 * @version 1.0 - 现代RAG架构
 * @since 2025-09-14
 */
@Service
@Slf4j
public class FullSyncService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private VectorStoreService vectorStoreService;

    @Value("${pangda-ai.rag.sync.batch-size:50}")
    private int batchSize;

    @Value("${pangda-ai.rag.sync.max-content-length:2000}")
    private int maxContentLength;

    @Value("${pangda-ai.rag.sync.auto-init:true}")
    private boolean autoInit;

    /**
     * 应用启动完成后自动检查并初始化向量数据库
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void onApplicationReady() {
        if (!autoInit) {
            log.info("⏭️ 自动初始化已禁用，跳过向量数据库检查");
            return;
        }

        log.info("🔍 检查向量数据库是否需要初始化...");
        
        try {
            // 检查向量数据库是否为空
            VectorStoreService.VectorStoreStats stats = vectorStoreService.getStats();
            if (stats.getDocumentCount() == 0) {
                log.info("📭 向量数据库为空，开始自动初始化...");
                
                // 延迟5秒启动，确保所有服务已准备就绪
                Thread.sleep(5000);
                
                SyncResult result = syncAll();
                if (result.isSuccess()) {
                    log.info("✅ 向量数据库自动初始化完成！同步了 {} 条数据", result.getTotalSynced());
                } else {
                    log.warn("⚠️ 向量数据库自动初始化失败：{}", result.getErrorMessage());
                }
            } else {
                log.info("✅ 向量数据库已存在 {} 条数据，跳过初始化", stats.getDocumentCount());
            }
            
        } catch (Exception e) {
            log.error("❌ 向量数据库自动初始化失败", e);
        }
    }

    /**
     * 全量同步所有数据
     * 手动触发的完整重建过程
     */
    public SyncResult syncAll() {
        log.info("🔄 开始全量数据同步（现代RAG架构）...");
        long startTime = System.currentTimeMillis();
        
        SyncResult result = new SyncResult();
        
        try {
            // 1. 清空现有向量数据
            vectorStoreService.clear();
            log.info("🗑️ 已清空现有向量数据");
            
            // 2. 同步核心静态知识数据（减少AI工具调用压力）
            log.info("📚 开始同步核心静态知识数据...");
            
            // 2.1 工作室基本信息（静态知识）
            result.addResult("studio_info", syncStudioInfo());
            result.addResult("support_contact", syncSupportContacts());
            
            // 2.2 组织架构信息（较稳定的结构数据）
            result.addResult("department", syncDepartments());
            result.addResult("training_direction", syncTrainingDirections());
            
            // 2.3 工具使用指南（RAG核心价值：告诉AI何时用什么工具）
            result.addResult("tool_guide", syncToolGuide());
            
            // 🗑️ 删除冗余同步：AI能力介绍、用户、学生、老师、角色、职位、荣誉、证书等
            
            log.info("✅ 核心静态知识同步完成");
            log.info("ℹ️  人员信息、公告、任务、资料等动态数据通过工具实时查询，确保数据准确性");
            
            // 5. 如果是内存存储，则持久化到文件
            if (vectorStoreService.getClass().getSimpleName().contains("InMemory")) {
                vectorStoreService.persistToFile();
            }
            
            long duration = System.currentTimeMillis() - startTime;
            result.setDurationMs(duration);
            result.setSuccess(true);
            
            log.info("✅ 全量同步完成！耗时: {}ms, 总计: {}条", duration, result.getTotalSynced());
            
            return result;
            
        } catch (Exception e) {
            log.error("❌ 全量同步失败: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            return result;
        }
    }





    /**
     * 同步工作室信息
     */
    private int syncStudioInfo() {
        log.info("🏢 同步工作室信息...");
        
        String sql = "SELECT id, name, establish_time, director, member_count, project_count, awards, " +
                     "phone, email, address, room " +
                     "FROM studio_info " +
                     "ORDER BY id DESC";
        
        List<Map<String, Object>> studioInfos = jdbcTemplate.queryForList(sql);
        
        // ✨ 动态计算实际成员数量，确保数据准确性
        long actualMemberCount;
        try {
            String countSql = "SELECT COUNT(*) FROM user WHERE status = '1'";
            Long count = jdbcTemplate.queryForObject(countSql, Long.class);
            actualMemberCount = (count != null) ? count : 0;
            log.info("📊 动态计算工作室成员数量: {}", actualMemberCount);
        } catch (Exception e) {
            log.warn("⚠️ 无法动态计算成员数量，将使用数据库中的值", e);
            actualMemberCount = -1; // -1 表示计算失败
        }
        
        int syncedCount = 0;
        for (Map<String, Object> studioInfo : studioInfos) {
            try {
                // 创建一个可变副本以修改成员数量
                Map<String, Object> mutableStudioInfo = new HashMap<>(studioInfo);
                
                // 如果动态计算成功，则更新成员数量
                if (actualMemberCount != -1) {
                    Object dbMemberCount = mutableStudioInfo.get("member_count");
                    if (dbMemberCount == null || ((Number)dbMemberCount).longValue() != actualMemberCount) {
                        log.info("🔄 成员数量已更新: 数据库值 '{}' -> 动态计算值 '{}'", dbMemberCount, actualMemberCount);
                    }
                    mutableStudioInfo.put("member_count", actualMemberCount);
                }

                String content = buildStudioInfoContent(mutableStudioInfo);
                Long businessId = ((Number) mutableStudioInfo.get("id")).longValue();
                
                vectorStoreService.upsert("studio_info", businessId, content);
                
                syncedCount++;
                
            } catch (Exception e) {
                log.warn("⚠️ 同步工作室信息失败 [ID: {}]: {}", studioInfo.get("id"), e.getMessage());
            }
        }
        
        log.info("✅ 工作室信息同步完成: {}/{}", syncedCount, studioInfos.size());
        return syncedCount;
    }

    /**
     * 同步技术支持联系人
     */
    private int syncSupportContacts() {
        log.info("📞 同步技术支持联系人...");
        
        String sql = "SELECT id, name, phone, email, position " +
                     "FROM support_contact " +
                     "WHERE status = '1' " +  // 只同步有效的联系人
                     "ORDER BY id ASC";
        
        List<Map<String, Object>> supportContacts = jdbcTemplate.queryForList(sql);
        
        int syncedCount = 0;
        for (Map<String, Object> supportContact : supportContacts) {
            try {
                String content = buildSupportContactContent(supportContact);
                Long businessId = ((Number) supportContact.get("id")).longValue();
                
                vectorStoreService.upsert("support_contact", businessId, content);
                
                syncedCount++;
                
            } catch (Exception e) {
                log.warn("⚠️ 同步技术支持联系人失败 [ID: {}]: {}", supportContact.get("id"), e.getMessage());
            }
        }
        
        log.info("✅ 技术支持联系人同步完成: {}/{}", syncedCount, supportContacts.size());
        return syncedCount;
    }

    /**
     * 同步工具使用说明 - RAG核心价值：减少AI工具调用时的困惑
     */
    private int syncToolGuide() {
        log.info("🔧 同步工具使用说明...");
        
        try {
            // 构建工具分类和使用说明
            StringBuilder toolGuide = new StringBuilder();
            toolGuide.append("何湘工作室AI助手工具使用指南\n\n");
            
            // 用户管理工具
            toolGuide.append("👥 用户管理工具：\n");
            toolGuide.append("- 查询用户档案：获取当前用户或指定用户的详细信息\n");
            toolGuide.append("- 用户增删改：需要管理员权限，先检查权限再操作\n");
            toolGuide.append("- 获取成员列表：查看工作室所有成员基本信息\n\n");
            
            // 工作室信息工具
            toolGuide.append("🏢 工作室信息工具：\n");
            toolGuide.append("- 成员统计：查询部门设置和人数分布\n");
            toolGuide.append("- 部门详情：获取特定部门的详细信息和成员列表\n\n");
            
            // 考勤管理工具
            toolGuide.append("📅 考勤管理工具：\n");
            toolGuide.append("- 考勤统计：查询指定日期的考勤情况\n\n");
            
            // 任务管理工具
            toolGuide.append("📝 任务管理工具：\n");
            toolGuide.append("- 用户任务：查询指定用户的任务列表\n");
            toolGuide.append("- 我的任务：获取当前用户的未完成任务\n\n");
            
            // 公告管理工具
            toolGuide.append("📢 公告管理工具：\n");
            toolGuide.append("- 查询公告：获取最新公告信息\n");
            toolGuide.append("- 公告管理：需要权限验证的增删改操作\n\n");
            
            // 课程管理工具
            toolGuide.append("📚 课程管理工具：\n");
            toolGuide.append("- 课程列表：查询所有可用课程\n");
            toolGuide.append("- 培训方向：查询培训方向列表\n\n");
            
            // 外部API工具
            toolGuide.append("🌐 外部API工具：\n");
            toolGuide.append("- 天气查询：获取今日天气或未来几天预报\n");
            toolGuide.append("- 新闻资讯：获取今日新闻或指定日期新闻\n\n");
            
            // 数据处理工具
            toolGuide.append("📊 数据处理工具：\n");
            toolGuide.append("- 表格转换：将文本数据转换为表格JSON格式\n\n");
            
            toolGuide.append("重要提醒：\n");
            toolGuide.append("- 所有管理操作（增删改）都需要先检查权限\n");
            toolGuide.append("- 优先使用工具获取实时数据，而不是依赖RAG中的静态信息\n");
            toolGuide.append("- 工具调用失败时可以基于RAG背景信息回答");
            
            vectorStoreService.upsert("tool_guide", 1L, toolGuide.toString());
            
            log.info("✅ 工具使用说明同步完成");
            return 1;
            
        } catch (Exception e) {
            log.error("❌ 工具使用说明同步失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    // 🗑️ 已删除 syncAiCapabilities - 减少冗余代码

    // 🗑️ 已删除 syncUsers - 人员信息通过工具实时查询

    // 🗑️ 已删除 syncStudents - 学生信息通过工具实时查询

    /**
     * 同步部门信息
     */
    private int syncDepartments() {
        log.info("🏢 同步部门信息...");
        
        String sql = "SELECT department_id as id, department_name, create_time " +
                     "FROM department " +
                     "ORDER BY department_id ASC";
        
        List<Map<String, Object>> departments = jdbcTemplate.queryForList(sql);
        
        int syncedCount = 0;
        for (Map<String, Object> department : departments) {
            try {
                String content = buildDepartmentContent(department);
                Long businessId = ((Number) department.get("id")).longValue();
                
                vectorStoreService.upsert("department", businessId, content);
                
                syncedCount++;
                
            } catch (Exception e) {
                log.warn("⚠️ 同步部门失败 [ID: {}]: {}", department.get("id"), e.getMessage());
            }
        }
        
        log.info("✅ 部门同步完成: {}/{}", syncedCount, departments.size());
        return syncedCount;
    }









    // ===================================================================
    // 文本构建方法
    // ===================================================================


    /**
     * 构建课程的向量化文本
     */
    private String buildCourseContent(Map<String, Object> course) {
        StringBuilder content = new StringBuilder();
        
        if (course.get("name") != null) {
            content.append("课程名称：").append(course.get("name")).append("\n");
        }
        
        if (course.get("description") != null) {
            content.append("课程描述：").append(course.get("description")).append("\n");
        }
        
        if (course.get("duration") != null) {
            content.append("课程时长：").append(course.get("duration")).append("\n");
        }
        
        if (course.get("location") != null) {
            content.append("上课地点：").append(course.get("location")).append("\n");
        }
        
        if (course.get("schedule") != null) {
            content.append("时间安排：").append(course.get("schedule")).append("\n");
        }
        
        return content.toString().trim();
    }


        
    

    /**
     * 构建工作室信息的向量化文本（增强版：包含同义词和多种表达方式）
     */
    private String buildStudioInfoContent(Map<String, Object> studioInfo) {
        StringBuilder content = new StringBuilder();
        
        // 添加文档类型标识和同义词 - 提高匹配率
        content.append("这是何湘工作室 | 何湘技能大师工作室 | 工作室基本信息 | 工作室概况 | 工作室详情\n");
        content.append("工作室相关 | 团队相关 | 组织相关 | 机构相关\n");
        
        if (studioInfo.get("name") != null) {
            String name = studioInfo.get("name").toString();
            content.append("工作室名称：").append(name).append("\n");
            content.append("工作室全称：").append(name).append("\n");
            content.append("团队名称：").append(name).append("\n");
            content.append("组织名称：").append(name).append("\n");
            content.append("机构名称：").append(name).append("\n");
            content.append("工作室：").append(name).append("\n");
        }
        
        if (studioInfo.get("establish_time") != null) {
            String establishTime = studioInfo.get("establish_time").toString();
            content.append("成立时间：").append(establishTime).append("\n");
            content.append("创建时间：").append(establishTime).append("\n");
            content.append("建立时间：").append(establishTime).append("\n");
            content.append("成立日期：").append(establishTime).append("\n");
            content.append("创办时间：").append(establishTime).append("\n");
        }
        
        if (studioInfo.get("director") != null) {
            String director = studioInfo.get("director").toString();
            content.append("负责人：").append(director).append("\n");
            content.append("工作室负责人：").append(director).append("\n");
            content.append("主任：").append(director).append("\n");
            content.append("工作室主任：").append(director).append("\n");
            content.append("领导：").append(director).append("\n");
            content.append("工作室领导：").append(director).append("\n");
            content.append("导师：").append(director).append("\n");
            content.append("指导老师：").append(director).append("\n");
        }
        
        if (studioInfo.get("member_count") != null) {
            String memberCount = studioInfo.get("member_count").toString();
            content.append("成员数量：").append(memberCount).append("人\n");
            content.append("人员数量：").append(memberCount).append("人\n");
            content.append("团队人数：").append(memberCount).append("人\n");
            content.append("工作室人数：").append(memberCount).append("人\n");
            content.append("总人数：").append(memberCount).append("人\n");
            content.append("成员：").append(memberCount).append("人\n");
            content.append("有多少人：").append(memberCount).append("人\n");
            content.append("多少成员：").append(memberCount).append("人\n");
        }
        
      
        
        if (studioInfo.get("awards") != null) {
            String awards = studioInfo.get("awards").toString();
            content.append("获奖情况：").append(awards).append("\n");
            content.append("荣誉奖项：").append(awards).append("\n");
            content.append("获得奖项：").append(awards).append("\n");
            content.append("奖项成果：").append(awards).append("\n");
            content.append("荣誉情况：").append(awards).append("\n");
            content.append("获奖成果：").append(awards).append("\n");
        }
        
        if (studioInfo.get("phone") != null) {
            String phone = studioInfo.get("phone").toString();
            content.append("联系电话：").append(phone).append("\n");
            content.append("工作室电话：").append(phone).append("\n");
            content.append("办公电话：").append(phone).append("\n");
            content.append("电话号码：").append(phone).append("\n");
            content.append("联系方式：").append(phone).append("\n");
            content.append("咨询电话：").append(phone).append("\n");
        }
        
        if (studioInfo.get("email") != null) {
            String email = studioInfo.get("email").toString();
            content.append("电子邮箱：").append(email).append("\n");
            content.append("工作室邮箱：").append(email).append("\n");
            content.append("联系邮箱：").append(email).append("\n");
            content.append("邮箱地址：").append(email).append("\n");
            content.append("邮件地址：").append(email).append("\n");
            content.append("邮箱：").append(email).append("\n");
        }
        
        if (studioInfo.get("address") != null) {
            String address = studioInfo.get("address").toString();
            content.append("地址：").append(address).append("\n");
            content.append("工作室地址：").append(address).append("\n");
            content.append("办公地址：").append(address).append("\n");
            content.append("详细地址：").append(address).append("\n");
            content.append("所在地址：").append(address).append("\n");
            content.append("位置：").append(address).append("\n");
            content.append("工作室位置：").append(address).append("\n");
            content.append("工作室在哪里：").append(address).append("\n");
            content.append("在哪里：").append(address).append("\n");
        }
        
        if (studioInfo.get("room") != null) {
            String room = studioInfo.get("room").toString();
            content.append("房间号：").append(room).append("\n");
            content.append("办公室：").append(room).append("\n");
            content.append("具体房间：").append(room).append("\n");
            content.append("房间：").append(room).append("\n");
            content.append("办公房间：").append(room).append("\n");
            content.append("工作室房间：").append(room).append("\n");
        }
        
        return content.toString().trim();
    }

    /**
     * 构建技术支持联系人的向量化文本
     */
    private String buildSupportContactContent(Map<String, Object> supportContact) {
        StringBuilder content = new StringBuilder();
        
        if (supportContact.get("name") != null) {
            content.append("联系人名称：").append(supportContact.get("name")).append("\n");
        }
        
        if (supportContact.get("phone") != null) {
            content.append("联系电话：").append(supportContact.get("phone")).append("\n");
        }
        
        if (supportContact.get("email") != null) {
            content.append("电子邮箱：").append(supportContact.get("email")).append("\n");
        }
        
        if (supportContact.get("position") != null) {
            content.append("职位：").append(supportContact.get("position")).append("\n");
        }
        
        return content.toString().trim();
    }

    /**
     * 构建用户基础信息的向量化文本
     */
    private String buildUserContent(Map<String, Object> user) {
        StringBuilder content = new StringBuilder();
        
        // 添加文档类型标识和同义词
        content.append("这是工作室成员 | 工作室人员 | 团队成员 | 用户信息 | 人员档案\n");
        content.append("成员相关 | 人员相关 | 用户相关 | 团队相关\n");
        
        // 🎯 关键修复：添加用户ID信息
        if (user.get("id") != null) {
            String userId = user.get("id").toString();
            content.append("用户ID：").append(userId).append("\n");
            content.append("用户ID: ").append(userId).append("\n");
            content.append("用户编号：").append(userId).append("\n");
            content.append("ID：").append(userId).append("\n");
            content.append("ID: ").append(userId).append("\n");
            content.append("用户标识：").append(userId).append("\n");
        }
        
        if (user.get("name") != null) {
            String name = user.get("name").toString();
            content.append("姓名：").append(name).append("\n");
            content.append("名字：").append(name).append("\n");
            content.append("成员姓名：").append(name).append("\n");
            content.append("人员姓名：").append(name).append("\n");
        }
        
        if (user.get("sex") != null) {
            String sexValue = user.get("sex").toString();
            String sex;
            if ("1".equals(sexValue)) {
                sex = "男";
            } else if ("0".equals(sexValue) || "2".equals(sexValue)) {
                sex = "女";
            } else {
                sex = sexValue;
            }
            content.append("性别：").append(sex).append("\n");
        }
        
        if (user.get("phone") != null) {
            String phone = user.get("phone").toString();
            content.append("联系电话：").append(phone).append("\n");
            content.append("手机号：").append(phone).append("\n");
            content.append("电话号码：").append(phone).append("\n");
            content.append("联系方式：").append(phone).append("\n");
        }
        
        if (user.get("email") != null) {
            String email = user.get("email").toString();
            content.append("电子邮箱：").append(email).append("\n");
            content.append("邮箱地址：").append(email).append("\n");
            content.append("邮件地址：").append(email).append("\n");
        }
        
        if (user.get("role_name") != null) {
            String role = user.get("role_name").toString();
            content.append("角色：").append(role).append("\n");
            content.append("身份：").append(role).append("\n");
            content.append("用户角色：").append(role).append("\n");
        }
        
        if (user.get("position_name") != null) {
            String position = user.get("position_name").toString();
            content.append("职位：").append(position).append("\n");
            content.append("职务：").append(position).append("\n");
            content.append("岗位：").append(position).append("\n");

            // ✨ 新增：为常见教师岗位添加明确的同义词和身份标识
            if (position.contains("导师") || position.contains("老师") || position.contains("讲师")) {
                content.append("身份标识：教师 | 导师 | 讲师 | 教学人员 | 指导老师\n");
            }
        }
        
        if (user.get("create_time") != null) {
            content.append("加入时间：").append(user.get("create_time")).append("\n");
            content.append("入团时间：").append(user.get("create_time")).append("\n");
        }

        if (user.get("department_name") != null) {
            String department = user.get("department_name").toString();
            content.append("所属部门：").append(department).append("\n");
            content.append("部门：").append(department).append("\n");
        }
        
        return content.toString().trim();
    }

    /**
     * 构建学生详细信息的向量化文本
     */
    private String buildStudentContent(Map<String, Object> student) {
        StringBuilder content = new StringBuilder();
        
        // 添加文档类型标识
        content.append("这是工作室学生 | 学生信息 | 学员档案 | 培训学员\n");
        content.append("学生相关 | 学员相关 | 培训相关\n");
        
        // 🎯 关键修复：添加用户ID信息
        if (student.get("user_id") != null) {
            String userId = student.get("user_id").toString();
            content.append("用户ID：").append(userId).append("\n");
            content.append("用户ID: ").append(userId).append("\n");
            content.append("用户编号：").append(userId).append("\n");
            content.append("ID：").append(userId).append("\n");
            content.append("ID: ").append(userId).append("\n");
            content.append("学生用户ID：").append(userId).append("\n");
        }
        
        if (student.get("name") != null) {
            content.append("学生姓名：").append(student.get("name")).append("\n");
        }
        
        if (student.get("student_number") != null) {
            content.append("学号：").append(student.get("student_number")).append("\n");
        }
        
        if (student.get("sex") != null) {
            String sexValue = student.get("sex").toString();
            String sex;
            if ("1".equals(sexValue)) {
                sex = "男";
            } else if ("0".equals(sexValue) || "2".equals(sexValue)) {
                sex = "女";
            } else {
                sex = sexValue;
            }
            content.append("性别：").append(sex).append("\n");
        }
        
        if (student.get("grade_year") != null) {
            content.append("年级：").append(student.get("grade_year")).append("\n");
        }
        
        if (student.get("majorClass") != null) {
            content.append("专业班级：").append(student.get("majorClass")).append("\n");
        }
        
        if (student.get("direction_name") != null) {
            content.append("培训方向：").append(student.get("direction_name")).append("\n");
        }
        
        if (student.get("department_name") != null) {
            content.append("所属部门：").append(student.get("department_name")).append("\n");
        }
        
        if (student.get("counselor") != null) {
            content.append("辅导员：").append(student.get("counselor")).append("\n");
        }
        
        if (student.get("dormitory") != null) {
            content.append("宿舍：").append(student.get("dormitory")).append("\n");
        }
        
        if (student.get("phone") != null) {
            content.append("联系电话：").append(student.get("phone")).append("\n");
        }
        
        if (student.get("email") != null) {
            content.append("电子邮箱：").append(student.get("email")).append("\n");
        }
        
        if (student.get("score") != null) {
            content.append("成绩/学分：").append(student.get("score")).append("\n");
        }
        
        return content.toString().trim();
    }

    /**
     * 构建部门信息的向量化文本
     */
    private String buildDepartmentContent(Map<String, Object> department) {
        StringBuilder content = new StringBuilder();
        
        // 添加文档类型标识和同义词
        content.append("这是工作室部门信息 | 组织架构 | 部门列表 | 组织部门 | 团队组织 | 工作室部门\n");
        content.append("部门相关 | 组织相关 | 架构相关 | 团队相关 | 部门设置 | 工作室组织\n");
        
        String deptName = null;
        if (department.get("department_name") != null) {
            deptName = department.get("department_name").toString();
            content.append("部门名称：").append(deptName).append("\n");
            content.append("部门：").append(deptName).append("\n");
            content.append("组织单位：").append(deptName).append("\n");
            content.append("团队：").append(deptName).append("\n");
        }
        
        if (department.get("create_time") != null) {
            content.append("成立时间：").append(department.get("create_time")).append("\n");
            content.append("创建时间：").append(department.get("create_time")).append("\n");
        }
        
        // 查询并添加部门人员统计信息
        Long departmentId = ((Number) department.get("id")).longValue();
        try {
            // 查询部门学生数量
            String studentCountSql = "SELECT COUNT(*) FROM student WHERE department_id = ?";
            Long studentCount = jdbcTemplate.queryForObject(studentCountSql, Long.class, departmentId);
            if (studentCount != null && studentCount > 0) {
                content.append("部门学生数量：").append(studentCount).append("人\n");
                content.append("学生人数：").append(studentCount).append("人\n");
                content.append("成员数量：").append(studentCount).append("人\n");
            }
            
            // 查询部门学生信息
            String studentInfoSql = "SELECT u.name, s.student_number, s.grade_year " +
                                   "FROM student s " +
                                   "LEFT JOIN user u ON s.user_id = u.user_id " +
                                   "WHERE s.department_id = ? AND u.status = '1' " +
                                   "ORDER BY s.grade_year DESC, s.student_number ASC";
            List<Map<String, Object>> students = jdbcTemplate.queryForList(studentInfoSql, departmentId);
            if (!students.isEmpty()) {
                content.append("部门成员 | 部门学生 | 团队成员：\n");
                for (Map<String, Object> student : students) {
                    content.append("  - ").append(student.get("name"));
                    if (student.get("student_number") != null) {
                        content.append(" (学号: ").append(student.get("student_number")).append(")");
                    }
                    if (student.get("grade_year") != null) {
                        content.append(" [").append(student.get("grade_year")).append("级]");
                    }
                    content.append("\n");
                }
            }
            
        } catch (Exception e) {
            log.debug("查询部门人员信息失败 [部门ID: {}]: {}", departmentId, e.getMessage());
        }
        
        // 添加通用的部门查询关键词，提高匹配概率
        content.append("\n工作室有哪些部门 | 部门列表 | 组织架构 | 团队组织 | 部门组成\n");
        content.append("部门情况 | 部门介绍 | 组织情况 | 团队情况 | 部门设置\n");
        content.append("工作室部门结构 | 工作室组织架构 | 工作室团队配置\n");
        content.append("何湘工作室部门 | 何湘技能大师工作室部门 | 工作室内部组织\n");
        
        // 🎯 为UI设计部增加更多匹配关键词
        if (deptName != null && (deptName.contains("UI") || deptName.contains("设计"))) {
            content.append("UI设计 | 用户界面设计 | 视觉设计 | 交互设计 | 前端设计\n");
            content.append("设计部门 | 设计团队 | 美工部门 | 界面设计部\n");
        }
        
        return content.toString().trim();
    }

    // ===================================================================
    // 工具方法
    // ===================================================================
    
    /**
     * 格式化文件大小显示
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    // ===================================================================
    // 结果类（重用DataSyncService中的SyncResult）
    // ===================================================================
    
    public static class SyncResult {
        private boolean success = false;
        private String errorMessage;
        private long durationMs;
        private Map<String, Integer> results = new HashMap<>();
        
        public void addResult(String type, int count) {
            results.put(type, count);
        }
        
        public int getTotalSynced() {
            return results.values().stream().mapToInt(Integer::intValue).sum();
        }
        
        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public long getDurationMs() { return durationMs; }
        public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
        
        public Map<String, Integer> getResults() { return results; }
    }

    
    /**
     * 🏆 同步用户荣誉数据
     */
    private int syncHonors() {
        log.info("🏆 同步用户荣誉数据...");
        
        String sql = "SELECT honors_id as id, user_id, honor_name, honor_level, issue_org, " +
                     "issue_date, certificate_no, description, create_time " +
                     "FROM user_honors " +
                     "ORDER BY create_time DESC";
        
        List<Map<String, Object>> honors = jdbcTemplate.queryForList(sql);
        
        int syncedCount = 0;
        for (Map<String, Object> honor : honors) {
            try {
                String content = buildHonorContent(honor);
                Long businessId = ((Number) honor.get("id")).longValue();
                
                vectorStoreService.upsert("honor", businessId, content);
                
                syncedCount++;
                
            } catch (Exception e) {
                log.warn("⚠️ 同步荣誉失败 [ID: {}]: {}", honor.get("id"), e.getMessage());
            }
        }
        
        log.info("✅ 荣誉同步完成: {}/{}", syncedCount, honors.size());
        return syncedCount;
    }
    
    /**
     * 🎖️ 同步用户证书数据
     */
    private int syncCertificates() {
        log.info("🎖️ 同步用户证书数据...");
        
        String sql = "SELECT certificate_id as id, user_id, certificate_name, certificate_level, " +
                     "certificate_no, issue_org, issue_date, expiry_date, description, " +
                     "verification_url, create_time " +
                     "FROM user_certificate " +
                     "ORDER BY create_time DESC";
        
        List<Map<String, Object>> certificates = jdbcTemplate.queryForList(sql);
        
        int syncedCount = 0;
        for (Map<String, Object> certificate : certificates) {
            try {
                String content = buildCertificateContent(certificate);
                Long businessId = ((Number) certificate.get("id")).longValue();
                
                vectorStoreService.upsert("certificate", businessId, content);
                
                syncedCount++;
                
            } catch (Exception e) {
                log.warn("⚠️ 同步证书失败 [ID: {}]: {}", certificate.get("id"), e.getMessage());
            }
        }
        
        log.info("✅ 证书同步完成: {}/{}", syncedCount, certificates.size());
        return syncedCount;
    }
    
    /**
     * 构建荣誉的向量化文本
     */
    private String buildHonorContent(Map<String, Object> honor) {
        StringBuilder content = new StringBuilder();
        
        // 添加文档类型标识和同义词
        content.append("这是用户荣誉 | 获奖信息 | 成就记录 | 荣誉证书 | 奖项\n");
        content.append("荣誉相关 | 获奖相关 | 成就相关 | 奖励相关 | 表彰相关\n");
        
        if (honor.get("user_id") != null) {
            content.append("用户ID：").append(honor.get("user_id")).append("\n");
        }
        
        if (honor.get("honor_name") != null) {
            String honorName = honor.get("honor_name").toString();
            content.append("荣誉名称：").append(honorName).append("\n");
            content.append("获奖名称：").append(honorName).append("\n");
            content.append("奖项名称：").append(honorName).append("\n");
        }
        
        if (honor.get("honor_level") != null) {
            String level = honor.get("honor_level").toString();
            content.append("荣誉级别：").append(level).append("\n");
            content.append("获奖等级：").append(level).append("\n");
        }
        
        if (honor.get("issue_org") != null) {
            String org = honor.get("issue_org").toString();
            content.append("颁发机构：").append(org).append("\n");
            content.append("颁奖单位：").append(org).append("\n");
        }
        
        if (honor.get("issue_date") != null) {
            content.append("颁发日期：").append(honor.get("issue_date")).append("\n");
            content.append("获奖时间：").append(honor.get("issue_date")).append("\n");
        }
        
        if (honor.get("certificate_no") != null) {
            content.append("证书编号：").append(honor.get("certificate_no")).append("\n");
        }
        
        if (honor.get("description") != null) {
            String desc = honor.get("description").toString();
            content.append("荣誉描述：").append(desc).append("\n");
            content.append("获奖说明：").append(desc).append("\n");
        }
        
        return content.toString().trim();
    }
    
    /**
     * 构建证书的向量化文本
     */
    private String buildCertificateContent(Map<String, Object> certificate) {
        StringBuilder content = new StringBuilder();
        
        // 添加文档类型标识和同义词
        content.append("这是用户证书 | 资格证书 | 认证证书 | 职业证书 | 技能认证\n");
        content.append("证书相关 | 认证相关 | 资格相关 | 技能相关 | 资质相关\n");
        
        if (certificate.get("user_id") != null) {
            content.append("用户ID：").append(certificate.get("user_id")).append("\n");
        }
        
        if (certificate.get("certificate_name") != null) {
            String certName = certificate.get("certificate_name").toString();
            content.append("证书名称：").append(certName).append("\n");
            content.append("认证名称：").append(certName).append("\n");
        }
        
        if (certificate.get("certificate_level") != null) {
            String level = certificate.get("certificate_level").toString();
            content.append("证书级别：").append(level).append("\n");
            content.append("认证等级：").append(level).append("\n");
        }
        
        if (certificate.get("certificate_no") != null) {
            content.append("证书编号：").append(certificate.get("certificate_no")).append("\n");
        }
        
        if (certificate.get("issue_org") != null) {
            String org = certificate.get("issue_org").toString();
            content.append("颁发机构：").append(org).append("\n");
            content.append("认证机构：").append(org).append("\n");
        }
        
        if (certificate.get("issue_date") != null) {
            content.append("颁发日期：").append(certificate.get("issue_date")).append("\n");
            content.append("认证日期：").append(certificate.get("issue_date")).append("\n");
        }
        
        if (certificate.get("expiry_date") != null) {
            content.append("到期日期：").append(certificate.get("expiry_date")).append("\n");
            content.append("有效期至：").append(certificate.get("expiry_date")).append("\n");
        }
        
        if (certificate.get("description") != null) {
            String desc = certificate.get("description").toString();
            content.append("证书描述：").append(desc).append("\n");
            content.append("认证说明：").append(desc).append("\n");
        }
        
        if (certificate.get("verification_url") != null) {
            content.append("验证链接：").append(certificate.get("verification_url")).append("\n");
        }
        
        return content.toString().trim();
    }
    
    /**
     * 🎯 同步培训方向数据
     */
    private int syncTrainingDirections() {
        log.info("🎯 同步培训方向数据...");
        
        String sql = "SELECT direction_id as id, direction_name, description, create_time " +
                     "FROM training_direction " +
                     "ORDER BY direction_id";
        
        List<Map<String, Object>> directions = jdbcTemplate.queryForList(sql);
        
        int syncedCount = 0;
        for (Map<String, Object> direction : directions) {
            try {
                String content = buildTrainingDirectionContent(direction);
                Long businessId = ((Number) direction.get("id")).longValue();
                
                vectorStoreService.upsert("training_direction", businessId, content);
                syncedCount++;
                
            } catch (Exception e) {
                log.error("同步培训方向失败: {}", direction, e);
            }
        }
        
        log.info("✅ 培训方向同步完成: {}/{}", syncedCount, directions.size());
        return syncedCount;
    }
    
    /**
     * 👨‍🏫 同步教师数据
     */
    private int syncTeachers() {
        log.info("👨‍🏫 同步教师数据...");
        
        String sql = "SELECT t.teacher_id as id, t.user_id, u.name, u.phone, u.email, " +
                     "t.office_location, t.title, td.direction_name " +
                     "FROM teacher t " +
                     "LEFT JOIN user u ON t.user_id = u.user_id " +
                     "LEFT JOIN training_direction td ON t.direction_id = td.direction_id " +
                     "WHERE u.status = '1' " +
                     "ORDER BY t.teacher_id";
        
        List<Map<String, Object>> teachers = jdbcTemplate.queryForList(sql);
        
        int syncedCount = 0;
        for (Map<String, Object> teacher : teachers) {
            try {
                String content = buildTeacherContent(teacher);
                Long businessId = ((Number) teacher.get("id")).longValue();
                
                vectorStoreService.upsert("teacher", businessId, content);
                syncedCount++;
                
            } catch (Exception e) {
                log.error("同步教师失败: {}", teacher, e);
            }
        }
        
        log.info("✅ 教师同步完成: {}/{}", syncedCount, teachers.size());
        return syncedCount;
    }
    
    /**
     * 🎭 同步角色数据
     */
    private int syncRoles() {
        log.info("🎭 同步角色数据...");
        
        String sql = "SELECT role_id as id, role_name, role_code, description " +
                     "FROM role " +
                     "ORDER BY role_id";
        
        List<Map<String, Object>> roles = jdbcTemplate.queryForList(sql);
        
        int syncedCount = 0;
        for (Map<String, Object> role : roles) {
            try {
                String content = buildRoleContent(role);
                Long businessId = ((Number) role.get("id")).longValue();
                
                vectorStoreService.upsert("role", businessId, content);
                syncedCount++;
                
            } catch (Exception e) {
                log.error("同步角色失败: {}", role, e);
            }
        }
        
        log.info("✅ 角色同步完成: {}/{}", syncedCount, roles.size());
        return syncedCount;
    }
    
    /**
     * 💼 同步职位数据
     */
    private int syncPositions() {
        log.info("💼 同步职位数据...");
        
        String sql = "SELECT position_id as id, role, position_name, permissions " +
                     "FROM position " +
                     "ORDER BY position_id";
        
        List<Map<String, Object>> positions = jdbcTemplate.queryForList(sql);
        
        int syncedCount = 0;
        for (Map<String, Object> position : positions) {
            try {
                String content = buildPositionContent(position);
                Long businessId = ((Number) position.get("id")).longValue();
                
                vectorStoreService.upsert("position", businessId, content);
                syncedCount++;
                
            } catch (Exception e) {
                log.error("同步职位失败: {}", position, e);
            }
        }
        
        log.info("✅ 职位同步完成: {}/{}", syncedCount, positions.size());
        return syncedCount;
    }
    
    /**
     * 构建培训方向的向量化文本
     */
    private String buildTrainingDirectionContent(Map<String, Object> direction) {
        StringBuilder content = new StringBuilder();
        
        content.append("【工作室文档】 这是培训方向信息 | 专业方向 | 学习方向 | 技能方向\n");
        content.append("方向相关 | 专业相关 | 培训相关 | 技能相关\n");
        
        if (direction.get("direction_name") != null) {
            String name = direction.get("direction_name").toString();
            content.append("方向名称：").append(name).append("\n");
            content.append("专业方向：").append(name).append("\n");
            content.append("培训方向：").append(name).append("\n");
        }
        
        if (direction.get("description") != null) {
            String desc = direction.get("description").toString();
            content.append("方向描述：").append(desc).append("\n");
            content.append("专业介绍：").append(desc).append("\n");
        }
        
        return content.toString().trim();
    }
    
    /**
     * 构建教师的向量化文本
     */
    private String buildTeacherContent(Map<String, Object> teacher) {
        StringBuilder content = new StringBuilder();
        
        content.append("【工作室文档】 这是教师信息 | 老师资料 | 师资信息 | 教员档案\n");
        content.append("教师相关 | 老师相关 | 师资相关 | 教员相关\n");
        
        if (teacher.get("name") != null) {
            String name = teacher.get("name").toString();
            content.append("教师姓名：").append(name).append("\n");
            content.append("老师姓名：").append(name).append("\n");
            content.append("教员姓名：").append(name).append("\n");
        }
        
        if (teacher.get("title") != null) {
            String title = teacher.get("title").toString();
            content.append("职称：").append(title).append("\n");
            content.append("教师职称：").append(title).append("\n");
        }
        
        if (teacher.get("office_location") != null) {
            String office = teacher.get("office_location").toString();
            content.append("办公室：").append(office).append("\n");
            content.append("办公地点：").append(office).append("\n");
        }
        
        if (teacher.get("direction_name") != null) {
            String direction = teacher.get("direction_name").toString();
            content.append("负责方向：").append(direction).append("\n");
            content.append("教学方向：").append(direction).append("\n");
        }
        
        if (teacher.get("phone") != null) {
            content.append("联系电话：").append(teacher.get("phone")).append("\n");
        }
        
        if (teacher.get("email") != null) {
            content.append("邮箱：").append(teacher.get("email")).append("\n");
        }
        
        return content.toString().trim();
    }
    
    /**
     * 构建角色的向量化文本
     */
    private String buildRoleContent(Map<String, Object> role) {
        StringBuilder content = new StringBuilder();
        
        content.append("【工作室文档】 这是角色权限信息 | 用户角色 | 权限角色 | 身份角色\n");
        content.append("角色相关 | 权限相关 | 身份相关 | 职责相关\n");
        
        if (role.get("role_name") != null) {
            String name = role.get("role_name").toString();
            content.append("角色名称：").append(name).append("\n");
            content.append("用户角色：").append(name).append("\n");
        }
        
        if (role.get("role_code") != null) {
            String code = role.get("role_code").toString();
            content.append("角色代码：").append(code).append("\n");
            content.append("角色级别：").append(code).append("\n");
        }
        
        if (role.get("description") != null) {
            String desc = role.get("description").toString();
            content.append("角色描述：").append(desc).append("\n");
            content.append("职责说明：").append(desc).append("\n");
        }
        
        return content.toString().trim();
    }
    
    /**
     * 构建职位的向量化文本
     */
    private String buildPositionContent(Map<String, Object> position) {
        StringBuilder content = new StringBuilder();
        
        content.append("【工作室文档】 这是职位信息 | 岗位信息 | 职务信息 | 工作岗位\n");
        content.append("职位相关 | 岗位相关 | 职务相关 | 工作相关\n");
        
        if (position.get("position_name") != null) {
            String name = position.get("position_name").toString();
            content.append("职位名称：").append(name).append("\n");
            content.append("岗位名称：").append(name).append("\n");
            content.append("职务名称：").append(name).append("\n");
        }
        
        if (position.get("role") != null) {
            String role = position.get("role").toString();
            content.append("对应角色：").append(role).append("\n");
            content.append("角色类型：").append(role).append("\n");
        }
        
        if (position.get("permissions") != null) {
            String permissions = position.get("permissions").toString();
            content.append("职位权限：").append(permissions).append("\n");
            content.append("权限范围：").append(permissions).append("\n");
        }
        
        return content.toString().trim();
    }
} 