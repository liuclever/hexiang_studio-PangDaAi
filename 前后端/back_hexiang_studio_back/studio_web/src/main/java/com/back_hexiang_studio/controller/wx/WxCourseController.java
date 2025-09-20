package com.back_hexiang_studio.controller.wx;

import com.back_hexiang_studio.result.Result;
import com.back_hexiang_studio.context.UserContextHolder;
import com.back_hexiang_studio.service.CourseService;
import com.back_hexiang_studio.dv.vo.CourseVo;
import com.back_hexiang_studio.dv.vo.StudentVo;
import com.back_hexiang_studio.dv.dto.ChangeStatusDto;
import com.back_hexiang_studio.dv.dto.PageCourseDto;
import com.back_hexiang_studio.result.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 微信端课程控制器
 */
@Slf4j
@RestController
@RequestMapping("/wx/course")
public class WxCourseController {

    @Autowired
    private CourseService courseService;

    /**
     * 获取课程列表（微信端）- 按角色权限控制
     * 学生：只能看见自己参与的课程
     * 老师：只能看自己创建的课程
     * 管理员：可以看见全部课程
     * @return 课程列表
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('COURSE_VIEW')")
    public Result getCourseList() {
        log.info("获取微信端课程列表（按角色权限控制）");
        try {
            // 获取当前用户ID
            Long currentUserId = UserContextHolder.getCurrentId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }
            
            // 调用CourseService根据用户角色获取课程列表
            List<CourseVo> courses = courseService.getCoursesByUserRole(currentUserId);
            
            log.info("课程列表查询成功，用户ID: {}, 课程数量: {}", currentUserId, 
                    courses != null ? courses.size() : 0);
            return Result.success(courses);
        } catch (Exception e) {
            log.error("获取课程列表失败: {}", e.getMessage());
            return Result.error("获取课程列表失败");
        }
    }

    /**
     * 获取课程详情（微信端）
     * @param id 课程ID
     * @return 课程详情
     */
    @GetMapping("/detail")
    @PreAuthorize("hasAuthority('COURSE_VIEW')")
    public Result getCourseDetail(@RequestParam Long id) {
        log.info("获取微信端课程详情，课程ID: {}", id);
        try {
            if (id == null) {
                return Result.error("课程ID不能为空");
            }
            
            // 调用CourseService获取课程详情
            CourseVo course = courseService.detail(id);
            
            if (course == null) {
                return Result.error("课程不存在");
            }
            
            log.info("课程详情查询成功，课程ID: {}, 课程名称: {}", id, course.getName());
            return Result.success(course);
        } catch (Exception e) {
            log.error("获取课程详情失败: {}", e.getMessage());
            return Result.error("获取课程详情失败");
        }
    }

    /**
     * 获取课程详情（路径参数版本）
     * @param courseId 课程ID
     * @return 课程详情
     */
    @GetMapping("/detail/{courseId}")
    @PreAuthorize("hasAuthority('COURSE_VIEW')")
    public Result getCourseDetailById(@PathVariable Long courseId) {
        log.info("获取微信端课程详情（路径参数），课程ID: {}", courseId);
        try {
            if (courseId == null) {
                return Result.error("课程ID不能为空");
            }
            
            // 调用CourseService获取课程详情
            CourseVo course = courseService.detail(courseId);
            
            if (course == null) {
                return Result.error("课程不存在");
            }
            
            log.info("课程详情查询成功（路径参数），课程ID: {}, 课程名称: {}", courseId, course.getName());
            return Result.success(course);
        } catch (Exception e) {
            log.error("获取课程详情失败: {}", e.getMessage());
            return Result.error("获取课程详情失败");
        }
    }

    /**
     * 获取教师授课列表（微信端）
     * @param teacherId 教师ID
     * @return 教学课程列表
     */
    @GetMapping("/teaching")
    @PreAuthorize("hasAuthority('COURSE_VIEW')")
    public Result getTeachingCourses(@RequestParam(required = false) Long teacherId) {
        log.info("获取教师的授课列表，教师ID: {}", teacherId);
        try {
            if (teacherId == null) {
                return Result.error("教师ID不能为空");
            }
            
            // 调用CourseService根据教师ID获取授课列表
            List<CourseVo> courses = courseService.getCoursesByTeacherId(teacherId);
            
            log.info("教师授课列表查询成功，教师ID: {}, 课程数量: {}", teacherId, 
                    courses != null ? courses.size() : 0);
            return Result.success(courses);
        } catch (Exception e) {
            log.error("获取教师授课列表失败: {}", e.getMessage());
            return Result.error("获取教学课程失败");
        }
    }

