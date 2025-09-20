import request from '../request';
import { formatDate } from '@/utils/date';

// 定义接口类型
export interface AttendancePlanParams {
  page: number;
  pageSize: number;
  type?: string;
  keyword?: string;
  startDate?: string;
  endDate?: string;
  status?: string;
}

export interface AttendanceRecordParams {
  page: number;
  pageSize: number;
  planId?: number;
  studentName?: string;
  status?: string;
  startDate?: string;
  endDate?: string;
}

export interface AttendancePlanData {
  planId?: number;
  type: string;
  name: string;
  startTime: Date | string;
  endTime: Date | string;
  location: string;
  locationCoords?: { lat: number; lng: number };
  radius: number;
  courseId?: number;
  note?: string;
  status?: number | boolean;
}

export interface AttendanceRecordData {
  recordId?: number;
  planId: number;
  studentId: number | string;
  status: string;
  signInTime?: Date | string;
  location?: string;
  locationCoords?: { lat: number; lng: number };
  remark?: string;
}

export interface ActivityReservationData {
  id?: number;
  planId: number;
  studentId: number;
  reserveTime?: Date | string;
  status?: number | boolean;
}

export interface DutyScheduleData {
  scheduleId?: number;
  studentId: number;
  dutyName: string;
  location: string;
  startTime: Date | string;
  endTime: Date | string;
  repeatType?: 'once' | 'daily' | 'weekly';
  status?: number | boolean;
}

/**
 * 获取考勤计划列表 - 管理员
 * @param params 查询参数
 * @returns 
 */
export function getAttendancePlanList(params: AttendancePlanParams) {
  return request({
    url: '/admin/attendance/plans',
    method: 'get',
    params
  });
}

/**
 * 获取考勤计划详情 - 管理员
 * @param planId 考勤计划ID
 * @returns 
 */
export function getAttendancePlanDetail(planId: number) {
  return request({
    url: `/admin/attendance/plan/${planId}`,
    method: 'get'
  });
}

/**
 * 创建考勤计划 - 管理员
 * @param data 考勤计划数据
 * @returns 
 */
export function createAttendancePlan(data: AttendancePlanData) {
  // 处理位置坐标
  const payload = {
    ...data,
    latitude: data.locationCoords?.lat,
    longitude: data.locationCoords?.lng
  };
  
  delete payload.locationCoords;
  
  return request({
    url: '/admin/attendance/plan',
    method: 'post',
    data: payload
  });
}

/**
 * 更新考勤计划 - 管理员
 * @param planId 考勤计划ID
 * @param data 考勤计划数据
 * @returns 
 */
export function updateAttendancePlan(planId: number, data: AttendancePlanData) {
  // 处理位置坐标
  const payload = {
    ...data,
    latitude: data.locationCoords?.lat,
    longitude: data.locationCoords?.lng
  };
  
  delete payload.locationCoords;
  
  return request({
    url: `/admin/attendance/plan/${planId}`,
    method: 'put',
    data: payload
  });
}

/**
 * 切换考勤计划状态 - 管理员
 * @param planId 考勤计划ID
 * @param status 状态：true-有效，false-已取消
 * @returns 
 */
export function toggleAttendancePlanStatus(planId: number, status: boolean) {
  return request({
    url: `/admin/attendance/plan/${planId}/status`,
    method: 'patch',
    data: { status: status ? 1 : 0 }
  });
}

/**
 * 获取考勤记录列表 - 管理员
 * @param params 查询参数
 * @returns 
 */
export function getAttendanceRecordList(params: AttendanceRecordParams) {
  return request({
    url: '/admin/attendance/records',
    method: 'get',
    params
  });
}

/**
 * 修改考勤记录 - 管理员
 * @param recordId 考勤记录ID
 * @param data 考勤记录数据
 * @returns 
 */
export function updateAttendanceRecord(recordId: number, data: AttendanceRecordData) {
  // 处理位置坐标
  const payload = {
    ...data,
    latitude: data.locationCoords?.lat,
    longitude: data.locationCoords?.lng
  };
  
  delete payload.locationCoords;
  
  return request({
    url: `/admin/attendance/record/${recordId}`,
    method: 'put',
    data: payload
  });
}

/**
 * 批量初始化考勤记录 - 管理员
 * @param planId 考勤计划ID
 * @param studentIds 学生ID数组
 * @returns 
 */
export function initAttendanceRecords(planId: number, studentIds: number[]) {
  return request({
    url: `/admin/attendance/plan/${planId}/records/init`,
    method: 'post',
    data: { studentIds }
  });
}

/**
 * 获取课程考勤统计 - 管理员
 * @param courseId 课程ID
 * @param params 查询参数
 * @returns 
 */
export function getCourseAttendanceStatistics(courseId: number, params: any = {}) {
  return request({
    url: `/admin/attendance/statistics/course/${courseId}`,
    method: 'get',
    params
  });
}

/**
 * 获取学生考勤统计 - 管理员/学生
 * @param studentId 学生ID
 * @param params 查询参数
 * @returns 
 */
export function getStudentAttendanceStatistics(studentId: number, params: any = {}) {
  return request({
    url: `/admin/attendance/statistics/student/${studentId}`,
    method: 'get',
    params
  });
}

/**
 * 获取课程列表(用于选择) - 管理员
 * @returns 
 */
export function getCourseOptions() {
  return request({
    url: '/admin/attendance/course-options',
    method: 'get'
  });
}

/**
 * 获取学生列表(用于选择) - 管理员
 * @returns 
 */
export function getStudentOptions() {
  return request({
    url: '/admin/attendance/students',
    method: 'get'
  });
}

