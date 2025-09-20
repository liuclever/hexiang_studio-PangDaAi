package com.back_hexiang_studio.controller.admin;

import com.back_hexiang_studio.GlobalException.BusinessException;
import com.back_hexiang_studio.GlobalException.ErrorCode;
import com.back_hexiang_studio.dv.dto.ChangeStatusDto;
import com.back_hexiang_studio.dv.dto.CourseDto;
import com.back_hexiang_studio.dv.dto.PageCourseDto;
import com.back_hexiang_studio.dv.dto.RemoveStudentDto;
import com.back_hexiang_studio.dv.dto.addStudentDto;

import com.back_hexiang_studio.dv.vo.CourseVo;
import com.back_hexiang_studio.dv.vo.StudentVo;
import com.back_hexiang_studio.result.PageResult;
import com.back_hexiang_studio.result.Result;
import com.back_hexiang_studio.service.CourseService;
import com.back_hexiang_studio.utils.FileValidationManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 课程管理控制器
 * 权限：超级管理员或老师、副主任、主任可以访问
 */
@Slf4j
@RestController
@RequestMapping("/admin/course")
@PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('COURSE_MANAGE')")
public class CourseController {
    @Autowired
    private CourseService courseService;

    @Autowired
    private FileValidationManager fileValidationManager;

    @Autowired
    private ObjectMapper objectMapper;


    /**
     * 返回课程列表（分页）
     * @param pageCourseDto
     * @return
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('COURSE_VIEW')")
    public Result<PageResult> list(PageCourseDto pageCourseDto){
        try{
            log.info("课程管理：返回课程列表（分页）{}",pageCourseDto.toString());
            return Result.success(courseService.list(pageCourseDto));
        }catch (Exception e){
            log.error("课程管理：返回课程列表（分页）{}",e.getMessage());
            throw new BusinessException("返回课程列表（分页）失败");
        }
    }

    /**
     * 新增课程
     * @param courseDtoString
     * @param coverImageFile
     * @param materialFiles
     * @return
     */
    @PostMapping("/add")
    public Result addCourse(@RequestPart("courseDto") String courseDtoString,
                            @RequestPart(value = "coverImageFile", required = false) MultipartFile coverImageFile,
                            @RequestPart(value = "materialFiles", required = false) List<MultipartFile> materialFiles) {
        try {
            CourseDto courseDto = objectMapper.readValue(courseDtoString, CourseDto.class);
            courseService.addCourseWithFiles(courseDto, coverImageFile, materialFiles);
            return Result.success();
        } catch (IOException e) {
            log.error("添加课程失败", e);
            return Result.error("添加课程失败：" + e.getMessage());
        }
    }

    /**
     * 获取课程信息
     * @param id
     * @return
     */
    @GetMapping("/detail")
    public Result<CourseVo> detail(Long id){
        log.info("课程管理：获取课程详情{}",id);
        CourseVo courseVo=courseService.detail(id);
        return Result.success(courseVo);
    }

    /**
     * 获取老师列表
     * @return
     */
    @GetMapping("teacher/list")
    public Result<List<String>> list(){
        List<String> teacherList= courseService.teacherList();
        return Result.success(teacherList);
    }

    /**
     * 更新课程
     * @param courseDtoString
     * @param coverImageFile
     * @param materialFiles
     * @param keepMaterialIdsString
     * @return
     */
    @PostMapping("/update")
    public Result updateCourse(@RequestPart("courseDto") String courseDtoString,
                               @RequestPart(value = "coverImageFile", required = false) MultipartFile coverImageFile,
                               @RequestPart(value = "materialFiles", required = false) List<MultipartFile> materialFiles,
                               @RequestPart(value = "keepMaterialIds", required = false) String keepMaterialIdsString) {
        try {
            CourseDto courseDto = objectMapper.readValue(courseDtoString, CourseDto.class);
            List<Long> keepMaterialIds = new ArrayList<>();
            if (StringUtils.hasText(keepMaterialIdsString)) {
                keepMaterialIds = objectMapper.readValue(keepMaterialIdsString, new TypeReference<List<Long>>() {});
            }
            courseService.updateCourseWithFiles(courseDto, coverImageFile, materialFiles, keepMaterialIds);
            return Result.success();
        } catch (IOException e) {
            log.error("更新课程失败", e);
            return Result.error("更新课程失败：" + e.getMessage());
        }
    }
    /**
     * 获取课程学生列表
     * @param courseDto
     * @return
     */
    @GetMapping("/students")
    public Result<PageResult> getCourseStudentsList(CourseDto courseDto){
        log.info("获取课程学生列表：{}",courseDto.toString());
        List<StudentVo> studentCurrentList=courseService.studentCureentList(courseDto);
        PageResult pageResult = new PageResult(studentCurrentList.size(), studentCurrentList);
        return Result.success(pageResult);
    }