    /**
     * 获取课程学生列表（微信端）
     * @param courseId 课程ID
     * @return 学生列表
     */
    @GetMapping("/students")
    @PreAuthorize("hasAuthority('COURSE_VIEW')")
    public Result getCourseStudents(@RequestParam Long courseId) {
        log.info("获取课程学生列表，课程ID: {}", courseId);
        try {
            if (courseId == null) {
                return Result.error("课程ID不能为空");
            }
            
            // 调用CourseService获取已选课程的学生列表
            List<StudentVo> students = courseService.getEnrolledStudents(courseId);
            
            log.info("课程学生列表查询成功，课程ID: {}, 学生数量: {}", courseId, 
                    students != null ? students.size() : 0);
            return Result.success(students);
        } catch (Exception e) {
            log.error("获取课程学生列表失败: {}", e.getMessage());
            return Result.error("获取课程学生列表失败");
        }
    }

    /**
     * 获取课程学生列表（路径参数版本）
     * @param courseId 课程ID
     * @return 学生列表
     */
    @GetMapping("/{courseId}/students")
    @PreAuthorize("hasAuthority('COURSE_VIEW')")
    public Result getCourseStudentsById(@PathVariable Long courseId) {
        log.info("获取课程学生列表（路径参数），课程ID: {}", courseId);
        try {
            if (courseId == null) {
                return Result.error("课程ID不能为空");
            }
            
            // 调用CourseService获取已选课程的学生列表
            List<StudentVo> students = courseService.getEnrolledStudents(courseId);
            
            log.info("课程学生列表查询成功（路径参数），课程ID: {}, 学生数量: {}", courseId, 
                    students != null ? students.size() : 0);
            return Result.success(students);
        } catch (Exception e) {
            log.error("获取课程学生列表失败: {}", e.getMessage());
            return Result.error("获取课程学生列表失败");
        }
    }

    // ========== 简化的课程管理功能API ==========

    /**
     * 获取课程列表（分页）（按角色权限控制）
     * 学生：只能看见自己参与的课程
     * 老师：只能看自己授课的课程
     * 管理员：可以看见全部课程
     * @param pageCourseDto 分页参数
     * @return 课程列表
     */
    @GetMapping("/all-courses")
    @PreAuthorize("hasAuthority('COURSE_VIEW')")
    public Result<PageResult> getAllCourses(PageCourseDto pageCourseDto) {
        log.info("微信端获取课程列表（按角色权限控制），参数: {}", pageCourseDto);
        try {
            // 获取当前用户ID
            Long currentUserId = UserContextHolder.getCurrentId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }
            
            // 调用Service根据用户角色获取课程列表
            List<CourseVo> courses = courseService.getCoursesByUserRole(currentUserId);
            
            // 转换为分页结果格式（兼容前端）
            PageResult pageResult = new PageResult();
            pageResult.setRecords(courses);
            pageResult.setTotal((long) courses.size());
            pageResult.setPage(pageCourseDto.getPage() != null ? pageCourseDto.getPage() : 1);
            pageResult.setPageSize(pageCourseDto.getPageSize() != null ? pageCourseDto.getPageSize() : courses.size());
            
            return Result.success(pageResult);
        } catch (Exception e) {
            log.error("微信端获取课程列表失败: {}", e.getMessage());
            return Result.error("获取课程列表失败");
        }
    }

    /**
     * 更新课程状态（管理员和老师可用）
     * @param changeStatusDto 状态更新信息
     * @return 更新结果
     */
    @PutMapping("/update-status")
    @PreAuthorize("hasAuthority('COURSE_APPROVE') or hasAuthority('COURSE_MANAGE')")
    public Result updateCourseStatus(@RequestBody ChangeStatusDto changeStatusDto) {
        log.info("微信端更新课程状态: {}", changeStatusDto);
        try {
            courseService.changeStatus(changeStatusDto);
            return Result.success("更新课程状态成功");
        } catch (Exception e) {
            log.error("微信端更新课程状态失败: {}", e.getMessage());
            return Result.error("更新课程状态失败: " + e.getMessage());
        }
    }
} 