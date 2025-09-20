// 培训方向接口
export interface TrainingDirection {
  id?: number;
  name?: string;
  directionId?: number;
  directionName?: string;
  description?: string;
}

// 用户数据接口
export interface UserVo {
  userId: number;
  name: string;
  userName?: string;
  sex: string;
  roleId?: string | number;
  roleName?: string;
  phone: string;
  positionName?: string;
  positionId?: number;
  position?: string;
  avatar?: string;
  status: string;
  email?: string;
  isOnline?: boolean; // 在线状态
  
  // 教师和学生共有的字段
  training?: TrainingDirection[];
  directionIdNames?: string[];
  
  // 学生特有字段
  studentNumber?: string;
  gradeYear?: string;
  major?: string; // 专业班级字段
  counselor?: string;
  dormitory?: string;
  score?: string;
  departmentId?: number; // 部门ID
  departmentName?: string; // 部门名称
  
  // 教师特有字段
  officeLocation?: string;
  title?: string;
  
  // 管理员特有字段
  createTime?: Date;
  createUser?: string;
  updateTime?: Date;
  updateUser?: string;
}

// 职位数据接口
export interface Position {
  positionId: number;
  role: string;
  positionName: string;
}

// 部门数据接口
export interface Department {
  departmentId: number;
  departmentName: string;
  createTime?: Date;
}

// 荣誉数据接口
export interface UserHonor {
  id: number;
  userId: number;
  honorId?: number; 
  honorName: string;
  honorLevel?: string;
  issueOrg?: string;
  issueDate?: Date;
  description?: string;
  attachment?: string;
  createTime?: Date;
  createUser?: string;
  updateTime?: Date;
  updateUser?: string;
}

// 证书数据接口
export interface UserCertificate {
  id: number;
  userId: number;
  certificateId?: number; 
  certificateName: string;
  certificateLevel?: string;
  issueOrg?: string;
  issueDate?: Date;
  certificateNo?: string;
  description?: string;
  attachment?: string;
  createTime?: Date;
  createUser?: string;
  updateTime?: Date;
  updateUser?: string;
}

// 用户表单数据
export interface UserForm {
  userId: number;
  name: string;
  sex: string;
  role: string;
  phone: string;
  email: string;
  userName: string;
  password?: string; // 密码在编辑时可选
  positionId: number | null;
  avatar: string;
  status: string;
  training: TrainingDirection[];
  
  // 学生特有字段
  studentNumber?: string;
  gradeYear?: string;
  major?: string; 
  counselor?: string;
  dormitory?: string;
  score?: string;
  departmentId?: number; // 部门ID

  // 教师特有字段
  officeLocation?: string;
  title?: string;
  
  // 管理员特有字段
  createTime?: Date | null;
  createUser?: string;
  updateTime?: Date | null;
  updateUser?: string;
}

// 荣誉表单数据
export interface HonorForm {
  id?: number;
  honorId?: number;
  userId?: number;
  honorName: string;
  honorLevel?: string;
  issueOrg?: string;
  issueDate?: string;
  description?: string;
  attachment?: string;
}

// 证书表单数据
export interface CertificateForm {
  id?: number;
  certificateId?: number;
  userId?: number;
  certificateName: string;
  certificateLevel?: string;
  issueOrg?: string;
  issueDate?: string;
  certificateNo?: string;
  description?: string;
  attachment?: string;
}

// 角色ID到名称的映射表
export const roleMap: Record<string | number, string> = {
  '0': '访客',
  '1': '学员',
  '2': '老师',
  '3': '管理员',
  '4': '超级管理员'
};

// 获取角色对应的标签类型
export const getRoleType = (roleName: string | undefined) => {
  if (!roleName) return '';
  
  switch (roleName.toLowerCase()) {
    case '超级管理员':
      return 'danger';
    case '管理员':
      return 'warning';
    case '老师':
      return 'success';
    case '学生':
    case '学员':
      return 'primary';
    case '访客':
      return 'info';
    default:
      return '';
  }
};

// 默认路径配置
export const defaultAvatar = new URL('/src/assets/default-avatar.png', import.meta.url).href;
export const defaultCertificate = '/src/assets/default-certificate.png'; 