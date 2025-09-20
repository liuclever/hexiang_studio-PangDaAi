package com.back_hexiang_studio.service.impl;


/**
 * 课程管理
 */

import com.back_hexiang_studio.GlobalException.BusinessException;
import com.back_hexiang_studio.annotation.AutoFill;
import com.back_hexiang_studio.context.UserContextHolder;
import com.back_hexiang_studio.dv.dto.*;
import com.back_hexiang_studio.dv.vo.CourseVo;
import com.back_hexiang_studio.dv.vo.NoticeVo;
import com.back_hexiang_studio.dv.vo.StudentVo;
import com.back_hexiang_studio.entity.Course;
import com.back_hexiang_studio.entity.CourseMaterial;
import com.back_hexiang_studio.entity.Student;
import com.back_hexiang_studio.entity.TrainingDirection;
import com.back_hexiang_studio.enumeration.OperationType;
import com.back_hexiang_studio.enumeration.FileType;
import com.back_hexiang_studio.mapper.*;
import com.back_hexiang_studio.result.PageResult;
import com.back_hexiang_studio.service.CourseService;
import com.back_hexiang_studio.utils.DateTimeUtils;
import com.back_hexiang_studio.utils.FileUtils;
import com.back_hexiang_studio.utils.NotificationUtils;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.experimental.Helper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;


import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.back_hexiang_studio.entity.User;

@Slf4j
@Service
public class CourseServiceImpl implements CourseService {

    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private TeacherMapper teacherMapper;
    @Autowired
    private TrainingMapper trainingMapper;
    @Autowired
    private StudentMapper studentMapper;
    @Autowired
    private CourseMaterialMapper courseMaterialMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;


    /**
     * 分页查询公告
     *
     * @param pageCourseDto
     * @return
     */
    @Override
    public PageResult list(PageCourseDto pageCourseDto) {
        // 获取当前用户ID并检查权限
        Long currentUserId = UserContextHolder.getCurrentId();
        boolean isAdmin = false;
        
        if (currentUserId != null) {
            try {
                // 获取用户信息检查是否为管理员/教师
                com.back_hexiang_studio.entity.User user = userMapper.getUserById(currentUserId);
                if (user != null && user.getPositionId() != null) {
                    // 职位ID: 5=老师, 6=主任, 7=副主任, 8=超级管理员
                    isAdmin = (user.getPositionId() >= 5);
                }
            } catch (Exception e) {
                log.warn("检查用户权限失败: {}", e.getMessage());
            }
        }
        
        // 非管理员只能看已发布的课程
        if (!isAdmin && pageCourseDto.getStatus() == null) {
            pageCourseDto.setStatus("1"); // 只显示已发布的课程
            log.info("非管理员用户，过滤只显示已发布课程");
        }
        
        // 构建缓存key（包含权限信息）
        StringBuilder cacheKeyBuilder = new StringBuilder("course:list:");
        cacheKeyBuilder.append(pageCourseDto.getPage()).append(":")
                .append(pageCourseDto.getPageSize()).append(":")
                .append(isAdmin ? "admin" : "user"); // 区分管理员和普通用户的缓存

        if (pageCourseDto.getName() != null) {
            cacheKeyBuilder.append(":name:").append(pageCourseDto.getName());
        }
        if (pageCourseDto.getTeacher() != null) {
            cacheKeyBuilder.append(":teacher:").append(pageCourseDto.getTeacher());
        }
        if (pageCourseDto.getStatus() != null) {
            cacheKeyBuilder.append(":status:").append(pageCourseDto.getStatus());
        }

        String cacheKey = cacheKeyBuilder.toString();

        // 从缓存获取
        Object cacheResult = redisTemplate.opsForValue().get(cacheKey);
        if (cacheResult != null) {
            log.info("从缓存获取课程列表");
            return (PageResult) cacheResult;
        }

        log.info("缓存未命中，从数据库查询课程列表");

        PageHelper.startPage(pageCourseDto.getPage(), pageCourseDto.getPageSize());
        //查找课程所需信息
        Page<CourseVo> corseList = courseMapper.list(pageCourseDto);
        PageInfo<CourseVo> pages = new PageInfo<>(corseList);
        List<CourseVo> list = pages.getList();
        PageResult result = new PageResult(pages.getTotal(), list, pages.getPageNum(), pages.getPageSize(), pages.getPages());
        // 将结果存入缓存，设置5分钟过期
        redisTemplate.opsForValue().set(cacheKey, result, 5, TimeUnit.MINUTES);
        return result;
    }


