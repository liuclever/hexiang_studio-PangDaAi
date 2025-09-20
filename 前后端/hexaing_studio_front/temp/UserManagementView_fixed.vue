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
          <template #append>
            <el-button @click="handleSearch">搜索</el-button>
          </template>
        </el-input>

        <div class="filter-container">
          <el-select v-model="filterRole" placeholder="角色" clearable @change="handleSearch">
            <el-option label="全部角色" value="" />
            <el-option label="管理员" value="3" />
            <el-option label="老师" value="2" />
            <el-option label="学生" value="1" />
            <el-option label="访客" value="0" />
          </el-select>
          
          <el-select v-model="filterStatus" placeholder="状态" clearable @change="handleSearch">
            <el-option label="全部状态" value="" />
            <el-option label="启用" value="1" />
            <el-option label="禁用" value="0" />
          </el-select>
        </div>
      </div>
    </el-card>

    <el-tabs v-model="activeTab" type="card" class="user-tabs" @tab-click="handleTabChange">
      <el-tab-pane label="全部人员" name="all"></el-tab-pane>
      <el-tab-pane label="管理员" name="manager"></el-tab-pane>
      <el-tab-pane label="老师" name="teacher"></el-tab-pane>
      <el-tab-pane label="学生" name="student"></el-tab-pane>
      <el-tab-pane label="访客" name="visitor"></el-tab-pane>
    </el-tabs>

    <div class="main-content">
      <el-card shadow="hover" class="table-card" style="margin-bottom: 0;">
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
                <el-button @click="loadUsers">刷新数据</el-button>
              </el-empty>
            </div>
          </template>
          <el-table-column type="selection" width="55" />
          <el-table-column label="头像" width="80">
            <template #default="scope">
              <el-avatar 
                :size="40" 
                :src="scope.row.avatar || defaultAvatar"
                @error="() => true"
              />
            </template>
          </el-table-column>
          <el-table-column prop="name" label="姓名" width="90" />
          <el-table-column prop="sex" label="性别" width="80">
            <template #default="scope">
              {{ scope.row.sex === '1' ? '男' : scope.row.sex === '0' ? '女' : '未知' }}
            </template>
          </el-table-column>
          <el-table-column label="角色" width="110">
            <template #default="scope">
              <el-tag :type="getRoleType(roleMap[String(scope.row.role_id)] || '')">
                {{ roleMap[String(scope.row.role_id)] || '未知' }}
              </el-tag>
            </template>
          </el-table-column>
            
          <!-- 职位列（访客角色不显示） -->
          <el-table-column prop="position_id" label="职位" width="90" v-if="activeTab !== 'visitor' && filterRole !== 'visitor'">
            <template #default="scope">
              <template v-if="scope.row.position_id !== undefined && positionMap[scope.row.position_id]">
                {{ positionMap[scope.row.position_id] }}
              </template>
              <template v-else-if="scope.row.role_id !== undefined">
                {{ getRoleDefaultPosition(scope.row.role_id) }}
              </template>
              <template v-else>
                未设置
              </template>
            </template>
          </el-table-column>
          
          <el-table-column prop="phone" label="联系电话" width="150" />
          <el-table-column prop="email" label="邮箱" width="180">
            <template #default="scope">
              {{ scope.row.email || '未设置' }}
            </template>
          </el-table-column>

          <!-- 学生专属列（只在学生角色显示） -->
          <el-table-column 
            v-if="activeTab === 'student' || filterRole === 'student'"
            prop="student_number" 
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
          
          <!-- 荣誉列（访客角色不显示） -->
          <el-table-column label="荣誉" width="100" v-if="activeTab !== 'visitor' && filterRole !== 'visitor'">
            <template #default="scope">
              <el-button 
                size="default"
                type="info"
                plain
                @click.stop="showAllUserHonors(scope.row)"
              >
                查看
              </el-button>
            </template>
          </el-table-column>
          
          <el-table-column label="操作" width="250" fixed="right">
            <template #default="scope">
              <el-button 
                size="default" 
                type="primary" 
                @click.stop="handleEdit(scope.row)"
              >
                编辑
              </el-button>
              <el-button 
                size="default" 
                type="danger" 
                @click.stop="handleDelete(scope.row)"
              >
                删除
              </el-button>
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
      </el-card>
      
      <!-- 右侧用户详情卡片（针对选中的用户） -->
      <el-card v-if="currentUser" class="user-detail-sidebar" shadow="hover">
        <div class="user-detail-header">
          <el-avatar :size="80" :src="currentUser.avatar || defaultAvatar"></el-avatar>
          <div class="user-header-info">
            <h3 style="font-size: 18px;">{{ currentUser.name || '未知用户' }}</h3>
            <div class="user-tags">
              <el-tag :type="getRoleType(roleMap[String(currentUser.role_id)] || '')" size="default">
                {{ roleMap[String(currentUser.role_id)] || '未知角色' }}
              </el-tag>
              <el-tag type="info" size="default" effect="plain" v-if="currentUser && currentUser.position_id !== undefined && positionMap[currentUser.position_id]">
                {{ positionMap[currentUser.position_id] }}
              </el-tag>
              <el-tag type="info" size="default" effect="plain" v-else-if="currentUser && currentUser.role_id !== undefined">
                {{ getRoleDefaultPosition(currentUser.role_id) }}
              </el-tag>
              <el-tag :type="currentUser.status === '1' ? 'success' : 'danger'" size="default">
                {{ currentUser.status === '1' ? '已启用' : '已禁用' }}
              </el-tag>
            </div>
          </div>
        </div>
        
        <el-divider />
        
        <div class="user-quick-info">
          <div class="info-row">
            <span class="info-label">ID:</span>
            <span class="info-value">{{ currentUser.user_id }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">性别:</span>
            <span class="info-value">{{ currentUser.sex === '1' ? '男' : currentUser.sex === '0' ? '女' : '未知' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">联系电话:</span>
            <span class="info-value">{{ currentUser.phone || '未设置' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">邮箱:</span>
            <span class="info-value">{{ currentUser.email || '未设置' }}</span>
          </div>
          
          <!-- 学生特有信息 -->
          <template v-if="isStudent(currentUser)">
            <el-divider content-position="left">学生信息</el-divider>
            <div class="info-row">
              <span class="info-label">学号:</span>
              <span class="info-value">{{ currentUser?.student_number || '未设置' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">年级:</span>
              <span class="info-value">{{ calculateGrade(currentUser?.grade_year || '') || '未设置' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">专业班级:</span>
              <span class="info-value">{{ currentUser?.major || '未设置' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">辅导员:</span>
              <span class="info-value">{{ currentUser?.counselor || '未设置' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">宿舍楼号:</span>
              <span class="info-value">{{ currentUser?.dormitory || '未设置' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">分数:</span>
              <span class="info-value">{{ currentUser?.score || '未设置' }}</span>
            </div>
          </template>
          
          <!-- 教师特有信息 -->
          <template v-if="isTeacher(currentUser)">
            <el-divider content-position="left">教师信息</el-divider>
            <div class="info-row">
              <span class="info-label">办公室:</span>
              <span class="info-value">{{ currentUser?.office_location || '未设置' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">职称:</span>
              <span class="info-value">{{ currentUser?.title || '未设置' }}</span>
            </div>
          </template>
          
          <!-- 培训方向（学生和教师共有） -->
          <template v-if="currentUser.direction_idNames && currentUser.direction_idNames.length > 0">
            <el-divider content-position="left">培训信息</el-divider>
            <div class="training-tags">
              <el-tag
                v-for="(direction, index) in currentUser.direction_idNames"
                :key="index"
                size="small"
                effect="light"
                class="training-tag"
              >
                {{ direction }}
              </el-tag>
            </div>
          </template>
          
          <!-- 用户荣誉信息 -->
          <el-divider content-position="left">荣誉信息</el-divider>
          <div class="honors-section">
            <div class="honor-actions" style="text-align: center; margin: 15px 0;">
              <el-button type="primary" size="default" @click="showAllUserHonors(currentUser)">
                <span style="color: white;">查看全部荣誉信息</span>
              </el-button>
            </div>
          </div>
        </div>
        
        <div class="user-action-buttons">
          <el-button type="primary" @click="handleEdit(currentUser)" size="default">编辑</el-button>
          <el-button type="danger" @click="handleDelete(currentUser)" size="default">删除</el-button>
          <el-button @click="currentUser = null" size="default">关闭</el-button>
        </div>
      </el-card>
    </div>

    <!-- 新增/编辑用户对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="isEditing ? '编辑人员信息' : '新增人员'" 
      width="500px" 
      :before-close="handleDialogClose"
    >
      <el-form 
        ref="userFormRef" 
        :model="userForm" 
        :rules="userFormRules" 
        label-width="100px"
      >
        <!-- 基本信息 -->
        <el-form-item label="头像" prop="avatar">
          <el-upload
            class="avatar-uploader"
            action="/api/admin/upload/image"
            :show-file-list="false"
            :on-success="handleAvatarSuccess"
            :before-upload="beforeAvatarUpload"
          >
            <img v-if="userForm.avatar" :src="userForm.avatar" class="avatar" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
          <div class="el-form-item-tip" style="font-size: 12px; color: #909399; margin-top: 5px;">
            点击上传头像，支持JPG、PNG格式，文件大小不超过2MB
          </div>
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="userForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="性别" prop="sex">
          <el-radio-group v-model="userForm.sex">
            <el-radio label="1">男</el-radio>
            <el-radio label="0">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        
        <el-form-item label="账号" prop="user_name" v-if="!isEditing">
          <el-input v-model="userForm.user_name" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!isEditing">
          <el-input v-model="userForm.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="userForm.role" placeholder="请选择角色" style="width: 100%" @change="handleRoleChange">
            <el-option label="访客" value="visitor" />
            <el-option label="学员" value="student" />
            <el-option label="老师" value="teacher" />
            <el-option label="管理员" value="manager" />
            <el-option label="超级管理员" value="admin" />
          </el-select>
        </el-form-item>
        <el-form-item label="职位" prop="position_id">
          <el-select v-model="userForm.position_id" placeholder="请选择职位" style="width: 100%">
            <el-option 
              v-for="(posItem, posIndex) in filteredPositions" 
              :key="posIndex"
              :label="posItem.position_name" 
              :value="posItem.position_id" 
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="userForm.status">
            <el-radio label="1">启用</el-radio>
            <el-radio label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 根据角色显示特有字段 -->
        <template v-if="userForm.role === 'student'">
          <el-divider content-position="left">学生特有信息</el-divider>
          <el-form-item label="学号" prop="student_number">
            <el-input v-model="userForm.student_number" placeholder="请输入学号" />
          </el-form-item>
          <el-form-item label="入学年份" prop="grade_year">
            <el-date-picker
              v-model="userForm.grade_year"
              type="year"
              placeholder="选择入学年份"
              format="YYYY"
              value-format="YYYY"
            />
          </el-form-item>
          <el-form-item label="专业班级" prop="major">
            <el-input v-model="userForm.major" placeholder="请输入专业班级" />
          </el-form-item>
          <el-form-item label="辅导员" prop="counselor">
            <el-input v-model="userForm.counselor" placeholder="请输入辅导员姓名" />
          </el-form-item>
          <el-form-item label="宿舍楼号" prop="dormitory">
            <el-input v-model="userForm.dormitory" placeholder="请输入宿舍楼号" />
          </el-form-item>
        </template>
        
        <!-- 教师特有字段 -->
        <template v-if="userForm.role === 'teacher'">
          <el-divider content-position="left">教师特有信息</el-divider>
          <el-form-item label="职称" prop="title">
            <el-input v-model="userForm.title" placeholder="请输入职称" />
          </el-form-item>
          <el-form-item label="办公室" prop="office_location">
            <el-input v-model="userForm.office_location" placeholder="请输入办公室位置" />
          </el-form-item>
        </template>

        <!-- 培训方向（学生和教师共有） -->
        <template v-if="['student', 'teacher'].includes(userForm.role)">
          <el-divider content-position="left">{{ userForm.role === 'teacher' ? '教师特有信息' : '培训信息' }}</el-divider>
          <el-form-item label="培训方向" prop="training">
            <el-select 
              v-model="userForm.training" 
              multiple
              collapse-tags
              placeholder="请选择培训方向（最多3个）" 
              style="width: 100%"
              @change="handleTrainingChange"
              value-key="id"
            >
              <el-option 
                v-for="direction in trainingDirections" 
                :key="direction.directionId || direction.id" 
                :label="direction.directionName || direction.name" 
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

    <!-- 荣誉展示对话框 -->
    <el-dialog
      v-model="honorsDialogVisible" 
      :title="selectedUser ? `${selectedUser.name}的荣誉与证书` : '用户荣誉与证书'"
      width="80%" 
      :before-close="handleHonorsDialogClose"
    >
      <div class="honors-dialog-content">
        <el-tabs type="border-card">
          <el-tab-pane label="荣誉列表">
            <div style="margin-bottom: 10px; text-align: right;">
              <el-button type="primary" size="small" @click="openHonorForm('add')">新增荣誉</el-button>
            </div>
            <el-table 
              :data="selectedUserHonors" 
              style="width: 100%" 
              v-loading="loading"
              border
              stripe
              highlight-current-row
              :cell-style="{padding: '8px'}"
              :row-style="{height: '50px'}"
              :header-cell-style="{background: '#f5f7fa', color: '#606266', fontWeight: 'bold', padding: '10px'}"
            >
              <el-table-column prop="honor_name" label="荣誉名称" min-width="150" />
              <el-table-column prop="honor_level" label="荣誉级别" width="130">
                <template #default="scope">
                  <el-tag :type="getHonorLevelType(scope.row.honor_level)" effect="dark" size="small">
                    {{ scope.row.honor_level || '未分级' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="issue_org" label="颁发机构" min-width="150" />
              <el-table-column prop="issue_date" label="颁发日期" width="120">
                <template #default="scope">
                  {{ formatDate(scope.row.issue_date) }}
                </template>
              </el-table-column>
              <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
              <el-table-column label="操作" width="250" fixed="right">
                <template #default="scope">
                  <el-button size="small" type="primary" @click.stop="previewHonorCertificate(scope.row)">查看荣誉</el-button>
                  <el-button size="small" type="success" @click.stop="openHonorForm('edit', scope.row)">编辑</el-button>
                  <el-button size="small" type="danger" @click.stop="deleteHonor(scope.row)">删除</el-button>
                </template>
              </el-table-column>
          </el-table>
          </el-tab-pane>
          
          <el-tab-pane label="证书列表">
            <div style="margin-bottom: 10px; text-align: right;">
              <el-button type="primary" size="small" @click="openCertificateForm('add')">新增证书</el-button>
            </div>
            <el-table 
              :data="selectedUserCertificates" 
              style="width: 100%" 
              v-loading="loading"
              border
              stripe
              highlight-current-row
              :cell-style="{padding: '8px'}"
              :row-style="{height: '50px'}"
              :header-cell-style="{background: '#f5f7fa', color: '#606266', fontWeight: 'bold', padding: '10px'}"
            >

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
          <template #append>
            <el-button @click="handleSearch">搜索</el-button>
          </template>
        </el-input>

        <div class="filter-container">
          <el-select v-model="filterRole" placeholder="角色" clearable @change="handleSearch">
            <el-option label="全部角色" value="" />
            <el-option label="管理员" value="3" />
            <el-option label="老师" value="2" />
            <el-option label="学生" value="1" />
            <el-option label="访客" value="0" />
          </el-select>
          
          <el-select v-model="filterStatus" placeholder="状态" clearable @change="handleSearch">
            <el-option label="全部状态" value="" />
            <el-option label="启用" value="1" />
            <el-option label="禁用" value="0" />
          </el-select>
        </div>
      </div>
    </el-card>

    <el-tabs v-model="activeTab" type="card" class="user-tabs" @tab-click="handleTabChange">
      <el-tab-pane label="全部人员" name="all"></el-tab-pane>
      <el-tab-pane label="管理员" name="manager"></el-tab-pane>
      <el-tab-pane label="老师" name="teacher"></el-tab-pane>
      <el-tab-pane label="学生" name="student"></el-tab-pane>
      <el-tab-pane label="访客" name="visitor"></el-tab-pane>
    </el-tabs>

    <div class="main-content">
    <el-card shadow="hover" class="table-card" style="margin-bottom: 0;">
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
              <el-button @click="loadUsers">刷新数据</el-button>
            </el-empty>
          </div>
        </template>
        <el-table-column type="selection" width="55" />
        <el-table-column label="头像" width="80">
          <template #default="scope">
            <el-avatar 
              :size="40" 
              :src="scope.row.avatar || defaultAvatar"
              @error="() => true"
            />
          </template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" width="90" />
        <el-table-column prop="sex" label="性别" width="80">
          <template #default="scope">
            {{ scope.row.sex === '1' ? '男' : scope.row.sex === '0' ? '女' : '未知' }}
          </template>
        </el-table-column>
        <el-table-column label="角色" width="110">
          <template #default="scope">
            <el-tag :type="getRoleType(roleMap[String(scope.row.role_id)] || '')">
              {{ roleMap[String(scope.row.role_id)] || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
          
          <!-- 职位列（访客角色不显示） -->
          <el-table-column prop="position_id" label="职位" width="90" v-if="activeTab !== 'visitor' && filterRole !== 'visitor'">
            <template #default="scope">
              <template v-if="scope.row.position_id !== undefined && positionMap[scope.row.position_id]">
                {{ positionMap[scope.row.position_id] }}
              </template>
              <template v-else-if="scope.row.role_id !== undefined">
                {{ getRoleDefaultPosition(scope.row.role_id) }}
              </template>
              <template v-else>
                未设置
              </template>
            </template>
          </el-table-column>
          
          <el-table-column prop="phone" label="联系电话" width="150" />
                  <el-table-column prop="email" label="邮箱" width="180">
          <template #default="scope">
            {{ scope.row.email || '未设置' }}
          </template>
        </el-table-column>

          <!-- 学生专属列（只在学生角色显示） -->
          <el-table-column 
            v-if="activeTab === 'student' || filterRole === 'student'"
            prop="student_number" 
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
          
          <!-- 荣誉列（访客角色不显示） -->
          <el-table-column label="荣誉" width="100" v-if="activeTab !== 'visitor' && filterRole !== 'visitor'">
          <template #default="scope">
            <el-button 
                size="default"
                type="info"
                plain
                @click.stop="showAllUserHonors(scope.row)"
              >
                查看
              </el-button>
            </template>
          </el-table-column>
          
          <el-table-column label="操作" width="250" fixed="right">
            <template #default="scope">
              <el-button 
                size="default" 
              type="primary" 
              @click.stop="handleEdit(scope.row)"
            >
              编辑
            </el-button>
            <el-button 
                size="default" 
              type="danger" 
              @click.stop="handleDelete(scope.row)"
            >
              删除
            </el-button>
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
    </el-card>
      
      <!-- 右侧用户详情卡片（针对选中的用户） -->
      <el-card v-if="currentUser" class="user-detail-sidebar" shadow="hover">
        <div class="user-detail-header">
          <el-avatar :size="80" :src="currentUser.avatar || defaultAvatar"></el-avatar>
          <div class="user-header-info">
            <h3 style="font-size: 18px;">{{ currentUser.name || '未知用户' }}</h3>
            <div class="user-tags">
              <el-tag :type="getRoleType(roleMap[String(currentUser.role_id)] || '')" size="default">
                {{ roleMap[String(currentUser.role_id)] || '未知角色' }}
              </el-tag>
              <el-tag type="info" size="default" effect="plain" v-if="currentUser && currentUser.position_id !== undefined && positionMap[currentUser.position_id]">
                {{ positionMap[currentUser.position_id] }}
              </el-tag>
              <el-tag type="info" size="default" effect="plain" v-else-if="currentUser && currentUser.role_id !== undefined">
                {{ getRoleDefaultPosition(currentUser.role_id) }}
              </el-tag>
              <el-tag :type="currentUser.status === '1' ? 'success' : 'danger'" size="default">
                {{ currentUser.status === '1' ? '已启用' : '已禁用' }}
              </el-tag>
            </div>
          </div>
        </div>
        
        <el-divider />
        
        <div class="user-quick-info">
          <div class="info-row">
            <span class="info-label">ID:</span>
            <span class="info-value">{{ currentUser.user_id }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">性别:</span>
            <span class="info-value">{{ currentUser.sex === '1' ? '男' : currentUser.sex === '0' ? '女' : '未知' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">联系电话:</span>
            <span class="info-value">{{ currentUser.phone || '未设置' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">邮箱:</span>
            <span class="info-value">{{ currentUser.email || '未设置' }}</span>
          </div>
          
          <!-- 学生特有信息 -->
          <template v-if="isStudent(currentUser)">
            <el-divider content-position="left">学生信息</el-divider>
            <div class="info-row">
              <span class="info-label">学号:</span>
              <span class="info-value">{{ currentUser?.student_number || '未设置' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">年级:</span>
              <span class="info-value">{{ calculateGrade(currentUser?.grade_year || '') || '未设置' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">专业班级:</span>
              <span class="info-value">{{ currentUser?.major || '未设置' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">辅导员:</span>
              <span class="info-value">{{ currentUser?.counselor || '未设置' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">宿舍楼号:</span>
              <span class="info-value">{{ currentUser?.dormitory || '未设置' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">分数:</span>
              <span class="info-value">{{ currentUser?.score || '未设置' }}</span>
            </div>
          </template>
          
          <!-- 教师特有信息 -->
          <template v-if="isTeacher(currentUser)">
            <el-divider content-position="left">教师信息</el-divider>
            <div class="info-row">
              <span class="info-label">办公室:</span>
              <span class="info-value">{{ currentUser?.office_location || '未设置' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">职称:</span>
              <span class="info-value">{{ currentUser?.title || '未设置' }}</span>
            </div>
          </template>
          
          <!-- 培训方向（学生和教师共有） -->
          <template v-if="currentUser.direction_idNames && currentUser.direction_idNames.length > 0">
            <el-divider content-position="left">培训信息</el-divider>
            <div class="training-tags">
              <el-tag
                v-for="(direction, index) in currentUser.direction_idNames"
                :key="index"
                size="small"
                effect="light"
                class="training-tag"
              >
                {{ direction }}
              </el-tag>
            </div>
          </template>
          
          <!-- 用户荣誉信息 -->
          <el-divider content-position="left">荣誉信息</el-divider>
          <div class="honors-section">
            <div class="honor-actions" style="text-align: center; margin: 15px 0;">
              <el-button type="primary" size="default" @click="showAllUserHonors(currentUser)">
                <span style="color: white;">查看全部荣誉信息</span>
              </el-button>
            </div>
          </div>
        </div>
        
        <div class="user-action-buttons">
          <el-button type="primary" @click="handleEdit(currentUser)" size="default">编辑</el-button>
          <el-button type="danger" @click="handleDelete(currentUser)" size="default">删除</el-button>
          <el-button @click="currentUser = null" size="default">关闭</el-button>
        </div>
      </el-card>
    </div>

    <!-- 新增/编辑用户对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="isEditing ? '编辑人员信息' : '新增人员'" 
      width="500px" 
      :before-close="handleDialogClose"
    >
      <el-form 
        ref="userFormRef" 
        :model="userForm" 
        :rules="userFormRules" 
        label-width="100px"
      >
        <!-- 基本信息 -->
        <el-form-item label="头像" prop="avatar">
          <el-upload
            class="avatar-uploader"
            action="/api/admin/upload/image"
            :show-file-list="false"
            :on-success="handleAvatarSuccess"
            :before-upload="beforeAvatarUpload"
          >
            <img v-if="userForm.avatar" :src="userForm.avatar" class="avatar" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
          <div class="el-form-item-tip" style="font-size: 12px; color: #909399; margin-top: 5px;">
            点击上传头像，支持JPG、PNG格式，文件大小不超过2MB
          </div>
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="userForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="性别" prop="sex">
          <el-radio-group v-model="userForm.sex">
            <el-radio label="1">男</el-radio>
            <el-radio label="0">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        
        <el-form-item label="账号" prop="user_name" v-if="!isEditing">
          <el-input v-model="userForm.user_name" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!isEditing">
          <el-input v-model="userForm.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="userForm.role" placeholder="请选择角色" style="width: 100%" @change="handleRoleChange">
            <el-option label="访客" value="visitor" />
            <el-option label="学员" value="student" />
            <el-option label="老师" value="teacher" />
            <el-option label="管理员" value="manager" />
            <el-option label="超级管理员" value="admin" />
          </el-select>
        </el-form-item>
        <el-form-item label="职位" prop="position_id">
          <el-select v-model="userForm.position_id" placeholder="请选择职位" style="width: 100%">
            <el-option 
              v-for="(posItem, posIndex) in filteredPositions" 
              :key="posIndex"
              :label="posItem.position_name" 
              :value="posItem.position_id" 
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="userForm.status">
            <el-radio label="1">启用</el-radio>
            <el-radio label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 根据角色显示特有字段 -->
        <template v-if="userForm.role === 'student'">
          <el-divider content-position="left">学生特有信息</el-divider>
          <el-form-item label="学号" prop="student_number">
            <el-input v-model="userForm.student_number" placeholder="请输入学号" />
          </el-form-item>
          <el-form-item label="入学年份" prop="grade_year">
            <el-date-picker
              v-model="userForm.grade_year"
              type="year"
              placeholder="选择入学年份"
              format="YYYY"
              value-format="YYYY"
            />
          </el-form-item>
          <el-form-item label="专业班级" prop="major">
            <el-input v-model="userForm.major" placeholder="请输入专业班级" />
          </el-form-item>
          <el-form-item label="辅导员" prop="counselor">
            <el-input v-model="userForm.counselor" placeholder="请输入辅导员姓名" />
          </el-form-item>
          <el-form-item label="宿舍楼号" prop="dormitory">
            <el-input v-model="userForm.dormitory" placeholder="请输入宿舍楼号" />
          </el-form-item>
        </template>
        
        <!-- 教师特有字段 -->
        <template v-if="userForm.role === 'teacher'">
          <el-divider content-position="left">教师特有信息</el-divider>
          <el-form-item label="职称" prop="title">
            <el-input v-model="userForm.title" placeholder="请输入职称" />
          </el-form-item>
          <el-form-item label="办公室" prop="office_location">
            <el-input v-model="userForm.office_location" placeholder="请输入办公室位置" />
          </el-form-item>
        </template>

        <!-- 培训方向（学生和教师共有） -->
        <template v-if="['student', 'teacher'].includes(userForm.role)">
          <el-divider content-position="left">{{ userForm.role === 'teacher' ? '教师特有信息' : '培训信息' }}</el-divider>
          <el-form-item label="培训方向" prop="training">
            <el-select 
              v-model="userForm.training" 
              multiple
              collapse-tags
              placeholder="请选择培训方向（最多3个）" 
              style="width: 100%"
              @change="handleTrainingChange"
              value-key="id"
            >
              <el-option 
                v-for="direction in trainingDirections" 
                :key="direction.directionId || direction.id" 
                :label="direction.directionName || direction.name" 
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

    <!-- 荣誉展示对话框 -->
    <el-dialog
      v-model="honorsDialogVisible" 
      :title="selectedUser ? `${selectedUser.name}的荣誉与证书` : '用户荣誉与证书'"
      width="80%" 
      :before-close="handleHonorsDialogClose"
    >
      <div class="honors-dialog-content">
        <el-tabs type="border-card">
          <el-tab-pane label="荣誉列表">
            <div style="margin-bottom: 10px; text-align: right;">
              <el-button type="primary" size="small" @click="openHonorForm('add')">新增荣誉</el-button>
            </div>
            <el-table 
              :data="selectedUserHonors" 
              style="width: 100%" 
              v-loading="loading"
              border
              stripe
              highlight-current-row
              :cell-style="{padding: '8px'}"
              :row-style="{height: '50px'}"
              :header-cell-style="{background: '#f5f7fa', color: '#606266', fontWeight: 'bold', padding: '10px'}"
            >
              <el-table-column prop="honor_name" label="荣誉名称" min-width="150" />
              <el-table-column prop="honor_level" label="荣誉级别" width="130">
                <template #default="scope">
                  <el-tag :type="getHonorLevelType(scope.row.honor_level)" effect="dark" size="small">
                    {{ scope.row.honor_level || '未分级' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="issue_org" label="颁发机构" min-width="150" />
              <el-table-column prop="issue_date" label="颁发日期" width="120">
                <template #default="scope">
                  {{ formatDate(scope.row.issue_date) }}
                </template>
              </el-table-column>
              <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
              <el-table-column label="操作" width="250" fixed="right">
                <template #default="scope">
                  <el-button size="small" type="primary" @click.stop="previewHonorCertificate(scope.row)">查看荣誉</el-button>
                  <el-button size="small" type="success" @click.stop="openHonorForm('edit', scope.row)">编辑</el-button>
                  <el-button size="small" type="danger" @click.stop="deleteHonor(scope.row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          
          <el-tab-pane label="证书列表">
            <div style="margin-bottom: 10px; text-align: right;">
              <el-button type="primary" size="small" @click="openCertificateForm('add')">新增证书</el-button>
            </div>
            <el-table 
              :data="selectedUserCertificates" 
              style="width: 100%" 
              v-loading="loading"
              border
              stripe
              highlight-current-row
              :cell-style="{padding: '8px'}"
              :row-style="{height: '50px'}"
              :header-cell-style="{background: '#f5f7fa', color: '#606266', fontWeight: 'bold', padding: '10px'}"
            >
              <el-table-column prop="certificate_name" label="证书名称" min-width="150" />
              <el-table-column prop="certificate_level" label="证书等级" width="90">
                <template #default="scope">
                  <el-tag :type="getCertificateLevelType(scope.row.certificate_level)" effect="plain" size="small">
                    {{ scope.row.certificate_level || '未分级' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="issue_org" label="颁发机构" min-width="150" />
              <el-table-column prop="issue_date" label="颁发日期" width="120">
                <template #default="scope">
                  {{ formatDate(scope.row.issue_date) }}
                </template>
              </el-table-column>
              <el-table-column prop="certificate_no" label="证书编号" width="190" />
              <el-table-column label="操作" width="250" fixed="right">
                <template #default="scope">
                  <el-button 
                    size="small"
                    type="primary" 
                    @click.stop="previewCertificate(scope.row)"
                  >
                    查看证书
                  </el-button>
                  <el-button size="small" type="success" @click.stop="openCertificateForm('edit', scope.row)">编辑</el-button>
                  <el-button size="small" type="danger" @click.stop="deleteCertificate(scope.row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="handleHonorsDialogClose">关闭</el-button>
        </span>
      </template>
    </el-dialog>
            
    <!-- 证书预览对话框 -->
    <el-dialog 
      v-model="certificatePreviewVisible" 
      title="证书预览"
      width="60%" 
      :before-close="handleCertificatePreviewClose"
    >
      <div class="certificate-preview" v-if="selectedCertificate">
        <div class="certificate-header">
          <h3>{{ getCertificateTitle() }}</h3>
          <div class="certificate-info">
            <p><span>颁发机构:</span> {{ selectedCertificate.issue_org }}</p>
            <p><span>颁发日期:</span> {{ formatDate(selectedCertificate.issue_date) }}</p>
            <p v-if="hasCertificateNumber()"><span>证书编号:</span> {{ getCertificateNumber() }}</p>
          </div>
        </div>
              
        <div class="certificate-body">
          <div class="certificate-image-placeholder">
            <img :src="selectedCertificate.attachment || defaultCertificate" alt="证书图片" class="certificate-image" />
          </div>
              
          <div class="certificate-description" v-if="selectedCertificate.description">
            <h4>证书描述</h4>
            <p>{{ selectedCertificate.description }}</p>
          </div>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="handleCertificatePreviewClose">关闭</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 荣誉表单弹窗 -->
    <el-dialog v-model="honorFormVisible" :title="honorFormMode==='add'?'新增荣誉':'编辑荣誉'" width="500px">
      <el-form :model="honorFormData" label-width="100px">
        <el-form-item label="荣誉名称" prop="honor_name">
          <el-input v-model="honorFormData.honor_name" />
        </el-form-item>
        <el-form-item label="荣誉级别" prop="honor_level">
          <el-select v-model="honorFormData.honor_level" placeholder="请选择荣誉级别" style="width: 100%">
            <el-option label="国家级" value="国家级" />
            <el-option label="省/市级" value="省/市级" />
            <el-option label="校级" value="校级" />
            <el-option label="院级" value="院级" />
          </el-select>
        </el-form-item>
        <el-form-item label="颁发机构" prop="issue_org">
          <el-input v-model="honorFormData.issue_org" />
        </el-form-item>
        <el-form-item label="颁发日期" prop="issue_date">
          <el-date-picker v-model="honorFormData.issue_date" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="honorFormData.description" type="textarea" />
        </el-form-item>
        <el-form-item label="附件图片" prop="attachment">
          <el-input v-model="honorFormData.attachment" placeholder="图片URL" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="honorFormVisible=false">取消</el-button>
        <el-button type="primary" @click="submitHonorForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 证书表单弹窗 -->
    <el-dialog v-model="certificateFormVisible" :title="certificateFormMode==='add'?'新增证书':'编辑证书'" width="500px">
      <el-form :model="certificateFormData" label-width="100px">
        <el-form-item label="证书名称" prop="certificate_name">
          <el-input v-model="certificateFormData.certificate_name" />
        </el-form-item>
        <el-form-item label="证书级别" prop="certificate_level">
          <el-select v-model="certificateFormData.certificate_level" placeholder="请选择证书级别" style="width: 100%">
            <el-option label="专业技能相关证书" value="专业技能相关证书" />
            <el-option label="文化能力相关证书" value="文化能力相关证书" />
          </el-select>
        </el-form-item>
        <el-form-item label="颁发机构" prop="issue_org">
          <el-input v-model="certificateFormData.issue_org" />
        </el-form-item>
        <el-form-item label="颁发日期" prop="issue_date">
          <el-date-picker v-model="certificateFormData.issue_date" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="证书编号" prop="certificate_no">
          <el-input v-model="certificateFormData.certificate_no" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="certificateFormData.description" type="textarea" />
        </el-form-item>
        <el-form-item label="附件图片" prop="attachment">
          <el-input v-model="certificateFormData.attachment" placeholder="图片URL" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="certificateFormVisible=false">取消</el-button>
        <el-button type="primary" @click="submitCertificateForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, Search, Delete } from '@element-plus/icons-vue';
import request from '@/utils/request';
import type { FormInstance } from 'element-plus';


// 默认头像，使用相对于项目根目录的路径
const defaultAvatar = new URL('../assets/default-avatar.png', import.meta.url).href;
// 默认证书图片
const defaultCertificate = '/src/assets/default-certificate.png';

// TrainingDirection接口
interface TrainingDirection {
  id?: number;
  name?: string;
  directionId?: number;
  directionName?: string;
  description?: string;
}

// 用户数据接口
interface UserVo {
  user_id: number;
  name: string;
  user_name?: string;
  sex: string;
  role_id?: string | number;
  roleName?: string;
  phone: string;
  positionName?: string;
  position_id?: number;
  position?: string;
  avatar?: string;
  status: string;
  email?: string;
  
  // 教师和学生共有的字段
  training?: TrainingDirection[];
  direction_idNames?: string[];
  
  // 学生特有字段
  student_number?: string;
  grade_year?: string;
  major?: string; // 专业班级字段
  counselor?: string;
  dormitory?: string;
  score?: string;
  
  // 教师特有字段
  office_location?: string;
  title?: string;
  
  // 管理员特有字段
  createTime?: Date;
  createUser?: string;
  updateTime?: Date;
  updateUser?: string;
}

// 职位数据接口
interface Position {
  position_id: number;
  role: string;
  position_name: string;
}

// 荣誉数据接口
interface UserHonor {
  id: number;
  user_id: number;
  honor_name: string;
  honor_level?: string;
  issue_org?: string;
  issue_date?: Date;
  description?: string;
  attachment?: string;
  create_time?: Date;
  create_user?: string;
  update_time?: Date;
  update_user?: string;
}

// 证书数据接口
interface UserCertificate {
  id: number;
  user_id: number;
  certificate_name: string;
  certificate_level?: string;
  issue_org?: string;
  issue_date?: Date;
  certificate_no?: string;
  description?: string;
  attachment?: string;
  create_time?: Date;
  create_user?: string;
  update_time?: Date;
  update_user?: string;
}

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
const positions = ref<Position[]>([]);
const trainingDirections = ref<TrainingDirection[]>([]);

// 详情抽屉控制
const currentUser = ref<UserVo | null>(null);

// 对话框控制
const dialogVisible = ref(false);
const isEditing = ref(false);
const userFormRef = ref<FormInstance | null>(null);
const userForm = reactive({
  user_id: 0,
  name: '',
  sex: '1',
  role: 'student',
  phone: '',
  email: '',
  user_name: '',
  password: '',
  position_id: null as number | null,
  avatar: '',
  status: '1',
  // 学生特有字段
  student_number: '',
  grade_year: '',
  major: '', // 专业班级字段
  counselor: '',
  dormitory: '',
  score: '',
  // 教师特有字段
  office_location: '',
  title: '',
  training: [] as TrainingDirection[],
  // 管理员特有字段
  createTime: null as Date | null,
  createUser: '',
  updateTime: null as Date | null,
  updateUser: ''
});

// 表单验证规则
const userFormRules = {
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 10, message: '长度在 2 到 10 个字符', trigger: 'blur' }
  ],
  user_name: [
    { required: !isEditing.value, message: '请输入账号', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: !isEditing.value, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ],
  position_id: [
    { required: true, message: '请选择职位', trigger: 'change' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
};

// 过滤后的职位列表
const filteredPositions = computed(() => {
  return positions.value.filter(pos => pos.role === userForm.role);
});

// 角色ID到名称的映射表
const roleMap: Record<string | number, string> = {
  '0': '访客',
  '1': '学员',
  '2': '老师',
  '3': '管理员',
  '4': '超级管理员'
};

// 职位ID到名称的映射表（会在加载职位列表后动态生成）
const positionMap = reactive<Record<number, string>>({});

// 过滤后的用户数据
const filteredUsers = computed(() => {
  return users.value.filter(user => {
    // 角色过滤
    if (filterRole.value !== '' && filterRole.value !== undefined) {
      return String(user.role_id) === String(filterRole.value);
    }
    // 标签过滤
    if (activeTab.value !== 'all') {
      let tabRoleId = '';
      if (activeTab.value === 'manager') tabRoleId = '3';
      else if (activeTab.value === 'teacher') tabRoleId = '2';
      else if (activeTab.value === 'student') tabRoleId = '1';
      else if (activeTab.value === 'visitor') tabRoleId = '0';
      return String(user.role_id) === tabRoleId;
    }
    // 状态过滤
    if (filterStatus.value && user.status !== filterStatus.value) {
      return false;
    }
    // 搜索
    if (searchQuery.value) {
      const query = searchQuery.value.toLowerCase();
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
  
  // 获取用户详细信息，只需要传递用户ID
  getUserDetail(row.user_id);
};

// 获取用户详情
const getUserDetail = (userId: number) => {
  loading.value = true;
  
  // 修改为使用查询参数的方式请求
  request.get(`/admin/user/detail`, { params: { user_id: userId } })
    .then(response => {
      const userData = response.data || {};
      
      console.log('用户详情原始数据:', userData);
      console.log('角色ID:', userData.role_id, typeof userData.role_id);
      console.log('学生特有字段:',
        'student_number:', userData.student_number,
        'grade_year:', userData.grade_year,
        'major:', userData.major,
        'counselor:', userData.counselor,
        'dormitory:', userData.dormitory,
        'score:', userData.score
      );
      
      // 根据role_id映射角色名称
      let roleName = '未知';
      const roleId = userData.role_id; // 后端返回的是role_id
      console.log('角色ID:', roleId, typeof roleId);
      
      if (roleId === 0 || roleId === '0') roleName = '访客';
      else if (roleId === 1 || roleId === '1') roleName = '学员';
      else if (roleId === 2 || roleId === '2') roleName = '老师';
      else if (roleId === 3 || roleId === '3') roleName = '管理员';
      else if (roleId === 4 || roleId === '4') roleName = '超级管理员';
      
      // 特殊处理：如果用户ID为1且没有正确识别角色，强制设置为学生
      if (userData.user_id === 1 && (!roleId || roleId === null)) {
        roleName = '学生';
        console.log('特殊处理: 用户ID为1，强制设置角色为学生');
      }
      
      // 如果有学生特有字段，也将角色设为学生
      if (userData.student_number || userData.grade_year || userData.major) {
        roleName = '学生';
        console.log('特殊处理: 检测到学生特有字段，设置角色为学生');
      }
      
      console.log('映射后的角色名称:', roleName);
      
      // 打印原始字段，便于调试
      console.log('所有字段:', Object.entries(userData).map(([key, value]) => `${key}: ${JSON.stringify(value)}`));
      
      // 构建用户对象
      const userDetail: UserVo = {
        user_id: userData.user_id,
        name: userData.name,
        role_id: userData.role_id,
        roleName: roleName,
        sex: userData.sex,
        phone: userData.phone,
        position_id: userData.position_id,
        positionName: userData.position || userData.positionName, // 兼容多种返回字段
        avatar: userData.avatar,
        status: userData.status,
        email: userData.email || '',
        student_number: userData.student_number ?? userData.studentNumber,
        grade_year: userData.grade_year ?? userData.gradeYear,
        major: userData.major || '',
        counselor: userData.counselor,
        dormitory: userData.dormitory,
        score: userData.score,
        office_location: userData.office_location ?? userData.officeLocation,
        title: userData.title,
        direction_idNames: userData.direction_idNames ?? userData.directionNames ?? [],
        createTime: userData.createTime,
        createUser: userData.createUser,
        updateTime: userData.updateTime,
        updateUser: userData.updateUser
      };
      
      console.log('构建的用户详情对象:', userDetail);
      
      currentUser.value = userDetail;
      console.log('处理后的用户详情:', currentUser.value);
      console.log('是否学生:', isStudent(currentUser.value));
      console.log('是否教师:', isTeacher(currentUser.value));
      
      loading.value = false;
    })
    .catch(error => {
      console.error('获取用户详情出错:', error);
      ElMessage.error('获取用户详情失败');
      loading.value = false;
    });
};

// 格式化日期
const formatDate = (date: Date | string | undefined): string => {
  if (!date) return '';
  
  try {
  const d = new Date(date);
  if (isNaN(d.getTime())) return '';
  
    return d.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
      day: '2-digit'
  });
  } catch (e) {
    console.error('日期格式化错误:', e);
    return '';
  }
};

// 获取角色对应的标签类型
const getRoleType = (roleName: string | undefined) => {
  if (!roleName) return '';
  
  switch (roleName.toLowerCase()) {
    case '管理员':
      return 'warning';
    case '老师':
      return 'success';
    case '学生':
      return 'primary';
    case '访客':
      return 'info';
    default:
      return '';
  }
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

// 加载用户数据
const loadUsers = () => {
  loading.value = true;
  
  // 构建查询参数
  const params: Record<string, any> = {
    page: currentPage.value,
    pageSize: pageSize.value,
    name: searchQuery.value || undefined,
    role: filterRole.value || undefined,
    status: filterStatus.value || undefined
  };
  
  // 确保有值的参数被包含在请求中
  Object.keys(params).forEach(key => {
    if (params[key] === undefined || params[key] === '') {
      delete params[key];
    }
  });
  
  console.log('发送的分页参数:', params);
  console.log('当前过滤角色:', filterRole.value);
  
  // 调用后端API获取用户列表
  request.get('/admin/user/list', { params })
    .then(response => {
      console.log('用户列表原始响应:', response);
      
      // 处理后端返回的数据结构
      const data = response.data || {};
      const records = data.records || [];
      
      console.log('后端返回的分页数据:', {
        总记录数: data.total,
        当前页记录数: records.length,
        记录详情: records
      });
      
      // 转换用户数据
      users.value = [];
      
      for (const user of records) {
        // 根据roleId映射角色名称（如果后端未提供）
        let roleName = user.roleName || '未知';
        const roleId = user.role_id || user.roleId; // 同时支持下划线和驼峰
        
        // 如果后端未提供roleName，则前端进行转换
        if (!user.roleName) {
          if (roleId === 0 || roleId === '0') roleName = '访客';
          else if (roleId === 1 || roleId === '1') roleName = '学员';
          else if (roleId === 2 || roleId === '2') roleName = '老师';
          else if (roleId === 3 || roleId === '3') roleName = '管理员';
          else if (roleId === 4 || roleId === '4') roleName = '超级管理员';
        }
        
        // 处理状态字段，确保它是字符串类型
        const userStatus = user.status === null ? '1' : String(user.status);
        
        // 创建用户对象，确保role_id也被正确添加
        users.value.push({
          user_id: user.user_id || user.userId || 0, // 同时支持下划线和驼峰
          name: user.name || '',
          sex: user.sex || '',
          role_id: roleId, // 已经处理过的roleId（支持下划线和驼峰）
          roleName: roleName, // 使用后端返回的roleName或前端转换的结果
          phone: user.phone || '',
          email: user.email || '',
          positionName: user.positionName || user.position || '', // 优先使用后端返回的positionName
          position_id: user.position_id || user.positionId,
          avatar: user.avatar || '',
          status: userStatus,
          student_number: user.student_number || user.studentNumber,
          grade_year: user.grade_year || user.gradeYear,
          major: user.major || '',
          counselor: user.counselor,
          dormitory: user.dormitory,
          score: user.score,
          office_location: user.office_location || user.officeLocation,
          title: user.title,
          direction_idNames: user.direction_idNames || user.directionNames || [],
          createTime: user.createTime,
          createUser: user.createUser,
          updateTime: user.updateTime,
          updateUser: user.updateUser
        });
      }
      
      // 使用后端返回的总记录数
      totalUsers.value = data.total || 0;
      
      // 打印最终的用户列表，用于调试
      console.log('处理后的用户列表:', users.value);
    })
    .catch(error => {
      console.error('获取用户列表出错:', error);
      ElMessage.error('获取用户列表失败');
    })
    .finally(() => {
      loading.value = false;
    });
};

// 加载职位列表
const loadPositions = () => {
  // 不按角色过滤，获取所有职位
  console.log('开始加载所有职位列表');
  
  request.get('/admin/position/list')
    .then(response => {
      console.log('获取职位列表响应:', response);
      positions.value = response.data || [];
      
      // 打印每个职位的详细信息，便于调试
      console.log('职位列表详情:');
      positions.value.forEach(pos => {
        console.log(`职位ID: ${pos.position_id}, 角色: ${pos.role}, 职位名称: ${pos.position_name}`);
      });
      
      // 动态生成职位ID到名称的映射表
      const map: Record<number, string> = {};
      positions.value.forEach(pos => {
        map[pos.position_id] = pos.position_name;
      });
      
      Object.assign(positionMap, map); // 正确的赋值方式，确保响应式更新
      console.log('职位映射表已更新:', positionMap);
    })
    .catch(error => {
      console.error('获取职位列表出错:', error);
    });
};

// 加载培训方向列表
const loadTrainingDirections = () => {
  console.log('开始加载培训方向数据');
  
  request.get('/admin/training-direction/list')
    .then(response => {
      console.log('获取培训方向列表响应:', response);
      
      // 处理后端返回的数据，确保格式一致
      const directions = (response.data || []).map((item: any) => {
        // 标准化字段名称，兼容前后端
        return {
          id: item.id || item.directionId,
          name: item.name || item.directionName,
          directionId: item.directionId || item.id,
          directionName: item.directionName || item.name,
          description: item.description
        };
      });
      
      trainingDirections.value = directions;
      console.log('处理后的培训方向列表:', trainingDirections.value);
    })
    .catch(error => {
      console.error('获取培训方向列表出错:', error);
    });
};

// 搜索处理
const handleSearch = () => {
  // 重置到第一页，但保留所有筛选条件
  currentPage.value = 1;
  
  // 确保filterRole被正确设置
  console.log('搜索时的过滤角色:', filterRole.value);
  
  // 加载用户数据
  loadUsers();
};

// 标签切换
const handleTabChange = (tab: any) => {
  console.log('标签切换，当前标签:', tab.props.name);
  
  // 使用事件参数中的标签名称，而不是依赖activeTab
  const tabName = tab.props.name;
  
  // 将标签名称映射到对应的role_id
  if (tabName === 'all') {
    filterRole.value = '';
  } else if (tabName === 'manager') {
    filterRole.value = '3';
  } else if (tabName === 'teacher') {
    filterRole.value = '2';
  } else if (tabName === 'student') {
    filterRole.value = '1';
  } else if (tabName === 'visitor') {
    filterRole.value = '0';
  }
  
  console.log('设置过滤角色为:', filterRole.value);
  
  // 重置页码并立即加载用户数据
  currentPage.value = 1;
  loadUsers();
};

// 分页处理
const handleSizeChange = (size: number) => {
  pageSize.value = size;
  loadUsers();
};

const handleCurrentChange = (page: number) => {
  currentPage.value = page;
  loadUsers();
};

// 状态变更处理
const handleStatusChange = (user: UserVo) => {
  // 保存原始状态，用于失败时恢复
  const originalStatus = user.status;
  
  request.post('/admin/user/update-status', {
    user_id: user.user_id,
    status: user.status
  })
    .then((response) => {
      // 获取响应数据
      const resp = response.data || {};
      console.log('状态更新响应:', resp); // 添加日志，便于调试
      
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
        
        // 如果当前正在查看该用户的详情，更新详情中的状态
        if (currentUser.value && currentUser.value.user_id === user.user_id) {
          currentUser.value.status = user.status;
        }
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

// 显示新增用户对话框
const showAddUserDialog = () => {
  console.log('打开新增用户对话框，重置前的表单数据:', JSON.stringify(userForm));
  
  // 先将isEditing设为false
  isEditing.value = false;
  
  // 完全重置表单数据
  Object.assign(userForm, {
    user_id: 0,
    name: '',
    sex: '1',
    role: 'student',
    phone: '',
    email: '',
    user_name: '',
    password: '',
    position_id: null,
    avatar: '',
    status: '1',
    // 学生特有字段
    student_number: '',
    grade_year: '',
    major: '',
    counselor: '',
    dormitory: '',
    score: '',
    // 教师特有字段
    office_location: '',
    title: '',
    training: [],
    // 管理员特有字段
    createTime: null,
    createUser: '',
    updateTime: null,
    updateUser: ''
  });
  
  // 重置表单校验
  if (userFormRef.value) {
    userFormRef.value.resetFields();
  }
  
  console.log('重置后的表单数据:', JSON.stringify(userForm));
  
  // 预加载职位和培训方向数据
  loadPositions();
  loadTrainingDirections();
  
  // 打开对话框
  dialogVisible.value = true;
  
  // 使用nextTick确保对话框打开后表单数据已经被重置
  import('vue').then(({ nextTick }) => {
    nextTick(() => {
      console.log('对话框打开后的表单数据:', JSON.stringify(userForm));
      // 再次确保表单被重置
      if (userFormRef.value) {
        userFormRef.value.resetFields();
      }
    });
  });
};

// 处理编辑操作
const handleEdit = (user: UserVo) => {
  console.log('编辑用户:', user);
  
  // 先获取用户的详细信息
  request.get(`/admin/user/detail`, { params: { user_id: user.user_id } })
    .then(response => {
      const userData = response.data || {};
      
      console.log('编辑时获取的用户详情:', userData);
      
      // 重置表单
      resetUserForm();
      
      // 设置正在编辑状态
      isEditing.value = true;
      
      // 根据用户角色设置role字段
      let role = 'student';
      const roleId = userData.role_id;
      if (roleId === 0 || roleId === '0') role = 'visitor';
      else if (roleId === 1 || roleId === '1') role = 'student';
      else if (roleId === 2 || roleId === '2') role = 'teacher';
      else if (roleId === 3 || roleId === '3') role = 'manager';
      else if (roleId === 4 || roleId === '4') role = 'admin';
      
      // 填充表单数据
      Object.assign(userForm, {
        user_id: userData.user_id,
        name: userData.name,
        sex: userData.sex,
        role: role,
        phone: userData.phone,
        email: userData.email === null ? '' : userData.email, // 确保email为空时也有值
        user_name: userData.user_name || '',
        position_id: userData.position_id,
        avatar: userData.avatar,
        status: userData.status,
        student_number: userData.student_number || '',
        grade_year: userData.grade_year || '',
        major: userData.major || '',
        counselor: userData.counselor || '',
        dormitory: userData.dormitory || '',
        score: userData.score || '',
        office_location: userData.office_location || '',
        title: userData.title || '',
        createUser: userData.createUser,
        updateUser: userData.updateUser,
        createTime: userData.createTime,
        updateTime: userData.updateTime
      });
      
      console.log('填充后的表单数据:', userForm);
      console.log('从后端获取的原始email值:', userData.email);
      console.log('填充到表单的email值:', userForm.email);
        
        // 处理培训方向
        if (userData.direction_idNames && userData.direction_idNames.length > 0) {
        console.log('接收到的培训方向:', userData.direction_idNames);
          userForm.training = userData.direction_idNames.map((name: string, index: number) => ({
          id: index + 1,
          name: name,
          directionId: index + 1,
          directionName: name
          }));
        console.log('设置后的培训方向数据:', userForm.training);
      } else {
        userForm.training = [];
        console.log('没有培训方向数据');
      }
      
      // 先加载培训方向数据，再打开对话框，确保数据先准备好
      loadPositions();
      if (role === 'student' || role === 'teacher') {
        loadTrainingDirections();
        }
      
      // 打开编辑对话框
      dialogVisible.value = true;
    })
    .catch(error => {
      console.error('获取编辑用户详情出错:', error);
      ElMessage.error('获取用户详情失败，无法编辑');
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
    request.post('/admin/user/delete', [user.user_id.toString()])
      .then((response) => {
        // 记录完整响应，便于调试
        console.log('删除操作返回数据:', response);
        console.log('响应数据类型:', typeof response);
        
        // 获取响应数据 - 兼容不同的响应结构
        let resp;
        
        // 确定正确的响应数据
        if (response && typeof response === 'object' && 'code' in response) {
          resp = response;
          console.log('直接使用response作为resp');
        } else {
          // axios的response.data才是真正的后端响应
          resp = response.data || {};
          console.log('使用response.data作为resp');
        }
        
        console.log('响应code:', resp.code);
        console.log('响应data:', resp.data);
        console.log('响应msg:', resp.msg);
        
        // 使用字符串比较，确保兼容数字和字符串类型的状态码
        const codeStr = String(resp.code);
        if (codeStr === '200' || codeStr === '1') {
          ElMessage({
            type: 'success',
            message: '删除成功'
          });
          
          loadUsers(); // 重新加载数据
        } else {
          const errorMsg = resp.msg || '删除失败';
          ElMessage.error(errorMsg);
        }
      })
      .catch(error => {
        console.error('删除用户出错:', error);
        ElMessage.error('删除失败');
      });
  }).catch(() => {
    // 取消删除
  });
};

// 关闭对话框
const handleDialogClose = () => {
  dialogVisible.value = false;
  resetUserForm();
};

// 角色变更处理
const handleRoleChange = () => {
  // 当角色变更时，重置职位选择
  userForm.position_id = null;
  
  // 加载职位列表
  loadPositions();
  
  // 如果是学生或老师角色，加载培训方向
  if (userForm.role === 'student' || userForm.role === 'teacher') {
    loadTrainingDirections();
    console.log('加载培训方向数据，当前角色:', userForm.role);
  } else {
    // 清空培训方向
    userForm.training = [];
  }
};

// 重置表单
const resetUserForm = () => {
  console.log('开始重置表单');
  
  // 完全重置所有字段
  Object.assign(userForm, {
    user_id: 0,
    name: '',
    sex: '1',
    role: 'student',
    phone: '',
    email: '',
    user_name: '',
    password: '',
    position_id: null,
    avatar: '',
    status: '1',
    // 学生特有字段
    student_number: '',
    grade_year: '',
    major: '',
    counselor: '',
    dormitory: '',
    score: '',
    // 教师特有字段
    office_location: '',
    title: '',
    training: [],
    // 管理员特有字段
    createTime: null,
    createUser: '',
    updateTime: null,
    updateUser: ''
  });
  
  // 重置表单校验
  if (userFormRef.value) {
    userFormRef.value.resetFields();
    console.log('表单校验已重置');
  }
  
  console.log('表单重置完成，当前表单数据:', JSON.stringify(userForm));
};

// 保存用户
const handleSaveUser = () => {
  if (!userFormRef.value) return;
  
  userFormRef.value.validate((valid: boolean) => {
    if (valid) {
      // 将前端角色名称映射为后端角色ID
      let roleId = '';
      
      switch (userForm.role) {
        case 'visitor':
          roleId = '0';
          break;
        case 'student':
          roleId = '1';
          break;
        case 'teacher':
          roleId = '2';
          break;
        case 'manager':
          roleId = '3';
          break;
        case 'admin':
          roleId = '4';
          break;
      }
      
      // 确保email字段有值，即使是空字符串
      const emailValue = userForm.email === undefined ? '' : userForm.email;
      
      // 构造提交数据 - 按照后端UserDto的结构
      const userData: any = {
        user_id: isEditing.value ? userForm.user_id : undefined,
        name: userForm.name,
        sex: userForm.sex,
        roleId: roleId, // 使用roleId字段
        role_id: roleId, // 同时也设置role_id字段，确保后端处理正确
        phone: userForm.phone,
        position_id: userForm.position_id,
        email: emailValue, // 使用处理后的email值
        status: userForm.status,
        avatar: userForm.avatar || '' // 添加头像字段
      };
      
      // 移除可能导致类型转换错误的字段
      delete userData.createTime;
      delete userData.updateTime;
      delete userData.createUser;
      delete userData.updateUser; // 确保不发送updateUser字段
      
      // 打印提交前的表单数据，特别关注email字段
      console.log('提交前表单数据:', {...userForm});
      console.log('Email字段值:', userForm.email);
      console.log('Email字段类型:', typeof userForm.email);
      console.log('email字段长度:', userForm.email ? userForm.email.length : 0);
      
      // 只在新增用户时添加用户名和密码
      if (!isEditing.value) {
        userData.user_name = userForm.user_name;
        userData.password = userForm.password;
      }
      
      // 根据角色添加特有字段
      if (userForm.role === 'student') {
        Object.assign(userData, {
          student_number: userForm.student_number,
          grade_year: userForm.grade_year,
          major: userForm.major,
          counselor: userForm.counselor,
          dormitory: userForm.dormitory,
          score: userForm.score
        });
      } else if (userForm.role === 'teacher') {
        Object.assign(userData, {
          office_location: userForm.office_location,
          title: userForm.title
        });
      }
      
      // 处理培训方向 - 对学生和教师都适用
      if (['student', 'teacher'].includes(userForm.role) && userForm.training.length > 0) {
        // 提取培训方向名称并去除重复项
        const trainingSet = new Set();
        const trainingNames = userForm.training
          .map((item: any) => item.name || item.directionName)
          .filter((name: string) => {
            if (!name) return false;
            // 如果名称已存在，则跳过
            if (trainingSet.has(name)) return false;
            // 否则添加到Set中并返回true
            trainingSet.add(name);
            return true;
          });
        
        console.log('去重后的培训方向:', trainingNames);
        
        // 添加到userData中
        userData.directionNames = trainingNames;
        userData.trainingNames = trainingNames; // 兼容不同的字段名称
      }
      
      console.log('提交的用户数据:', userData);
      
      // 特别追踪email字段值
      console.log('最终提交的email字段值:', userData.email);
      console.log('email字段类型:', typeof userData.email);
      console.log('email字段长度:', userData.email ? userData.email.length : 0);
      
      // 发送请求
      const url = isEditing.value ? '/admin/user/update' : '/admin/user/add';
      request.post(url, userData)
        .then((response) => {
          console.log('保存用户响应:', response);
          console.log('响应类型:', typeof response);
          console.log('响应对象详情:', JSON.stringify(response));
          
          // 获取响应数据 - 兼容不同的响应结构
          let resp;
          
          // 确定正确的响应数据
          if (response && typeof response === 'object' && 'code' in response) {
            resp = response;
            console.log('直接使用response作为resp');
          } else {
            // axios的response.data才是真正的后端响应
            resp = response.data || {};
            console.log('使用response.data作为resp');
          }
          
          // 输出响应的详细信息用于调试
          console.log('响应code:', resp.code);
          console.log('响应data:', resp.data);
          console.log('响应msg:', resp.msg);
          
          // 使用统一的成功条件判断 - 将code转换为字符串进行比较
          const codeStr = String(resp.code);
          let isSuccess = false;
          let successMessage = '';
          
          // 主要判断code是否为1或200
          if (codeStr === '1' || codeStr === '200') {
            isSuccess = true;
            successMessage = resp.msg || (isEditing.value ? '更新成功' : '添加成功');
          } 
          // 其他情况：检查响应数据中是否包含"成功"字样
          else if (resp.data && typeof resp.data === 'string' && resp.data.includes('成功')) {
            isSuccess = true;
            successMessage = resp.data;
          } else if (resp.msg && resp.msg.includes('成功')) {
            isSuccess = true;
            successMessage = resp.msg;
          }
          
          if (isSuccess) {
            ElMessage({
              type: 'success',
              message: successMessage
            });
            dialogVisible.value = false;
            resetUserForm();
            loadUsers(); // 重新加载数据
          } else {
            // 显示错误信息
            const errorMsg = (resp.data && resp.data.msg) || 
                             resp.msg || 
                             '操作失败';
            ElMessage.error(errorMsg);
          }
        })
        .catch(error => {
          console.error(isEditing.value ? '更新用户出错:' : '添加用户出错:', error);
          // 获取后端返回的错误信息
          let errorMsg = '操作失败';
          
          // 尝试从不同可能的位置获取错误信息
          if (error.response && error.response.data) {
            // 直接使用后端返回的错误信息（优先）
            errorMsg = error.response.data.msg || errorMsg;
          } else if (error.message) {
            // 如果没有返回data.msg，则尝试使用错误的message属性
            errorMsg = error.message;
          }
          
          // 显示错误信息
          ElMessage({
            type: 'error',
            message: errorMsg,
            duration: 5000  // 延长显示时间，让用户能看清错误信息
          });
        });
    } else {
      ElMessage({
        type: 'error',
        message: '请检查表单填写是否正确'
      });
    }
  });
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
      userForm.training = uniqueDirections;
      console.log('去除重复项后:', userForm.training);
      
      // 提示用户
      ElMessage({
        type: 'warning',
        message: '已自动去除重复的培训方向'
      });
    }
  }
  
  // 如果超过3个，只保留前3个
  if (userForm.training.length > 3) {
    userForm.training = userForm.training.slice(0, 3);
    console.log('超过3个，截取后:', userForm.training);
    
    // 提示用户
    ElMessage({
      type: 'warning',
      message: '最多只能选择3个培训方向'
    });
  }
};

// 显示用户荣誉
const showUserHonors = (user: UserVo) => {
  ElMessage.info(`正在为 ${user.name} 加载荣誉信息...`);
  
  // 设置加载状态
  loading.value = true;
  
  // 调用后端API获取用户荣誉信息
  request.get(`/admin/user-achievement/honors?userId=${user.user_id}`)
    .then(response => {
      console.log('获取用户荣誉信息响应:', response);
      console.log('响应状态码:', response.status);
      
      // 获取并处理响应数据 - 兼容不同的响应结构
      let responseData;
      
      // 详细记录响应结构，以便调试
      console.log('响应类型:', typeof response);
      console.log('响应是否有data属性:', 'data' in response);
      console.log('响应完整结构:', JSON.stringify(response));
      
      // 确定正确的响应数据
      if (response && typeof response === 'object' && 'code' in response) {
        const respCode = String(response['code']);
        if (respCode === '200' || respCode === '1') {
          responseData = response;
          console.log('直接使用response作为responseData');
        } else {
          responseData = response.data || {};
          console.log('使用response.data作为responseData');
        }
      } else {
        // 否则从response.data中获取
        responseData = response.data || {};
        console.log('从response.data中获取responseData');
      }
      
      console.log('处理后的responseData:', responseData);
      console.log('API返回code:', responseData.code);
      
      // 判断API调用是否成功 - 将code转换为字符串进行比较
      const codeStr = String(responseData.code);
      if (codeStr === '200' || codeStr === '1') {
        // 详细记录data结构
        console.log('API data字段:', responseData.data);
        
        // 处理后端返回的荣誉数据
        const honorsData = responseData.data || [];
        try {
          if (Array.isArray(honorsData)) {
            console.log('荣誉数据是数组，长度:', honorsData.length);
            userHonors.value = honorsData.map((item: any) => ({
              id: item.id || item.honors_id,
              user_id: user.user_id,
              honor_name: item.honor_name || '未命名荣誉',
              honor_level: item.honor_level || '未分类',
              issue_org: item.issue_org || '未知机构',
              issue_date: item.issue_date ? new Date(item.issue_date) : undefined,
              description: item.description || '',
              attachment: item.attachment || undefined,
              create_time: item.create_time ? new Date(item.create_time) : undefined,
              create_user: item.create_user || '',
              update_time: item.update_time ? new Date(item.update_time) : undefined,
              update_user: item.update_user || ''
            }));
            
            console.log('处理后的荣誉数据:', userHonors.value);
            if (userHonors.value.length === 0) {
              ElMessage.info('该用户暂无荣誉信息');
            } else {
              ElMessage.success('荣誉信息加载成功');
            }
          } else {
            console.error('荣誉数据不是数组:', honorsData);
            userHonors.value = [];
            ElMessage.warning('获取的荣誉数据格式不正确');
          }
        } catch (error) {
          console.error('处理荣誉数据时出错:', error);
          userHonors.value = [];
          ElMessage.error('处理荣誉数据时出错');
        }
      } else {
        // API调用失败
        ElMessage.error(responseData.msg || '获取荣誉信息失败');
        console.error('获取荣誉信息失败 (API调用失败):', responseData);
      }
    })
    .catch(error => {
      console.error('获取用户荣誉信息出错 (网络错误):', error);
      ElMessage.error('获取荣誉信息失败，请重试');
      
      // 当后端API不可用时，使用模拟数据（开发阶段）
    userHonors.value = [
      {
        id: 1,
        user_id: user.user_id,
          honor_name: '优秀学员奖（模拟数据）',
        honor_level: '校级',
        issue_org: '何湘技能大师工作室',
        issue_date: new Date('2025-03-15'),
        description: '在2025年春季学期表现突出，获得优秀学员称号',
        create_time: new Date('2025-06-08 16:59:32'),
        create_user: 'admin',
        update_time: new Date('2025-06-08 16:59:32'),
        update_user: 'admin'
      },
      {
        id: 2,
        user_id: user.user_id,
          honor_name: '技能大赛金奖（模拟数据）',
        honor_level: '国家级',
        issue_org: '中国技能大赛组委会',
        issue_date: new Date('2025-06-01'),
        description: '在2025年全国技能大赛中获得金奖',
        create_time: new Date('2025-06-08 16:59:32'),
        create_user: 'admin',
        update_time: new Date('2025-06-08 16:59:32'),
        update_user: 'admin'
      }
    ];
    
      ElMessage.warning('无法连接服务器，显示模拟数据');
    })
    .finally(() => {
      loading.value = false;
    });
};

// 用户荣誉数据
const userHonors = ref<UserHonor[]>([]);

// 根据荣誉级别获取对应的标签类型
const getHonorLevelType = (level: string | undefined): string => {
  if (!level) return 'info';
  
  switch (level) {
    case '国家级':
      return 'danger';
    case '省/市级':
      return 'warning';
    case '校级':
      return 'success';
    case '院级':
      return 'primary';
    default:
      return 'info';
  }
};

// 获取证书级别对应的标签类型
const getCertificateLevelType = (level: string | undefined): string => {
  if (!level) return 'info';
  
  switch (level) {
    case '专业技能相关证书':
      return 'danger';
    case '文化能力相关证书':
      return 'success';
    default:
      return 'info';
  }
};

// 获取证书标题
const getCertificateTitle = () => {
  if (!selectedCertificate.value) return '';
  
  if ('certificate_name' in selectedCertificate.value) {
    return (selectedCertificate.value as UserCertificate).certificate_name;
  } else {
    return (selectedCertificate.value as UserHonor).honor_name;
  }
};

// 检查是否有证书编号
const hasCertificateNumber = () => {
  if (!selectedCertificate.value) return false;
  
  return 'certificate_no' in selectedCertificate.value && 
    !!(selectedCertificate.value as UserCertificate).certificate_no;
};

// 获取证书编号
const getCertificateNumber = () => {
  if (!selectedCertificate.value || !hasCertificateNumber()) return '';
  
  return (selectedCertificate.value as UserCertificate).certificate_no;
};

// 荣誉展示对话框控制
const honorsDialogVisible = ref(false);
const selectedUserHonors = ref<UserHonor[]>([]);
const selectedUserCertificates = ref<UserCertificate[]>([]);
const selectedUser = ref<UserVo | null>(null);

// 证书预览对话框控制
const certificatePreviewVisible = ref(false);
const selectedCertificate = ref<UserHonor | UserCertificate | null>(null);

// 显示所有用户荣誉（弹出对话框）
const showAllUserHonors = (user: UserVo) => {
  if (!user) return;
  
  selectedUser.value = user;
  ElMessage.info(`正在为 ${user.name} 加载荣誉与证书信息...`);
  
  // 设置加载状态
  loading.value = true;
  
  // 请求荣誉信息 - 使用API路径
  request({
    url: `/admin/user-achievement/all`,
    method: 'get',
    params: { userId: user.user_id }
  })
    .then(response => {
      console.log('获取用户荣誉与证书信息响应 (完整):', response);
      console.log('响应状态码:', response.status);
      
      // 获取并处理响应数据 - axios返回的response对象
      // 如果后端已通过axios拦截器处理，则response就是后端返回的数据
      // 所以这里可能需要兼容两种情况
      let responseData;
      
      // 详细记录响应结构，以便调试
      console.log('响应类型:', typeof response);
      console.log('响应是否有data属性:', response.hasOwnProperty('data'));
      console.log('响应完整结构:', JSON.stringify(response));
      
      // 如果response已经是后端返回的数据（经过axios拦截器处理）
      if (response && typeof response === 'object' && 'code' in response) {
        const respCode = String(response['code']);
        if (respCode === '200') {
          responseData = response;
          console.log('直接使用response作为responseData');
        } else {
          responseData = response.data || {};
          console.log('使用response.data作为responseData');
        }
      } else {
        // 否则从response.data中获取
        responseData = response.data || {};
        console.log('从response.data中获取responseData');
      }
      
      console.log('处理后的responseData:', responseData);
      console.log('API返回code:', responseData.code);
      
      // 判断API调用是否成功 - 将code转换为字符串进行比较
      const codeStr = String(responseData.code);
      if (codeStr === '200') {
        // 详细记录data结构
        console.log('API data字段:', responseData.data);
        if (responseData.data) {
          console.log('API data.honors字段:', responseData.data.honors);
          console.log('API data.certificates字段:', responseData.data.certificates);
        } else {
          console.error('API响应缺少data字段或data字段为空');
          ElMessage.error('获取荣誉信息失败: 响应数据为空');
          loading.value = false;
          return;
        }
        
        // 处理后端返回的荣誉数据
        const honorsData = responseData.data.honors || [];
        try {
          if (Array.isArray(honorsData)) {
            console.log('荣誉数据是数组，长度:', honorsData.length);
            selectedUserHonors.value = honorsData.map((item: any) => ({
              id: item.id || item.honors_id,
              user_id: user.user_id,
              honor_name: item.honor_name || '未命名荣誉',
              honor_level: item.honor_level || '未分类',
              issue_org: item.issue_org || '未知机构',
              issue_date: item.issue_date ? new Date(item.issue_date) : undefined,
              description: item.description || '',
              attachment: item.attachment || undefined,
              create_time: item.create_time ? new Date(item.create_time) : undefined,
              create_user: item.create_user || '',
              update_time: item.update_time ? new Date(item.update_time) : undefined,
              update_user: item.update_user || ''
            }));
          } else {
            console.error('荣誉数据不是数组:', honorsData);
            selectedUserHonors.value = [];
          }
        } catch (error) {
          console.error('处理荣誉数据时出错:', error);
          selectedUserHonors.value = [];
        }
        
        // 处理后端返回的证书数据
        const certificatesData = responseData.data.certificates || [];
        try {
          if (Array.isArray(certificatesData)) {
            console.log('证书数据是数组，长度:', certificatesData.length);
            selectedUserCertificates.value = certificatesData.map((item: any) => ({
              id: item.id || item.certificate_id,
              user_id: user.user_id,
              certificate_name: item.certificate_name || '未命名证书',
              certificate_level: item.certificate_level || item.certificate_type || '未分类',
              issue_org: item.issue_org || '未知机构',
              issue_date: item.issue_date ? new Date(item.issue_date) : undefined,
              certificate_no: item.certificate_no || '',
              description: item.description || '',
              attachment: item.attachment || undefined,
              create_time: item.create_time ? new Date(item.create_time) : undefined,
              create_user: item.create_user || '',
              update_time: item.update_time ? new Date(item.update_time) : undefined,
              update_user: item.update_user || ''
            }));
          } else {
            console.error('证书数据不是数组:', certificatesData);
            selectedUserCertificates.value = [];
          }
        } catch (error) {
          console.error('处理证书数据时出错:', error);
          selectedUserCertificates.value = [];
        }
        
        // 即使荣誉或证书数据为空，也打开对话框显示
        honorsDialogVisible.value = true;
        if (selectedUserHonors.value.length === 0 && selectedUserCertificates.value.length === 0) {
          ElMessage.info('该用户暂无荣誉和证书信息');
        } else {
          ElMessage.success('荣誉与证书信息加载成功');
        }
      } else {
        // API调用失败
        ElMessage.error(responseData.msg || '获取荣誉信息失败');
        console.error('获取荣誉信息失败:', responseData);
      }
    })
    .catch(error => {
      console.error('获取用户荣誉信息出错:', error);
      ElMessage.error('获取荣誉信息失败，请重试');
      
      // 当后端API不可用时，使用模拟数据（开发阶段）
    selectedUserHonors.value = [
      {
        id: 1,
        user_id: user.user_id,
          honor_name: '优秀学员奖（模拟数据）',
        honor_level: '校级',
        issue_org: '何湘技能大师工作室',
        issue_date: new Date('2025-03-15'),
        description: '在2025年春季学期表现突出，获得优秀学员称号',
        attachment: '/certificates/cert001.jpg',
        create_time: new Date('2025-06-08 16:59:32'),
        create_user: 'admin',
        update_time: new Date('2025-06-08 16:59:32'),
        update_user: 'admin'
      },
      {
        id: 2,
        user_id: user.user_id,
          honor_name: '技能大赛金奖（模拟数据）',
        honor_level: '国家级',
        issue_org: '中国技能大赛组委会',
        issue_date: new Date('2025-06-01'),
        description: '在2025年全国技能大赛中获得金奖',
        attachment: '/certificates/cert002.jpg',
        create_time: new Date('2025-06-08 16:59:32'),
        create_user: 'admin',
        update_time: new Date('2025-06-08 16:59:32'),
        update_user: 'admin'
      }
    ];
    
    // 模拟证书数据
    selectedUserCertificates.value = [
      {
        id: 1,
        user_id: user.user_id,
          certificate_name: '计算机二级证书（模拟数据）',
        certificate_level: '国家级',
        issue_org: '教育部考试中心',
        issue_date: new Date('2025-07-10'),
        certificate_no: 'HN2025089',
        description: '全国计算机等级考试二级证书',
        attachment: '/certificates/cert003.jpg',
        create_time: new Date('2025-07-12 10:20:32'),
        create_user: 'admin',
        update_time: new Date('2025-07-12 10:20:32'),
        update_user: 'admin'
      },
      {
        id: 2,
        user_id: user.user_id,
          certificate_name: '英语四级证书（模拟数据）',
        certificate_level: '国家级',
        issue_org: '教育部考试中心',
        issue_date: new Date('2025-05-20'),
        certificate_no: 'CX2025042',
        description: '大学英语四级考试合格证书',
        attachment: '/certificates/cert004.jpg',
        create_time: new Date('2025-05-22 14:30:00'),
        create_user: 'admin',
        update_time: new Date('2025-05-22 14:30:00'),
        update_user: 'admin'
      }
    ];
    
      // 打开荣誉对话框（显示模拟数据）
    honorsDialogVisible.value = true;
      ElMessage.warning('无法连接服务器，显示模拟数据');
    })
    .finally(() => {
      loading.value = false;
    });
};

// 关闭荣誉对话框
const handleHonorsDialogClose = () => {
  honorsDialogVisible.value = false;
  selectedUserHonors.value = [];
  selectedUserCertificates.value = [];
};

// 预览荣誉证书
const previewHonorCertificate = (honor: UserHonor) => {
  loading.value = true;
  // 获取荣誉详情
  request({
    url: `/admin/user-achievement/honors/detail/${honor.id || honor.honors_id}`,
    method: 'get'
  })
    .then((res: any) => {
      if (String(res.code) === '200') {
        // 使用后端返回的详细数据
        selectedCertificate.value = res.data;
      } else {
        // 如果API失败，使用传入的数据
        selectedCertificate.value = honor;
        ElMessage.warning('获取详细信息失败，显示基本信息');
      }
    })
    .catch(() => {
      // 如果API不可用，使用传入的数据
      selectedCertificate.value = honor;
      ElMessage.warning('获取详细信息失败，显示基本信息');
    })
    .finally(() => {
      loading.value = false;
      certificatePreviewVisible.value = true;
    });
};

// 预览证书
const previewCertificate = (certificate: UserCertificate) => {
  loading.value = true;
  // 获取证书详情
  request({
    url: `/admin/user-achievement/certificates/detail/${certificate.id || certificate.certificate_id}`,
    method: 'get'
  })
    .then((res: any) => {
      if (String(res.code) === '200') {
        // 使用后端返回的详细数据
        selectedCertificate.value = res.data;
      } else {
        // 如果API失败，使用传入的数据
        selectedCertificate.value = certificate;
        ElMessage.warning('获取详细信息失败，显示基本信息');
      }
    })
    .catch(() => {
      // 如果API不可用，使用传入的数据
      selectedCertificate.value = certificate;
      ElMessage.warning('获取详细信息失败，显示基本信息');
    })
    .finally(() => {
      loading.value = false;
      certificatePreviewVisible.value = true;
    });
};

// 关闭证书预览对话框
const handleCertificatePreviewClose = () => {
  certificatePreviewVisible.value = false;
  selectedCertificate.value = null;
};

// 加载初始数据
onMounted(() => {
  // 先加载职位列表，再加载用户列表
  loadPositions();
  // 加载用户列表数据
  loadUsers();
});

// 头像上传成功处理
const handleAvatarSuccess = (res: any, file: File) => {
  if (res.code === 200) {
    userForm.avatar = res.data; // 后端返回的图片URL
    ElMessage.success('头像上传成功');
  } else {
    ElMessage.error('头像上传失败');
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

// 处理表格多选
const handleSelectionChange = (selection: UserVo[]) => {
  selectedUsers.value = selection;
};

// 批量删除用户
const handleBatchDelete = () => {
  if (selectedUsers.value.length === 0) {
    ElMessage.warning('请先选择要删除的用户');
    return;
  }
  
  const userNames = selectedUsers.value.map(user => user.name).join('、');
  const userIds = selectedUsers.value.map(user => user.user_id.toString());
  
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
        // 获取响应数据 - 兼容不同的响应结构
        let resp;
        
        console.log('批量删除操作返回数据:', response);
        console.log('响应数据类型:', typeof response);
        
        // 确定正确的响应数据
        if (response && typeof response === 'object' && 'code' in response) {
          resp = response;
          console.log('直接使用response作为resp');
        } else {
          // axios的response.data才是真正的后端响应
          resp = response.data || {};
          console.log('使用response.data作为resp');
        }
        
        console.log('批量删除响应:', resp);
        
        // 使用字符串比较，确保兼容数字和字符串类型的状态码
        const codeStr = String(resp.code);
        if (codeStr === '200' || codeStr === '1') {
          ElMessage({
            type: 'success',
            message: `已成功删除 ${selectedUsers.value.length} 个用户`
          });
          
          selectedUsers.value = []; // 清空选中项
          loadUsers(); // 重新加载数据
        } else {
          const errorMsg = resp.msg || '批量删除失败';
          ElMessage.error(errorMsg);
        }
      })
      .catch(error => {
        console.error('批量删除用户出错:', error);
        ElMessage.error('批量删除失败，请重试');
      });
  }).catch(() => {
    // 用户取消删除操作
  });
};

// 添加一个辅助函数，根据角色返回默认职位名称
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

// 判断是否为学生
const isStudent = (user: UserVo | null): boolean => {
  return user?.role_id === '1' || user?.role_id === 1;
};

// 判断是否为教师
const isTeacher = (user: UserVo | null): boolean => {
  return user?.role_id === '2' || user?.role_id === 2;
};

// 荣誉表单弹窗控制
const honorFormVisible = ref(false);
const honorFormMode = ref<'add'|'edit'>('add');
const honorFormData = ref<any>({});

// 打开新增/编辑荣誉表单
const openHonorForm = (mode: 'add'|'edit', row?: any) => {
  honorFormMode.value = mode;
  if (mode === 'edit' && row) {
    // 编辑模式下，先获取详细信息
    loading.value = true;
    request({
      url: `/admin/user-achievement/honors/detail/${row.id || row.honors_id}`,
      method: 'get'
    })
      .then((res: any) => {
        if (String(res.code) === '200') {
          // 使用后端返回的详细数据
          honorFormData.value = { ...res.data };
          honorFormVisible.value = true;
        } else {
          ElMessage.error(res.msg || '获取荣誉详情失败');
        }
      })
      .catch(() => {
        ElMessage.error('获取荣誉详情失败');
        // 如果API不可用，仍然使用表格中的数据
        honorFormData.value = { ...row };
        honorFormVisible.value = true;
      })
      .finally(() => {
        loading.value = false;
      });
  } else {
    // 新增模式
    honorFormData.value = {
      honor_name: '',
      honor_level: '',
      issue_org: '',
      issue_date: '',
      description: '',
      attachment: ''
    };
    honorFormVisible.value = true;
  }
};

// 提交荣誉表单
const submitHonorForm = () => {
  if (!selectedUser.value) return;
  const isEdit = honorFormMode.value === 'edit';
  const url = '/admin/user-achievement/honors';
  const method = isEdit ? 'put' : 'post'; // 新增用post，编辑用put
  const data = {
    ...honorFormData.value,
    user_id: selectedUser.value.user_id,
    honors_id: honorFormData.value.id || honorFormData.value.honors_id
  };
  request({ url, method, data })
    .then((res: any) => {
      // 直接使用res而不是res.data
      if (String(res.code) === '200' ) {
        ElMessage.success(isEdit ? '编辑成功' : '新增成功');
        honorFormVisible.value = false;
        showAllUserHonors(selectedUser.value!);
      } else {
        ElMessage.error(res.msg || '操作失败');
      }
    })
    .catch(() => ElMessage.error('操作失败'));
};

// 删除荣誉
const deleteHonor = (row: any) => {
  ElMessageBox.confirm('确定要删除该荣誉吗？', '提示', { type: 'warning' })
    .then(() => {
      request({ url: `/admin/user-achievement/honors/${row.id || row.honors_id}`, method: 'delete' })
        .then((res: any) => {
          // 直接使用res而不是res.data
          if (String(res.code) === '200' ) {
            ElMessage.success('删除成功');
            showAllUserHonors(selectedUser.value!);
          } else {
            ElMessage.error(res.msg || '删除失败');
          }
        })
        .catch(() => ElMessage.error('删除失败'));
    });
};

// 证书表单弹窗控制
const certificateFormVisible = ref(false);
const certificateFormMode = ref<'add'|'edit'>('add');
const certificateFormData = ref<any>({});

// 打开新增/编辑证书表单
const openCertificateForm = (mode: 'add'|'edit', row?: any) => {
  certificateFormMode.value = mode;
  if (mode === 'edit' && row) {
    // 编辑模式下，先获取详细信息
    loading.value = true;
    request({
      url: `/admin/user-achievement/certificates/detail/${row.id || row.certificate_id}`,
      method: 'get'
    })
      .then((res: any) => {
        if (String(res.code) === '200') {
          // 使用后端返回的详细数据
          certificateFormData.value = { ...res.data };
          certificateFormVisible.value = true;
        } else {
          ElMessage.error(res.msg || '获取证书详情失败');
        }
      })
      .catch(() => {
        ElMessage.error('获取证书详情失败');
        // 如果API不可用，仍然使用表格中的数据
        certificateFormData.value = { ...row };
        certificateFormVisible.value = true;
      })
      .finally(() => {
        loading.value = false;
      });
  } else {
    // 新增模式
    certificateFormData.value = {
      certificate_name: '',
      certificate_level: '',
      issue_org: '',
      issue_date: '',
      description: '',
      attachment: ''
    };
    certificateFormVisible.value = true;
  }
};

// 提交证书表单
const submitCertificateForm = () => {
  if (!selectedUser.value) return;
  const isEdit = certificateFormMode.value === 'edit';
  const url = '/admin/user-achievement/certificates';
  const method = isEdit ? 'put' : 'post'; // 新增用post，编辑用put
  const data = {
    ...certificateFormData.value,
    user_id: selectedUser.value.user_id,
    certificate_id: certificateFormData.value.id || certificateFormData.value.certificate_id
  };
  request({ url, method, data })
    .then((res: any) => {
      // 直接使用res而不是res.data
      if (String(res.code) === '200' ) {
        ElMessage.success(isEdit ? '编辑成功' : '新增成功');
        certificateFormVisible.value = false;
        showAllUserCertificates(selectedUser.value!);
      } else {
        ElMessage.error(res.msg || '操作失败');
      }
    })
    .catch(() => ElMessage.error('操作失败'));
};

// 删除证书
const deleteCertificate = (row: any) => {
  ElMessageBox.confirm('确定要删除该证书吗？', '提示', { type: 'warning' })
    .then(() => {
      request({ url: `/admin/user-achievement/certificates/${row.id || row.certificate_id}`, method: 'delete' })
        .then((res: any) => {
          // 直接使用res而不是res.data
          if (String(res.code) === '200' ) {
            ElMessage.success('删除成功');
            showAllUserCertificates(selectedUser.value!);
          } else {
            ElMessage.error(res.msg || '删除失败');
          }
        })
        .catch(() => ElMessage.error('删除失败'));
    });
};

// 显示所有用户证书（弹出对话框）
const showAllUserCertificates = (user: UserVo) => {
  if (!user) return;
  
  selectedUser.value = user;
  ElMessage.info(`正在为 ${user.name} 加载证书信息...`);
  
  // 设置加载状态
  loading.value = true;
  
  // 请求证书信息
  request({
    url: `/admin/user-achievement/certificates`,
    method: 'get',
    params: { userId: user.user_id }
  })
    .then((res: any) => {
      // 直接使用res而不是res.data
      if (String(res.code) === '200') {
        selectedUserCertificates.value = res.data || [];
        honorsDialogVisible.value = true;
        if (selectedUserCertificates.value.length === 0) {
          ElMessage.info('该用户暂无证书信息');
        } else {
          ElMessage.success('证书信息加载成功');
        }
      } else {
        ElMessage.error(res.msg || '获取证书信息失败');
      }
    })
    .catch(error => {
      console.error('获取用户证书信息出错:', error);
      ElMessage.error('获取证书信息失败，请重试');
      
      // 当后端API不可用时，使用模拟数据（开发阶段）
      selectedUserCertificates.value = [
        {
          id: 1,
          user_id: user.user_id,
          certificate_name: '计算机二级证书（模拟数据）',
          certificate_level: '国家级',
          issue_org: '教育部考试中心',
          issue_date: new Date('2025-07-10'),
          certificate_no: 'HN2025089',
          description: '全国计算机等级考试二级证书',
          attachment: '/certificates/cert003.jpg',
          create_time: new Date('2025-07-12 10:20:32'),
          create_user: 'admin',
          update_time: new Date('2025-07-12 10:20:32'),
          update_user: 'admin'
        }
      ];
      
      honorsDialogVisible.value = true;
      ElMessage.warning('无法连接服务器，显示模拟数据');
    })
    .finally(() => {
      loading.value = false;
    });
};
</script>

<style lang="scss" scoped>
.employees-view {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
  
  .page-header-card {
  margin-bottom: 10px;
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
  
  .search-card {
  margin-bottom: 10px;
}

    .search-container {
      display: flex;
  gap: 16px;
      align-items: center;
}
      
      .search-input {
        width: 300px;
      }
      
      .filter-container {
        display: flex;
  gap: 16px;
  margin-left: auto;
}

.user-tabs {
  margin-bottom: 10px;
        }

/* 新增主内容布局样式 */
.main-content {
  display: flex;
  gap: 20px;
  width: 100%;
  overflow-x: auto;
  margin-top: -5px;
}
        
.table-card {
  flex: 1;
  min-width: 900px;
}

.table-card :deep(.el-table) {
  --el-table-border-color: #e0e3e9;
  --el-table-header-background-color: #f5f7fa;
  --el-table-row-hover-background-color: #ecf5ff;
  font-size: 14px;
}

.table-card :deep(.el-table th) {
  font-weight: 600;
  font-size: 14px;
}

/* 表格边框和阴影 */
.table-card :deep(.el-table) {
  border-radius: 4px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

/* 调整表格行高和间距 */
.table-card :deep(.el-table__row) {
  height: 55px;
}
  
/* 让表格内容充分展示 */
.table-card :deep(.el-table__inner-wrapper) {
  min-width: 100%;
}

/* 表格单元格内容设置 */
.table-card :deep(.el-table .cell) {
  padding-left: 12px;
  padding-right: 12px;
  line-height: 22px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 操作按钮间距 */
.table-card :deep(.el-table .el-button+.el-button) {
  margin-left: 10px;
}

/* 调整按钮样式 */
.table-card :deep(.el-button) {
  padding: 8px 16px;
}

.user-detail-sidebar {
  width: 400px;
  height: fit-content;
}

.user-detail-header {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 15px;
}

.user-header-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.user-header-info h3 {
  margin: 0;
}

.user-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.user-quick-info {
  display: flex;
  flex-direction: column;
  gap: 10px;
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
}

.training-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 8px;
}

.user-action-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 15px;
}

/* 详情对话框样式 */
    .user-detail-container {
  padding: 20px;
}
      
      .user-header {
        display: flex;
  gap: 20px;
        margin-bottom: 20px;
}
        
        .user-avatar-section {
  text-align: center;
}
          
          .user-status {
  margin-top: 8px;
        }
        
        .user-basic-info {
  flex-grow: 1;
}
          
          .user-name {
  margin: 0 0 10px;
          }
          
          .user-role {
            display: flex;
            align-items: center;
            gap: 10px;
  margin-bottom: 10px;
}
            
            .user-position {
  color: #909399;
  font-size: 14px;
      }
      
      .user-info-grid {
        display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
        gap: 20px;
}
        
        .info-section {
  margin-bottom: 20px;
}
          
          .section-title {
  font-size: 16px;
  margin: 10px 0;
  color: #303133;
          }
          
          .info-item {
            display: flex;
  margin-bottom: 8px;
}
            
            .label {
  width: 100px;
  color: #909399;
            }
            
            .value {
              flex: 1;
}
              
.text-muted {
  color: #c0c4cc;
                font-style: italic;
              }

.training-tag {
  margin-right: 6px;
  margin-bottom: 6px;
            }
            
.detail-actions {
              display: flex;
  justify-content: flex-end;
  gap: 10px;
}

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

.honors-section {
  margin-top: 12px;
  margin-bottom: 20px;
}

.honors-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.honor-item {
  background-color: #f5f7fa;
  border-radius: 6px;
  padding: 12px;
}

.honor-header {
  display: flex;
              gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}

.honor-name {
  font-weight: 500;
}

.honor-detail {
  font-size: 13px;
              }
              
.honor-info-item {
  display: flex;
  margin-bottom: 4px;
}

.honor-info-item .info-label {
  width: 90px;
  color: #909399;
  font-weight: normal;
}

.honor-description {
  color: #606266;
                font-style: italic;
              }

.honor-actions {
  margin-top: 12px;
  text-align: center;
}

.honors-dialog-content {
  padding: 20px;
}

.certificate-preview {
  padding: 20px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.certificate-header {
  margin-bottom: 20px;
  text-align: center;
  }
  
.certificate-header h3 {
  font-size: 20px;
  color: #303133;
  margin-bottom: 15px;
}

.certificate-info {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-around;
  gap: 20px;
  margin-bottom: 20px;
}

.certificate-info p {
  margin: 0;
}

.certificate-info p span {
  font-weight: bold;
  color: #606266;
}

.certificate-body {
    display: flex;
  flex-direction: column;
  gap: 20px;
  }

.certificate-image-placeholder {
  text-align: center;
  margin: 20px 0;
}

.certificate-image {
  max-width: 100%;
  max-height: 400px;
  border: 1px solid #dcdfe6;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.certificate-description {
  padding: 20px;
  background: #f5f7fa;
  border-radius: 4px;
}

.avatar-uploader .el-upload {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.avatar-uploader .el-upload:hover {
  border-color: #409EFF;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 178px;
  height: 178px;
  line-height: 178px;
  text-align: center;
}

.avatar {
  width: 178px;
  height: 178px;
  display: block;
}

.employees-view {
  min-height: calc(100vh - 60px);
  display: flex;
  flex-direction: column;
}

.main-content {
  flex: 1;
  margin-bottom: 0;
  padding-bottom: 0;
}
</style>