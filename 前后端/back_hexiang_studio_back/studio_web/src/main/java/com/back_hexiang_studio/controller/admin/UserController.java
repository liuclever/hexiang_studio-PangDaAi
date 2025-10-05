package com.back_hexiang_studio.controller.admin;

import com.back_hexiang_studio.GlobalException.BusinessException;
import com.back_hexiang_studio.dv.dto.*;
import com.back_hexiang_studio.dv.vo.UserLoginVo;
import com.back_hexiang_studio.dv.vo.UserVo;
import com.back_hexiang_studio.entity.User;
import com.back_hexiang_studio.mapper.UserMapper;
import com.back_hexiang_studio.result.PageResult;
import com.back_hexiang_studio.result.Result;
import com.back_hexiang_studio.service.*;

import java.util.List;
import java.util.regex.Pattern;


import com.back_hexiang_studio.context.UserContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import  com.back_hexiang_studio.securuty.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;


/**
 * 用户管理控制器
 * 包含登录登出（所有人可访问）和用户管理功能（需要权限）
 */
@Slf4j
@RestController
@RequestMapping("/admin/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private TrainingDirectionService trainingDirectionService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private PositionService positionService;

    @Autowired
    private LoginSecurityService loginSecurityService;
    @Autowired
    private CaptchaService captchaService;



    private final UserMapper userMapper;


    public UserController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    // 学号格式：纯数字
    private static final Pattern STUDENT_NUMBER_PATTERN = Pattern.compile("^\\d+$");
    
    // 专业班级格式：2023级现代通信工程02班
    private static final Pattern MAJOR_PATTERN = Pattern.compile("^\\d{4}级[\\u4e00-\\u9fa5]+\\d{2}班$");
    
    // 辅导员姓名格式：2-4个中文字符
    private static final Pattern COUNSELOR_PATTERN = Pattern.compile("^[\\u4e00-\\u9fa5]{2,4}$");
    
    // 宿舍楼号格式：13栋、11栋
    private static final Pattern DORMITORY_PATTERN = Pattern.compile("^\\d{1,2}栋$");
    
    // 职称格式：仅允许中文字符
    private static final Pattern TITLE_PATTERN = Pattern.compile("^[\\u4e00-\\u9fa5]+$");


    /**
     * 用户登录
     *
     * 安全特性：
     * 1. 账户锁定检查
     * 2. 智能验证码要求
     * 3. 登录失败统计
     * 4. 记住我支持
     * 5. 邮件安全通知
     */
    @PostMapping("/login")
    public Result login(@RequestBody UserLoginDto userLoginDto, HttpServletRequest request, HttpServletResponse response) {
        String username = userLoginDto.getUserName();
        String clientIp = getClientIpAddress(request);

        log.info("用户登录请求 - Username: {}, IP: {}", username, clientIp);

        try {
            //  第1步：检查账户是否被锁定
            Result<Boolean> lockResult = loginSecurityService.checkAccountLocked(username);
            if (lockResult.getCode() != 200) {
                log.warn("账户已锁定，拒绝登录 - Username: {}, IP: {}", username, clientIp);
                return lockResult;
            }

            //  第2步：验证码校验（如果前端提供了验证码参数，就必须验证）
            boolean hasCaptchaParams = userLoginDto.getCaptchaSessionId() != null && userLoginDto.getCaptchaCode() != null;
            boolean needsCaptcha = loginSecurityService.requiresCaptcha(username);
            
            log.info("验证码检查 - Username: {}, needsCaptcha: {}, hasCaptchaParams: {}, sessionId: {}, inputCode: {}", 
                    username, needsCaptcha, hasCaptchaParams, userLoginDto.getCaptchaSessionId(), userLoginDto.getCaptchaCode());
            
            // 如果前端发送了验证码参数，就必须验证
            if (hasCaptchaParams) {
                log.info("前端提供了验证码参数，开始验证 - Username: {}, IP: {}", username, clientIp);

                // 验证码校验
                Result<Boolean> captchaResult = captchaService.validateCaptcha(
                        userLoginDto.getCaptchaSessionId(),
                        userLoginDto.getCaptchaCode()
                );

                if (captchaResult.getCode() != 200 || !captchaResult.getData()) {
                    log.warn("验证码验证失败 - Username: {}, IP: {}, Reason: {}",
                            username, clientIp, captchaResult.getMsg());
                    
                    //  验证码错误也要记录登录失败
                    Result<String> failResult = loginSecurityService.recordLoginFailure(username, clientIp);
                    return Result.error("验证码错误，请重新输入");
                }

                log.info("验证码验证通过 - Username: {}, IP: {}", username, clientIp);
            } else if (needsCaptcha) {
                // 后端认为需要验证码但前端没提供
                log.warn("需要验证码但前端未提供 - Username: {}, IP: {}", username, clientIp);
                return Result.error("请输入验证码");
            }

            //  第3步：进行用户名密码验证
            UserLoginVo loginUser = userService.login(userLoginDto);

            //  第4步：登录成功处理
            log.info("用户密码验证成功 - Username: {}, IP: {}", username, clientIp);

            // 清除登录失败记录
            loginSecurityService.clearLoginFailures(username);

            //  第5步：Token生成（支持记住我）
            String accessToken;
            if (Boolean.TRUE.equals(userLoginDto.getRememberMe())) {
                // 记住我：使用长期Token
                log.info("用户选择记住我 - Username: {}", username);
                accessToken = tokenService.createLongTermTokenPair(loginUser.getUserId(), loginUser.getUserName(), response);
            } else {
                // 标准Token
                accessToken = tokenService.createTokenPair(loginUser.getUserId(), loginUser.getUserName(), response);
            }

            loginUser.setToken(accessToken);

            log.info("登录成功 - Username: {}, IP: {}, RememberMe: {}",
                    username, clientIp, userLoginDto.getRememberMe());

            return Result.success(loginUser);

        } catch (BusinessException e) {
            // ❌ 登录失败处理
            log.warn("登录失败 - Username: {}, IP: {}, Reason: {}", username, clientIp, e.getMessage());

            // 记录登录失败（会自动处理锁定和邮件通知）
            Result<String> failResult = loginSecurityService.recordLoginFailure(username, clientIp);

            // 返回安全友好的错误信息
            return Result.error(failResult.getMsg());

        } catch (Exception e) {
            log.error("登录系统异常 - Username: {}, IP: {}", username, clientIp, e);
            return Result.error("系统繁忙，请稍后重试");
        }
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * 刷新Access Token接口
     */
    @PostMapping("/refresh") 
    public Result refresh(HttpServletRequest request) {
        try {
            String refreshToken = tokenService.getRefreshTokenFromCookie(request);
            if (refreshToken == null) {
                return Result.error("未登录或登录已过期，请重新登录");
            }
            
            String newAccessToken = tokenService.refreshAccessToken(refreshToken);
            if (newAccessToken == null) {
                return Result.error("登录已过期，请重新登录");
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("accessToken", newAccessToken);
            return Result.success(data);
        } catch (Exception e) {
            log.error("Token刷新异常", e);
            return Result.error("系统异常，请稍后重试");
        }
    }

    @PostMapping("/logout")
    public Result logout() {
        Long userId=UserContextHolder.getCurrentId();
        if (userId != null) {
            tokenService.logout(userId);
            UserContextHolder.clear();
            return Result.success("登出成功");
        }
        return Result.error("登出失败");
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('USER_MANAGE')")
    public Result<PageResult> list(PageDto pageDto) {
        log.info("页码返回{}",pageDto.toString());
        PageResult result= userService.list(pageDto);
        return Result.success(result);
    }
    
    /**
     * 验证用户数据字段格式
     * @param userDto 用户数据
     * @return 错误信息，如果没有错误则返回null
     */
    private String validateUserFields(UserDto userDto) {
        if (userDto.getRoleId() != null) {
            // 学生角色验证
            if (userDto.getRoleId() == 1) {
                // 验证学号
                if (userDto.getStudentNumber() != null && !STUDENT_NUMBER_PATTERN.matcher(userDto.getStudentNumber()).matches()) {
                    return "学号只能包含数字";
                }
                
                // 验证专业班级
                if (userDto.getMajor() != null && !MAJOR_PATTERN.matcher(userDto.getMajor()).matches()) {
                    return "专业班级格式应为：2023级现代通信工程02班";
                }
                
                // 验证辅导员姓名
                if (userDto.getCounselor() != null && !COUNSELOR_PATTERN.matcher(userDto.getCounselor()).matches()) {
                    return "辅导员姓名应为2-4个中文字符";
                }
                
                // 验证宿舍楼号
                if (userDto.getDormitory() != null && !DORMITORY_PATTERN.matcher(userDto.getDormitory()).matches()) {
                    return "宿舍楼号格式应为：13栋、11栋等";
                }
            }
            
            // 教师角色验证
            else if (userDto.getRoleId() == 2) {
                // 验证职称
                if (userDto.getTitle() != null && !TITLE_PATTERN.matcher(userDto.getTitle()).matches()) {
                    return "职称只能包含中文字符";
                }
            }
        }
        
        return null; // 验证通过
    }

    /**
     * 新增用户，同时处理头像文件
     * @param userDto 用户数据
     * @param avatarFile 可选的头像文件
     * @return Result
     */
    @PostMapping("/add")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('USER_MANAGE')")
    public Result add(UserDto userDto, @RequestParam(value = "file", required = false) MultipartFile avatarFile) {
        log.info("添加用户请求数据: {}, 文件: {}", userDto.toString(), avatarFile != null ? avatarFile.getOriginalFilename() : "无");
        log.info("培训方向数据: {}", userDto.getTraining() != null ? userDto.getTraining().toString() : "无");
        log.info("部门ID: {}", userDto.getDepartmentId());
        
        // 字段格式验证
        String validationError = validateUserFields(userDto);
        if (validationError != null) {
            return Result.error(validationError);
        }
        
        userService.add(userDto, avatarFile);
        return Result.success("添加用户成功");
    }

    @GetMapping("/detail")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('USER_MANAGE')")
    public Result<UserVo> selectById(@RequestParam("userId") Long userId){
        log.info("查询用户详情，用户ID: {}", userId);
        UserVo user = userService.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(user);
    }

    /**
     * 更新用户信息，同时处理头像文件
     * @param userDto 用户数据
     * @param avatarFile 可选的头像文件
     * @return Result
     */
    @PostMapping("/update")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('USER_MANAGE')")
    public Result update(UserDto userDto, @RequestParam(value = "file", required = false) MultipartFile avatarFile) {
        log.info("修改用户{}, 文件: {}", userDto.toString(), avatarFile != null ? avatarFile.getOriginalFilename() : "无");
        
        // 字段格式验证
        String validationError = validateUserFields(userDto);
        if (validationError != null) {
            return Result.error(validationError);
        }
        
        userService.update(userDto, avatarFile);
        return Result.success("用户更新成功");
    }

    @PostMapping("/delete")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('USER_MANAGE')")
    public Result delete(@RequestBody List<String> userIds) {
        log.info("删除用户id为：{}", userIds.toString());
        userService.delete(userIds);
        return Result.success();
    }

    @PostMapping("/update-status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('USER_MANAGE')")
    public Result updateStatus(@RequestBody UserStatusUpdateDto userStatusUpdateDto) {
        log.info("修改用户状态: {}", userStatusUpdateDto.toString());
        userService.updateStatus(userStatusUpdateDto.getUserId(), userStatusUpdateDto.getStatus());
        return Result.success("用户状态更新成功");
    }

    /**
     * 获取培训方向列表
     * @return 培训方向列表
     */
    @GetMapping("/training-directions")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('USER_MANAGE')")
    public Result getTrainingDirections() {
        log.info("获取培训方向列表");
        return Result.success(trainingDirectionService.getAllTrainingDirections());
    }
    
    /**
     * 获取职位列表
     * @param role 角色
     * @return 职位列表
     */
    @GetMapping("/positions")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('USER_MANAGE')")
    public Result getPositions(@RequestParam(required = false) String role) {
        log.info("获取职位列表，角色过滤: {}", role);
        if (role != null && !role.isEmpty()) {
            return Result.success(positionService.getByRole(role));
        } else {
            return Result.success(positionService.getAll());
    }
    }
    
    /**
     * 获取部门列表
     * @return 部门列表
     */
    @GetMapping("/departments")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('USER_MANAGE')")
    public Result getDepartments() {
        log.info("获取部门列表");
        return Result.success(departmentService.getAllDepartments());
    }
    
    /**
     * 修改用户密码
     * @param changePasswordDto 包含旧密码和新密码的DTO
     * @return 操作结果
     */
    @PostMapping("/change-password")
    
    public Result changePassword(@RequestBody ChangePasswordDto changePasswordDto) {
        log.info("修改密码请求，用户ID: {}", changePasswordDto.getUserId());
        
        try {
            // 获取当前登录用户ID
            Long currentUserId = UserContextHolder.getCurrentId();
            log.info("当前登录用户ID: {}", currentUserId);
            
            // 安全检查：只允许用户修改自己的密码，除非是管理员
            if (currentUserId != null && changePasswordDto.getUserId() != null && !currentUserId.equals(changePasswordDto.getUserId())) {
                User currentUser = userService.getUserById(currentUserId);
                if (currentUser == null || currentUser.getRoleId() < 3) { // 3是管理员角色ID
                    log.warn("非管理员用户尝试修改其他用户密码，当前用户ID: {}, 目标用户ID: {}", 
                             currentUserId, changePasswordDto.getUserId());
                    return Result.error("无权修改其他用户的密码");
                }
            } else if (currentUserId == null) {
                log.warn("未获取到当前用户ID，可能是会话已过期");
                return Result.error("会话已过期，请重新登录后再试");
            }
            
            // 确保用户ID不为空
            if (changePasswordDto.getUserId() == null) {
                log.warn("修改密码请求中未提供用户ID");
                return Result.error("请提供有效的用户ID");
            }
            
            // 调用服务层方法修改密码，如果有异常会抛出
            userService.changePassword(changePasswordDto);
            
            // 如果没有异常，则表示修改成功
            log.info("密码修改成功，用户ID: {}", changePasswordDto.getUserId());
            return Result.success("密码修改成功");
            
        } catch (BusinessException e) {
            // 业务异常，返回具体错误信息
            log.warn("修改密码业务异常: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            // 其他异常，返回通用错误信息
            log.error("修改密码过程中发生异常: {}", e.getMessage(), e);
            return Result.error("系统错误，密码修改失败");
        }
    }

    /**
     * 获取当前用户的个人信息
     * 所有已认证用户都可以访问（查看自己的信息）
     * @return 当前用户的详细信息
     */
    @GetMapping("/profile")
    public Result<UserVo> getCurrentUserProfile() {
        Long currentUserId = UserContextHolder.getCurrentId();
        if (currentUserId == null) {
            log.warn("未获取到当前用户ID，可能是会话已过期");
            return Result.error("会话已过期，请重新登录");
        }
        
        log.info("获取个人信息，用户ID: {}", currentUserId);
        UserVo user = userService.selectById(currentUserId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        return Result.success(user);
    }
}










