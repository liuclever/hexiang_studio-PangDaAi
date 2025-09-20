import request from '@/utils/request';

// 接口参数类型定义
export interface AttendanceQueryParams {
  page: number;
  pageSize: number;
  type?: string;
  status?: string;
  name?: string;
  startDate?: string;
  endDate?: string;
  studentName?: string;
  keyword?: string;
}

export interface AttendancePlanData {
  planId?: number;
  type: string;
  name: string;
  startTime: Date | string;
  endTime: Date | string;
  location: string;
  locationLat?: number;
  locationLng?: number;
  radius: number;
  courseId?: number;
  note?: string;
  status?: number;
  createUser?: number;
  scheduleId?: number;
}

export interface AttendanceRecordData {
  recordId?: number;
  planId: number;
  studentId: number;
  status: string;
  signInTime?: Date | string;
  location?: string;
  locationLat?: number;
  locationLng?: number;
  remark?: string;
  studentName?: string;
}

/**
 * 获取考勤计划列表
 * @param params 查询参数
 */
export function getAttendancePlans(params: {
  page?: number;
  pageSize?: number;
  type?: string;
  keyword?: string; // 考勤名称关键词
  courseName?: string;
  status?: string;
  startDate?: string;
  endDate?: string;
}) {
  return request({
    url: '/admin/attendance/plans',
    method: 'get',
    params
  });
}

/**
 * 获取考勤计划详情
 * @param planId 考勤计划ID
 */
export function getAttendancePlanDetail(planId: number) {
  return request({
    url: `/admin/attendance/plan/${planId}`,
    method: 'get'
  });
}

/**
 * 创建考勤计划
 * @param data 考勤计划数据
 */
export function createAttendancePlan(data: {
  type: string; // 'activity' | 'course' | 'duty'
  name: string;
  startTime: string;
  endTime: string;
  location: string;
  locationLng: number;
  locationLat: number;
  radius: number;
  courseId?: number;
  scheduleId?: number;
  commonLocationId?: number;
  note?: string;
}) {
  return request({
    url: '/admin/attendance/plan',
    method: 'post',
    data
  });
}

/**
 * 更新考勤计划
 * @param planId 考勤计划ID
 * @param data 考勤计划数据
 */
export function updateAttendancePlan(planId: number, data: {
  type?: string;
  name?: string;
  startTime?: string;
  endTime?: string;
  location?: string;
  locationLng?: number;
  locationLat?: number;
  radius?: number;
  courseId?: number;
  scheduleId?: number;
  commonLocationId?: number;
  remark?: string;
}) {
  return request({
    url: `/admin/attendance/plan/${planId}`,
    method: 'put',
    data
  });
}

/**
 * 删除考勤计划
 * @param planId 计划ID
 */
export function deleteAttendancePlan(planId: number) {
  return request({
    url: `/admin/attendance/plan/${planId}`,
    method: 'delete'
  });
}

/**
 * 切换考勤计划状态
 * @param planId 考勤计划ID
 * @param status 状态 1-有效，0-已取消
 */
export function toggleAttendanceStatus(planId: number, status: number) {
  return request({
    url: `/admin/attendance/plan/${planId}/status`,
    method: 'put',
    data: { status }
  });
}

/**
 * 获取考勤记录列表
 * @param params 查询参数
 */
export function getAttendanceRecords(params: {
  page?: number;
  pageSize?: number;
  planId?: number;
  keyword?: string; // 考勤名称关键词
  studentName?: string;
  status?: string; // 'present' | 'late' | 'absent' | 'leave'
  startDate?: string;
  endDate?: string;
}) {
  return request({
    url: '/admin/attendance/records',
    method: 'get',
    params
  });
}

/**
 * 更新考勤记录状态
 * @param recordId 考勤记录ID
 * @param status 状态 'present' | 'late' | 'absent' | 'leave' | 'pending'
 * @param remark 备注
 */
export function updateAttendanceRecordStatus(recordId: number, status: string, remark?: string) {
  return request({
    url: `/admin/attendance/records/${recordId}/status`,
    method: 'put',
    data: { status, remark }
  });
}

/**
 * 更新考勤计划的状态
 * @param planId 考勤计划ID
 * @param status 新的状态 (true:恢复/启用, false:禁用)
 */
