package com.back_hexiang_studio.mapper;


import com.back_hexiang_studio.dv.dto.ChangeStatusDto;

import com.back_hexiang_studio.dv.dto.PageCourseDto;
import com.back_hexiang_studio.dv.vo.CourseVo;

import com.back_hexiang_studio.entity.Course;

import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CourseMapper {


    //分页类型查询
    public Page<CourseVo> list(PageCourseDto pageCourseDto) ;

    //查找满足条件的课程
    Long countCourse(PageCourseDto pageCourseDto);

    //添加基本信息
    @Insert(" insert into course(name,description,teacher_id,create_time,status,duration,cover_image,material_url,category_id,location,schedule,create_user,update_user,update_time) " +
            "values(#{name},#{description},#{teacherId},#{createTime},#{status},#{duration},#{coverImage},#{materialUrl},#{categoryId},#{location},#{schedule},#{createUser},#{updateUser},#{updateTime})")
    void add( Course course);

    //根据id查询课程
    CourseVo detail(Long id);
    //更新课程
    void update(Course course);
    //删除课程
    void deleteCourse(Long id);
    //修改课程状态
    void changeStatus(ChangeStatusDto changeStatusDto);
    
    //根据课程数量
    @Select("SELECT COUNT(*) FROM course")
    int countCourses();


    //根据课程id查询课程
    @Select("SELECT * FROM course WHERE course_id = #{courseId}")
    Course selectById(Long courseId);

    //根据学生id和课程id判断学生是否已经报名
    boolean isSrudentEnrolled(@Param("studentId") Long studentId, @Param("courseId") Long courseId);

    //查询课程是否存在

    boolean selectByIdNoNull(Long courseId);

    /**
     * 搜索课程（用于下拉选择）
     * @param query 搜索关键词
     * @return 课程列表
     */
    List<CourseVo> searchCourses(@Param("query") String query);

    /**
     * 根据学生ID查询课程列表
     * @param studentId 学生ID
     * @return 课程列表
     */
    List<CourseVo> getCoursesByStudentId(@Param("studentId") Long studentId);
    
    /**
     * 根据教师ID获取课程列表（老师看自己授课的课程）
     * @param teacherId 教师用户ID
     * @return 课程列表
     */
    List<CourseVo> getCoursesByTeacherId(@Param("teacherId") Long teacherId);
    
    Course getById(Long id);
}
