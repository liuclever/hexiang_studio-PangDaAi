package com.back_hexiang_studio.mapper;

import com.back_hexiang_studio.entity.CourseMaterial;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CourseMaterialMapper {

    /**
     * 批量插入课程资料
     * @param materials
     */
    void insertBatch(List<CourseMaterial> materials);

    /**
     * 根据课程ID查询资料列表
     * @param courseId
     * @return
     */
    @Select("select * from course_material where course_id = #{courseId}")
    List<CourseMaterial> getByCourseId(Long courseId);

    /**
     * 根据课程ID删除所有资料
     * @param courseId
     */
    @Delete("delete from course_material where course_id = #{courseId}")
    void deleteByCourseId(Long courseId);

    /**
     * 根据资料ID列表删除资料
     * @param ids
     */
    void deleteByIds(List<Long> ids);

    /**
     * 根据资料ID查询资料
     */
    @Select("select * from course_material where material_id = #{id}")
    CourseMaterial getById(Long id);
} 