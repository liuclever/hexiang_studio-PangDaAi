import request from '../request';

// 定义接口类型
export interface CourseQueryParams {
  page: number;
  pageSize: number;
  name?: string;
  teacher?: string;
  status?: string;
  directionId?: number | 'all';
}

export interface CourseData {
  courseId?: number;
  name: string;
  description: string;
  teacherId?: number;
  status: number; // 0-草稿，1-已发布，2-已下架
  duration?: string;
  coverImage?: string;
  materialUrl?: string;
  categoryId?: number;
  location?: string;
  schedule?: string;
}

export interface StudentCourseData {
  studentId: number;
  courseId: number;
  createUser?: string;
}

export interface TeacherData {
  id: number;
  name: string;
  title?: string; // 职称
  department?: string; // 部门
}

/**
 * 获取教师列表
 * @returns 
 */
export function getTeacherList() {
  return request({
    url: '/admin/course/teacher/list',
    method: 'get'
  });
}

/**
 * 获取课程列表
 * @param params 查询参数
 * @returns 
 */
export function getCourseList(params: CourseQueryParams) {
  return request({
    url: '/admin/course/list',
    method: 'get',
    params
  });
}

/**
 * 获取课程详情
 * @param id 课程ID
 * @returns 
 */
export function getCourseDetail(id: number) {
  return request({
    url: '/admin/course/detail',
    method: 'get',
    params: { id }
  });
}

/**
 * 添加课程
 * @param formData 课程数据（包含文件）
 * @returns 
 */
export function addCourse(formData: FormData) {
  return request({
    url: '/admin/course/add',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
}

/**
 * 更新课程
 * @param formData 课程数据（包含文件）
 * @returns 
 */
export function updateCourse(formData: FormData) {
  return request({
    url: '/admin/course/update',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
}

/**
 * 删除课程
 * @param ids 课程ID数组
 * @returns 
 */
export function deleteCourse(ids: number[]) {
  return request({
    url: '/admin/course/delete',
    method: 'post',
    data: ids
  });
}

/**
 * 更新课程状态
 * @param id 课程ID
 * @param status 状态值 (0-草稿，1-已发布，2-已下架)
 * @returns 
 */
export function updateCourseStatus(id: number, status: number) {
  return request({
    url: '/admin/course/updateStatus',
    method: 'post',
    data: { id, status }
  });
}

/**
 * 获取课程学生列表
 * @param courseId 课程ID
 * @returns 
 */
export function getCourseStudents(courseId: number) {
  return request({
    url: '/admin/course/students',
    method: 'get',
    params: { courseId }
  });
}

/**
 * 获取已选课程的学生列表
 * @param courseId 课程ID
 * @returns 
 */
export function getEnrolledStudents(courseId: number) {
  return request({
    url: '/admin/course/enrolled-students',
    method: 'get',
    params: { courseId }
  });
}

/**
 * 获取未选课但培训方向匹配的学生列表
 * @param courseId 课程ID
 * @returns 
 */
export function getEligibleStudents(courseId: number) {
  return request({
    url: '/admin/course/eligible-students',
    method: 'get',
    params: { courseId }
  });
}

/**
 * 添加学生到课程
 * @param data 学生课程关联数据
 * @returns 
 */
export function addStudentToCourse(data: StudentCourseData) {
  return request({
    url: '/admin/course/addStudent',
    method: 'post',
    data
  });
}

/**
 * 从课程中移除学生
 * @param studentId 学生ID
 * @param courseId 课程ID
 * @returns 
 */
export function removeStudentFromCourse(studentId: number, courseId: number) {
  return request({
    url: '/admin/course/removeStudent',
    method: 'post',
    data: { studentId, courseId }
  });
}

/**
 * 搜索学生
 * @param keyword 搜索关键词
 * @returns 
 */
export function searchStudents(keyword: string) {
  return request({
    url: '/admin/student/search',
    method: 'get',
    params: { keyword }
  });
}

/**
 * 获取学生列表（分页）
 * @param params 分页参数
 * @returns 
 */
export function getStudentList(params: { page: number; pageSize: number; keyword?: string; }) {
  return request({
    url: '/admin/course/student/list',
    method: 'get',
    params
  });
}

/**
 * 获取培训方向列表
 * @returns 
 */
export function getTrainingDirections() {
  return request({
    url: '/admin/training-directions/list',
    method: 'get'
  });
} 