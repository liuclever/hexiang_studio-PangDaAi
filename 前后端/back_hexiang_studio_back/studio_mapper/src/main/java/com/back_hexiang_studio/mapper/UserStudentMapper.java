package com.back_hexiang_studio.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserStudentMapper {

    /**
     * 根据用户id批量删除学生课程信息
     * @param userIds
     */
    public void delete(@Param("userIds") List<String> userIds);
}
