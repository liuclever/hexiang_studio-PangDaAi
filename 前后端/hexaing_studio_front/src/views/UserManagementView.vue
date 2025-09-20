<template>
  <div class="employees-view" style="margin-bottom: 0;">
    <el-card shadow="hover" class="page-header-card">
      <div class="page-title">
        <h2 style="margin: 0; font-size: 20px;">人员管理</h2>
        <div class="header-buttons">
          <el-button type="danger" @click="handleBatchDelete" :disabled="selectedUsers.length === 0">
            <el-icon><Delete /></el-icon>批量删除
          </el-button>
          <el-button type="primary" @click="showAddUserDialog">
            <el-icon><Plus /></el-icon>新增人员
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 搜索和筛选 -->
    <el-card shadow="hover" class="search-card">
      <div class="search-container">
        <el-input 
          v-model="searchQuery" 
          placeholder="搜索姓名、账号或联系方式" 
          class="search-input"
          clearable
          @clear="handleSearch"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <div class="filter-container">
          <el-select v-model="filterRole" placeholder="角色" clearable @change="handleRoleChange" style="width: 120px;">
            <el-option label="全部角色" value="" />
            <el-option label="超级管理员" value="4" />
            <el-option label="管理员" value="3" />
            <el-option label="老师" value="2" />
            <el-option label="学生" value="1" />
            <el-option label="访客" value="0" />
          </el-select>
          
          <el-select v-model="filterStatus" placeholder="状态" clearable @change="handleSearch" style="width: 120px;">
            <el-option label="全部状态" value="" />
            <el-option label="启用" value="1" />
            <el-option label="禁用" value="0" />
          </el-select>
          
          <!-- 部门筛选器 -->
          <el-select 
            v-model="filterDepartment" 
            placeholder="部门" 
            clearable 
            @change="handleSearch" 
            style="width: 120px;"
          >
            <el-option label="全部部门" value="" />
            <el-option 
              v-for="dept in departments" 
              :key="dept.departmentId" 
              :label="dept.departmentName" 
              :value="dept.departmentId.toString()" 
            />
          </el-select>
        </div>
      </div>
    </el-card>

    <div class="main-content">
      <el-card shadow="hover" class="table-card" :class="{ 'with-detail': currentUser }">
        <!-- 用户表格组件 -->
        <user-table
          :users="users"
          :loading="loading"
          :total-users="totalUsers"
          :current-page="currentPage"
          :page-size="pageSize"
          :search-query="searchQuery"
          :active-tab="activeTab"
          :filter-role="filterRole"
          :filter-status="filterStatus"
          :position-map="positionMap"
          @update:current-page="handleCurrentPageChange"
          @update:page-size="handleSizeChange"
          @selection-change="handleSelectionChange"
          @row-click="handleRowClick"
          @reload="loadUsers"
          @status-change="handleUserStatusChange"
          @show-honors="showAllUserHonors"
          @edit-user="handleEdit"
          @delete-user="handleDelete"
        />
      </el-card>
      
      <!-- 用户详情卡片 -->
      <user-detail-card
        v-if="currentUser"
        :user="currentUser"
        :position-map="positionMap"
        @edit-user="handleEdit"
        @delete-user="handleDelete"
        @show-honors="showAllUserHonors"
        @close="currentUser = null"
        class="detail-card"
      />
    </div>

    <!-- 用户表单组件 -->
    <user-form
      v-model:visible="dialogVisible"
      :is-editing="isEditing"
      :initial-data="userFormData"
      :positions="positions"
      :training-directions="trainingDirections"
      :departments="departments"
      @save-success="handleSaveSuccess"
    />

    <!-- 用户荣誉与证书对话框 -->
    <user-honor-and-certificate-dialog
      v-model:visible="honorsDialogVisible"
      :user="selectedUser"
      @refresh="refreshUserDetail"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, Search, Delete } from '@element-plus/icons-vue';