export function updateAttendancePlanStatus(planId: number, status: boolean) {
  console.log('API函数内 - planId:', planId, 'status:', status, 'typeof status:', typeof status);
  
  // 明确设置data为对象，并指定status属性
  const data = { status: status };
  console.log('API函数内 - 请求数据:', data);
  
  return request({
    url: `/admin/attendance/plan/${planId}/status`,
    method: 'put',
    data
  });
}

/**
 * 删除考勤记录
 * @param recordId 考勤记录ID
 */
export function deleteAttendanceRecord(recordId: number) {
  return request({
    url: `/admin/attendance/record/${recordId}`,
    method: 'delete'
  });
}

/**
 * 获取考勤详情（包括计划信息和学生签到情况）
 * @param planId 考勤计划ID
 */
export function getAttendanceDetail(planId: number) {
  return request({
    url: `/admin/attendance/plan/${planId}`,
    method: 'get'
  });
}

/**
 * 获取考勤统计数据
 * @param params 查询参数
 */
export function getAttendanceStatistics(params: {
  type?: string; 
  startDate?: string; 
  endDate?: string;
}) {
  return request({
    url: '/admin/attendance/statistics/overall',
    method: 'get',
    params
  });
}

/**
 * 获取考勤类型统计数据
 * @param params 查询参数
 */
export function getAttendanceTypeStatistics(params: { 
  type?: string; 
  startDate?: string; 
  endDate?: string;
}) {
  return request({
    url: '/admin/attendance/statistics/type-distribution',
    method: 'get',
    params
  });
}

/**
 * 获取考勤趋势统计数据
 * @param params 查询参数
 */
export function getAttendanceTrendStatistics(params: { 
  type?: string; 
  startDate?: string; 
  endDate?: string;
}) {
  return request({
    url: '/admin/attendance/statistics/trends',
    method: 'get',
    params
  });
}

/**
 * 获取学生考勤统计数据
 * @param params 查询参数
 */
export function getStudentAttendanceStatistics(params: { 
  page?: number; 
  pageSize?: number;
  studentId?: number;
  type?: string; 
  startDate?: string; 
  endDate?: string;
}) {
  if (params.studentId) {
    // 单个学生统计
    return request({
      url: `/admin/attendance/statistics/student/${params.studentId}`,
      method: 'get',
      params
    });
  } else {
    // 学生统计列表
    return request({
      url: '/admin/attendance/statistics/student-list',
      method: 'get',
      params
    });
  }
}

/**
 * 获取课程考勤统计数据
 * @param courseId 课程ID
 */
export function getCourseAttendanceStatistics(courseId: number) {
  return request({
    url: `/admin/attendance/statistics/course/${courseId}`,
    method: 'get'
  });
}

/**
 * 获取学生考勤详情
 * @param studentId 学生ID
 */
export function getStudentAttendanceDetail(studentId: number) {
  return request({
    url: `/admin/attendance/statistics/student/${studentId}`,
    method: 'get'
  });
}

/**
 * 学生签到
 * @param planId 考勤计划ID
 * @param data 签到数据
 */
export function studentSignIn(planId: number, data: {
  location?: string;
  longitude: number;
  latitude: number;
}) {
  return request({
    url: `/admin/attendance/studentCheckIn`,
    method: 'post',
    data: {
      planId,
      studentId: localStorage.getItem('userId'),
      latitude: data.latitude,
      longitude: data.longitude,
      location: data.location || ''
    }
  });
}

/**
 * 获取学生可签到的考勤计划
 */
export function getStudentAttendancePlans() {
  return request({
    url: '/admin/attendance/plans',
    method: 'get',
    params: { status: 1 }
  });
}

/**
 * 获取常用地点列表
 */
export function getCommonLocations() {
  return request({
    url: '/admin/locations/common',
    method: 'get'
  });
}

/**
 * 获取值班表数据
 * @param params 查询参数
 */
export function getDutySchedule(params: { 
  startDate: string;
  endDate: string;
}) {
  return request({
    url: '/admin/duty/weekly-table',
    method: 'get',
    params,
    transformResponse: [(data) => {
      try {
        const parsed = JSON.parse(data);
        // 处理嵌套的data结构
        if (parsed && parsed.code === 200 && parsed.data && parsed.data.code === 200 && parsed.data.data) {
          return {
            code: 200,
            data: parsed.data.data
          };
        }
        return parsed;
      } catch (e) {
        console.error('解析值班表数据失败:', e);
        return data;
      }
    }]
  });
}

