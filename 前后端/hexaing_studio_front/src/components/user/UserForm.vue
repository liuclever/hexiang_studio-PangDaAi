<template>
  <el-dialog 
    v-model="dialogVisible" 
    :title="isEditing ? '编辑人员信息' : '新增人员'" 
    width="500px" 
    :before-close="handleDialogClose"
  >
    <el-form 
      ref="formRef" 
      :model="form" 
      :rules="formRules" 
      label-width="100px"
    >
      <!-- 基本信息 -->
      <el-form-item label="头像" prop="avatar">
        <el-upload
          class="avatar-uploader"
          action="#"
          :show-file-list="false"
          :auto-upload="false"
          :on-change="handleAvatarChange"
          :before-upload="beforeAvatarUpload"
        >
          <img v-if="avatarUrl" :src="avatarUrl" class="avatar" />
          <div v-else class="avatar-placeholder">
            <el-icon class="avatar-uploader-icon"><Plus /></el-icon>
            <div class="upload-text">点击上传头像</div>
          </div>
        </el-upload>
        <div class="el-form-item-tip" style="font-size: 13px; color: #606266; margin-top: 8px;">
          <strong>必填：</strong>支持JPG、PNG格式，文件大小不超过2MB
        </div>
      </el-form-item>
      <el-form-item label="姓名" prop="name">
        <el-input v-model="form.name" placeholder="请输入姓名" />
      </el-form-item>
      <el-form-item label="性别" prop="sex">
        <el-radio-group v-model="form.sex">
          <el-radio label="1">男</el-radio>
          <el-radio label="0">女</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="form.phone" placeholder="请输入手机号" />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="form.email" placeholder="请输入邮箱" />
      </el-form-item>
      
      <el-form-item label="账号" prop="userName" v-if="!isEditing">
        <el-input v-model="form.userName" placeholder="请输入账号" autocomplete="off" />
      </el-form-item>
      <el-form-item label="密码" prop="password" v-if="!isEditing">
        <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password autocomplete="new-password" />
      </el-form-item>
      <el-form-item label="角色" prop="role">
        <el-select v-model="form.role" placeholder="请选择角色" style="width: 100%" @change="handleRoleChange">
          <el-option label="访客" value="visitor" />
          <el-option label="学员" value="student" />
          <el-option label="老师" value="teacher" />
          <el-option label="管理员" value="manager" />
          <el-option label="超级管理员" value="admin" />
        </el-select>
      </el-form-item>
      <el-form-item label="职位" prop="positionId">
        <el-select v-model="form.positionId" placeholder="请选择职位" style="width: 100%">
          <el-option 
            v-for="posItem in filteredPositions" 
            :key="posItem.positionId"
            :label="posItem.positionName" 
            :value="posItem.positionId" 
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="form.status">
          <el-radio label="1">启用</el-radio>
          <el-radio label="0">禁用</el-radio>
        </el-radio-group>
      </el-form-item>

      <!-- 根据角色显示特有字段 -->
      <template v-if="form.role === 'student'">
        <el-divider content-position="left">学生特有信息</el-divider>
        <el-form-item label="学号" prop="studentNumber">
          <el-input v-model="form.studentNumber" placeholder="请输入学号" />
        </el-form-item>
        <el-form-item label="入学年份" prop="gradeYear">
          <el-date-picker
            v-model="form.gradeYear"
            type="year"
            placeholder="选择入学年份"
            format="YYYY"
            value-format="YYYY"
          />
        </el-form-item>
        <el-form-item label="专业班级" prop="major">
          <el-input v-model="form.major" placeholder="请输入专业班级" />
        </el-form-item>
        <el-form-item label="辅导员" prop="counselor">
          <el-input v-model="form.counselor" placeholder="请输入辅导员姓名" />
        </el-form-item>
        <el-form-item label="宿舍楼号" prop="dormitory">
          <el-input v-model="form.dormitory" placeholder="请输入宿舍楼号" />
        </el-form-item>
        <el-form-item label="所属部门" prop="departmentId">
          <el-select v-model="form.departmentId" placeholder="请选择所属部门" clearable>
            <el-option
              v-for="dept in departments"
              :key="dept.departmentId"
              :label="dept.departmentName"
              :value="dept.departmentId"
            />
          </el-select>
        </el-form-item>
      </template>
      
      <!-- 教师特有字段 -->
      <template v-if="form.role === 'teacher'">
        <el-divider content-position="left">教师特有信息</el-divider>
        <el-form-item label="职称" prop="title">
          <el-input v-model="form.title" placeholder="请输入职称" />
        </el-form-item>
        <el-form-item label="办公室" prop="officeLocation">
          <el-input v-model="form.officeLocation" placeholder="请输入办公室位置" />
        </el-form-item>
      </template>

      <!-- 培训方向（学生和教师共有） -->
      <template v-if="['student', 'teacher'].includes(form.role)">
        <el-divider content-position="left">培训信息</el-divider>
        <el-form-item label="培训方向" prop="training">
          <el-select 
            v-model="form.training" 
            multiple
            placeholder="请选择培训方向（最多3个）" 
            style="width: 100%"
            @change="handleTrainingChange"
            value-key="id"
          >
            <el-option 
              v-for="direction in props.trainingDirections" 
              :key="direction.directionId || direction.id" 
              :label="direction.directionName || direction.name || direction.description" 
              :value="direction"
            />
          </el-select>
          <div class="el-form-item-tip" style="font-size: 12px; color: #909399; margin-top: 5px;">
            提示：最多可选择3个培训方向
          </div>
        </el-form-item>
      </template>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleDialogClose">取消</el-button>
        <el-button type="primary" @click="handleSaveUser">确定</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, reactive, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import request from '@/utils/request';
