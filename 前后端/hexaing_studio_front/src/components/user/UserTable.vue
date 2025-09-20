<template>
  <div>
    <!-- 用户表格 -->
    <el-table 
      :data="filteredUsers" 
      style="width: 100%" 
      v-loading="loading"
      border
      stripe
      highlight-current-row
      :cell-style="{padding: '8px 12px'}"
      :row-style="{height: '55px'}"
      :header-cell-style="{background: '#f5f7fa', color: '#606266', fontWeight: 'bold', padding: '12px'}"
      size="large"
      table-layout="auto"
      @row-click="handleRowClick"
      @selection-change="handleSelectionChange"
    >
      <template #empty>
        <div style="text-align: center; padding: 20px;">
          <el-empty description="暂无数据" :image-size="100">
            <el-button @click="$emit('reload')">刷新数据</el-button>
          </el-empty>
        </div>
      </template>
      <el-table-column type="selection" width="55" />
      <el-table-column label="头像" width="80">
        <template #default="scope">
          <el-avatar 
            :size="40" 
            :src="resolveAvatarUrl(scope.row.avatar)"
            @error="() => true"
          >
            <img :src="defaultAvatar" />
          </el-avatar>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="姓名" min-width="120" />
      <el-table-column prop="sex" label="性别" width="80">
        <template #default="scope">
          {{ scope.row.sex === '1' ? '男' : scope.row.sex === '0' ? '女' : '未知' }}
        </template>
      </el-table-column>
      <el-table-column label="角色" width="110">
        <template #default="scope">
          <el-tag :type="getRoleType(roleMap[String(scope.row.roleId)] || '')" effect="light" round>
            {{ roleMap[String(scope.row.roleId)] || '未知' }}
          </el-tag>
        </template>
      </el-table-column>
        
      <!-- 职位列（访客角色不显示） -->
      <el-table-column prop="positionId" label="职位" min-width="160" v-if="activeTab !== 'visitor' && filterRole !== 'visitor'">
        <template #default="scope">
          <template v-if="scope.row.positionId !== undefined && scope.row.positionId !== null">
            {{ positionMap[scope.row.positionId] || '未知职位' }}
          </template>
          <template v-else>
            未设置
          </template>
        </template>
      </el-table-column>
      
      <el-table-column prop="phone" label="联系电话" min-width="150" />
      <el-table-column prop="email" label="邮箱" min-width="180">
        <template #default="scope">
          {{ scope.row.email || '未设置' }}
        </template>
      </el-table-column>

      <!-- 学生专属列（只在学生角色显示） -->
      <el-table-column 
        v-if="activeTab === 'student' || filterRole === 'student'"
        prop="studentNumber" 
        label="学号" 
        width="130" 
      />
    
      <el-table-column label="状态" width="90">
        <template #default="scope">
          <el-switch
            v-model="scope.row.status"
            :active-value="'1'"
            :inactive-value="'0'"
            @change="handleStatusChange(scope.row)"
          />
        </template>
      </el-table-column>
      
      <!-- 在线状态列 -->
      <el-table-column label="在线状态" width="100">
        <template #default="scope">
          <div class="online-status">
            <span 
              class="status-dot" 
              :class="{ online: scope.row.isOnline, offline: !scope.row.isOnline }"
            ></span>
            <span class="status-text">
              {{ scope.row.isOnline ? '在线' : '离线' }}
            </span>
          </div>
        </template>
      </el-table-column>
      
      <!-- 荣誉列（访客角色不显示） -->
      <el-table-column label="荣誉" width="100" v-if="activeTab !== 'visitor' && filterRole !== 'visitor'">
        <template #default="scope">
          <el-button 
            size="default"
            type="info"
            plain
            @click.stop="$emit('show-honors', scope.row)"
          >
            查看
          </el-button>
        </template>
      </el-table-column>
      
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="scope">
          <div class="action-buttons">
            <el-button 
              size="default" 
              type="primary" 
              @click.stop="$emit('edit-user', scope.row)"
            >
              编辑
            </el-button>
            <el-button 
              size="default" 
              type="danger" 
              @click.stop="$emit('delete-user', scope.row)"
            >
              删除
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-container">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="totalUsers"
        :current-page="currentPage"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { ElMessage } from 'element-plus';
import request from '@/utils/request';
import type { UserVo } from './types';
import { roleMap, getRoleType } from './types';
import { resolveAvatarUrl } from '@/utils/fileUtils';

// 定义属性
const props = defineProps({
  users: {
    type: Array as () => UserVo[],
    required: true
  },
  loading: {
    type: Boolean,
    default: false
  },
  totalUsers: {
    type: Number,
    default: 0
  },
  currentPage: {
    type: Number,
    default: 1
  },
  pageSize: {
    type: Number,
    default: 10
  },
  searchQuery: {
    type: String,
    default: ''
  },
  activeTab: {
    type: String,
    default: 'all'
  },
  filterRole: {
    type: String,
    default: ''
  },
  filterStatus: {
    type: String,
    default: ''
  },
  positionMap: {
    type: Object as () => Record<number, string>,
    required: true
  }
});

