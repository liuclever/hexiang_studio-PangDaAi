package com.back_hexiang_studio.service.impl;

import com.back_hexiang_studio.GlobalException.BusinessException;
import com.back_hexiang_studio.GlobalException.DatabaseException;
import com.back_hexiang_studio.GlobalException.ErrorCode;
import com.back_hexiang_studio.annotation.AutoFill;
import com.back_hexiang_studio.dv.dto.UserDto;
import com.back_hexiang_studio.dv.dto.UserLoginDto;
import com.back_hexiang_studio.dv.dto.PageDto;
import com.back_hexiang_studio.dv.dto.ChangePasswordDto;
import com.back_hexiang_studio.dv.vo.*;
import com.back_hexiang_studio.entity.Student;
import com.back_hexiang_studio.entity.Teacher;
import com.back_hexiang_studio.entity.User;
import com.back_hexiang_studio.enumeration.OperationType;
import com.back_hexiang_studio.mapper.*;
import com.back_hexiang_studio.result.PageResult;

import com.back_hexiang_studio.service.UserService;
import com.back_hexiang_studio.service.DepartmentService;
// import com.back_hexiang_studio.securuty.TokenService;


import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.back_hexiang_studio.utils.FileUtils;
import org.springframework.transaction.annotation.Transactional;
import com.back_hexiang_studio.enumeration.FileType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

