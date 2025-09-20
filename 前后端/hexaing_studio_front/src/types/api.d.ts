// 全局API数据类型定义

// 考勤状态枚举
export type AttendanceStatus = 'pending' | 'present' | 'late' | 'absent' | 'leave';

// 考勤计划类型枚举
export type AttendanceType = 'activity' | 'course' | 'duty';

// 考勤记录数据结构
export interface AttendanceRecord {
  id: number;
  planId: number;
  planName?: string;
  studentId: number;
  studentName?: string;
  status: AttendanceStatus;
  checkInTime?: string;
  location?: string;
  longitude?: number;
  latitude?: number;
  remark?: string;
  createTime?: string;
  updateTime?: string;
}

// 考勤计划数据结构
export interface AttendancePlan {
  id: number;
  name: string;
  type: AttendanceType;
  startTime: string;
  endTime: string;
  location: string;
  longitude: number;
  latitude: number;
  radius: number;
  status: number; // 1-有效，0-已取消
  courseId?: number;
  scheduleId?: number;
  remark?: string;
  createTime?: string;
  updateTime?: string;
}

// 值班安排数据结构
export interface DutySchedule {
  id?: number;
  date: string;
  timeSlot: string;
  studentId: number;
  studentName?: string;
  status?: AttendanceStatus;
  remark?: string;
}

// 分页结果
export interface PageResult<T> {
  total: number;
  records: T[];
}

// 通用API响应接口
export interface ApiResponse<T = any> {
  code: number;
  msg?: string;
  data: T;
}

// 培训方向
export interface TrainingDirection {
  direction_id: number;
  direction_name: string;

  // 兼容前端el-option的字段
  directionId?: number;
  directionName?: string;
  id?: number;
  name?: string;
}

// 用户相关接口
export interface User {
  userId: number;
  userName: string;
  name: string;
  password?: string;
  avatar?: string;
  sex: string;
  phone: string;
  email?: string;
  roleId: string;
  status: string;
  positionId?: number;
  createTime?: string;
  updateTime?: string;
  createUser?: number;
  updateUser?: number;

  // 学生特有字段
  studentId?: number;
  gradeYear?: string;
  major?: string;
  studentNumber?: string;
  counselor?: string;
  dormitory?: string;

  // 教师特有字段
  teacherId?: number;
  officeLocation?: string;
  title?: string;

  // 关联信息
  training?: TrainingDirection[];
  certificates?: UserCertificate[];
  honors?: UserHonor[];
}

// 证书相关接口
export interface UserCertificate {
  id: number; // 主键
  certificate_id: number;
  user_id: number;
  certificate_name: string;
  certificate_type: string;
  certificate_level: string;
  certificate_no?: string;
  issue_org: string;
  issue_date: string | Date; // 允许Date类型以兼容el-date-picker
  attachment?: string;
  description?: string;

  // 兼容前端可能在表单中使用的字段
  imageUrl?: string;
  certificateName?: string;
  issueOrg?: string;
  issueDate?: string | Date;
  certificateType?: string;
  certificateLevel?: string;
  certificateNo?: string;
}

// 荣誉相关接口
export interface UserHonor {
  id: number; // 主键
  honors_id: number;
  user_id: number;
  honor_name: string;
  honor_level: string;
  issue_org: string;
  issue_date: string | Date; // 允许Date类型以兼容el-date-picker
  attachment?: string;
  description?: string;

  // 兼容前端可能在表单中使用的字段
  imageUrl?: string;
  honorName?: string;
  issueOrg?: string;
  issueDate?: string | Date;
  honorLevel?: string;
}

// 位置信息
export interface Location {
  id?: number;
  name: string;
  address: string;
  longitude: number;
  latitude: number;
  radius?: number;
}

// 学生信息
export interface Student {
  id: number;
  name: string;
  studentNumber: string;
  avatar?: string;
  classId?: number;
  className?: string;
  major?: string;
}

// 考勤统计信息
export interface AttendanceStatistics {
  total: number;
  present: number;
  late: number;
  absent: number;
  leave: number;
  pending: number;
  presentRate?: string;
}

// 考勤趋势数据
export interface AttendanceTrend {
  date: string;
  present: number;
  late: number;
  absent: number;
  leave: number;
  pending: number;
}

// 任务类型定义
export interface Task {
  id?: number;
  title: string;
  description: string;
  startDate: string;
  endDate: string;
  status?: string;
  creator?: string;
  completionRate?: number;
  subtasks?: Subtask[];
  subtaskStats?: {
    total: number;
    completed: number;
  };
  memberCount?: number;
}

export interface Subtask {
  id?: number;
  title: string;
  description?: string;
  members?: SubtaskMember[];
}

export interface SubtaskMember {
  id: string | number;
  name: string;
  role?: string;
  status?: string;
  reviewStatus?: string;
}

// 任务查询参数
export interface TaskQueryParams {
  page: number;
  pageSize: number;
  query?: string;
  status?: string;
  startDate?: string | null;
  endDate?: string | null;
}

// 任务统计
export interface TaskStatistics {
  total: number;
  inProgress: number;
  completed: number;
  overdue: number;
  completionRate: number;
}

// 子任务提交
export interface TaskSubmission {
  id?: number;
  subtaskId: number;
  memberId: string | number;
  content: string;
  submitTime?: string;
  status?: string;
  reviewComment?: string;
  reviewStatus?: string;
  reviewTime?: string;
  reviewerId?: string | number;
} 