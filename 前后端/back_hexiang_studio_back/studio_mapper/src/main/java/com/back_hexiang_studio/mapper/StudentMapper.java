package com.back_hexiang_studio.mapper;

import com.back_hexiang_studio.dv.dto.*;
import com.back_hexiang_studio.dv.vo.StudentVo;
import com.back_hexiang_studio.dv.vo.UserVo;
import com.back_hexiang_studio.entity.Student;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Mapper
@Repository
public interface StudentMapper {
    void insert(Student student);
 // 添加学生信息
 int addWithStudent(UserDto userDto);

 // 添加学生培训方向关联
 int addStudentDirection(@Param("studentId") Long studentId, @Param("directionId") Long directionId);

 // 根据培训方向名称获取ID
 Long getDirectionIdByName(@Param("directionName") String directionName);

 // 获取学生ID
 Long getStudentIdByUserId(@Param("userId") Long userId);

 //获取培训方向
Long  getDirectionId ( Long studenId );

//更具培训方向id获取培训方向
 List<String> getdirections(Long directionId);

 // 更新学生信息
 int updateWithStudent(UserDto userDto);

 // 删除学生培训方向关联
 int deleteStudentDirections(Long studentId);

 // 检查学生培训方向关联是否存在
 int checkStudentDirectionExists(@Param("studentId") Long studentId, @Param("directionId") Long directionId);
 // 获取学生详细信息
Student getStudentInfo(Long studentId);


 // 获取学生所有培训方向
 List<String> getStudentAllDirections(Long studentId);


 // 删除学生记录
 int deleteStudent(Long userId);

// 获取当前课程学生列表
 List<StudentVo> getStudentCurrentList(CourseDto courseDto);

// 添加学生
    void addStudent(addStudentDto addStudentDto);

// 从课程中移出学生
 @Delete("DELETE FROM student_course WHERE student_id = #{studentId} AND course_id = #{courseId}")
 int removeStudentFromCourse(RemoveStudentDto removeStudentDto);

// 获取学生列表
 Page<StudentVo> getStudentList(PageCourseDto pageCourseDto);
 // 删除学生关联的课程
 void deleteCourseWithTeacher(Long id);

 /**
  * 根据学生ID获取学生信息
  * @param studentId 学生ID
  * @return 学生实体
  */
 Student getStudentById(@Param("studentId") Long studentId);

 /**
  * 查询所有学生（包含用户信息）
  * @return 学生列表
  */
 @Select("SELECT s.*, u.user_name, u.name, u.sex, u.phone, u.email, td.direction_name " +
         "FROM student s " +
         "LEFT JOIN user u ON s.user_id = u.user_id " +
         "LEFT JOIN training_direction td ON s.direction_id = td.direction_id " +
         "WHERE u.status = '1'")
 List<Student> selectAllStudents();

 /**
  * 获取学生列表（包含姓名）用于值班安排
  * @return 学生列表（包含ID和姓名）
  */
 @Select("SELECT s.student_id as id, u.name as name " +
         "FROM student s " +
         "LEFT JOIN user u ON s.user_id = u.user_id " +
         "WHERE u.status = '1' " +
         "ORDER BY s.student_id")
 List<Map<String, Object>> selectStudentsWithNames();
 
 /**
  * 统计活跃学生数量
  * @return 活跃学生数量
  */
 @Select("SELECT COUNT(*) FROM student s " +
         "LEFT JOIN user u ON s.user_id = u.user_id " +
         "WHERE u.status = '1'")
 int countActiveStudents();

 //查询学生是否存在
 @Select("SELECT COUNT(*) > 0 FROM student WHERE student_id = #{studentId}")
 Boolean selectById(Long studentId);
    
 /**
  * 根据关键词搜索学生
  * @param query 搜索关键词（姓名、学号等）
  * @return 符合条件的学生列表
  */
 @Select("SELECT s.student_id as id, u.name as name, s.student_number as studentNumber " +
         "FROM student s " +
         "LEFT JOIN user u ON s.user_id = u.user_id " +
         "WHERE u.status = '1' AND (u.name LIKE CONCAT('%', #{query}, '%') OR s.student_number LIKE CONCAT('%', #{query}, '%')) " +
         "ORDER BY s.student_id " +
         "LIMIT 20")
 List<Map<String, Object>> searchStudents(@Param("query") String query);

 /**
  * 根据课程ID查询选择了该课程的学生列表
  * @param courseId 课程ID
  * @return 学生列表
  */
 @Select("SELECT s.student_id, u.name as student_name " +
         "FROM student s " +
         "LEFT JOIN user u ON s.user_id = u.user_id " +
         "INNER JOIN student_course sc ON s.student_id = sc.student_id " +
         "WHERE sc.course_id = #{courseId} AND u.status = '1' " +
         "ORDER BY s.student_id")
 List<Map<String, Object>> selectStudentsByCourseId(@Param("courseId") Long courseId);

    /**
     * 根据用户ID列表批量删除学生信息
     * @param userIds 用户ID列表
     * @return 影响行数
     */
    int deleteStudentByUserIds(@Param("userIds") List<String> userIds);


    /**
     * // 获取用户角色信息
     * @param studentId
     * @return
     */
    List<Long> getDirectionIdsByStudentId(Long studentId);
    
    /**
     * 获取已选课程的学生列表
     * @param courseId 课程ID
     * @return 已选该课程的学生列表
     */
    List<StudentVo> getEnrolledStudents(Long courseId);
    
    /**
     * 获取未选课但培训方向匹配的学生列表
     * @param courseId 课程ID
     * @param directionId 培训方向ID
     * @return 未选课但培训方向匹配的学生列表
     */
    List<StudentVo> getEligibleStudents(@Param("courseId") Long courseId, @Param("directionId") Long directionId);
}