import request from '@/utils/request';
import UserTable from '@/components/user/UserTable.vue';
import UserDetailCard from '@/components/user/UserDetailCard.vue';
import UserForm from '@/components/user/UserForm.vue';
import UserHonorAndCertificateDialog from '@/components/user/UserHonorAndCertificateDialog.vue';
import { UserVo, Position, TrainingDirection, Department, roleMap } from '@/components/user/types';
import { resolveAvatarUrl } from '@/utils/fileUtils';

// 表格数据和控制
const loading = ref(false);
const users = ref<UserVo[]>([]);
const selectedUsers = ref<UserVo[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const totalUsers = ref(0);
const activeTab = ref('all');
const searchQuery = ref('');
const filterRole = ref('');
const filterStatus = ref('');
const filterDepartment = ref('');
const positions = ref<Position[]>([]);
const trainingDirections = ref<TrainingDirection[]>([]);
const departments = ref<Department[]>([]);

// 详情卡片控制
const currentUser = ref<UserVo | null>(null);

// 对话框控制
const dialogVisible = ref(false);
const isEditing = ref(false);
const userFormData = ref<Partial<UserVo>>({});

// 荣誉对话框控制
const honorsDialogVisible = ref(false);
const selectedUser = ref<UserVo | null>(null);

// 职位ID到名称的映射表
const positionMap = ref<Record<number, string>>({});

// 搜索处理
const handleSearch = () => {
  // 重置到第一页，但保留所有筛选条件
  currentPage.value = 1;
  
  // 加载用户数据
  loadUsers();
};

// 处理角色变化
const handleRoleChange = () => {
  // 如果不是学生角色，清空部门筛选
  if (filterRole.value !== '1') {
    filterDepartment.value = '';
  }
  handleSearch();
};

// 标签切换
const handleTabChange = (tab: any) => {
  // 使用事件参数中的标签名称，而不是依赖activeTab
  const tabName = tab.props.name;
  
  // 将标签名称映射到对应的role_id
  if (tabName === 'all') {
    filterRole.value = '';
  } else if (tabName === 'superadmin') {
    filterRole.value = '4';
  } else if (tabName === 'manager') {
    filterRole.value = '3';
  } else if (tabName === 'teacher') {
    filterRole.value = '2';
  } else if (tabName === 'student') {
    filterRole.value = '1';
  } else if (tabName === 'visitor') {
    filterRole.value = '0';
  }
  
  // 如果不是学生角色，清空部门筛选
  if (filterRole.value !== '1') {
    filterDepartment.value = '';
  }
  
  // 重置页码并立即加载用户数据
  currentPage.value = 1;
  loadUsers();
};

// 分页处理
const handleSizeChange = (size: number) => {
  pageSize.value = size;
  loadUsers();
};

const handleCurrentPageChange = (page: number) => {
  currentPage.value = page;
  loadUsers();
};

// 处理行点击事件，显示用户详情
const handleRowClick = (row: UserVo) => {
  // 获取用户详细信息
  getUserDetail(row.userId);
};

// 处理用户状态更改
const handleUserStatusChange = (user: UserVo) => {
  // 检查 currentUser 是否存在，以及其 userId 是否与传入的 user 的 userId 匹配
  if (currentUser.value && currentUser.value.userId === user.userId) {
    // 如果匹配，则调用 getUserDetail 函数刷新用户详情
    getUserDetail(user.userId);
  }
};

// 加载用户数据
const loadUsers = () => {
  loading.value = true;
  
  // 构建查询参数
  const params: Record<string, any> = {
    page: currentPage.value,
    pageSize: pageSize.value,
    name: searchQuery.value || undefined,
    roleId: filterRole.value || undefined,
    status: filterStatus.value || undefined,
    departmentId: filterDepartment.value || undefined
  };
  
  // 确保有值的参数被包含在请求中
  Object.keys(params).forEach(key => {
    if (params[key] === undefined || params[key] === '') {
      delete params[key];
    }
  });
  
  // 调用后端API获取用户列表
  request.get('/admin/user/list', { params })
    .then(response => {
      // 处理后端返回的数据结构
      const data = response.data || {};
      const records = data.records || [];
      
      // 转换用户数据
      users.value = [];
      
      for (const user of records) {
        // 根据roleId映射角色名称（如果后端未提供）
        let roleName = user.roleName || '未知';
        const roleId = user.role_id || user.roleId; // 同时支持下划线和驼峰
        
        // 如果后端未提供roleName，则前端进行转换
        if (!user.roleName) {
          roleName = roleMap[roleId] || '未知';
        }
        
        // 处理状态字段，确保它是字符串类型
        const userStatus = user.status === null ? '1' : String(user.status);
        
        // 创建用户对象，确保roleId也被正确添加
        users.value.push({
          userId: user.user_id || user.userId || 0, // 同时支持下划线和驼峰
          name: user.name || '',
          sex: user.sex || '',
          roleId: roleId, // 已经处理过的roleId（支持下划线和驼峰）
          roleName: roleName, // 使用后端返回的roleName或前端转换的结果
          phone: user.phone || '',
          email: user.email || '',
          positionName: user.positionName || user.position || '', // 优先使用后端返回的positionName
          positionId: user.position_id || user.positionId,
          avatar: resolveAvatarUrl(user.avatar),
          status: userStatus,
          studentNumber: user.student_number || user.studentNumber,
          gradeYear: user.grade_year || user.gradeYear,
          major: user.major || '',
          counselor: user.counselor,
          dormitory: user.dormitory,
          score: user.score,
          officeLocation: user.office_location || user.officeLocation,
          title: user.title,
          directionIdNames: user.directionIdNames || user.directionNames || [],
          departmentId: user.department_id || user.departmentId,
          departmentName: user.department_name || user.departmentName,
          isOnline: user.isOnline || false // 🔥 添加在线状态字段
        });
      }
      
      // 使用后端返回的总记录数
      totalUsers.value = data.total || 0;
    })
    .catch(error => {
      console.error('获取用户列表出错:', error);
      // ElMessage.error('获取用户列表失败'); // 由拦截器统一处理
    })
    .finally(() => {
      loading.value = false;
    });
};

// 获取用户详情
const getUserDetail = (userId: number) => {
  loading.value = true;
  
  // 使用查询参数的方式请求
  request.get(`/admin/user/detail`, { params: { userId: userId } })
    .then(response => {
      const userData = response.data || {};
      
      // 根据role_id映射角色名称
      let roleName = '未知';
      const roleId = userData.role_id || userData.roleId;
      
      if (roleId !== undefined) {
        roleName = roleMap[roleId] || '未知';
      }
      
      // 特殊处理：如果用户ID为1且没有正确识别角色，强制设置为学生
      if (userData.user_id === 1 && (!roleId || roleId === null)) {
        roleName = '学生';
      }
      
      // 如果有学生特有字段，也将角色设为学生
      if (userData.student_number || userData.grade_year || userData.major) {
        roleName = '学生';
      }
      
      // 构建用户对象
      const userDetail: UserVo = {
        userId: userData.user_id || userData.userId,
        name: userData.name,
        roleId: roleId,
        roleName: roleName,
        sex: userData.sex,
        phone: userData.phone,
        positionId: userData.position_id || userData.positionId,
        positionName: userData.position || userData.positionName, // 兼容多种返回字段
        avatar: resolveAvatarUrl(userData.avatar),
        status: userData.status,
        email: userData.email || '',
        studentNumber: userData.student_number ?? userData.studentNumber,
        gradeYear: userData.grade_year ?? userData.gradeYear,
        major: userData.major || '',
        counselor: userData.counselor,
        dormitory: userData.dormitory,
        score: userData.score,
        officeLocation: userData.office_location ?? userData.officeLocation,
        title: userData.title,
        directionIdNames: userData.directionIdNames ?? userData.directionNames ?? [],
        departmentId: userData.department_id ?? userData.departmentId,
        departmentName: userData.department_name ?? userData.departmentName
      };
      
      currentUser.value = userDetail;
      loading.value = false;
    })
    .catch(error => {
      console.error('获取用户详情出错:', error);
      // ElMessage.error('获取用户详情失败'); // 由拦截器统一处理
      loading.value = false;
    });
};

// 加载职位列表
const loadPositions = () => {
  request.get('/admin/user/positions')
    .then(response => {
      positions.value = response.data || [];
      
      // 动态生成职位ID到名称的映射表
      const map: Record<number, string> = {};
      positions.value.forEach(pos => {
        map[pos.positionId] = pos.positionName;
      });
      
      positionMap.value = map;
    })
    .catch(error => {
      console.error('获取职位列表出错:', error);
    });
};

// 加载培训方向列表
const loadTrainingDirections = () => {
  request.get('/admin/user/training-directions')
    .then(response => {
      const directions = ((response.data || []).map((item: any) => {
        return {
          id: item.directionId || item.id || 0,
          name: item.directionName || item.name || item.description || '未命名',
          directionId: item.directionId || item.id || 0,
          directionName: item.directionName || item.name || item.description || '未命名',
          description: item.description || ''
        };
      }));
      trainingDirections.value = directions;
    })
    .catch(error => {
      console.error('获取培训方向列表出错:', error);
      // ElMessage.error('加载培训方向列表失败'); // 由拦截器统一处理
    });
};

// 加载部门列表
const loadDepartments = () => {
  request.get('/admin/user/departments')
    .then(response => {
      departments.value = response.data || [];
    })
    .catch(error => {
      console.error('获取部门列表出错:', error);
      // ElMessage.error('加载部门列表失败'); // 由拦截器统一处理
    });
};

// 根据角色ID获取角色key
const getRoleKeyFromId = (roleId: number | string | undefined): string => {
  if (roleId === undefined) return 'student'; // 默认角色
  const roleIdNum = Number(roleId);
  const roleMap: { [key: number]: string } = {
    0: 'visitor',
    1: 'student',
    2: 'teacher',
    3: 'admin',
    4: 'superadmin'
  };
  return roleMap[roleIdNum] || 'student';
};

// 显示新增用户对话框
const showAddUserDialog = () => {
  isEditing.value = false;
  userFormData.value = {
    sex: '1',
    status: '1',
  };
  dialogVisible.value = true;
};

// 处理编辑操作
const handleEdit = (user: UserVo) => {
  request.get(`/admin/user/detail`, { params: { userId: user.userId } })
    .then(response => {
      const userData = response.data;
      
      isEditing.value = true; // 标记为编辑状态
      userFormData.value = {
        ...userData,
        directionIdNames: userData.directionIdNames || [],
      };
      
      dialogVisible.value = true;
    })
    .catch(error => {
      console.error('获取编辑用户详情出错:', error);
      // ElMessage.error('获取用户详情失败，无法编辑'); // 由拦截器统一处理
    });
};

// 删除用户
const handleDelete = (user: UserVo) => {
  ElMessageBox.confirm(
    `确定要删除 ${user.name} 吗？`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    request.post('/admin/user/delete', [user.userId.toString()])
      .then((response) => {
        // 获取响应数据
        let resp = response.data || {};
        
        // 确定正确的响应数据
        if (response && typeof response === 'object' && 'code' in response) {
          resp = response;
        }
        
        // 使用字符串比较，确保兼容数字和字符串类型的状态码
        const codeStr = String(resp.code);
        if (codeStr === '200' || codeStr === '1') {
          ElMessage({
            type: 'success',
            message: '删除成功'
          });
          
          // 如果删除的是当前正在查看的用户，则关闭详情视图
          if (currentUser.value && currentUser.value.userId === user.userId) {
            currentUser.value = null;
          }
          
          loadUsers(); // 重新加载数据
        } else {
          const errorMsg = resp.msg || '删除失败';
          ElMessage.error(errorMsg);
        }
      })
      .catch(error => {
        console.error('删除用户出错:', error);
        // ElMessage.error('删除失败'); // 由拦截器统一处理
      });
  }).catch(() => {
    // 取消删除
  });
};