/**
 * 获取学生考勤计划列表 - 学生
 * @param params 查询参数
 * @returns 
 */
export function getStudentAttendancePlans(params: AttendancePlanParams) {
  return request({
    url: '/student/attendance/plans',
    method: 'get',
    params
  });
}

/**
 * 获取学生考勤计划详情 - 学生
 * @param planId 考勤计划ID 
 * @returns 
 */
export function getStudentAttendancePlanDetail(planId: number) {
  return request({
    url: `/student/attendance/plan/${planId}`,
    method: 'get'
  });
}

/**
 * 学生签到
 * @param data 签到数据
 * @returns 
 */
// 定义API响应类型
export interface ApiResponse<T = any> {
  code: number;
  msg: string;
  data: T;
  timestamp?: number;
}

export function studentCheckIn(data: {
  planId: number;
  studentId: number;
  signInTime: string;
  location: string;
  locationLat: number;
  locationLng: number;
  status: string;
}): Promise<ApiResponse> {
  return request({
    url: `/student/attendance/check-in`,
    method: 'post',
    data: {
      planId: data.planId,
      studentId: data.studentId,
      signInTime: data.signInTime,
      location: data.location,
      latitude: data.locationLat,
      longitude: data.locationLng,
      status: data.status
    }
  });
} 

/**
 * 获取活动预约列表 - 学生/管理员
 * @param planId 考勤计划ID
 * @returns 
 */
export function getActivityReservations(planId: number) {
  return request({
    url: `/common/activity/${planId}/reservations`,
    method: 'get'
  });
}

/**
 * 添加活动预约 - 学生
 * @param data 预约数据
 * @returns 
 */
export function addActivityReservation(data: ActivityReservationData) {
  return request({
    url: '/student/activity/reservation',
    method: 'post',
    data
  });
}

/**
 * 取消活动预约 - 学生
 * @param id 预约ID
 * @returns 
 */
export function cancelActivityReservation(id: number) {
  return request({
    url: `/student/activity/reservation/${id}/cancel`,
    method: 'patch'
  });
}

/**
 * 获取值班安排列表
 * @param params 查询参数
 * @returns 
 */
export function getDutySchedules(params: any = {}) {
  return request({
    url: '/attendance/duty/schedules',
    method: 'get',
    params
  });
}

/**
 * 获取值班安排详情
 * @param scheduleId 值班安排ID
 * @returns 
 */
export function getDutyScheduleDetail(scheduleId: number) {
  return request({
    url: `/attendance/duty/schedule/${scheduleId}`,
    method: 'get'
  });
}

/**
 * 添加值班安排
 * @param data 值班安排数据
 * @returns 
 */
export function addDutySchedule(data: any) {
  return request({
    url: '/attendance/duty/schedule',
    method: 'post',
    data
  });
}

/**
 * 更新值班安排
 * @param scheduleId 值班安排ID
 * @param data 值班安排数据
 * @returns 
 */
export function updateDutySchedule(scheduleId: number, data: any) {
  return request({
    url: `/attendance/duty/schedule/${scheduleId}`,
    method: 'put',
    data
  });
}

/**
 * 取消值班安排
 * @param scheduleId 值班安排ID
 * @returns 
 */
export function cancelDutySchedule(scheduleId: number) {
  return request({
    url: `/attendance/duty/schedule/${scheduleId}/cancel`,
    method: 'patch'
  });
}

/**
 * 获取今日值班安排
 * @returns 
 */
export function getTodayDutySchedules() {
  return request({
    url: '/attendance/duty/today',
    method: 'get'
  });
}

/**
 * 生成值班考勤计划
 * @returns 
 */
export function generateDutyAttendancePlans() {
  return request({
    url: '/admin/attendance/duty/generate-plans',
    method: 'post'
  });
}

/**
 * 获取课程学生列表
 * @param courseId 课程ID
 * @returns 
 */
export function getCourseStudents(courseId: number) {
  return request({
    url: `/admin/courses/${courseId}/students`,
    method: 'get'
  });
}

/**
 * 获取常用位置列表
 */
export const getCommonLocations = () => {
  return request({
    url: '/attendance/locations',
    method: 'get'
  });
};

/**
 * 添加常用位置
 * @param data 位置数据
 */
export const createCommonLocation = (data: any) => {
  return request({
    url: '/attendance/locations',
    method: 'post',
    data
  });
};

/**
 * 删除常用位置
 * @param id 位置ID
 */
export const deleteCommonLocation = (id: number) => {
  return request({
    url: `/attendance/locations/${id}`,
    method: 'delete'
  });
};

/**
 * 获取考勤计划详情
 */
export function getAttendanceDetail(planId: number) {
  return request({
    url: `/admin/attendance/detail/${planId}`,
    method: 'get'
  });
}

/**
 * 获取值班计划列表
 */
export const getAttendancePlans = (params?: any) => {
  return request({
    url: '/attendance/plans',
    method: 'GET',
    params
  });
};

/**
 * 搜索学生
 */
export const searchStudentsApi = (query: string) => {
  return request({
    url: '/students/search',
    method: 'GET',
    params: { query }
  });
};

/**
 * 获取值班表结构（不包含考勤状态）
 */
export function getDutyStructure() {
  return request({
    url: '/admin/duty-schedule/structure',
    method: 'get'
  });
}

/**
 * 获取考勤状态
 * @param startDate 开始日期
 * @param endDate 结束日期
 */
export function getDutyAttendanceStatus(startDate: string, endDate: string) {
  return request({
    url: '/admin/duty-schedule/attendance-status',
    method: 'get',
    params: {
      startDate,
      endDate
    }
  });
} 