    /**
     * 获取课程详情
     *
     * @param id
     * @return
     */
    @Override
    public CourseVo detail(Long id) {
        // 定义缓存key
        String cacheKey = "course:detail:" + id;

        // 从缓存获取
        Object cacheResult = redisTemplate.opsForValue().get(cacheKey);
        if (cacheResult != null) {
            log.info("从缓存获取课程详情, id: {}", id);
            return (CourseVo) cacheResult;
        }

        log.info("缓存未命中，从数据库查询课程详情, id: {}", id);

        CourseVo course = courseMapper.detail(id);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }

        // 获取课程资料列表
        List<CourseMaterial> materials = courseMaterialMapper.getByCourseId(id);
        course.setMaterials(materials);


        // 将结果存入缓存，设置10分钟过期
        redisTemplate.opsForValue().set(cacheKey, course, 10, TimeUnit.MINUTES);

        return course;
    }

    /**
     * 获取老师列表
     * @return
     */
    @Override
    public List<String> teacherList() {
        // 定义缓存key
        String cacheKey = "teacher:list:all";

        // 从缓存获取
        Object cacheResult = redisTemplate.opsForValue().get(cacheKey);
        if (cacheResult != null) {
            log.info("从缓存获取老师列表");
            return (List<String>) cacheResult;
        }

        log.info("缓存未命中，从数据库查询老师列表");

        List<String> teachers=teacherMapper.getTeacherList();

        // 将结果存入缓存，设置30分钟过期
        redisTemplate.opsForValue().set(cacheKey, teachers, 30, TimeUnit.MINUTES);

        return  teachers;
    }

    /**
     * 返回学生列表
     * @param courseDto
     * @return
     *
     */
    @Override
    public List<StudentVo> studentCureentList(CourseDto courseDto) {
        // 定义缓存key
        String cacheKey = "course:students:" + courseDto.getCourseId();

        // 从缓存获取
        Object cacheResult = redisTemplate.opsForValue().get(cacheKey);
        if (cacheResult != null) {
            log.info("从缓存获取课程学生列表, courseId: {}", courseDto.getCourseId());
            return (List<StudentVo>) cacheResult;
        }

        log.info("缓存未命中，从数据库查询课程学生列表, courseId: {}", courseDto.getCourseId());

        List<StudentVo> students=studentMapper.getStudentCurrentList(courseDto);
        if(students==null){
            throw new BusinessException("学生列表为空");
        }


        // 将结果存入缓存，设置5分钟过期
        redisTemplate.opsForValue().set(cacheKey, students, 5, TimeUnit.MINUTES);

        return students;
    }

    /**
     * 添加课程学生
     * @param addStudentDto
     */
    @AutoFill(value=OperationType.INSERT)
    @Override
    public void addStudent(addStudentDto addStudentDto) {

        // 1. 获取课程信息，特别是其要求的培训方向ID
        Course course = courseMapper.getById(addStudentDto.getCourseId());
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        Long requiredDirectionId = course.getCategoryId();
        if (requiredDirectionId == null) {
            // 如果课程没有指定培训方向，则默认所有学生都可以加入
            log.warn("课程 {} 没有指定培训方向，允许任何学生加入。", course.getName());
        } else {
            // 2. 获取学生拥有的所有培训方向ID
            List<Long> studentDirectionIds = studentMapper.getDirectionIdsByStudentId(addStudentDto.getStudentId());

            // 3. 进行逻辑判断
            if (studentDirectionIds == null || !studentDirectionIds.contains(requiredDirectionId)) {
                throw new BusinessException("添加失败：该学生未选择此课程所需的培训方向");
            }
        }

        // 4. 验证通过或课程无方向要求，执行添加操作
        addStudentDto.setJoinTime(LocalDateTime.now());
        try{
            studentMapper.addStudent(addStudentDto);

            /*
            // 5. 创建选课通知 - 暂时注释，需要根据实际情况修改
            try {
                // 获取当前用户ID作为发送者
                Long senderId = UserContextHolder.getCurrentId();
                
                // 查询学生关联的用户ID
                Student student = studentMapper.selectById(addStudentDto.getStudentId());
                
                if (student != null && student.getUserId() != null) {
                    // 查询课程教师信息
                    String teacherName = "未指定";
                    if (course.getTeacherId() != null) {
                        // 直接使用课程DTO中的教师名称
                        teacherName = courseMapper.getTeacherNameByCourseId(course.getCourseId());
                        if (teacherName == null || teacherName.isEmpty()) {
                            teacherName = "未指定";
                        }
                    }
                    
                    // 创建选课通知
                    String title = "您已被添加到课程: " + course.getName();
                    String content = "您已被添加到课程: " + course.getName() + 
                        "\n授课教师: " + teacherName + 
                        "\n上课地点: " + course.getLocation() + 
                        "\n上课时间: " + course.getSchedule();
                    
                    NotificationUtils.createCourseNotification(
                        title,
                        content,
                        addStudentDto.getCourseId(),
                        senderId,
                        student.getUserId(),
                        1     // 重要程度: 1表示重要
                    );
                    
                    log.info("成功为学生ID: {} 创建选课通知", addStudentDto.getStudentId());
                }
            } catch (Exception e) {
                log.error("为选课创建通知失败: {}", e.getMessage(), e);
                // 通知创建失败不影响选课
            }
            */

            // 清除相关缓存
            String enrolledStudentsCacheKey = "course:enrolled_students:" + addStudentDto.getCourseId();
            String eligibleStudentsCacheKey = "course:eligible_students:" + addStudentDto.getCourseId();
            redisTemplate.delete(enrolledStudentsCacheKey);
            redisTemplate.delete(eligibleStudentsCacheKey);

        }catch (Exception e){
            // 捕获可能由数据库唯一约束（学生已在课程中）等原因导致的异常
            log.error("添加学生到课程时发生数据库异常: {}", e.getMessage());
            throw new BusinessException("添加学生失败，可能该学生已在该课程中");
        }
    }

    /**
     * 从课程中移出学生
     * @param removeStudentDto
     */
    @Override
    public void removeStudent(RemoveStudentDto removeStudentDto) {
        try {
            // 检查课程和学生是否存在
            if (removeStudentDto.getCourseId() == null || removeStudentDto.getStudentId() == null) {
                throw new BusinessException("课程ID或学生ID不能为空");
            }

            // 调用mapper删除学生与课程的关联
            int result = studentMapper.removeStudentFromCourse(removeStudentDto);

            if (result <= 0) {
                throw new BusinessException("移出学生失败，可能该学生未关联此课程");
            }

            // 清除相关缓存
            String enrolledStudentsCacheKey = "course:enrolled_students:" + removeStudentDto.getCourseId();
            String eligibleStudentsCacheKey = "course:eligible_students:" + removeStudentDto.getCourseId();
            redisTemplate.delete(enrolledStudentsCacheKey);
            redisTemplate.delete(eligibleStudentsCacheKey);

            log.info("成功从课程{}中移出学生{}", removeStudentDto.getCourseId(), removeStudentDto.getStudentId());
        } catch (Exception e) {
            log.error("移出学生失败: {}", e.getMessage());
            throw new BusinessException("移出学生失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有学生信息
     * @param pageCourseDto
     * @return
     */
    @Override
    public PageResult studentList(PageCourseDto pageCourseDto) {
        // 构建缓存key
        StringBuilder cacheKeyBuilder = new StringBuilder("course:studentList:");
        cacheKeyBuilder.append(pageCourseDto.getPage()).append(":")
                .append(pageCourseDto.getPageSize());

        if (pageCourseDto.getName() != null) {
            cacheKeyBuilder.append(":name:").append(pageCourseDto.getName());
        }
        if (pageCourseDto.getStudentNumber() != null) {
            cacheKeyBuilder.append(":studentNumber:").append(pageCourseDto.getStudentNumber());
        }

        String cacheKey = cacheKeyBuilder.toString();

        // 从缓存获取
        Object cacheResult = redisTemplate.opsForValue().get(cacheKey);
        if (cacheResult != null) {
            log.info("从缓存获取学生分页列表");
            return (PageResult) cacheResult;
        }

        log.info("缓存未命中，从数据库查询学生分页列表");

        PageHelper.startPage(pageCourseDto.getPage(), pageCourseDto.getPageSize());
        Page<StudentVo> page=studentMapper.getStudentList(pageCourseDto);
        if(page==null){
            throw new BusinessException("获取学生信息失败");
        }
        Long pages = page.getTotal();
        List<StudentVo> list = page.getResult();



        PageResult result = new PageResult(pages, list);

        // 将结果存入缓存，设置5分钟过期
        redisTemplate.opsForValue().set(cacheKey, result, 5, TimeUnit.MINUTES);

        return result;
    }

    /**
     * 删除课程
     * @param ids
     */
    @Override
    public void deleteCourse( List<Long> ids) {
        try{
            //删除学生关联表
            for (Long id : ids) {
                studentMapper.deleteCourseWithTeacher(id);
                //删除课程
                courseMapper.deleteCourse(id);

                // 清除课程详情缓存
                String detailCacheKey = "course:detail:" + id;
                redisTemplate.delete(detailCacheKey);

                // 清除课程学生列表缓存
                String studentListCacheKey = "course:students:" + id;
                redisTemplate.delete(studentListCacheKey);
            }

            // 清除课程列表缓存
            Set<String> listKeys = redisTemplate.keys("course:list:*");
            if (listKeys != null && !listKeys.isEmpty()) {
                redisTemplate.delete(listKeys);
            }

            // 清除学生分页列表缓存
            Set<String> pageKeys = redisTemplate.keys("course:studentList:*");
            if (pageKeys != null && !pageKeys.isEmpty()) {
                redisTemplate.delete(pageKeys);
            }

        } catch (Exception e){
            log.info(e.getMessage());
            throw new BusinessException("删除学生失败");
        }
    }

    @Override
    public void changeStatus(ChangeStatusDto changeStatusDto) {
        courseMapper.changeStatus(changeStatusDto);

        // 清除课程详情缓存
        String detailCacheKey = "course:detail:" + changeStatusDto.getId();
        redisTemplate.delete(detailCacheKey);

        // 清除课程列表缓存
        clearCourseCache();
    }

    /**
     * 搜索课程（用于下拉选择）
     *
     * @param query 搜索关键词
     * @return 课程列表
     */
    @Override
    public List<CourseVo> searchCourses(String query) {
        // 定义缓存key
        String cacheKey = "course:search:" + (query != null ? query : "all");

        // 从缓存获取
        Object cacheResult = redisTemplate.opsForValue().get(cacheKey);
        if (cacheResult != null) {
            log.info("从缓存获取课程搜索结果, query: {}", query);
            return (List<CourseVo>) cacheResult;
        }

        log.info("缓存未命中，从数据库搜索课程, query: {}", query);

        List<CourseVo> courses = courseMapper.searchCourses(query);

        // 日期字段已经是String类型，不需要格式化

        // 将结果存入缓存，设置5分钟过期
        redisTemplate.opsForValue().set(cacheKey, courses, 5, TimeUnit.MINUTES);

        return courses;
    }

    /**
     * 添加课程（包含封面图片和课程资料）
     * @param courseDto 课程基本信息
     * @param coverImageFile 封面图片文件
     * @param materialFiles 课程资料文件列表
     * @throws IOException 文件处理异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addCourseWithFiles(CourseDto courseDto, MultipartFile coverImageFile, List<MultipartFile> materialFiles) throws IOException {
        try {
            // 1. 处理封面图片
            String coverImageUrl = null;
            if (coverImageFile != null && !coverImageFile.isEmpty()) {
                coverImageUrl = FileUtils.saveFile(coverImageFile, FileType.COURSE_COVER);
            }

            // 2. 处理授课老师
            Long teacherId = teacherMapper.getTeacherIdByName(courseDto.getTeacherName());
            if (teacherId == null) {
                throw new BusinessException("授课老师不存在");
            }

            // 3. 创建课程对象并保存
            Course course = new Course();
            course.setName(courseDto.getName());
            course.setDescription(courseDto.getDescription());
            course.setTeacherId(teacherId);
            course.setStatus(courseDto.getStatus());
            course.setDuration(courseDto.getDuration());
            course.setCoverImage(coverImageUrl);
            course.setCategoryId(courseDto.getCategoryId());
            course.setLocation(courseDto.getLocation());
            course.setSchedule(courseDto.getSchedule());
            course.setCreateTime(LocalDateTime.now());
            course.setUpdateTime(LocalDateTime.now());
            course.setCreateUser(UserContextHolder.getCurrentId());
            course.setUpdateUser(UserContextHolder.getCurrentId());


            // 4. 保存课程基本信息
            courseMapper.add(course);
            Long courseId = course.getCourseId();

            // 5. 处理课程资料文件
            if (materialFiles != null && !materialFiles.isEmpty()) {
                List<CourseMaterial> materials = new ArrayList<>();

                for (MultipartFile file : materialFiles) {
                    if (file != null && !file.isEmpty()) {
                        String filePath = FileUtils.saveFile(file, FileType.COURSE_MATERIAL);

                        CourseMaterial material = CourseMaterial.builder()
                                .courseId(course.getCourseId())
                                .fileName(file.getOriginalFilename())
                                .filePath(filePath)
                                .fileSize(file.getSize())
                                .fileType(file.getContentType())
                                .uploadTime(LocalDateTime.now())
                                .uploaderId(UserContextHolder.getCurrentId())
                                .downloadCount(0)
                                .build();

                        materials.add(material);
                    }
                }

                if (!materials.isEmpty()) {
                    courseMaterialMapper.insertBatch(materials);
                }
            }

            // 6. 如果课程状态为已发布，创建课程通知
            if (courseDto.getStatus() == 1) {
                try {
                    // 获取当前用户ID作为发送者
                    Long senderId = UserContextHolder.getCurrentId();
                    
                    // 创建课程发布通知
                    String title = "新课程发布: " + courseDto.getName();
                    String content = "新课程已发布: " + courseDto.getName() + 
                        "\n授课教师: " + courseDto.getTeacherName() + 
                        "\n上课地点: " + courseDto.getLocation() + 
                        "\n上课时间: " + courseDto.getSchedule();
                    
                    // 创建全局通知
                    NotificationUtils.createCourseNotification(
                        title,
                        content,
                        courseId,
                        senderId,
                        null, // null表示全局通知
                        1     // 重要程度: 1表示重要
                    );
                    
                    log.info("成功为新课程ID: {} 创建通知", courseId);
                } catch (Exception e) {
                    log.error("为新课程创建通知失败: {}", e.getMessage(), e);
                    // 通知创建失败不影响课程创建
                }
            }

            // 7. 清理缓存
            clearCourseCache();

        } catch (Exception e) {
            log.error("添加课程失败", e);
            throw new BusinessException("添加课程失败: " + e.getMessage());
        }
    }

    /**
     * 更新课程（包含封面图片和课程资料）
     * @param courseDto 课程基本信息
     * @param coverImageFile 封面图片文件
     * @param materialFiles 新增课程资料文件列表
     * @param keepMaterialIds 保留的课程资料ID列表
     * @throws IOException 文件处理异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCourseWithFiles(CourseDto courseDto, MultipartFile coverImageFile, List<MultipartFile> materialFiles, List<Long> keepMaterialIds) throws IOException {
        try {
            Long courseId = courseDto.getCourseId();
            if (courseId == null) {
                throw new BusinessException("课程ID不能为空");
            }

            // 1. 获取原课程信息
            Course existingCourse = courseMapper.getById(courseId);
            if (existingCourse == null) {
                throw new BusinessException("要更新的课程不存在");
            }

            // 2. 处理封面图片
            String coverImageUrl = existingCourse.getCoverImage();
            if (coverImageFile != null && !coverImageFile.isEmpty()) {
                // 删除旧封面图片
                if (coverImageUrl != null && !coverImageUrl.isEmpty()) {
                    FileUtils.deleteFile(coverImageUrl);
                }

                // 上传新封面图片
                coverImageUrl = FileUtils.saveFile(coverImageFile, FileType.COURSE_COVER);
            }

            // 3. 处理授课老师
            Long teacherId = teacherMapper.getTeacherIdByName(courseDto.getTeacherName());
            if (teacherId == null) {
                throw new BusinessException("授课老师不存在");
            }

            // 4. 更新课程基本信息
            Course course = new Course();
            course.setCourseId(courseId);
            course.setName(courseDto.getName());
            course.setDescription(courseDto.getDescription());
            course.setTeacherId(teacherId);
            course.setStatus(courseDto.getStatus());
            course.setDuration(courseDto.getDuration());
            course.setCoverImage(coverImageUrl);
            course.setCategoryId(courseDto.getCategoryId());
            course.setLocation(courseDto.getLocation());
            course.setSchedule(courseDto.getSchedule());
            course.setCreateTime(existingCourse.getCreateTime());
            course.setUpdateTime(LocalDateTime.now());
            course.setCreateUser(existingCourse.getCreateUser());
            course.setUpdateUser(UserContextHolder.getCurrentId());

            courseMapper.update(course);

            // 5. 处理课程资料
            boolean hasMaterialChanges = false;
            
            // 5.1 清理不再需要的旧资料
            List<CourseMaterial> oldMaterials = courseMaterialMapper.getByCourseId(courseId);
            if (oldMaterials != null && !oldMaterials.isEmpty()) {
                List<Long> materialsToDeleteIds = oldMaterials.stream()
                        .map(CourseMaterial::getMaterialId)
                        .filter(id -> keepMaterialIds == null || !keepMaterialIds.contains(id))
                        .collect(Collectors.toList());

                if (!materialsToDeleteIds.isEmpty()) {
                    List<String> materialFilesToDelete = oldMaterials.stream()
                            .filter(m -> materialsToDeleteIds.contains(m.getMaterialId()))
                            .map(CourseMaterial::getFilePath)
                            .collect(Collectors.toList());

                    courseMaterialMapper.deleteByIds(materialsToDeleteIds);
                    materialFilesToDelete.forEach(FileUtils::deleteFile);
                    hasMaterialChanges = true;
                }
            }

            // 5.2 添加新的课程资料
            if (materialFiles != null && !materialFiles.isEmpty()) {
                List<CourseMaterial> newMaterials = new ArrayList<>();

                for (MultipartFile file : materialFiles) {
                    if (file != null && !file.isEmpty()) {
                        String filePath = FileUtils.saveFile(file, FileType.COURSE_MATERIAL);

                        CourseMaterial material = CourseMaterial.builder()
                                .courseId(courseId)
                                .fileName(file.getOriginalFilename())
                                .filePath(filePath)
                                .fileSize(file.getSize())
                                .fileType(file.getContentType())
                                .uploadTime(LocalDateTime.now())
                                .uploaderId(UserContextHolder.getCurrentId())
                                .downloadCount(0)
                                .build();

                        newMaterials.add(material);
                    }
                }

                if (!newMaterials.isEmpty()) {
                    courseMaterialMapper.insertBatch(newMaterials);
                    hasMaterialChanges = true;
                }
            }

            // 6. 如果课程状态为已发布且有资料变更，创建课程资料更新通知
            if (courseDto.getStatus() == 1 && hasMaterialChanges) {
                try {
                    // 获取当前用户ID作为发送者
                    Long senderId = UserContextHolder.getCurrentId();
                    
                    // 获取已选该课程的学生列表
                    List<StudentVo> enrolledStudents = getEnrolledStudents(courseId);
                    
                    if (enrolledStudents != null && !enrolledStudents.isEmpty()) {
                        // 创建课程资料更新通知
                        String title = "课程资料更新: " + courseDto.getName();
                        String content = "课程资料已更新: " + courseDto.getName() + 
                            "\n授课教师: " + courseDto.getTeacherName() + 
                            "\n请查看最新的课程资料。";
                        
                        // 为每个已选课学生创建通知
                        for (StudentVo student : enrolledStudents) {
                            NotificationUtils.createCourseNotification(
                                title,
                                content,
                                courseId,
                                senderId,
                                student.getUserId(),
                                0     // 重要程度: 0表示普通
                            );
                        }
                        
                        log.info("成功为课程ID: {} 的{}名学生创建资料更新通知", courseId, enrolledStudents.size());
                    }
                } catch (Exception e) {
                    log.error("为课程资料更新创建通知失败: {}", e.getMessage(), e);
                    // 通知创建失败不影响课程更新
                }
            }
            
            // 7. 如果课程状态从未发布变为已发布，创建课程发布通知
            if (existingCourse.getStatus() != 1 && courseDto.getStatus() == 1) {
                try {
                    // 获取当前用户ID作为发送者
                    Long senderId = UserContextHolder.getCurrentId();
                    
                    // 创建课程发布通知
                    String title = "课程已发布: " + courseDto.getName();
                    String content = "课程已正式发布: " + courseDto.getName() + 
                        "\n授课教师: " + courseDto.getTeacherName() + 
                        "\n上课地点: " + courseDto.getLocation() + 
                        "\n上课时间: " + courseDto.getSchedule();
                    
                    // 创建全局通知
                    NotificationUtils.createCourseNotification(
                        title,
                        content,
                        courseId,
                        senderId,
                        null, // null表示全局通知
                        1     // 重要程度: 1表示重要
                    );
                    
                    log.info("成功为发布课程ID: {} 创建通知", courseId);
                } catch (Exception e) {
                    log.error("为发布课程创建通知失败: {}", e.getMessage(), e);
                    // 通知创建失败不影响课程更新
                }
            }

            // 8. 清理缓存
            clearCourseCache();
            clearCourseCacheById(courseId);

        } catch (Exception e) {
            log.error("更新课程失败", e);
            throw new BusinessException("更新课程失败: " + e.getMessage());
        }
    }

    private void clearCourseCache() {
        Set<String> listKeys = redisTemplate.keys("course:*");
        if (listKeys != null && !listKeys.isEmpty()) {
            redisTemplate.delete(listKeys);
        }
        Set<String> teacherKeys = redisTemplate.keys("teacher:list:all");
        if (teacherKeys != null && !teacherKeys.isEmpty()) {
            redisTemplate.delete(teacherKeys);
        }
    }

    private void clearCourseCacheById(Long courseId) {
        // 精确清理详情缓存
        redisTemplate.delete("course:detail:" + courseId);
        redisTemplate.delete("course:students:" + courseId);

        // 模糊清理列表缓存
        Set<String> listKeys = redisTemplate.keys("course:list:*");
        if (listKeys != null && !listKeys.isEmpty()) {
            redisTemplate.delete(listKeys);
        }
    }
    
    /**
     * 获取已选课程的学生列表
     * @param courseId 课程ID
     * @return 已选该课程的学生列表
     */
    @Override
    public List<StudentVo> getEnrolledStudents(Long courseId) {
        // 定义缓存key
        String cacheKey = "course:enrolled_students:" + courseId;

        // 从缓存获取
        Object cacheResult = redisTemplate.opsForValue().get(cacheKey);
        if (cacheResult != null) {
            log.info("从缓存获取已选课学生列表, courseId: {}", courseId);
            return (List<StudentVo>) cacheResult;
        }

        log.info("缓存未命中，从数据库查询已选课学生列表, courseId: {}", courseId);

        // 获取课程信息，确认课程存在
        Course course = courseMapper.getById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }

        // 查询已选课的学生列表
        List<StudentVo> enrolledStudents = studentMapper.getEnrolledStudents(courseId);
        
        // 将结果存入缓存，设置5分钟过期
        redisTemplate.opsForValue().set(cacheKey, enrolledStudents, 5, TimeUnit.MINUTES);
        
        return enrolledStudents;
    }
    
    /**
     * 获取未选课但培训方向匹配的学生列表
     * @param courseId 课程ID
     * @return 未选课但培训方向匹配的学生列表
     */
    @Override
    public List<StudentVo> getEligibleStudents(Long courseId) {
        // 定义缓存key
        String cacheKey = "course:eligible_students:" + courseId;

        // 从缓存获取
        Object cacheResult = redisTemplate.opsForValue().get(cacheKey);
        if (cacheResult != null) {
            log.info("从缓存获取未选课但培训方向匹配的学生列表, courseId: {}", courseId);
            return (List<StudentVo>) cacheResult;
        }

        log.info("缓存未命中，从数据库查询未选课但培训方向匹配的学生列表, courseId: {}", courseId);

        // 获取课程信息，确认课程存在
        Course course = courseMapper.getById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        
        // 获取课程的培训方向ID
        Long directionId = course.getCategoryId();
        if (directionId == null) {
            throw new BusinessException("课程未设置培训方向");
        }
        
        // 查询未选课但培训方向匹配的学生列表
        List<StudentVo> eligibleStudents = studentMapper.getEligibleStudents(courseId, directionId);
        
        // 将结果存入缓存，设置5分钟过期
        redisTemplate.opsForValue().set(cacheKey, eligibleStudents, 5, TimeUnit.MINUTES);
        
        return eligibleStudents;
    }

    /**
     * 根据教师ID获取课程列表
     * @param teacherId 教师ID
     * @return 课程列表
     */
    @Override
    public List<CourseVo> getCoursesByTeacherId(Long teacherId) {
        // 定义缓存key
        String cacheKey = "course:teacher:" + teacherId;

        // 从缓存获取
        Object cacheResult = redisTemplate.opsForValue().get(cacheKey);
        if (cacheResult != null) {
            log.info("从缓存获取教师课程列表, teacherId: {}", teacherId);
            return (List<CourseVo>) cacheResult;
        }

        log.info("缓存未命中，从数据库查询教师课程列表, teacherId: {}", teacherId);

        List<CourseVo> courses = courseMapper.getCoursesByTeacherId(teacherId);
        
        // 将结果存入缓存，设置10分钟过期
        redisTemplate.opsForValue().set(cacheKey, courses, 10, TimeUnit.MINUTES);

        return courses;
    }

    /**
     * 根据学生ID获取课程列表
     * @param studentId 学生ID
     * @return 课程列表
     */
    @Override
    public List<CourseVo> getCoursesByStudentId(Long studentId) {
        try {
            log.info("根据学生ID获取课程列表: {}", studentId);
            
            List<CourseVo> courses = courseMapper.getCoursesByStudentId(studentId);
            log.info("学生ID {} 获取到 {} 门课程", studentId, courses != null ? courses.size() : 0);
            
            return courses != null ? courses : new ArrayList<>();
        } catch (Exception e) {
            log.error("根据学生ID获取课程列表失败: {}", e.getMessage());
            throw new BusinessException("获取学生课程列表失败");
        }
    }
    
    @Override
    public List<CourseVo> getCoursesByUserRole(Long currentUserId) {
        try {
            log.info("根据用户角色获取课程列表，用户ID: {}", currentUserId);
            
            // 获取用户信息，判断角色
            User currentUser = userMapper.getUserById(currentUserId);
            if (currentUser == null) {
                throw new BusinessException("用户不存在");
            }
            
            Long positionId = currentUser.getPositionId();
            if (positionId == null) {
                throw new BusinessException("用户职位信息不完整");
            }
            
            List<CourseVo> courses = new ArrayList<>();
            
            // 根据职位ID判断角色权限
            if (positionId == 8L) {
                // 超级管理员：查看所有课程
                log.info("超级管理员权限，获取所有课程");
                PageCourseDto allCoursesDto = new PageCourseDto();
                allCoursesDto.setPage(1);
                allCoursesDto.setPageSize(1000); // 设置足够大的分页
                PageResult allCourses = this.list(allCoursesDto);
                courses = (List<CourseVo>) allCourses.getRecords();
                
            } else if (positionId == 6L || positionId == 7L) {
                // 主任、副主任：也可以查看所有课程（管理员权限）
                log.info("管理员权限，获取所有课程");
                PageCourseDto allCoursesDto = new PageCourseDto();
                allCoursesDto.setPage(1);
                allCoursesDto.setPageSize(1000);
                PageResult allCourses = this.list(allCoursesDto);
                courses = (List<CourseVo>) allCourses.getRecords();
                
            } else if (positionId == 5L) {
                // 老师：只能看自己授课的课程（使用teacher_id字段）
                log.info("老师权限，获取自己授课的课程");
                // 🔧 修复：先通过user_id获取teacher_id
                Long teacherId = teacherMapper.getTeacherIdByUserId(currentUserId);
                if (teacherId != null) {
                    courses = courseMapper.getCoursesByTeacherId(teacherId);
                    log.info("用户ID {} 对应教师ID {}，查询到 {} 门课程", currentUserId, teacherId, courses.size());
                } else {
                    log.warn("用户ID {} 未找到对应的教师记录", currentUserId);
                }
                
            } else {
                // 学生（positionId: 1, 3, 4）：只能看自己参与的课程
                log.info("学生权限，获取自己参与的课程");
                // 先通过user_id获取student_id
                Long studentId = studentMapper.getStudentIdByUserId(currentUserId);
                if (studentId != null) {
                    courses = courseMapper.getCoursesByStudentId(studentId);
                } else {
                    log.warn("用户ID {} 未找到对应的学生记录", currentUserId);
                }
            }
            
            log.info("用户ID {} (职位ID: {}) 获取到 {} 门课程", currentUserId, positionId, 
                    courses != null ? courses.size() : 0);
            
            return courses != null ? courses : new ArrayList<>();
            
        } catch (Exception e) {
            log.error("根据用户角色获取课程列表失败: {}", e.getMessage());
            throw new BusinessException("获取课程列表失败: " + e.getMessage());
        }
    }
    
    @Override
    public List<TrainingDirection> findDirectionByName(String directionName) {
        // 临时实现，返回空列表
        return new ArrayList<>();
    }
}