// 批量删除用户
const handleBatchDelete = () => {
  if (selectedUsers.value.length === 0) {
    ElMessage.warning('请先选择要删除的用户');
    return;
  }
  
  const userNames = selectedUsers.value.map(user => user.name).join('、');
  const userIds = selectedUsers.value.map(user => user.userId.toString());
  
  ElMessageBox.confirm(
    `确定要删除选中的 ${selectedUsers.value.length} 个用户(${userNames})吗？`,
    '批量删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    // 使用与单个删除相同的API路径，但发送多个ID
    request.post('/admin/user/delete', userIds)
      .then((response) => {
        // 获取响应数据
        let resp = response.data || {};
        
        // 确定正确的响应数据
        if (response && typeof response === 'object' && 'code' in response) {
          resp = response;
        }
        
        // 使用字符串比较，确保兼容数字和字符串类型的状态码
        const codeStr = String(resp.code);
        if (codeStr === '200' || codeStr === '1') {
          ElMessage({
            type: 'success',
            message: `已成功删除 ${selectedUsers.value.length} 个用户`
          });
          
          // 如果删除的用户包含当前正在查看的用户，则关闭详情视图
          if (currentUser.value && selectedUsers.value.some(user => user.userId === currentUser.value?.userId)) {
            currentUser.value = null;
          }
          
          selectedUsers.value = []; // 清空选中项
          loadUsers(); // 重新加载数据
        } else {
          const errorMsg = resp.msg || '批量删除失败';
          ElMessage.error(errorMsg);
        }
      })
      .catch(error => {
        console.error('批量删除用户出错:', error);
        // ElMessage.error('批量删除失败，请重试'); // 由拦截器统一处理
      });
  }).catch(() => {
    // 用户取消删除操作
  });
};

