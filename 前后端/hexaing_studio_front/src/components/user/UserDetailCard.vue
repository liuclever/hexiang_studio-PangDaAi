<template>
  <el-card v-if="user" class="user-detail-sidebar" shadow="hover">
    <div class="user-detail-header">
      <el-avatar
        :size="80"
        :src="getAvatarUrl(user.avatar)"
        @error="onAvatarError"
      ></el-avatar>
      <div class="user-header-info">
        <h3 class="user-name">{{ user.name || '未知用户' }}</h3>
        <div class="user-tags">
          <el-tag :type="getRoleType(roleMap[String(user.roleId)] || '')" size="default" effect="light" round>
            {{ roleMap[String(user.roleId)] || '未知角色' }}
          </el-tag>
          <el-tag type="info" size="default" effect="light" round v-if="user && user.positionId !== undefined && positionMap[user.positionId]">
            {{ positionMap[user.positionId] }}
          </el-tag>
          <el-tag :type="user.status === '1' ? 'success' : 'danger'" size="default" effect="light" round>
            {{ user.status === '1' ? '已启用' : '已禁用' }}
          </el-tag>
        </div>
      </div>
    </div>
    
    <el-divider />
    
    <div class="user-quick-info">
      <div class="info-row">
        <span class="info-label">ID:</span>
        <span class="info-value">{{ user.userId }}</span>
      </div>
      <div class="info-row">
        <span class="info-label">性别:</span>
        <span class="info-value">{{ user.sex === '1' ? '男' : user.sex === '0' ? '女' : '未知' }}</span>
      </div>
      <div class="info-row">
        <span class="info-label">联系电话:</span>
        <span class="info-value">{{ user.phone || '未设置' }}</span>
      </div>
      <div class="info-row">
        <span class="info-label">邮箱:</span>
        <span class="info-value">{{ user.email || '未设置' }}</span>
      </div>
      
      <!-- 学生特有信息 -->
      <template v-if="isStudent(user)">
        <el-divider content-position="left">学生信息</el-divider>
        <div class="info-row">
          <span class="info-label">学号:</span>
          <span class="info-value">{{ user?.studentNumber || '未设置' }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">年级:</span>
          <span class="info-value">{{ calculateGrade(user?.gradeYear || '') || '未设置' }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">专业班级:</span>
          <span class="info-value">{{ user?.major || '未设置' }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">所属部门:</span>
          <span class="info-value">{{ user?.departmentName || '未设置' }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">辅导员:</span>
          <span class="info-value">{{ user?.counselor || '未设置' }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">宿舍楼号:</span>
          <span class="info-value">{{ user?.dormitory || '未设置' }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">分数:</span>
          <span class="info-value">{{ user?.score || '未设置' }}</span>
        </div>
      </template>
      
      <!-- 教师特有信息 -->
      <template v-if="isTeacher(user)">
        <el-divider content-position="left">教师信息</el-divider>
        <div class="info-row">
          <span class="info-label">职称:</span>
          <span class="info-value">{{ user?.title || '未设置' }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">办公室:</span>
          <span class="info-value">{{ user?.officeLocation || '未设置' }}</span>
        </div>
      </template>

      <!-- 培训方向 -->
      <template v-if="user && user.directionIdNames && user.directionIdNames.length > 0">
        <el-divider content-position="left">培训方向</el-divider>
        <div class="info-row-tags">
          <el-tag
            v-for="direction in user.directionIdNames"
            :key="direction"
            type="success"
            effect="light"
            round
          >
            {{ direction }}
          </el-tag>
        </div>
      </template>
      
      <!-- 荣誉和证书信息 -->
      <el-divider content-position="left">荣誉与证书</el-divider>
      <div class="actions-group">
        <div class="honor-actions" style="text-align: center; margin: 15px 0;">
          <el-button type="primary" size="default" @click="$emit('show-honors', user)">
            <span style="color: white;">查看全部荣誉信息</span>
          </el-button>
        </div>
      </div>
    </div>
    
    <div class="user-action-buttons">
      <el-button type="primary" @click="$emit('edit-user', user)" size="default">编辑</el-button>
      <el-button type="danger" @click="$emit('delete-user', user)" size="default">删除</el-button>
      <el-button @click="$emit('close')" size="default">关闭</el-button>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { UserVo } from './types';
import { defaultAvatar, roleMap, getRoleType } from './types';

const props = defineProps({
  user: {
    type: Object as () => UserVo | null,
    default: null
  },
  positionMap: {
    type: Object as () => Record<number, string>,
    required: true
  }
});

const emit = defineEmits(['edit-user', 'delete-user', 'show-honors', 'close']);

const getAvatarUrl = (path: string | undefined) => {
  if (!path) {
    return defaultAvatar;
  }
  
  // 如果已经是完整URL，直接返回
  if (path.startsWith('http') || path.startsWith('/api/')) {
    return path;
  }
  
  // 否则添加API前缀
  return `/api/admin/file/view/${path}`;
};

const onAvatarError = (e: Event) => {
  const target = e.target as HTMLImageElement;
  target.src = defaultAvatar;
};

// 判断是否为学生
const isStudent = (user: UserVo | null): boolean => {
  return user?.roleId === '1' || user?.roleId === 1;
};

// 判断是否为教师
const isTeacher = (user: UserVo | null): boolean => {
  return user?.roleId === '2' || user?.roleId === 2;
};

// 计算学生年级
const calculateGrade = (gradeYear: string | undefined): string => {
  if (!gradeYear) return '未知';
  
  const currentYear = new Date().getFullYear();
  const enrollYear = parseInt(gradeYear);
  const yearDiff = currentYear - enrollYear;
  
  if (yearDiff <= 0) return '未入学';
  if (yearDiff === 1) return '大一';
  if (yearDiff === 2) return '大二';
  if (yearDiff === 3) return '大三';
  if (yearDiff === 4) return '大四';
  return '已毕业';
};

// 获取角色默认职位名称
const getRoleDefaultPosition = (roleId: string | number | undefined) => {
  if (roleId === undefined) return '未设置';
  
  const roleIdStr = String(roleId);
  switch (roleIdStr) {
    case '0': return '访客';
    case '1': return '学员';
    case '2': return '老师';
    case '3': return '管理员';
    case '4': return '超级管理员';
    default: return '未设置';
  }
};
</script>

<style lang="scss" scoped>
.user-detail-sidebar {
  width: 400px;
  height: 100%;
  border: none;
  border-radius: 8px;
}

:deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.user-detail-header {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px;
  background: linear-gradient(135deg, #eef1f5, #e6e9ef);
  border-radius: 8px;
  margin-bottom: 20px;
}

.user-header-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.user-name {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

.user-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.user-quick-info {
  padding: 0 10px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  font-size: 14px;
}

.info-row {
  display: flex;
  align-items: center;
}

.info-label {
  width: 80px;
  font-weight: 500;
  color: #606266;
}

.info-value {
  flex: 1;
  color: #303133;
}

.info-row-tags {
  padding: 0 10px; 
  display: flex; 
  flex-wrap: wrap; 
  gap: 8px;
}

.el-divider--horizontal {
  margin: 20px 0;
}

.el-divider__text {
  font-weight: 600;
  color: #303133;
}

.user-action-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: auto;
  padding: 20px 10px 10px;
  border-top: 1px solid #f0f2f5;
}
</style> 