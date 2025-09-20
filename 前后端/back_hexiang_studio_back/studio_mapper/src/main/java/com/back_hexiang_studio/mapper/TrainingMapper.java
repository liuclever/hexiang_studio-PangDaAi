package com.back_hexiang_studio.mapper;

import com.back_hexiang_studio.entity.TrainingDirection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TrainingMapper {



    /**
     * 根据名称查询培训方向
     * @param  names
     * @return
     */
    List<Long> getIdsByNames(@Param("names") List<String> names);



    /**
     * 插入一条用户与培训方向的关联记录
     * @param userId 用户ID（学生ID或教师ID）
     * @param trainingId 培训方向ID
     * @param roleType 角色类型，可以是"student"或"teacher"
     */
    void insertRelation(@Param("userId") Long userId, @Param("trainingId") Long trainingId, @Param("roleType") String roleType);


}