// 定义事件
const emit = defineEmits([
  'update:currentPage', 
  'update:pageSize', 
  'selection-change', 
  'row-click', 
  'reload',
  'status-change',
  'show-honors',
  'edit-user',
  'delete-user'
]);

// 过滤后的用户数据
const filteredUsers = computed(() => {
  return props.users.filter(user => {
    // 角色过滤
    if (props.filterRole !== '' && props.filterRole !== undefined) {
      return String(user.roleId) === String(props.filterRole);
    }
    // 标签过滤
    if (props.activeTab !== 'all') {
      let tabRoleId = '';
      if (props.activeTab === 'manager') tabRoleId = '3';
      else if (props.activeTab === 'teacher') tabRoleId = '2';
      else if (props.activeTab === 'student') tabRoleId = '1';
      else if (props.activeTab === 'visitor') tabRoleId = '0';
      return String(user.roleId) === tabRoleId;
    }
    // 状态过滤
    if (props.filterStatus && user.status !== props.filterStatus) {
      return false;
    }
    // 搜索
    if (props.searchQuery) {
      const query = props.searchQuery.toLowerCase();
      return (
        user.name.toLowerCase().includes(query) ||
        (user.phone && user.phone.includes(query))
      );
    }
    return true;
  });
});

// 处理行点击事件，显示用户详情
const handleRowClick = (row: UserVo, event: MouseEvent) => {
  // 检查点击的元素或其父元素是否包含 el-switch 类，如果是则不展示详情
  if (event.target instanceof Element) {
    // 检查元素本身或其父元素是否是开关
    const clickedElement = event.target as Element;
    if (
      clickedElement.classList.contains('el-switch') || 
      clickedElement.closest('.el-switch')
    ) {
      console.log('点击了状态开关，不打开用户详情');
      return;
    }
  }
  
  // 触发行点击事件
  emit('row-click', row);
};

// 状态变更处理
const handleStatusChange = (user: UserVo) => {
  // 保存原始状态，用于失败时恢复
  const originalStatus = user.status;
  
  request.post('/admin/user/update-status', {
    userId: user.userId,
    status: user.status
  })
    .then((response) => {
      // 获取响应数据
      const resp = response.data || {};
      
      // 改进的成功判断逻辑
      let isSuccess = false;
      
      // 检查code是否为成功状态码（数字或字符串形式）
      if ([1, 200, '1', '200'].includes(resp.code)) {
        isSuccess = true;
      } 
      // 检查data字段是否包含成功信息
      else if (resp.data && typeof resp.data === 'string' && resp.data.includes('成功')) {
        isSuccess = true;
      } 
      // 检查msg字段是否包含成功信息
      else if (resp.msg && typeof resp.msg === 'string' && resp.msg.includes('成功')) {
        isSuccess = true;
      }
      // 检查整个响应文本是否包含成功信息（兜底方案）
      else if (JSON.stringify(resp).includes('成功')) {
        isSuccess = true;
      }
      
      if (isSuccess) {
        ElMessage({
          type: 'success',
          message: `${user.name}状态已${user.status === '1' ? '启用' : '禁用'}`
        });
        emit('status-change', user);
      } else {
        // 如果更新失败，恢复原状态
        user.status = originalStatus;
        const errorMsg = resp.msg || '状态更新失败';
        ElMessage.error(errorMsg);
      }
    })
    .catch((error) => {
      console.error('状态更新请求失败:', error);
      // 如果更新失败，恢复原状态
      user.status = originalStatus;
      ElMessage.error('状态更新失败');
  });
};

// 分页处理
const handleSizeChange = (size: number) => {
  emit('update:pageSize', size);
};

const handleCurrentChange = (page: number) => {
  emit('update:currentPage', page);
};

// 处理表格多选
const handleSelectionChange = (selection: UserVo[]) => {
  emit('selection-change', selection);
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
.pagination-container {
  margin-top: 20px;
  margin-bottom: 0;
  display: flex;
  justify-content: center;
}

.pagination-container :deep(.el-pagination) {
  padding: 5px 0;
  font-size: 14px;
}

.pagination-container :deep(.el-pagination .el-pagination__jump) {
  margin-left: 15px;
}
</style> 

<style lang="scss" scoped>
.user-table-container {
  width: 100%;
}

.el-table {
  // 悬浮高亮
  :deep(.el-table__row) {
    transition: background-color 0.2s ease-in-out;
    &:hover {
      background-color: #ecf5ff !important;
    }
  }

  // 在线状态样式
  .online-status {
    display: flex;
    align-items: center;
    
    .status-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      margin-right: 6px;
      
      &.online {
        background-color: #67c23a; // 绿色 - 在线
        box-shadow: 0 0 4px rgba(103, 194, 58, 0.4);
      }
      
      &.offline {
        background-color: #ddd; // 灰色 - 离线
      }
    }
    
    .status-text {
      font-size: 12px;
      color: #606266;
    }
  }

  // 美化操作按钮
  .action-buttons {
    display: flex;
    gap: 8px;

    .el-button {
      padding: 8px 12px;
      border-radius: 6px;
    }
  }

  // 角色标签样式
  :deep(.el-tag) {
    border: none;
    font-weight: 500;
  }
}
</style> 