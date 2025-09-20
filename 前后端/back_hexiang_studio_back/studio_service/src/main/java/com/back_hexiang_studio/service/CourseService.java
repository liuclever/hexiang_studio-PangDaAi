package com.back_hexiang_studio.service;

import com.back_hexiang_studio.dv.dto.ChangeStatusDto;
import com.back_hexiang_studio.dv.dto.CourseDto;
import com.back_hexiang_studio.dv.dto.PageCourseDto;
import com.back_hexiang_studio.dv.dto.RemoveStudentDto;
import com.back_hexiang_studio.dv.dto.addStudentDto;
import com.back_hexiang_studio.dv.vo.CourseVo;
import com.back_hexiang_studio.dv.vo.StudentVo;
import com.back_hexiang_studio.entity.TrainingDirection;
import com.back_hexiang_studio.result.PageResult;
import com.back_hexiang_studio.result.Result;

import java.io.IOException;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface CourseService  {
    /**
     * 分页查询课程
     * @param pageNoticeDto
     * @return
     */
    public PageResult list(PageCourseDto pageNoticeDto);

    /**
     * 获取课程详情
     * @param id
     * @return
     */
    CourseVo detail(Long id);

    /**
     * 获取老师列表
     * @return
     */
    List<String> teacherList();

 

    /**
     * 返回学生列表
     * @param courseDto
     * @return
     */
    List<StudentVo> studentCureentList(CourseDto courseDto);

    /**
     * 添加课程学生
     * @param addStudentDto
     */
    void addStudent(addStudentDto addStudentDto);

    /**
     * 从课程中移出学生
     * @param removeStudentDto
     */
    void removeStudent(RemoveStudentDto removeStudentDto);

    /**
     * 获取所有学生信息
     * @param pageCourseDto
     * @return
     */
    PageResult studentList(PageCourseDto pageCourseDto);

    /*
 * 删除课程
     */
    void deleteCourse(List<Long> ids);

    /**
     * 修改课程状态
     * @param
     */
    void changeStatus(ChangeStatusDto changeStatusDto);

    /**
     * 搜索课程（用于下拉选择）
     * @param query 搜索关键词
     * @return 课程列表
     */
    List<CourseVo> searchCourses(String query);

    void addCourseWithFiles(CourseDto courseDto, MultipartFile coverImageFile, List<MultipartFile> materialFiles) throws IOException;

    void updateCourseWithFiles(CourseDto courseDto, MultipartFile coverImageFile, List<MultipartFile> materialFiles, List<Long> keepMaterialIds) throws IOException;
    
    /**
     * 获取已选课程的学生列表
     * @param courseId 课程ID
     * @return 已选该课程的学生列表
     */
    List<StudentVo> getEnrolledStudents(Long courseId);
    
    /**
     * 获取未选课但培训方向匹配的学生列表
     * @param courseId 课程ID
     * @return 未选课但培训方向匹配的学生列表
     */
    List<StudentVo> getEligibleStudents(Long courseId);

    /**
     * 根据教师ID获取课程列表
     * @param teacherId 教师ID
     * @return 课程列表
     */
    List<CourseVo> getCoursesByTeacherId(Long teacherId);

    /**
     * 根据学生ID获取课程列表
     * @param studentId 学生ID
     * @return 课程列表
     */
    List<CourseVo> getCoursesByStudentId(Long studentId);

    /**
     * 根据培训方向选择课程
     * @param directionName
     * @return
     */
    List<TrainingDirection> findDirectionByName(String directionName);
    
    /**
     * 根据用户角色获取课程列表
     * 学生：只能看见自己参与的课程
     * 老师：只能看自己创建的课程  
     * 管理员：可以看见全部课程
     * @param currentUserId 当前用户ID
     * @return 课程列表
     */
    List<CourseVo> getCoursesByUserRole(Long currentUserId);
}