import type { FormInstance } from 'element-plus';
import { UserForm, TrainingDirection, Position, UserVo, Department } from './types';
import { resolveAvatarUrl } from '@/utils/fileUtils'; // 引入转换函数

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  isEditing: {
    type: Boolean,
    default: false
  },
  initialData: {
    type: Object as () => Partial<UserVo>,
    default: () => ({})
  },
  positions: {
    type: Array as () => Position[],
    default: () => []
  },
  trainingDirections: {
    type: Array as () => TrainingDirection[],
    default: () => []
  },
  departments: {
    type: Array as () => Department[],
    default: () => []
  }
});

const emit = defineEmits(['update:visible', 'save-success']);

const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
});

// 表单引用
const formRef = ref<FormInstance | null>(null);
const avatarFile = ref<File | null>(null); // 存储头像文件
const avatarUrl = ref<string>(''); // 头像预览URL

// 表单数据
const form = reactive<UserForm>({
  userId: 0,
  name: '',
  sex: '1',
  role: 'student',
  phone: '',
  email: '',
  userName: '',
  password: '',
  positionId: null,
  avatar: '',
  status: '1',
  // 学生特有字段
  studentNumber: '',
  gradeYear: '',
  major: '', 
  counselor: '',
  dormitory: '',
  score: '',
  departmentId: undefined,
  // 教师特有字段
  officeLocation: '',
  title: '',
  training: [],
  // 管理员特有字段
  createTime: null,
  createUser: '',
  updateTime: null,
  updateUser: ''
});

const handleRoleChange = (newRole: string) => {
  // 角色切换时，根据新角色清空不相干的字段
  if (newRole !== 'teacher') {
    form.title = '';
    form.officeLocation = '';
  }
  if (newRole !== 'student') {
    form.studentNumber = '';
    form.gradeYear = '';
    form.major = '';
    form.counselor = '';
    form.dormitory = '';
    form.score = '';
    form.departmentId = undefined;
  }
  if (newRole !== 'student' && newRole !== 'teacher') {
    form.training = [];
  }
  
  // 当角色变更时，重置职位选择，后续由watch自动处理
  form.positionId = null;

};

const validateTraining = (rule: any, value: any, callback: any) => {
  if (['student', 'teacher'].includes(form.role)) {
    if (!form.training || form.training.length === 0) {
      callback(new Error('请至少选择一个培训方向'));
    } else {
      callback();
    }
  } else {
    callback();
  }
};

// 处理培训方向变更，限制最多选择3个并去除重复项
const handleTrainingChange = (value: TrainingDirection[]) => {
  console.log('培训方向选择变化:', value);
  console.log('当前选择数量:', value.length);
  
  // 去除重复项
  if (value.length > 0) {
    const uniqueMap = new Map();
    const uniqueDirections: TrainingDirection[] = [];
    
    value.forEach(direction => {
      const name = direction.name || direction.directionName;
      if (name && !uniqueMap.has(name)) {
        uniqueMap.set(name, direction);
        uniqueDirections.push(direction);
      }
    });
    
    // 如果有重复项，则更新选择值
    if (uniqueDirections.length < value.length) {
      form.training = uniqueDirections;
      
      // 提示用户
      ElMessage({
        type: 'warning',
        message: '已自动去除重复的培训方向'
      });
    }
  }
  
  // 如果超过3个，只保留前3个
  if (form.training.length > 3) {
    form.training = form.training.slice(0, 3);
    
    // 提示用户
    ElMessage({
      type: 'warning',
      message: '最多只能选择3个培训方向'
    });
  }
};

