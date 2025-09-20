package com.back_hexiang_studio.mapper;

import com.back_hexiang_studio.dv.dto.PageDto;
import com.back_hexiang_studio.dv.dto.UserDto;
import com.back_hexiang_studio.dv.dto.ChangePasswordDto;
import com.back_hexiang_studio.dv.vo.basicUserVo;
import com.back_hexiang_studio.dv.vo.task.UserList;
import com.back_hexiang_studio.entity.User;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Arrays;

@Mapper
public interface UserMapper {

    // 登录验证
    User select(@Param("user_name") String userName, @Param("password") String password);

    // 根据用户名查询用户（用于检查用户名是否已存在）
    @Select("select * from user where user_name=#{username}")
    User getUserByUsername(@Param("username") String username);

    /**
     * 新增用户，并回填userId
     * @param userDto 用户数据
     * @return 影响的行数
     */
    int addUser(UserDto userDto);

    // 获取用户信息信息的基础信息
    User getUserById(Long userId);

    int updateStatus(@Param("userId") Long userId, @Param("status")String status);

    /**
     * 更新用户基本信息
     * @param userDto 用户数据
     * @return 影响的行数
     */
    int updateUser(UserDto userDto);

    //获取用户列表（分页）
    Page<basicUserVo> selectByPage(PageDto pageDto);

    // 获取用户权限
    List<String> getPermissions(Long roleId);

    //获取用户职位
    String getPosition(Long positionId);

    //获取用户角色
    String getRole(Long currentId);

    // 添加用户角色关联
    int addUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    //删除用户
    void delete(@Param("userIds") List<String> userIds);

    /**
     * 更新用户头像
     * @param userDto 包含用户ID和新头像路径的DTO对象
     * @return 影响的行数
     */
    int updateAvatar(UserDto userDto);

    //获取用户列表（任务管理，带名称搜索）
    List<UserList> getUSerListOfTaskWithKeyword(@Param("keyword") String keyword);

    //根据用户id找名字
    @Select("select user_name from user where user_id = #{userId}")
    String getUserNameById(Long userId);
    
    //根据用户id找真实姓名
    @Select("select name from user where user_id = #{userId}")
    String getRealNameById(Long userId);

    //根据真实姓名找用户ID
    @Select("select user_id from user where name = #{realName} and status = '1' limit 1")
    Long getUserIdByRealName(@Param("realName") String realName);

    /**
     * 修改用户密码
     * @param userId 用户ID
     * @param newPassword 新密码
     * @return 影响的行数
     */
    int updatePassword(@Param("userId") Long userId, @Param("newPassword") String newPassword);
    
    /**
     * 验证用户旧密码是否正确
     * @param userId 用户ID
     * @param oldPassword 旧密码
     * @return 匹配的用户数量
     */
    int validatePassword(@Param("userId") Long userId, @Param("oldPassword") String oldPassword);

    /**
     * 按职位ID统计用户数量
     */
    @Select("SELECT COUNT(*) FROM user WHERE position_id = #{positionId}")
    int countByPositionId(@Param("positionId") Long positionId);

    /**
     * 按多个职位ID统计用户数量
     */
    int countByPositionIds(@Param("positionIds") List<Long> positionIds);

    /**
     * 按职位ID获取用户列表
     */
    List<Map<String, Object>> getUsersByPositionId(@Param("positionId") Long positionId);
}