    /**
     * 获取课程已选学生列表（点击课程数量查看）
     * @param courseId 课程ID
     * @return 已选该课程的学生列表
     */
    @GetMapping("/enrolled-students")
    public Result<List<StudentVo>> getEnrolledStudents(@RequestParam Long courseId) {
        try {
            log.info("获取课程已选学生列表，courseId: {}", courseId);
            List<StudentVo> enrolledStudents = courseService.getEnrolledStudents(courseId);
            return Result.success(enrolledStudents);
        } catch (Exception e) {
            log.error("获取课程已选学生列表失败: {}", e.getMessage());
            return Result.error("获取课程已选学生列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取未选课但培训方向匹配的学生列表（点击添加学员查看）
     * @param courseId 课程ID
     * @return 未选课但培训方向匹配的学生列表
     */
    @GetMapping("/eligible-students")
    public Result<List<StudentVo>> getEligibleStudents(@RequestParam Long courseId) {
        try {
            log.info("获取未选课但培训方向匹配的学生列表，courseId: {}", courseId);
            List<StudentVo> eligibleStudents = courseService.getEligibleStudents(courseId);
            return Result.success(eligibleStudents);
        } catch (Exception e) {
            log.error("获取未选课但培训方向匹配的学生列表失败: {}", e.getMessage());
            return Result.error("获取未选课但培训方向匹配的学生列表失败: " + e.getMessage());
        }
    }

    /**
     * 添加学生到课程
     * @param addStudentDto
     * @return
     */
    @PostMapping("/addStudent")
    public Result addStudent(@RequestBody addStudentDto addStudentDto){
        try{
            log.info("添加学生信息：{}",addStudentDto.toString());
            courseService.addStudent(addStudentDto);
            return Result.success("添加成功！");
        }catch (Exception e){
            log.error("添加学生失败：{}", e.getMessage());
            return Result.error(ErrorCode.BUSINESS_ERROR.getCode(),"添加失败"+e.getMessage());
        }
    }

    /**
     * 从课程中移出学生
     * @param removeStudentDto
     * @return
     */
    @PostMapping("/removeStudent")
    public Result removeStudent(@RequestBody RemoveStudentDto removeStudentDto){
        try{
            log.info("移出学生信息：{}", removeStudentDto.toString());
            courseService.removeStudent(removeStudentDto);
            return Result.success("移出成功！");
        }catch (Exception e){
            log.error("移出学生失败：{}", e.getMessage());
            return Result.error(ErrorCode.BUSINESS_ERROR.getCode(),"移出失败"+e.getMessage());
        }
    }

    /**
     * 获取课程学生列表
     * @param pageCourseDto
     * @return
     */
    @GetMapping("/student/list")
    public Result<PageResult> studentList(PageCourseDto pageCourseDto){
        log.info("获取课程学生列表：{}",pageCourseDto.toString());
        PageResult   studentList=courseService.studentList(pageCourseDto);
        return Result.success(studentList);
    }

    /**
     * 删除课程
     * @param ids
     * @return
     */
        @PostMapping("/delete")
    public Result deleteCourse(@RequestBody List<Long> ids){
        try{
            log.info("删除课程：{}",ids);
            courseService.deleteCourse(ids);
            return Result.success("删除成功！");
        }
        catch (Exception e){
            return Result.error(ErrorCode.BUSINESS_ERROR.getCode(),"删除失败"+e.getMessage());
        }

        }

    /**
     * 修改课程状态
     * @param changeStatusDto
     * @return
     */
    @PostMapping("/updateStatus")
    public Result changeStatus(@RequestBody ChangeStatusDto changeStatusDto){
        try{
            log.info("修改课程状态：{}",changeStatusDto.toString());
            courseService.changeStatus(changeStatusDto);
            return Result.success("修改成功！");
        } catch (Exception e){
            return Result.error(ErrorCode.BUSINESS_ERROR.getCode(),"修改失败"+e.getMessage());
        }
    }

    /**
     * 搜索课程（用于下拉选择）
     * @param query 搜索关键词
     * @return
     */
    @GetMapping("/search")
    public Result<List<CourseVo>> searchCourses(@RequestParam(required = false, defaultValue = "") String query){
        try{
            log.info("课程搜索：{}", query);
            List<CourseVo> courses = courseService.searchCourses(query);
            return Result.success(courses);
        }catch (Exception e){
            log.error("课程搜索失败：{}", e.getMessage());
            throw new BusinessException("课程搜索失败");
        }
    }
}




