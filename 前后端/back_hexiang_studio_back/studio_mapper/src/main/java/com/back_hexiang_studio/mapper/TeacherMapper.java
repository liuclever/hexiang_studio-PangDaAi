package com.back_hexiang_studio.mapper;

import com.back_hexiang_studio.dv.dto.UserDto;
import com.back_hexiang_studio.dv.vo.UserVo;
import com.back_hexiang_studio.entity.Teacher;
import com.back_hexiang_studio.entity.TrainingDirection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TeacherMapper {


    // 添加老师信息
    int addWithTeacher(UserDto userDto);

    // 添加教师培训方向关联
    int addTeacherDirection(@Param("teacherId") Long teacherId, @Param("directionId") Long directionId);

    // 根据培训方向名称获取ID
    Long getDirectionIdByName(String directionName);

    //根据用户id获取教师id
    Long getTeacherIdByUserId(Long userId);

    //根据教师id获取用户id
    Long getDirectionId(Long teacherId);

    //根据教师id获取教师信息
    List<String> getdirections(Long teacherId);



    // 更新教师信息
    int updateWithTeacher(UserDto userDto);

    // 删除教师培训方向关联
    int deleteTeacherDirections(Long teacherId);

    // 检查教师培训方向关联是否存在
    int checkTeacherDirectionExists(@Param("teacherId") Long teacherId, @Param("directionId") Long directionId);

    // 获取教师详细信息
    Teacher getTeacherInfo(Long teacherId);

    // 获取教师所有培训方向
    List<String> getTeacherAllDirections(Long teacherId);

    // 删除教师记录
    int deleteTeacher(Long userId);

    void insert(Teacher teacher);

    void deleteTeacherDirectionsByTeacherId(Long teacherId);

    Teacher getTeacherByUserId(Long userId);


    //通过老师名字找id
    Long getTeacherIdByName(String teacherName);

    //获取老师列表
    List<String> getTeacherList();

    /**
     * 根据用户ID列表批量删除教师信息
     * @param userIds 用户ID列表
     * @return 影响行数
     */
    int deleteTeacherByUserIds(@Param("userIds") List<String> userIds);
}