/**
 * 创建值班安排
 * @param data 值班安排数据
 */
export function createDutySchedule(data: {
  title: string;
  dutyDate: string;
  startTime: string;
  endTime: string;
  location: string;
  note?: string;
  studentIds: number[];
}) {
  return request({
    url: '/admin/duty/schedule',
    method: 'post',
    data
  });
}

/**
 * 更新值班安排
 * @param scheduleId 值班安排ID
 * @param data 值班安排数据
 */
export function updateDutySchedule(scheduleId: number, data: { 
  title?: string;
  dutyDate?: string;
  startTime?: string;
  endTime?: string;
  location?: string;
  note?: string;
  studentIds?: number[];
}) {
  return request({
    url: `/admin/duty/schedule/${scheduleId}`,
    method: 'put',
    data
  });
}

/**
 * 删除值班安排
 * @param scheduleId 值班安排ID
 */
export function deleteDutySchedule(scheduleId: number) {
  return request({
    url: `/admin/duty/schedule/${scheduleId}`,
    method: 'delete'
  });
}

/**
 * 批量保存值班安排
 * @param data 值班安排数据列表
 */
export function batchSaveDutySchedules(data: Array<{
  dutyName: string;
  startTime: string;
  endTime: string;
  location: string;
  status: number;
  studentId?: number;
}>) {
  return request({
    url: '/admin/duty/schedules/batch',
    method: 'post',
    data
  });
}

/**
 * 批量删除值班安排
 * @param scheduleIds 值班安排ID列表
 */
export function batchDeleteDutySchedules(scheduleIds: number[]) {
  return request({
    url: '/admin/duty/schedules/batch',
    method: 'delete',
    data: { scheduleIds }
  });
}

/**
 * 为值班安排生成考勤计划
 * @param scheduleId 值班安排ID
 */
export function generateAttendancePlanFromDutySchedule(scheduleId: number) {
  return request({
    url: `/admin/duty/schedule/${scheduleId}/generate-attendance`,
    method: 'post'
  });
}

/**
 * 为下一周生成值班安排
 * @param weekStart 当前周的开始日期
 */
export function generateNextWeekDutySchedules(weekStart: string) {
  return request({
    url: '/admin/duty/generate-next-week',
    method: 'post',
    data: { weekStart }
  });
}

/**
 * 获取值班安排详情
 * @param scheduleId 值班安排ID
 */
export function getDutyScheduleDetail(scheduleId: number) {
  return request({
    url: `/admin/duty/schedule/${scheduleId}`,
    method: 'get'
  });
}

/**
 * 添加学生到值班安排
 * @param scheduleId 值班安排ID
 * @param studentId 学生ID
 */
export function addStudentToDutySchedule(scheduleId: number, studentId: number) {
  return request({
    url: `/admin/duty/schedule/${scheduleId}/student/${studentId}`,
    method: 'post'
  });
}

/**
 * 从值班安排中移除学生
 * @param scheduleId 值班安排ID
 * @param studentId 学生ID
 */
export function removeStudentFromDutySchedule(scheduleId: number, studentId: number) {
  return request({
    url: `/admin/duty/schedule/${scheduleId}/student/${studentId}`,
    method: 'delete'
  });
}

/**
 * 获取值班安排列表
 */
export function getDutySchedulesApi() {
  return request({
    url: '/admin/duty/schedules',
    method: 'get'
  });
}

/**
 * 搜索课程
 * @param query 课程名称或关键词
 */
export function searchCoursesApi(query: string) {
  return request({
    url: '/admin/course/search',
    method: 'get',
    params: { query }
  });
}

/**
 * 搜索学生
 * @param query 学生姓名或关键词
 */
export function searchStudentsApi(query: string) {
  return request({
    url: '/admin/students/search',
    method: 'get',
    params: { query }
  });
}

/**
 * 手动生成考勤统计
 */
export function generateAttendanceStatistics() {
  return request({
    url: '/admin/attendance/statistics/generate',
    method: 'post',
    data: {
      date: new Date().toISOString().split('T')[0]
    }
  });
}