// 表单验证规则
const formRules = {
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 10, message: '长度在 2 到 10 个字符', trigger: 'blur' }
  ],
  userName: [
    { required: !props.isEditing, message: '请输入账号', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: !props.isEditing, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ],
  positionId: [
    { required: true, message: '请选择职位', trigger: 'change' }
  ],
  phone: [
    { required: true, message: '请输入手机号码', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  training: [
    { validator: validateTraining, trigger: 'change' }
  ],
  sex: [
    { required: true, message: '请选择性别', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ],
  // 学生特有字段
  studentNumber: [
    { required: (form: any) => form.role === 'student', message: '请输入学号', trigger: 'blur' },
    { pattern: /^\d+$/, message: '学号只能包含数字', trigger: 'blur' }
  ],
  gradeYear: [
    { required: (form: any) => form.role === 'student', message: '请选择入学年份', trigger: 'change' }
  ],
  major: [
    { required: (form: any) => form.role === 'student', message: '请输入专业班级', trigger: 'blur' },
    { pattern: /^\d{4}级[\u4e00-\u9fa5]+\d{2}班$/, message: '格式应为：2023级现代通信工程02班', trigger: 'blur' }
  ],
  counselor: [
    { required: (form: any) => form.role === 'student', message: '请输入辅导员姓名', trigger: 'blur' },
    { pattern: /^[\u4e00-\u9fa5]{2,4}$/, message: '请输入2-4个中文字符的姓名', trigger: 'blur' }
  ],
  dormitory: [
    { required: (form: any) => form.role === 'student', message: '请输入宿舍楼号', trigger: 'blur' },
    { pattern: /^\d{1,2}栋$/, message: '格式应为：13栋、11栋等', trigger: 'blur' }
  ],
  // 教师特有字段
  title: [
    { required: (form: any) => form.role === 'teacher', message: '请输入职称', trigger: 'blur' },
    { pattern: /^[\u4e00-\u9fa5]+$/, message: '职称只能包含中文字符', trigger: 'blur' }
  ],
  officeLocation: [
    { required: (form: any) => form.role === 'teacher', message: '请输入办公室位置', trigger: 'blur' }
  ],
  avatar: [
    { required: true, message: '请上传头像', trigger: 'change' }
  ]
};

// 过滤后的职位列表
const filteredPositions = computed(() => {
  return props.positions.filter(pos => pos.role === form.role);
});

// 监听过滤后的职位列表，如果只有一个选项，则自动选中
watch(filteredPositions, (newPositions) => {
  // 当角色切换后，职位列表会更新
  // 如果更新后的列表只有一个职位（例如访客或老师），则自动选中该职位
  if (newPositions.length === 1) {
    form.positionId = newPositions[0].positionId;
  } else if (form.role === 'manager' && newPositions.length > 0) {
    // 对于管理员角色，默认选择"主任"职位(positionId=4)
    const managerPosition = newPositions.find(pos => pos.positionId === 4);
    if (managerPosition) {
      form.positionId = managerPosition.positionId;
    }
  } else if (form.role === 'admin' && newPositions.length > 0) {
    // 对于超级管理员角色，默认选择"超级管理员"职位(positionId=5)
    const adminPosition = newPositions.find(pos => pos.positionId === 5);
    if (adminPosition) {
      form.positionId = adminPosition.positionId;
    }
  }
});

// 头像上传成功处理
const handleAvatarSuccess = (response: any, uploadFile: any) => {
  if (response.code === 1) {
    // 保存原始文件路径，不需要在这里构建完整URL
    form.avatar = response.data;
    avatarFile.value = uploadFile.raw; // 保存原始文件
  } else {
    ElMessage.error(response.msg || '头像上传失败');
  }
};

// 头像上传前的验证
const beforeAvatarUpload = (file: File) => {
  // 验证文件类型和大小
  const isJPG = file.type === 'image/jpeg';
  const isPNG = file.type === 'image/png';
  const isLt2M = file.size / 1024 / 1024 < 2;

  if (!isJPG && !isPNG) {
    ElMessage.error('头像只能是JPG或PNG格式!');
    return false;
  }
  if (!isLt2M) {
    ElMessage.error('头像大小不能超过2MB!');
    return false;
  }
  return true;
};

// 头像选择变更处理
const handleAvatarChange = (file: any) => {
  const rawFile = file.raw;
  if (rawFile) {
    // 检查文件类型和大小
    if (beforeAvatarUpload(rawFile)) {
      avatarFile.value = rawFile;
      avatarUrl.value = URL.createObjectURL(rawFile);
      // 关键修复：当用户选择文件后，同步更新表单的avatar字段
      // 这样验证逻辑就能通过
      form.avatar = rawFile.name; // 用文件名作为临时值以通过非空验证
    }
  }
};

// 重置表单
const resetForm = () => {
  if (formRef.value) {
    formRef.value.resetFields();
  }
  Object.assign(form, {
    userId: 0,
    name: '',
    sex: '1',
    role: 'student',
    phone: '',
    email: '',
    userName: '',
    password: '',
    positionId: null,
    avatar: '',
    status: '1',
    studentNumber: '',
    gradeYear: '',
    major: '',
    counselor: '',
    dormitory: '',
    score: '',
    officeLocation: '',
    title: '',
    training: [],
    createTime: null,
    createUser: '',
    updateTime: null,
    updateUser: ''
  });
  avatarFile.value = null;
  avatarUrl.value = '';
};

// 关闭对话框
const handleDialogClose = () => {
  emit('update:visible', false);
  resetForm();
};

// 根据角色ID获取角色key
const getRoleKeyFromId = (roleId: number | string | undefined): string => {
  if (roleId === undefined) return 'student'; // 默认角色
  const roleIdNum = Number(roleId);
  const roleMap: { [key: number]: string } = {
    0: 'visitor',
    1: 'student',
    2: 'teacher',
    3: 'manager',
    4: 'superadmin'
  };
  return roleMap[roleIdNum] || 'student';
};

// 监听initialData和trainingDirections的变化
watch(
  [() => props.initialData, () => props.trainingDirections],
  ([newData, availableDirections]) => {
    if (newData && Object.keys(newData).length > 0) {
      // 恢复手动赋值，确保数据正确转换
      form.userId = newData.userId || 0;
      form.name = newData.name || '';
      form.sex = newData.sex || '1';
      form.phone = newData.phone || '';
      form.email = newData.email || '';
      form.userName = newData.userName || '';
      form.positionId = newData.positionId || null;
      form.avatar = newData.avatar || '';
      form.status = newData.status || '1';
      form.role = getRoleKeyFromId(newData.roleId);
      
      form.studentNumber = newData.studentNumber || '';
      form.gradeYear = newData.gradeYear || '';
      form.major = newData.major || '';
      form.counselor = newData.counselor || '';
      form.dormitory = newData.dormitory || '';
      form.score = newData.score || '';
      form.departmentId = newData.departmentId || undefined;
      
      form.officeLocation = newData.officeLocation || '';
      form.title = newData.title || '';

      // 处理头像回显
      if (newData.avatar) {
        avatarUrl.value = resolveAvatarUrl(newData.avatar);
      } else {
        avatarUrl.value = '';
      }

      // 回显培训方向
      if (newData.directionIdNames && Array.isArray(newData.directionIdNames) && availableDirections && availableDirections.length > 0) {
        const selectedDirections: TrainingDirection[] = [];
        for (const name of newData.directionIdNames) {
          // 更灵活的匹配逻辑
          const found = availableDirections.find(dir => {
            // 尝试所有可能的字段组合
            const dirName = dir.directionName || dir.name || dir.description || '';
            const dirDesc = dir.description || '';
            
            // 进行不区分大小写的包含匹配
            return dirName.toLowerCase().includes(name.toLowerCase()) || 
                   dirDesc.toLowerCase().includes(name.toLowerCase());
          });

          if (found) {
            selectedDirections.push(found);
          }
        }
        form.training = selectedDirections;
      } else {
        form.training = [];
      }
    } else {
      resetForm();
    }
  },
  { immediate: true, deep: true }
);

// 监听visible变化
watch(() => props.visible, (visible) => {
  if (visible) {
    console.log('对话框打开，可用培训方向:', props.trainingDirections);
    if (!props.isEditing) {
      // 打开添加对话框时重置表单
      resetForm();
    }
  }
});

// 保存用户
const handleSaveUser = () => {
  formRef.value?.validate(async (valid) => {
    if (valid) {
      const url = props.isEditing ? '/admin/user/update' : '/admin/user/add';
      
      const formData = new FormData();
      
      // 1. 处理文件
      if (avatarFile.value) {
        formData.append('file', avatarFile.value);
      }
      
      // 2. 处理表单数据 - 必须确保所有必填字段都有值
      // 根据角色转换roleId
      const roleId = getRoleId(form.role);
      
      // 添加必填字段
      formData.append('userName', form.userName);
      formData.append('name', form.name);
      formData.append('sex', form.sex);
      formData.append('roleId', String(roleId));
      formData.append('status', form.status);
      
      // 添加可选字段
      if (form.password) formData.append('password', form.password);
      if (form.phone) formData.append('phone', form.phone);
      if (form.email) formData.append('email', form.email);
      
      // 确保职位ID被正确添加，特别是对于访客角色
      if (form.positionId !== null) {
        formData.append('positionId', String(form.positionId));
      }
      
      if (form.avatar) formData.append('avatar', form.avatar); // 如果已有头像路径
      
      // 添加编辑时需要的userId
      if (props.isEditing && form.userId) {
        formData.append('userId', String(form.userId));
      }
      
      // 添加角色特有字段
      if (form.role === 'student') {
        if (form.studentNumber) formData.append('studentNumber', form.studentNumber);
        if (form.gradeYear) formData.append('gradeYear', form.gradeYear);
        if (form.major) formData.append('major', form.major);
        if (form.counselor) formData.append('counselor', form.counselor);
        if (form.dormitory) formData.append('dormitory', form.dormitory);
        if (form.score) formData.append('score', form.score);
        // 只有选择了部门才发送departmentId
        if (form.departmentId) {
          formData.append('departmentId', String(form.departmentId));
        }
      } else if (form.role === 'teacher') {
        // 确保老师特有字段始终被提交，即使是空字符串
        formData.append('officeLocation', form.officeLocation || '');
        formData.append('title', form.title || '');
      }
      
      // 处理培训方向
      if (form.training && form.training.length > 0 && ['student', 'teacher'].includes(form.role)) {
        const trainingIds = form.training
          .map(t => t.directionId || t.id)
          .filter(id => id);
        
        trainingIds.forEach((id, index) => {
          formData.append(`training[${index}]`, String(id));
        });
      }

      try {
        await request({
          url,
          method: 'post',
          data: formData,
          headers: { 'Content-Type': 'multipart/form-data' }
        });
        
        ElMessage.success(props.isEditing ? '更新成功' : '新增成功');
        emit('save-success');
        handleDialogClose();
      } catch (error) {
        console.error('保存用户失败:', error);
        ElMessage.error(props.isEditing ? '更新失败' : '新增失败');
      }
    }
  });
};

// 根据角色名称获取角色ID
const getRoleId = (role: string): number => {
  switch (role) {
    case 'visitor': return 0;
    case 'student': return 1;
    case 'teacher': return 2;
    case 'manager': return 3;
    case 'admin': return 4;
    default: return 1; // 默认为学生
  }
};

// 根据角色ID获取角色名称
const getRoleNameFromId = (roleId: string | number | undefined): string => {
  if (!roleId) return 'student';
  
  switch (Number(roleId)) {
    case 0: return 'visitor';
    case 1: return 'student';
    case 2: return 'teacher';
    case 3: return 'manager';
    case 4: return 'admin';
    default: return 'student';
  }
};

// 头像URL处理
const getAvatarUrl = (avatar: string): string => {
  if (!avatar) return '';
  
  // 如果已经是完整URL，直接返回
  if (avatar.startsWith('http') || avatar.startsWith('/api/')) {
    return avatar;
  }
  
  // 否则添加API前缀
  return `/api/admin/file/view/${avatar}`;
};
</script>

<style lang="scss" scoped>
.avatar-uploader {
  :deep(.el-upload) {
    border: 1px solid var(--el-border-color);
    border-radius: 50%; // 恢复为圆形
    cursor: pointer;
    position: relative;
    overflow: hidden;
    transition: var(--el-transition-duration-fast);
    width: 140px;
    height: 140px;
    display: flex;
    justify-content: center;
    align-items: center;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1); // 添加阴影
    background-color: #f7f7f7; // 添加背景色

    &:hover {
      border-color: var(--el-color-primary);
      box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15); // 悬停时加深阴影
    }
  }

  .avatar {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .el-icon.avatar-uploader-icon {
    font-size: 36px; // 增大图标
    color: #8c939d;
  }
  
  .avatar-placeholder {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    width: 100%;
    height: 100%;
  }
  
  .upload-text {
    margin-top: 8px;
    font-size: 14px;
    color: #606266;
  }
}

.el-form-item-tip {
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
  margin-top: 4px;
}
</style> 