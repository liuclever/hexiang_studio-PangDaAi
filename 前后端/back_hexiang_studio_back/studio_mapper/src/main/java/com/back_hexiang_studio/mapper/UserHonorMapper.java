package com.back_hexiang_studio.mapper;

import com.back_hexiang_studio.dv.dto.UserHonorDto;
import com.back_hexiang_studio.dv.vo.UserHonorVo;
import com.back_hexiang_studio.entity.UserHonor; // 导入实体类
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserHonorMapper {

    /**
     * 根据用户ID查询用户的荣誉信息列表
     * @param userId 用户ID
     * @return 荣誉信息列表
     */
    List<UserHonorVo> findByUserId(@Param("userId") Long userId);

    /**
     * 根据荣誉ID查询荣誉信息
     * @param honorId 荣誉ID
     * @return 荣誉信息
     */
    UserHonorVo findById(@Param("honorId") Long honorId);

    /**
     * 添加用户荣誉信息
     * @param honor 荣誉信息实体
     * @return 影响行数
     */
    int insert(UserHonor honor);

    /**
     * 更新用户荣誉信息
     * @param honor 荣誉信息实体
     * @return 影响行数
     */
    int update(UserHonor honor);

    /**
     * 删除用户荣誉信息
     * @param honorId 荣誉ID
     * @return 影响行数
     */
    int delete(@Param("honorId") Long honorId);
    
    /**
     * 根据用户ID列表批量删除荣誉信息
     * @param userIds 用户ID列表
     * @return 影响行数
     */
    int deleteByUserIds(@Param("userIds") List<String> userIds);
} 