// 处理表格多选
const handleSelectionChange = (selection: UserVo[]) => {
  selectedUsers.value = selection;
};

// 显示用户荣誉对话框
const showAllUserHonors = (user: UserVo) => {
  selectedUser.value = user;
  honorsDialogVisible.value = true;
};

// 保存用户表单成功后的回调
const handleSaveSuccess = () => {
  loadUsers();
  
  // 如果当前正在查看的用户是被编辑的用户，则刷新详情
  if (isEditing.value && currentUser.value && currentUser.value.userId === userFormData.value.userId) {
    getUserDetail(currentUser.value.userId);
  }
};

// 刷新用户详情
const refreshUserDetail = () => {
  if (currentUser.value) {
    getUserDetail(currentUser.value.userId);
  }
};

// 加载初始数据
onMounted(() => {
  // 先加载职位列表、培训方向列表和部门列表
  loadPositions();
  loadTrainingDirections();
  loadDepartments();
  // 加载用户列表数据
  loadUsers();
});
</script>

<style lang="scss" scoped>
.employees-view {
  min-height: calc(100vh - 50px); // 调整高度以适应导航栏
  background-color: #f4f6f8;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px; // 统一间距
}
  
.page-header-card, .search-card, .table-card {
  border-radius: 8px; // 轻微的圆角
  border: none;
  box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1); // 更柔和的阴影
}

