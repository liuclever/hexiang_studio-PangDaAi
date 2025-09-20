package com.back_hexiang_studio.controller.wx;

import com.back_hexiang_studio.dv.dto.UserLoginDto;
import com.back_hexiang_studio.dv.dto.PageDto;
import com.back_hexiang_studio.dv.vo.UserLoginVo;
import com.back_hexiang_studio.dv.vo.UserVo;
import com.back_hexiang_studio.result.Result;
import com.back_hexiang_studio.result.PageResult;
import com.back_hexiang_studio.securuty.TokenService;
import com.back_hexiang_studio.service.UserService;
import com.back_hexiang_studio.service.TrainingDirectionService;
import com.back_hexiang_studio.service.UserAchievementService;
import com.back_hexiang_studio.service.DepartmentService;
import com.back_hexiang_studio.mapper.TeacherMapper;
import com.back_hexiang_studio.mapper.StudentMapper;
import com.back_hexiang_studio.GlobalException.BusinessException;
import com.back_hexiang_studio.context.UserContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

/**
 * 微信端用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/wx/user")
public class WxUserController {

    @Autowired
    private UserService userService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private TrainingDirectionService trainingDirectionService;

    @Autowired
    private UserAchievementService userAchievementService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private StudentMapper studentMapper;

    /**
     * 微信用户登录接口
     * @param userLoginDto 登录信息
     * @return 登录结果
     */
    @PostMapping("/login")
    public Result login(@RequestBody UserLoginDto userLoginDto) {
        log.info("微信用户登录: {}", userLoginDto.getUserName());
        try {
            UserLoginVo loginUser = userService.login(userLoginDto);
            String token = tokenService.createToken(loginUser.getUserId(), loginUser.getUserName());
            loginUser.setToken(token);
            
            log.info("微信用户登录成功: {}", loginUser.getUserName());
            return Result.success(loginUser);
        } catch (BusinessException e) {
            log.warn("微信用户登录失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 用户登出
     * @return 登出结果
     */
    @PostMapping("/logout")
    public Result logout() {
        log.info("微信用户登出");
        try {
            // 获取当前登录用户ID
            Long userId = UserContextHolder.getCurrentId();
            if (userId != null) {
                // 调用TokenService清除token
                tokenService.logout(userId);
                // 清除ThreadLocal中的用户信息
                UserContextHolder.clear();
                log.info("用户登出成功，用户ID: {}", userId);
                return Result.success("登出成功");
            }
            return Result.success("登出成功");
        } catch (Exception e) {
            log.error("用户登出失败: {}", e.getMessage());
            return Result.error("登出失败");
        }
    }

    /**
     * 验证token有效性
     * @return 验证结果
     */
    @GetMapping("/verify")
    public Result verifyToken() {
        log.info("验证token有效性");
        try {
            // 获取当前登录用户ID
            Long userId = UserContextHolder.getCurrentId();
            if (userId != null) {
                return Result.success("token有效");
            }
            return Result.error("token无效");
        } catch (Exception e) {
            log.error("验证token失败: {}", e.getMessage());
            return Result.error("验证token失败");
        }
    }

    /**
     * 获取用户详情
     * @param userId 用户ID
     * @return 用户详情
     */
    @GetMapping("/detail")
    public Result getUserDetail(@RequestParam Long userId) {
        log.info("获取微信用户详情，用户ID: {}", userId);
        try {
            return Result.success(userService.selectById(userId));
        } catch (Exception e) {
            log.error("获取用户详情失败: {}", e.getMessage());
            return Result.error("获取用户详情失败");
        }
    }

    /**
     * 获取用户列表（微信端）
     * @param name 搜索关键词
     * @param roleId 角色ID筛选
     * @param page 页码
     * @param pageSize 每页大小
     * @return 用户列表分页结果
     */
    @GetMapping("/list")
    public Result<PageResult> getUserList(@RequestParam(required = false) String name,
                                        @RequestParam(required = false) Long roleId,
                                        @RequestParam(required = false) Long departmentId,
                                        @RequestParam(defaultValue = "1") Integer page,
                                        @RequestParam(defaultValue = "15") Integer pageSize) {
        log.info("获取微信用户列表，搜索条件: {}, 角色: {}, 部门: {}, 页码: {}, 每页: {}", name, roleId, departmentId, page, pageSize);
        try {
            // 构建分页查询DTO
            PageDto pageDto = new PageDto();
            pageDto.setPage(page);
            pageDto.setPageSize(pageSize);
            pageDto.setName(name);
            if (roleId != null) {
                pageDto.setRoleId(roleId.toString());
            }
            if (departmentId != null) {
                pageDto.setDepartmentId(departmentId.toString());
            }
            
            // 调用UserService获取分页数据
            PageResult result = userService.list(pageDto);
            
            log.info("微信端用户列表查询成功，总数: {}", result.getTotal());
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取微信用户列表失败: {}", e.getMessage());
            return Result.error("获取用户列表失败");
        }
    }

    /**
     * 获取部门列表（微信端）
     * @return 部门列表
     */
    @GetMapping("/departments")
    public Result getDepartments() {
        log.info("获取微信端部门列表");
        try {
            // 获取所有部门
            List<?> departments = departmentService.getAllDepartments();
            return Result.success(departments);
        } catch (Exception e) {
            log.error("获取部门列表失败: {}", e.getMessage());
            return Result.error("获取部门列表失败");
        }
    }

    /**
     * 获取用户统计数据（微信端）
     * @return 用户统计信息
     */
    @GetMapping("/stats")
    public Result getUserStats() {
        log.info("获取微信端用户统计数据");
        try {
            // 获取用户统计数据
            Map<String, Object> stats = new HashMap<>();
            
            // 调用UserService获取统计数据
            // 这里可能需要在UserService中添加相应的统计方法
            // 暂时提供模拟数据，后续需要实现具体的统计逻辑
            PageDto pageDto = new PageDto();
            pageDto.setPage(1);
            pageDto.setPageSize(999); // 获取所有数据用于统计
            PageResult allUsers = userService.list(pageDto);
            
            long total = allUsers.getTotal();
            long active = total; // 假设所有用户都是活跃的，实际需要根据业务逻辑判断
            long departments = departmentService.countDepartments(); // 实际查询数据库获取部门数量
            
            stats.put("total", total);
            stats.put("active", active);
            stats.put("departments", departments);
            
            log.info("微信端用户统计查询成功，总用户数: {}, 部门数: {}", total, departments);
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取微信端用户统计失败: {}", e.getMessage());
            return Result.error("获取用户统计失败");
        }
    }

    /**
     * 获取培训方向列表（微信端）
     * @return 培训方向列表
     */
    @GetMapping("/training-directions")
    public Result getTrainingDirections() {
        log.info("获取微信端培训方向列表");
        try {
            // 调用TrainingDirectionService获取培训方向列表
            return Result.success(trainingDirectionService.getAllTrainingDirections());
        } catch (Exception e) {
            log.error("获取微信端培训方向列表失败: {}", e.getMessage());
            return Result.error("获取培训方向列表失败");
        }
    }

    /**
     * 根据用户ID获取学生ID（微信端）
     * @param userId 用户ID
     * @return 学生ID
     */
    @GetMapping("/getStudentId")
    public Result getStudentId(@RequestParam Long userId) {
        log.info("根据用户ID获取学生ID: {}", userId);
        try {
            if (userId == null) {
                return Result.error("用户ID不能为空");
            }

            // 获取用户信息，检查是否为学生角色
            UserVo user = userService.selectById(userId);
            if (user == null) {
                return Result.error("用户不存在");
            }

            if (user.getRoleId() != 1) { // 1表示学生角色
                return Result.error("该用户不是学生角色");
            }

            // 调用StudentMapper根据用户ID获取学生ID
            Long studentId = studentMapper.getStudentIdByUserId(userId);
            
            if (studentId == null) {
                return Result.error("该用户不存在对应的学生记录");
            }
            
            log.info("学生ID获取成功，用户ID: {}, 学生ID: {}", userId, studentId);
            return Result.success(studentId);
        } catch (Exception e) {
            log.error("根据用户ID获取学生ID失败: {}", e.getMessage());
            return Result.error("获取学生ID失败");
        }
    }

    /**
     * 根据用户ID获取教师ID（微信端）
     * @param userId 用户ID
     * @return 教师ID
     */
    @GetMapping("/getTeacherId")
    public Result getTeacherId(@RequestParam Long userId) {
        log.info("根据用户ID获取教师ID: {}", userId);
        try {
            if (userId == null) {
                return Result.error("用户ID不能为空");
            }

            // 获取用户信息，检查是否为教师角色
            UserVo user = userService.selectById(userId);
            if (user == null) {
                return Result.error("用户不存在");
            }

            if (user.getRoleId() != 2) { // 2表示教师角色
                return Result.error("该用户不是教师角色");
            }

            // 调用TeacherMapper根据用户ID获取教师ID
            Long teacherId = teacherMapper.getTeacherIdByUserId(userId);
            
            if (teacherId == null) {
                return Result.error("该用户不存在对应的教师记录");
            }
            
            log.info("教师ID获取成功，用户ID: {}, 教师ID: {}", userId, teacherId);
            return Result.success(teacherId);
        } catch (Exception e) {
            log.error("根据用户ID获取教师ID失败: {}", e.getMessage());
            return Result.error("获取教师ID失败");
        }
    }

    /**
     * 获取用户成就统计（微信端）
     * @param userId 用户ID
     * @return 成就统计
     */
    @GetMapping("/achievements/stats")
    public Result getAchievementStats(@RequestParam Long userId) {
        // 🔧 优化：频繁查询，降级为DEBUG，减少用户信息泄露
        log.debug("获取用户成就统计");
        try {
            if (userId == null) {
                return Result.error("用户ID不能为空");
            }

            // 获取用户荣誉和证书数据
            List<?> honors = userAchievementService.getUserHonors(userId);
            List<?> certificates = userAchievementService.getUserCertificates(userId);

            // 构建统计数据
            Map<String, Object> stats = new HashMap<>();
            stats.put("honorsCount", honors != null ? honors.size() : 0);
            stats.put("certificatesCount", certificates != null ? certificates.size() : 0);
            stats.put("totalAchievements", (honors != null ? honors.size() : 0) + (certificates != null ? certificates.size() : 0));
            stats.put("projectsCount", 0); // 项目数暂时设为0，可后续扩展

            // 🔧 优化：统计信息降级为DEBUG，减少用户信息泄露
            log.debug("用户成就统计查询成功，荣誉: {}, 证书: {}", 
                    stats.get("honorsCount"), stats.get("certificatesCount"));
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取用户成就统计失败: {}", e.getMessage());
            return Result.error("获取成就统计失败");
        }
    }

    /**
     * 获取用户荣誉列表（微信端）
     * @param userId 用户ID
     * @return 荣誉列表
     */
    @GetMapping("/honors")
    public Result getUserHonors(@RequestParam Long userId) {
        // 🔧 优化：频繁查询，降级为DEBUG，减少用户信息泄露
        log.debug("获取用户荣誉列表");
        try {
            if (userId == null) {
                return Result.error("用户ID不能为空");
            }

            // 调用UserAchievementService获取用户荣誉列表
            List<?> honors = userAchievementService.getUserHonors(userId);
            
            // 🔧 优化：查询结果降级为DEBUG，减少用户信息泄露
            log.debug("用户荣誉列表查询成功，荣誉数量: {}", 
                    honors != null ? honors.size() : 0);
            return Result.success(honors);
        } catch (Exception e) {
            log.error("获取用户荣誉列表失败: {}", e.getMessage());
            return Result.error("获取荣誉列表失败");
        }
    }

    /**
     * 获取用户证书列表（微信端）
     * @param userId 用户ID
     * @return 证书列表
     */
    @GetMapping("/certificates")
    public Result getUserCertificates(@RequestParam Long userId) {
        // 🔧 优化：频繁查询，降级为DEBUG，减少用户信息泄露
        log.debug("获取用户证书列表");
        try {
            if (userId == null) {
                return Result.error("用户ID不能为空");
            }

            // 调用UserAchievementService获取用户证书列表
            List<?> certificates = userAchievementService.getUserCertificates(userId);
            
            // 🔧 优化：查询结果降级为DEBUG，减少用户信息泄露
            log.debug("用户证书列表查询成功，证书数量: {}", 
                    certificates != null ? certificates.size() : 0);
            return Result.success(certificates);
        } catch (Exception e) {
            log.error("获取用户证书列表失败: {}", e.getMessage());
            return Result.error("获取证书列表失败");
        }
    }

    /**
     * 获取学生列表（用于选择器）
     * @param keyword 搜索关键词
     * @return 学生列表
     */
    @GetMapping("/students")
    public Result getStudentList(@RequestParam(required = false) String keyword) {
        log.info("获取学生列表，关键词: {}", keyword);
        try {
            List<Map<String, Object>> students;
            // 过滤无效的keyword值
            if (keyword != null && !keyword.trim().isEmpty() && !"undefined".equals(keyword.trim()) && !"null".equals(keyword.trim())) {
                students = studentMapper.searchStudents(keyword.trim());
                log.info("按关键词搜索学生: {}", keyword.trim());
            } else {
                students = studentMapper.selectStudentsWithNames();
                log.info("获取所有学生列表");
            }
            
            log.info("获取学生列表成功，数量: {}", students.size());
            return Result.success(students);
        } catch (Exception e) {
            log.error("获取学生列表失败: {}", e.getMessage());
            return Result.error("获取学生列表失败: " + e.getMessage());
        }
    }

    /**
     * 更新用户状态（微信端）
     * @param requestBody 包含userId和status的请求体
     * @return 更新结果
     */
    @PostMapping("/updateStatus")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
    public Result updateUserStatus(@RequestBody Map<String, Object> requestBody) {
        Long currentUserId = UserContextHolder.getCurrentId();
        log.info("管理员用户状态更新请求，操作人ID: {}, 请求参数: {}", currentUserId, requestBody);
        
        try {
            // 从请求体中获取参数
            Object userIdObj = requestBody.get("userId");
            Object statusObj = requestBody.get("status");
            
            if (userIdObj == null || statusObj == null) {
                return Result.error("用户ID和状态不能为空");
            }
            
            Long userId = Long.valueOf(userIdObj.toString());
            Integer status = Integer.valueOf(statusObj.toString());
            
            if (status != 0 && status != 1) {
                return Result.error("状态值只能是0（禁用）或1（启用）");
            }
            
            // 防止用户禁用自己
            if (currentUserId.equals(userId)) {
                return Result.error("不能修改自己的账户状态");
            }
            
            // 获取目标用户当前信息
            UserVo targetUser = userService.selectById(userId);
            if (targetUser == null) {
                return Result.error("目标用户不存在");
            }
            
            String oldStatus = targetUser.getStatus();
            String newStatus = status.toString();
            
            // 如果状态没有变化，直接返回
            if (oldStatus.equals(newStatus)) {
                String statusText = status == 1 ? "启用" : "禁用";
                return Result.success("用户已处于" + statusText + "状态");
            }
            
            // 调用userService更新状态
            userService.updateStatus(userId, newStatus);
            
            // 如果禁用用户，立即清除其token（强制下线）
            if (status == 0) {
                try {
                    tokenService.logout(userId);
                    log.info("已强制用户下线，用户ID: {}", userId);
                } catch (Exception e) {
                    log.warn("清除用户token失败，用户ID: {}, 错误: {}", userId, e.getMessage());
                }
            }
            
            String statusText = status == 1 ? "启用" : "禁用";
            log.info("用户状态更新成功 - 操作人: {}, 目标用户: {} ({}), 状态变更: {} -> {}", 
                    currentUserId, userId, targetUser.getName(), oldStatus, newStatus);
            
            return Result.success(statusText + "成功");
        } catch (NumberFormatException e) {
            log.error("参数格式错误，操作人: {}, 错误: {}", currentUserId, e.getMessage());
            return Result.error("参数格式错误");
        } catch (Exception e) {
            log.error("更新用户状态失败，操作人: {}, 错误: {}", currentUserId, e.getMessage());
            return Result.error("状态更新失败：" + e.getMessage());
        }
    }
} 