/**
 * 导出考勤统计数据
 * @param params 查询参数
 */
export function exportAttendanceStatistics(params: { type?: string; startDate?: string; endDate?: string }) {
  return request({
    url: '/admin/attendance/statistics/export',
    method: 'get',
    params,
    responseType: 'blob'
  });
}

/**
 * 批量更新值班安排学生
 * @param data 批量更新数据
 * @returns 更新结果
 * @deprecated 此方法已弃用，请使用 batchSyncDutySchedules 方法替代
 */
export function batchUpdateDutyScheduleStudents(data: any[]) {
  console.warn('batchUpdateDutyScheduleStudents方法已弃用，请使用batchSyncDutySchedules方法替代');
  return request({
    url: '/admin/duty-schedule/batch-update-students',
    method: 'post',
    data
  });
}

/**
 * 获取所有学生列表（包含姓名）
 * @returns 学生列表
 */
export function getAllStudents() {
  return request({
    url: '/admin/students/list-with-names',
    method: 'get'
  });
}

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

/**
 * 批量同步值班安排
 * 根据日期和时间段同步值班安排，支持增删改一体化操作
 * @param data 同步数据列表，每项包含日期、时间段和学生ID列表
 * @returns 同步结果
 */
export function batchSyncDutySchedules(data: Array<{
  dutyDate: string;
  timeSlot: string;
  location: string;
  studentIds: number[];
}>) {
  return request({
    url: '/admin/duty-schedule/batch-sync',
    method: 'post',
    data
  });
}

/**
 * 手动复制当前周值班安排到下一周
 * @returns 复制结果
 */
export function copyCurrentWeekToNext() {
  return request({
    url: '/admin/duty-schedule/copy-to-next-week',
    method: 'post'
  });
}

// =================================================================
// ==================== 请假审批相关API =====================
// =================================================================

/**
 * 获取请假申请列表
 * @param params 查询参数
 */
export function getLeaveRequests(params: {
  page?: number;
  pageSize?: number;
  studentName?: string;
  status?: 'pending' | 'approved' | 'rejected';
  startDate?: string;
  endDate?: string;
}) {
  return request({
    url: '/admin/approval/leave/list',
    method: 'get',
    params
  });
}

/**
 * 批准请假申请
 * @param requestId 请假申请ID
 */
export function approveLeaveRequest(requestId: number) {
  return request({
    url: `/admin/approval/leave/${requestId}/approve`,
    method: 'post'
  });
}

/**
 * 驳回请假申请
 * @param requestId 请假申请ID
 * @param data 包含驳回理由的对象
 */
export function rejectLeaveRequest(requestId: number, data: { remark: string }) {
  return request({
    url: `/admin/approval/leave/${requestId}/reject`,
    method: 'post',
    data
  });
}

/**
 * 获取请假申请详情
 * @param requestId 请假申请ID
 */
export function getLeaveRequestDetail(requestId: number) {
  return request({
    url: `/admin/approval/leave/${requestId}`,
    method: 'get'
  });
}

// ============= 活动预约相关 API =============

/**
 * 批量创建活动预约
 * @param data 预约数据
 */
export function batchCreateActivityReservation(data: {
  planId: number;
  studentIds: number[];
  remark?: string;
}) {
  return request({
    url: '/admin/attendance/activity/reservation/batch',
    method: 'post',
    data
  });
}

/**
 * 获取活动预约学生列表
 * @param planId 活动计划ID
 */
export function getActivityReservations(planId: number) {
  return request({
    url: `/admin/attendance/activity/plan/${planId}/reservations`,
    method: 'get'
  });
}

/**
 * 取消活动预约
 * @param planId 活动计划ID
 * @param studentIds 学生ID列表
 */
export function cancelActivityReservation(planId: number, studentIds: number[]) {
  return request({
    url: '/admin/attendance/activity/reservation/cancel',
    method: 'post',
    data: {
      planId,
      studentIds
    }
  });
}

/**
 * 获取活动预约列表（分页）
 * @param params 查询参数
 */
export function getActivityReservationList(params: {
  planId: number;
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
}) {
  return request({
    url: '/admin/attendance/activity/reservation/list',
    method: 'get',
    params
  });
}