.page-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
  
.header-buttons {
  display: flex;
  gap: 10px;
  align-items: center;
}

.search-container {
  display: flex;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
}

.search-input {
  flex-grow: 1; // 让搜索框占据更多空间
  min-width: 250px;
  max-width: 400px; // 限制最大宽度
}
      
.filter-container {
  display: flex;
  gap: 16px;
}

.user-tabs {
  // 移除边距，依赖父容器的gap
}

.main-content {
  display: flex;
  gap: 20px;
  width: 100%;
  flex: 1;

  > :deep(.table-card) {
    flex-grow: 1;
    transition: margin-right 0.3s ease;
    min-width: 0; // 关键：允许表格收缩
  }
}
        
.table-card {
  display: flex;
  flex-direction: column;
}

.detail-card {
  width: 380px;
  flex-shrink: 0;
  transition: all 0.3s ease;
}

.main-content {
  padding-bottom: 0;
}

.table-card :deep(.el-table) {
  font-size: 20px;
}

/* 增大右侧详情框字体 */
.detail-card :deep(.el-descriptions__label),
.detail-card :deep(.el-descriptions__content) {
  font-size: 16px;
}

/* 增大表单字体 */
.employees-view :deep(.el-form-item__label),
.employees-view :deep(.el-form-item__content),
.employees-view :deep(.el-input__inner),
.employees-view :deep(.el-select-dropdown__item),
.employees-view :deep(.el-radio__label) {
  font-size: 16px;
}
</style>