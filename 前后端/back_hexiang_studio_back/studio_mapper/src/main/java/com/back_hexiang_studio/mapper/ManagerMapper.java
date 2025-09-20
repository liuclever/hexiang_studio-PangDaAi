package com.back_hexiang_studio.mapper;


import com.back_hexiang_studio.dv.dto.PageDto;
import com.back_hexiang_studio.dv.dto.UserDto;
import com.back_hexiang_studio.dv.vo.UserVo;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ManagerMapper {

    /**
     * 分页查询
     * @param pageDto
     * @return
     */
    Page<UserVo> selectByPage(PageDto pageDto);

    /**
     * 添加用户
     * @param userDto
     */
    void addWithManager(UserDto userDto);

    /**
     * 修改管理
     * @param userDto
     */
    void updateWithManager(UserDto userDto);

    /**
     * 删除管理员记录
     * @param userId 用户ID
     */
    int deleteManager(Long userId);

    /**
     * 根据用户ID列表批量删除管理员信息
     * @param userIds 用户ID列表
     * @return 影响行数
     */
    int deleteManagerByUserIds(@Param("userIds") List<String> userIds);
}

