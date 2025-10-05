package com.back_hexiang_studio.service;

import com.back_hexiang_studio.dv.dto.UserDto;
import com.back_hexiang_studio.dv.dto.UserLoginDto;
import com.back_hexiang_studio.dv.dto.PageDto;
import com.back_hexiang_studio.dv.dto.ChangePasswordDto;
import com.back_hexiang_studio.dv.vo.UserLoginVo;
import com.back_hexiang_studio.dv.vo.UserVo;
import com.back_hexiang_studio.entity.User;
import com.back_hexiang_studio.result.PageResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface UserService {
    /**
     * 用户登录
     *
     * @param userLoginDto 用户登录信息
     * @return 用户实体
     */
  UserLoginVo login(UserLoginDto userLoginDto);

    /**
     * 添加用户
     * @param userDto 用户数据
     * @param avatarFile 头像文件（可选）
     */
    void add(UserDto userDto, MultipartFile avatarFile);

    /**
     * 更新用户信息
     * @param userDto 用户数据
     * @param avatarFile 头像文件（可选）
     */
    void update(UserDto userDto, MultipartFile avatarFile);



    /**
     * 删除用户
     * @param userIds 用户ID列表
     */
    void delete(List<String> userIds);

    /**
     * 分页获取用户列表
     * @param pageDto 分页参数
     * @return 分页结果
     */
    PageResult list(PageDto pageDto);

    /**
     * 根据ID获取用户详情
     * @param userId 用户ID
     * @return 用户视图对象
     */
    UserVo selectById(Long userId);

    /**
     * 更新用户状态
     * @param userId 用户ID
     * @param status 新状态
     */
    void updateStatus(Long userId, String status);

    /**
     * 更新用户头像
     * @param userDto 用户数据
     * @return 是否更新成功
     */
    boolean updateAvatar(UserDto userDto);

    /**
     * 根据ID获取用户
     * @param userId 用户ID
     * @return 用户实体
     */
    User getUserById(Long userId);

    /**
     * 根据角色ID获取权限列表
     * @param roleId 角色ID
     * @return 权限列表
     */
    List<String> getPermissionsByRole(Long roleId);
    
    /**
     * 修改密码
     * @param changePasswordDto 包含旧密码和新密码的DTO
     */
    void changePassword(ChangePasswordDto changePasswordDto);
    
    /**
     * 统计当前在线用户数量
     * @return 在线用户数量
     */
    long countOnlineUsers();

    /**
     * 按职位ID统计用户数量
     * @param positionId 职位ID
     * @return 用户数量
     */
    int countByPositionId(Long positionId);

    /**
     * 按多个职位ID统计用户数量
     * @param positionIds 职位ID列表
     * @return 用户数量
     */
    int countByPositionIds(List<Long> positionIds);

    /**
     * 按职位ID获取用户列表
     * @param positionId 职位ID
     * @return 用户列表
     */
    List<Map<String, Object>> getUsersByPositionId(Long positionId);
}