@Service
@Slf4j
public class userServiceimpl implements UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ManagerMapper managerMapper;
    @Autowired
    private TeacherMapper teacherMapper;
    @Autowired
    private StudentMapper studentMapper;
    @Autowired
    private PermissionsMapper permissionsMapper;
    @Autowired
    private UserStudentMapper userStudentMapper;
    @Autowired
    private TrainingMapper trainingMapper;
    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private UserRoleMapper userRoleMapper;
    @Autowired
    private UserHonorMapper userHonorMapper;
    @Autowired
    private UserCertificateMapper userCertificateMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private DepartmentService departmentService;
    // @Autowired
    // private TokenService tokenService;

    /**
     * MD5加密工具方法
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

    /**
     *登录逻辑
     * @param userDto
     * @return
     */
    @Override
    public UserLoginVo login(UserLoginDto userDto) {
        String userName = userDto.getUserName();
        String password = userDto.getPassword();
        log.info("用户登录: {}", userName);

        // 对输入的密码进行MD5加密
        String encryptedPassword = encryptMD5(password);
        log.info("md5加密完成");

        User user = userMapper.select(userName, encryptedPassword);

        if (user == null) {
            log.warn("用户登录失败: 用户名或密码错误, 用户名: {}", userName);
            throw new BusinessException("用户名或密码错误");
        }

        // 检查用户状态，"0"表示禁用
        if ("0".equals(user.getStatus())) {
            log.warn("用户登录失败: 账号已被禁用, 用户名: {}", userName);
            throw new BusinessException("账号已被禁用，请联系管理员");
        }

        log.info("用户验证成功: {}", user.getUserName());
        UserLoginVo userLoginVo = new UserLoginVo() ;
        BeanUtils.copyProperties( user, userLoginVo);
        userLoginVo.setPassword("*****");
        return userLoginVo;
    }

    /**
     * 返回用户列表（分页）
     * @param pageDto
     * @return
     */
    @Override
    public PageResult list(PageDto pageDto) {
        //获取用户列表
        String cacheKey="user:list"+pageDto.getPage()+":"+pageDto.getPageSize();

        if (pageDto.getName() != null) {
            cacheKey += ":name:" + pageDto.getName();
        }
        if (pageDto.getRoleId() != null) {
            cacheKey += ":role:" + pageDto.getRoleId();
        }
        if (pageDto.getStatus() != null) {
            cacheKey += ":status:" + pageDto.getStatus();
        }
        if (pageDto.getDepartmentId() != null) {
            cacheKey += ":dept:" + pageDto.getDepartmentId();
        }

        // 从缓存获取用户基础数据（不包含在线状态）
        Object cacheResult = redisTemplate.opsForValue().get(cacheKey);
        PageResult result;

        if (cacheResult != null) {
            log.debug("从缓存获取用户列表数据");
            result = (PageResult) cacheResult;
        } else {
            log.debug("缓存未命中，从数据库查询用户列表");
            //传入当前页，长度
            PageHelper.startPage(pageDto.getPage(),pageDto.getPageSize());
            //获取用户基本信息
            Page<basicUserVo> userList = userMapper.selectByPage(pageDto);
            result = new PageResult(userList.getTotal(), userList.getResult());

            // 保存基础数据到缓存，设置5分钟过期（不包含在线状态）
            redisTemplate.opsForValue().set(cacheKey, result, 5, TimeUnit.MINUTES);
        }

        // 无论是否来自缓存，都重新计算在线状态（实时数据，不缓存）
        // @SuppressWarnings("unchecked")
        // List<basicUserVo> userList = (List<basicUserVo>) result.getRecords();
        // if (userList != null) {
        //     log.info("开始计算 {} 个用户的在线状态", userList.size());
        //     int onlineCount = 0;
        //     for (basicUserVo user : userList) {
        //         if (user.getUserId() != null) {
        //             boolean isOnline = tokenService.isUserOnline(user.getUserId());
        //             user.setIsOnline(isOnline);
        //             if (isOnline) {
        //                 onlineCount++;
        //             }
        //             log.debug("用户 {} ({}) 在线状态: {}", user.getUserId(), user.getName(), isOnline);
        //         }
        //     }
        //     log.info("在线状态计算完成 - 总用户数: {}, 在线用户数: {}", userList.size(), onlineCount);
        // }

        return result;
    }


    /**
     *根据用户id获取详细信息
     * @param userId
     * @return
     */
    @Override
    public UserVo selectById(Long userId) {
        String cacheKey="user:info:"+userId;

        //从缓存获取
        Object cacheResult = redisTemplate.opsForValue().get(cacheKey);
        if (cacheResult != null) {
            return (UserVo) cacheResult;
        }

        // 获取用户基本信息
        User user = userMapper.getUserById(userId);
        if (user == null) {
            return null;
        }
        // 创建并填充UserVo对象
        UserVo userVo = new UserVo();
        BeanUtils.copyProperties(user, userVo);


        // 获取培训方向列表
        List<String> directions = new ArrayList<>();

        try {
            // 根据角色获取特有信息
            if (user.getRoleId() != null) {
                // 学生角色
                if (user.getRoleId() == 1) {
                    Long studentId = studentMapper.getStudentIdByUserId(userId);
                    if (studentId != null) {
                        // 获取学生表特有字段
                        Student studentInfo = studentMapper.getStudentInfo(studentId);
                        if (studentInfo != null) {
                            // 复制学生特有字段到userVo
                            userVo.setStudentNumber(studentInfo.getStudentNumber());
                            userVo.setGradeYear(studentInfo.getGradeYear());
                            userVo.setMajor(studentInfo.getMajorClass());
                            userVo.setCounselor(studentInfo.getCounselor());
                            userVo.setDormitory(studentInfo.getDormitory());
                            userVo.setScore(studentInfo.getScore());
                            userVo.setDepartmentId(studentInfo.getDepartmentId());

                            // 获取部门名称
                            if (studentInfo.getDepartmentId() != null) {
                                String departmentName = departmentService.getDepartmentNameById(studentInfo.getDepartmentId());
                                userVo.setDepartmentName(departmentName);
                            }
                        }

                        // 获取学生所有培训方向
                        List<String> studentDirections = studentMapper.getStudentAllDirections(studentId);
                        if (studentDirections != null && !studentDirections.isEmpty()) {
                            directions.addAll(studentDirections);
                        }
                    }
                }
                // 教师角色
                else if (user.getRoleId() == 2) {
                    Long teacherId = teacherMapper.getTeacherIdByUserId(userId);
                    if (teacherId != null) {
                        // 获取教师表特有字段
                        Teacher teacherInfo = teacherMapper.getTeacherInfo(teacherId);
                        if (teacherInfo != null) {
                            // 复制教师特有字段到userVo
                            userVo.setOfficeLocation(teacherInfo.getOfficeLocation());
                            userVo.setTitle(teacherInfo.getTitle());
                        }

                        // 获取教师所有培训方向
                        List<String> teacherDirections = teacherMapper.getTeacherAllDirections(teacherId);
                        if (teacherDirections != null && !teacherDirections.isEmpty()) {
                            directions.addAll(teacherDirections);
                        }
                    }
                }
                // 管理员角色
                else if (user.getRoleId() == 3) {
                    // 管理员角色特有处理（如果有的话）
                }
            }
        } catch (Exception e) {
            log.error("获取用户详细信息失败: {}", e.getMessage(), e);
        }

        // 设置培训方向列表
        userVo.setDirectionIdNames(directions);

        // 保存到缓存，设置10分钟过期
        redisTemplate.opsForValue().set(cacheKey, userVo, 10, TimeUnit.MINUTES);

        return userVo;
    }

    /**
     * 添加用户
     * @param userDto
     */
    @AutoFill(value = OperationType.INSERT)
    @Transactional
    @Override
    public void add(UserDto userDto, MultipartFile avatarFile)  {
        String newAvatarPath = null;
        try {
            // 1. 文件上传
            if (avatarFile != null && !avatarFile.isEmpty()) {
                FileType fileType = getFileTypeByRole(String.valueOf(userDto.getRoleId()));
                newAvatarPath = FileUtils.saveFile(avatarFile, fileType);
                userDto.setAvatar(newAvatarPath);
                log.info("用户头像上传成功，路径：{}", newAvatarPath);
            } else {
                userDto.setAvatar(getDefaultAvatarByRole(String.valueOf(userDto.getRoleId())));
                log.info("未提供头像文件，为用户设置默认头像");
            }




            // 2. 对密码进行MD5加密
            if (userDto.getPassword() != null && !userDto.getPassword().isEmpty()) {
                String encryptedPassword = encryptMD5(userDto.getPassword());
                userDto.setPassword(encryptedPassword);
                log.info("用户密码已进行MD5加密");
            }

            // 3. 插入用户主表数据
            userMapper.addUser(userDto);
            Long userId = userDto.getUserId();
            if (userId == null) {
                throw new RuntimeException("未能获取到新用户的ID，数据库插入失败。");
            }
            log.info("成功向 'user' 表插入主记录, 用户ID: {}", userId);

            // 4. 根据角色处理特定信息
            switch (userDto.getRoleId().intValue()) {
                case 1: // 学员
                    Student student = new Student();
                    student.setUserId(userId);
                    student.setStudentNumber(userDto.getStudentNumber());
                    student.setGradeYear(userDto.getGradeYear());
                    student.setMajorClass(userDto.getMajor());
                    student.setCounselor(userDto.getCounselor());
                    student.setDormitory(userDto.getDormitory());
                    student.setScore(userDto.getScore());
                    student.setDepartmentId(userDto.getDepartmentId());

                    // 主攻方向设置
                    if (userDto.getTraining() != null && !userDto.getTraining().isEmpty()) {
                        student.setDirectionId(userDto.getTraining().get(0).longValue());
                    } else {
                        throw new IllegalArgumentException("学员必须至少关联一个培训方向。");
                    }

                    studentMapper.insert(student);
                    log.info("成功向 'student' 表插入学员特有信息, 用户ID: {}", userId);

                    // 处理学员与培训方向的多对多关系
                    Long studentId = studentMapper.getStudentIdByUserId(userId);
                    if (studentId != null && userDto.getTraining() != null && !userDto.getTraining().isEmpty()) {
                        for (Integer directionId : userDto.getTraining()) {
                            studentMapper.addStudentDirection(studentId, directionId.longValue());
                        }
                        log.info("成功向 'student_direction' 表插入关联数据, 学员ID: {}", studentId);
                    }
                    break;

                case 2: // 老师
                    Teacher teacher = new Teacher();
                    teacher.setUserId(userId);
                    teacher.setTitle(userDto.getTitle());
                    teacher.setOfficeLocation(userDto.getOfficeLocation());

                    // 恢复主研究方向设置
                    if (userDto.getTraining() != null && !userDto.getTraining().isEmpty()) {
                        teacher.setDirectionId(userDto.getTraining().get(0).longValue());
                    } else {
                        throw new IllegalArgumentException("老师必须至少关联一个培训方向。");
                    }

                    teacherMapper.insert(teacher);
                    log.info("成功向 'teacher' 表插入教师特有信息, 用户ID: {}", userId);

                    // 处理老师与培训方向的多对多关系
                    Long teacherId = teacherMapper.getTeacherIdByUserId(userId);
                    if (teacherId != null && userDto.getTraining() != null && !userDto.getTraining().isEmpty()) {
                        for (Integer directionId : userDto.getTraining()) {
                            teacherMapper.addTeacherDirection(teacherId, directionId.longValue());
                        }
                        log.info("成功向 'teacher_direction' 表插入关联数据, 教师ID: {}", teacherId);
                    }
                    break;

                case 0: // 访客
                    log.info("添加访客用户, 无需额外表操作, 用户ID: {}", userId);
                    break;

                default:
                    log.warn("未知的角色ID: {}, 无特定角色表操作。", userDto.getRoleId());
                    break;
            }

            log.info("用户 '{}' (ID: {}) 已完全成功添加到数据库", userDto.getUserName(), userId);
            clearUserListCache();

        }  catch (DuplicateKeyException e){
            log.warn("新增用户失败，用户名或学号已存在：{}", e.getMessage());
            // 抛出业务异常，让全局异常处理器能够捕获并返回给前端具体信息
            throw new BusinessException("用户名或学号已存在，请使用其他名称。");
        }
        catch (Exception e) {
            // 如果发生异常，回滚文件上传
            if (newAvatarPath != null) {
                FileUtils.deleteFile(newAvatarPath);
                log.info("数据库操作失败，已回滚并删除文件：{}", newAvatarPath);
            }
            log.error("添加用户时发生严重错误: {}", e.getMessage(), e);
            throw new RuntimeException("添加用户失败，事务已回滚。", e);
        }

    }


    /**
     * 更新用户
     * @param userDto
     */
    @Override

    @AutoFill(value = OperationType.UPDATE)
    public void update(UserDto userDto, MultipartFile avatarFile) {
        log.info("开始更新用户ID: {}", userDto.getUserId());

        User existingUser = userMapper.getUserById(userDto.getUserId());
        if (existingUser == null) {
            throw new RuntimeException("用户不存在，无法更新");
        }

        String oldAvatarPath = existingUser.getAvatar();
        String tempAvatarPath = null;
        String finalAvatarPath = null;
        int uniUser=0;

        try {
            // 1. 如果有新头像文件，先上传到临时目录
            if (avatarFile != null && !avatarFile.isEmpty()) {
                tempAvatarPath = FileUtils.saveFile(avatarFile, FileType.TEMP);
                userDto.setAvatar(tempAvatarPath); // DTO中暂时使用临时路径
                log.info("新头像已上传至临时目录: {}", tempAvatarPath);
            } else {
                userDto.setAvatar(oldAvatarPath); // 保留旧头像
            }

            // 2. 如果更新了密码，进行MD5加密
            if (userDto.getPassword() != null && !userDto.getPassword().isEmpty()) {
                String encryptedPassword = encryptMD5(userDto.getPassword());
                userDto.setPassword(encryptedPassword);
                log.info("更新用户密码已进行MD5加密");
            }

            // 3. 更新数据库中的用户信息
            userMapper.updateUser(userDto);

            // 4. 如果上传了临时头像，将其移动到正式目录
            if (tempAvatarPath != null) {
                FileType finalFileType = getFileTypeByRole(String.valueOf(userDto.getRoleId()));
                finalAvatarPath = FileUtils.moveFileToPermanentDirectory(tempAvatarPath, finalFileType);

                // 5. 更新数据库中的头像路径为【最终路径】
                UserDto avatarUpdateDto = new UserDto();
                avatarUpdateDto.setUserId(userDto.getUserId());
                avatarUpdateDto.setAvatar(finalAvatarPath);
                userMapper.updateUser(avatarUpdateDto);
                log.info("新头像已从临时目录移动到正式目录：{}", finalAvatarPath);

                // 6. 删除旧头像（如果存在且不是默认头像）
                if (oldAvatarPath != null && !oldAvatarPath.contains("default")) {
                    FileUtils.deleteFile(oldAvatarPath);
                    log.info("旧头像 {} 已删除", oldAvatarPath);
                }
            }

            // 7. 根据角色处理特定信息
            switch (userDto.getRoleId().intValue()) {
                case 1: // 学生
                    userDto.setStudentId(userDto.getUserId());
                    studentMapper.updateWithStudent(userDto);
                    Long studentId = studentMapper.getStudentIdByUserId(userDto.getUserId());
                    if (studentId != null && userDto.getTraining() != null && !userDto.getTraining().isEmpty()) {
                        // 先删除该学生所有现有的培训方向关联
                        log.info("删除学生ID={}的所有培训方向关联", studentId);
                        studentMapper.deleteStudentDirections(studentId);

                        // 添加新的培训方向关联
                        for (Integer directionId : userDto.getTraining()) {
                            studentMapper.addStudentDirection(studentId, directionId.longValue());
                        }
                    }
                    break;
                case 2: // 教师
                    // 更新教师特有信息
                    Long teacherIdToUpdate = teacherMapper.getTeacherIdByUserId(userDto.getUserId());
                    if (teacherIdToUpdate == null) {
                        // 如果教师记录不存在，则创建
                        Teacher teacher = new Teacher();
                        teacher.setUserId(userDto.getUserId());
                        teacher.setTitle(userDto.getTitle());
                        teacher.setOfficeLocation(userDto.getOfficeLocation());

                        // 从培训方向列表中设置主方向ID
                        if (userDto.getTraining() != null && !userDto.getTraining().isEmpty()) {
                            teacher.setDirectionId(userDto.getTraining().get(0).longValue());
                        } else {
                            log.error("尝试为不存在的教师创建记录，但未提供任何培训方向. UserDto: {}", userDto);
                            throw new IllegalArgumentException("教师必须至少关联一个培训方向。");
                        }

                        log.info("更新时创建老师记录，职称: {}, 办公室位置: {}, 主方向ID: {}",
                                userDto.getTitle(), userDto.getOfficeLocation(), teacher.getDirectionId());
                        teacherMapper.insert(teacher);
                        teacherIdToUpdate = teacher.getTeacherId();
                    } else {
                        // 如果教师记录已存在，则更新
                        UserDto updateDto = new UserDto();
                        updateDto.setUserId(userDto.getUserId());
                        updateDto.setTitle(userDto.getTitle());
                        updateDto.setOfficeLocation(userDto.getOfficeLocation());

                        // 设置主方向ID用于更新
                        if (userDto.getTraining() != null && !userDto.getTraining().isEmpty()) {
                            updateDto.setDirectionId(userDto.getTraining().get(0).longValue());
                        }

                        log.info("更新老师信息，职称: {}, 办公室位置: {}, 主方向ID: {}",
                                updateDto.getTitle(), updateDto.getOfficeLocation(), updateDto.getDirectionId());
                        teacherMapper.updateWithTeacher(updateDto);
                    }

                    // 更新教师的培训方向（先删除旧的，再添加新的）
                    teacherMapper.deleteTeacherDirectionsByTeacherId(teacherIdToUpdate);
                    if (userDto.getTraining() != null && !userDto.getTraining().isEmpty()) {
                        for (Integer directionId : userDto.getTraining()) {
                            teacherMapper.addTeacherDirection(teacherIdToUpdate, directionId.longValue());
                        }
                    }
                    break;
                case 0: // 访客
                    // 访客只需要更新基本信息和职位ID，已经在userMapper.updateUser中处理
                    log.info("更新访客用户，职位ID: {}", userDto.getPositionId());
                    break;
                default:
                    // 其他角色只更新基本信息
                    break;
            }

            // 清除缓存
            clearUserCache(userDto.getUserId()); // 清除用户缓存
            clearUserListCache(); // 清除用户列表缓存

            // 如果更新了头像且旧头像不是默认头像，则删除旧头像
            if (finalAvatarPath != null && oldAvatarPath != null && !oldAvatarPath.equals(finalAvatarPath)) {
                FileUtils.deleteFile(oldAvatarPath);
                log.info("旧头像 {} 已删除", oldAvatarPath);
            }

            log.info("用户 {} 的信息更新成功", userDto.getUserId());

        } catch(DuplicateKeyException e) {
            log.warn("新增用户失败，用户名或学号已存在：{}", e.getMessage());
            // 抛出业务异常，让全局异常处理器能够捕获并返回给前端具体信息
            throw new BusinessException("用户名或学号已存在，请使用其他名称。");
        }catch (Exception e) {
            // 异常处理：删除可能已上传的临时文件或正式文件
            if (tempAvatarPath != null) {
                FileUtils.deleteFile(tempAvatarPath);
                log.info("更新失败，已删除临时上传的头像：{}", tempAvatarPath);
            }
            if (finalAvatarPath != null) {
                FileUtils.deleteFile(finalAvatarPath);
                log.info("更新失败，已删除移动到正式目录的头像：{}", finalAvatarPath);
            }

            log.error("更新用户失败: {}", e.getMessage(), e);
            throw new RuntimeException("更新用户失败", e);
        }
    }


    /**
     * 批量删除用户
     * @param userIds
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<String> userIds) {
        // 🔒 终极安全检查：管理员只能被超级管理员删除
        // 获取当前操作用户（如果在安全上下文中）
        try {
            com.back_hexiang_studio.context.UserContextHolder.getCurrentId(); // 确保有用户上下文
            Long currentUserId = com.back_hexiang_studio.context.UserContextHolder.getCurrentId();
            if (currentUserId != null) {
                // 检查是否有管理员要被删除，如果有，确保当前用户是超级管理员
                for (String userIdStr : userIds) {
                    try {
                        Long userId = Long.parseLong(userIdStr);
                        User userToDelete = userMapper.getUserById(userId);
                        if (userToDelete != null && userToDelete.getRoleId() != null) {
                            // 如果要删除的是管理员(roleId=3)，检查当前用户权限
                            if (userToDelete.getRoleId() == 3L) {
                                User currentUser = userMapper.getUserById(currentUserId);
                                if (currentUser == null || !Objects.equals(currentUser.getRoleId(), 4L)) {
                                    log.error("🚨 安全违规：用户{}(角色{})尝试删除管理员用户{}(角色{})", 
                                        currentUserId, currentUser != null ? currentUser.getRoleId() : "未知", 
                                        userId, userToDelete.getRoleId());
                                    throw new SecurityException("管理员账户只能由超级管理员删除");
                                }
                            }
                            // 如果要删除的是超级管理员，直接拒绝
                            if (userToDelete.getRoleId() == 4L) {
                                log.error("🚨 安全违规：尝试删除超级管理员用户{}", userId);
                                throw new SecurityException("不能删除超级管理员账户");
                            }
                        }
                    } catch (NumberFormatException e) {
                        log.warn("用户ID格式错误: {}", userIdStr);
                        continue;
                    }
                }
            }
        } catch (SecurityException e) {
            throw e; // 重新抛出安全异常
        } catch (Exception e) {
            log.warn("无法获取当前用户上下文，跳过服务层安全检查: {}", e.getMessage());
            // 如果无法获取用户上下文，继续执行（可能是系统级调用）
        }
        
        // Step 1: 准备一个列表，用于收集所有需要删除的文件的【相对路径】
        List<String> filePathsToDelete = new ArrayList<>();
        // 收集需要删除的缓存键
        Set<String> keysToDelete = new HashSet<>();
        Set<Long> roleIdsToDelete = new HashSet<>();

        // Step 2: 遍历所有待删除的用户ID，收集文件路径并删除关联数据
        for (String userIdStr : userIds) {
            try {
                Long userId = Long.parseLong(userIdStr);
                User user = userMapper.getUserById(userId);

                if (user != null) {
                    // a. 收集需要删除的头像文件路径
                    if (user.getAvatar() != null && !user.getAvatar().contains("default-avatar")) {
                        filePathsToDelete.add(user.getAvatar());
                    }

                    // b. 收集荣誉和证书的附件路径
                    List<UserHonorVo> honors = userHonorMapper.findByUserId(userId);
                    for (UserHonorVo honor : honors) {
                        if (honor.getAttachment() != null && !honor.getAttachment().isEmpty()) {
                            filePathsToDelete.add(honor.getAttachment());
                        }
                    }
                    List<UserCertificateVo> certificates = userCertificateMapper.findByUserId(userId);
                    for (UserCertificateVo certificate : certificates) {
                        if (certificate.getAttachment() != null && !certificate.getAttachment().isEmpty()) {
                            filePathsToDelete.add(certificate.getAttachment());
                        }
                    }

                    Long roleId = user.getRoleId();
                    if (roleId != null) {
                        roleIdsToDelete.add(roleId);
                    }

                    // c. 根据角色删除相关联的表数据 (这部分逻辑在批量删除前执行，以处理个体依赖)
                    if (roleId == 1) { // 学生
                        Long studentId = studentMapper.getStudentIdByUserId(userId);
                        if (studentId != null) {
                            studentMapper.deleteStudentDirections(studentId);
                        }
                    } else if (roleId == 2) { // 老师
                        Long teacherId = teacherMapper.getTeacherIdByUserId(userId);
                        if (teacherId != null) {
                            teacherMapper.deleteTeacherDirections(teacherId);
                        }
                    }
                    // 管理员或其他角色没有额外的方向表等需要预先清理

                    // d. 收集用户相关的缓存键
                    keysToDelete.add("user:info:" + userId);
                    keysToDelete.add("login:token:" + userId);
                    keysToDelete.add("login:user:" + userId);
                }
            } catch (Exception e) {
                log.error("删除用户关联数据失败，用户ID: {}, 错误: {}", userIdStr, e.getMessage(), e);
            }
        }

        // Step 3: 批量删除数据库记录
        if (!userIds.isEmpty()) {
            // a. 删除荣誉和证书
            userHonorMapper.deleteByUserIds(userIds);
            userCertificateMapper.deleteByUserIds(userIds);

            // b. 删除学生/老师/管理员等角色特定信息
            studentMapper.deleteStudentByUserIds(userIds);
            teacherMapper.deleteTeacherByUserIds(userIds);
            managerMapper.deleteManagerByUserIds(userIds);

            // c. 删除用户-学生/角色关联
            userStudentMapper.delete(userIds);
            userRoleMapper.deleteByUserIds(userIds);

            // d. 最后删除用户表中的记录
            userMapper.delete(userIds);
        }


        // Step 4: 从服务器磁盘上删除物理文件
        for (String filePath : filePathsToDelete) {
            if (filePath != null && !filePath.isEmpty()) {
                FileUtils.deleteFile(filePath);
            }
        }

        // Step 5: 清理所有相关缓存
        // a. 批量删除收集的用户缓存键
        if (!keysToDelete.isEmpty()) {
            redisTemplate.delete(keysToDelete);
        }

        // b. 删除角色权限缓存
        for (Long roleId : roleIdsToDelete) {
            redisTemplate.delete("permission:role:" + roleId);
        }

        // c. 删除所有用户列表缓存
        clearUserListCache();

        // d. 删除用户荣誉和证书相关缓存
        for (String userIdStr : userIds) {
            // 清理用户荣誉列表缓存
            redisTemplate.delete("user:honors:" + userIdStr);
            // 清理用户证书列表缓存
            redisTemplate.delete("user:certificates:" + userIdStr);
            // 清理用户详细信息缓存
            clearUserCache(Long.parseLong(userIdStr));
        }

        // e. 清理可能存在的全局缓存
        Set<String> globalPatterns = new HashSet<>();
        globalPatterns.add("user:all*");
        globalPatterns.add("users:count*");
        globalPatterns.add("users:stats*");
        globalPatterns.add("dashboard:user*");

        for (String pattern : globalPatterns) {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("清理全局缓存，模式: {}, 键数量: {}", pattern, keys.size());
            }
        }

        log.info("成功删除用户，ID: {}, 并清理了相关数据、文件和缓存。", userIds);
    }


    /**
     * 根据用户id获取用户
     * @param userId
     * @return
     */
    public User getUserById(Long userId) {
        String cacheKey = "user:" + userId;
        User user = (User) redisTemplate.opsForValue().get(cacheKey);
        if (user == null) {
            user = userMapper.getUserById(userId);
            if (user != null) {
                redisTemplate.opsForValue().set(cacheKey, user, 10, TimeUnit.MINUTES);
            }
        }
        return user;
    }

    @Override
    public List<String> getPermissionsByRole(Long positionId) {
        // 根据数据库中的职位ID映射到角色权限
        List<String> permissions = new ArrayList<>();

        if (positionId != null) {
            switch (positionId.intValue()) {
                case 8: // admin - 超级管理员
                    permissions.add("ROLE_ADMIN");
                    // 保留原有权限
                    permissions.add("TASK_CREATE");
                    permissions.add("TASK_APPROVE");
                    permissions.add("COURSE_CREATE");
                    permissions.add("COURSE_APPROVE");
                    permissions.add("COURSE_MANAGE");
                    permissions.add("COURSE_VIEW");
                    // 添加新权限 - 超级管理员拥有所有权限
                    permissions.add("ATTENDANCE_MANAGE");
                    permissions.add("USER_MANAGE");
                    permissions.add("TASK_MANAGE");
                    permissions.add("NOTICE_MANAGE");
                    permissions.add("MATERIAL_MANAGE");
                    permissions.add("DASHBOARD_VIEW");
                    permissions.add("STUDIO_INFO_VIEW");
                    break;
                case 6: // manager - 主任
                case 7: // manager - 副主任  
                    permissions.add("ROLE_MANAGER");
                    // 保留原有权限
                    permissions.add("TASK_CREATE");
                    permissions.add("TASK_APPROVE");
                    permissions.add("COURSE_CREATE");
                    permissions.add("COURSE_APPROVE");
                    permissions.add("COURSE_MANAGE");
                    permissions.add("COURSE_VIEW");
                    // 添加新权限 - 主任和副主任：考勤管理，人员管理，任务管理，课程管理，公告管理，资料管理
                    permissions.add("ATTENDANCE_MANAGE");
                    permissions.add("USER_MANAGE");
                    permissions.add("TASK_MANAGE");
                    permissions.add("NOTICE_MANAGE");
                    permissions.add("MATERIAL_MANAGE");
                    permissions.add("DASHBOARD_VIEW");
                    permissions.add("STUDIO_INFO_VIEW");
                    break;
                case 5: // teacher - 老师
                    permissions.add("ROLE_TEACHER");
                    // 保留原有权限
                    permissions.add("TASK_CREATE");
                    permissions.add("TASK_APPROVE");
                    permissions.add("COURSE_CREATE");
                    permissions.add("COURSE_MANAGE");
                    permissions.add("COURSE_VIEW");
                    // 添加新权限 - 老师：课程管理，首页和信息门户
                    permissions.add("DASHBOARD_VIEW");
                    permissions.add("STUDIO_INFO_VIEW");
                    break;
                case 3: // student - 部长
                case 4: // student - 副部长
                    permissions.add("ROLE_STUDENT");
                    // 保留原有权限
                    permissions.add("TASK_VIEW");
                    permissions.add("COURSE_VIEW");
                    // 添加新权限 - 部长和副部长：公告管理，资料管理，首页和信息门户
                    permissions.add("NOTICE_MANAGE");
                    permissions.add("MATERIAL_MANAGE");
                    permissions.add("DASHBOARD_VIEW");
                    permissions.add("STUDIO_INFO_VIEW");
                    break;
                case 1: // student - 普通学员
                    permissions.add("ROLE_STUDENT");
                    // 普通学员不应该有任务查看权限，只保留课程查看
                    permissions.add("COURSE_VIEW");
                    // 添加新权限 - 普通学员：首页和信息门户
                    permissions.add("DASHBOARD_VIEW");
                    permissions.add("STUDIO_INFO_VIEW");
                    break;
                default:
                    permissions.add("ROLE_STUDENT"); // 默认学生权限
                    // 默认权限也不包含任务查看权限
                    permissions.add("COURSE_VIEW");
                    // 默认权限：首页和信息门户
                    permissions.add("DASHBOARD_VIEW");
                    permissions.add("STUDIO_INFO_VIEW");
                    break;
            }
        }

        // 🔧 优化：权限查询频繁，降级为DEBUG，避免权限信息泄露
        log.debug("职位ID: {} 对应权限数量: {}", positionId, permissions.size());
        return permissions;
    }

    /**
     * 修改用户密码
     * @param changePasswordDto 包含旧密码和新密码的DTO
     * @throws BusinessException 如果密码修改失败（如旧密码不正确）
     */
    @Override
    public void changePassword(ChangePasswordDto changePasswordDto) {
        log.info("修改用户密码，用户ID: {}", changePasswordDto.getUserId());

        try {
            // 检查用户是否存在
            User user = userMapper.getUserById(changePasswordDto.getUserId());
            if (user == null) {
                log.warn("修改密码失败，用户不存在，用户ID: {}", changePasswordDto.getUserId());
                throw new BusinessException("用户不存在");
            }

            // 1. 对旧密码进行MD5加密后验证
            String encryptedOldPassword = encryptMD5(changePasswordDto.getOldPassword());
            int count = userMapper.validatePassword(changePasswordDto.getUserId(), encryptedOldPassword);
            log.info("密码验证结果: 用户ID={}, 匹配数={}", changePasswordDto.getUserId(), count);

            if (count == 0) {
                log.warn("修改密码失败，旧密码不正确，用户ID: {}", changePasswordDto.getUserId());
                throw new BusinessException("原密码不正确");
            }

            // 2. 对新密码进行MD5加密后更新
            String encryptedNewPassword = encryptMD5(changePasswordDto.getNewPassword());
            int rows = userMapper.updatePassword(changePasswordDto.getUserId(), encryptedNewPassword);
            log.info("密码更新结果: 用户ID={}, 影响行数={}", changePasswordDto.getUserId(), rows);

            if (rows <= 0) {
                log.warn("修改密码失败，数据库更新失败，用户ID: {}", changePasswordDto.getUserId());
                throw new BusinessException("密码更新失败");
            }

            // 3. 清除用户相关缓存
            clearUserCache(changePasswordDto.getUserId());
            log.info("用户密码修改成功，用户ID: {}", changePasswordDto.getUserId());
        } catch (BusinessException e) {
            // 业务异常直接抛出
            throw e;
        } catch (Exception e) {
            log.error("修改密码时发生错误: {}", e.getMessage(), e);
            throw new BusinessException("系统错误，密码修改失败");
        }
    }

    /**
     * 修改用户状态
     * @param userId 用户ID
     * @param status 状态值：0-禁用，1-启用
     */
    @Override
    public void updateStatus(Long userId, String status) {
        log.info("更新用户状态 - 用户ID: {}, 新状态: {}", userId, status);

        try {
            int affectedRows = userMapper.updateStatus(userId, status);
            if (affectedRows > 0) {
                // 清除用户相关缓存
                clearUserCache(userId);
                clearUserListCache();

                log.info("用户状态更新成功 - 用户ID: {}, 状态: {}", userId,
                        "1".equals(status) ? "启用" : "禁用");
            } else {
                log.warn("用户状态更新失败或状态未改变 - 用户ID: {}", userId);
                throw new RuntimeException("更新用户状态失败，可能用户不存在");
            }
        } catch (Exception e) {
            log.error("更新用户状态异常 - 用户ID: {}, 错误: {}", userId, e.getMessage());
            throw new RuntimeException("更新用户状态失败: " + e.getMessage());
        }
    }

    @Override
    public boolean updateAvatar(UserDto userDto) {
        return false;
    }

    /**
     * 清除单个用户的缓存
     * @param userId
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

        // 如果有其他与用户相关的特定缓存，也在这里添加
        redisTemplate.delete(keysToDelete);
        log.info("清理用户缓存，用户ID: {}, 键数量: {}", userId, keysToDelete.size());
    }

    /**
     * 清除所有与用户列表相关的缓存
     */
    private void clearUserListCache() {
        // 匹配所有以 "user:list" 开头的键
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
                log.info("清理用户列表缓存，模式: {}, 键数量: {}", pattern, keys.size());
            }
        }

        if (totalDeleted > 0) {
            log.info("总共清理了 {} 个用户列表相关缓存键", totalDeleted);
        } else {
            log.info("没有找到需要清理的用户列表缓存");
        }
    }

    /**
     * 根据角色ID获取文件类型
     * @param roleId 角色ID
     * @return 文件类型
     */
    private FileType getFileTypeByRole(String roleId) {
        if (roleId == null) {
            return FileType.TEMP; // 默认类型
        }
        switch (roleId) {
            case "1":
                return FileType.AVATAR_STUDENT;
            case "2":
                return FileType.AVATAR_TEACHER;
            case "3":
                return FileType.AVATAR_ADMIN;
            default:
                return FileType.TEMP;
        }
    }

    /**
     * 根据角色ID获取默认头像路径
     * @param roleId 角色ID
     * @return 默认头像路径
     */
    private String getDefaultAvatarByRole(String roleId) {
        if (roleId == null) {
            return "/path/to/default/avatar.png"; // 通用默认头像
        }
        switch (roleId) {
            case "1":
                return "/path/to/default/student_avatar.png";
            case "2":
                return "/path/to/default/teacher_avatar.png";
            case "3":
                return "/path/to/default/admin_avatar.png";
            default:
                return "/path/to/default/avatar.png";
        }
    }

    /**
     * 统计当前在线用户数量
     * @return 在线用户数量
     */
    @Override
    public long countOnlineUsers() {
        return 0; // 此方法依赖TokenService，暂时禁用
        // try {
        //     // 获取所有用户列表
        //     PageDto pageDto = new PageDto();
        //     pageDto.setPage(1);
        //     pageDto.setPageSize(999); // 获取所有用户
        //     PageResult result = this.list(pageDto);
        //
        //     if (result == null || result.getRecords() == null) {
        //         return 0;
        //     }
        //
        //     @SuppressWarnings("unchecked")
        //     List<basicUserVo> users = (List<basicUserVo>) result.getRecords();
        //
        //     // 统计在线用户数量
        //     long onlineCount = 0;
        //     for (basicUserVo user : users) {
        //         if (user.getUserId() != null && tokenService.isUserOnline(user.getUserId())) {
        //             onlineCount++;
        //         }
        //     }
        //
        //     log.info("统计在线用户完成 - 总用户数: {}, 在线用户数: {}", users.size(), onlineCount);
        //     return onlineCount;
        // } catch (Exception e) {
        //     log.error("统计在线用户失败: {}", e.getMessage());
        //     return 0; // 出错时返回0
        // }
    }

    /**
     * 按职位ID统计用户数量
     * @param positionId 职位ID
     * @return 用户数量
     */
    @Override
    public int countByPositionId(Long positionId) {
        try {
            // 调用UserMapper的方法，您需要在UserMapper中添加
            return userMapper.countByPositionId(positionId);
        } catch (Exception e) {
            log.error("按职位统计用户数量失败", e);
            return 0;
        }
    }

    /**
     * 按多个职位ID统计用户数量
     * @param positionIds 职位ID列表
     * @return 用户数量
     */
    @Override
    public int countByPositionIds(List<Long> positionIds) {
        try {
            return userMapper.countByPositionIds(positionIds);
        } catch (Exception e) {
            log.error("按职位列表统计用户数量失败", e);
            return 0;
        }
    }

    /**
     * 按职位ID获取用户列表
     * @param positionId 职位ID
     * @return 用户列表
     */
    @Override
    public List<Map<String, Object>> getUsersByPositionId(Long positionId) {
        try {
            return userMapper.getUsersByPositionId(positionId);
        } catch (Exception e) {
            log.error("按职位查询用户列表失败", e);
            return new ArrayList<>();
        }
    }
}










