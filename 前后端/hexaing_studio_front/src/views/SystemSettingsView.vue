<template>
  <div class="system-settings-view">
    <!-- 页面头部 -->
    <el-card shadow="hover" class="page-header-card">
      <div class="page-title">
        <h2>系统设置</h2>
        <p class="subtitle">管理系统基础配置</p>
      </div>
    </el-card>

    <!-- 设置标签页 -->
    <el-tabs v-model="activeTab" class="settings-tabs">
      <!-- 部门管理 -->
      <el-tab-pane label="部门管理" name="departments">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>部门管理</span>
              <el-button type="primary" @click="showAddDepartmentDialog">
                <el-icon><Plus /></el-icon> 新增部门
              </el-button>
            </div>
          </template>

          <el-table :data="departments" border stripe v-loading="departmentLoading">
            <el-table-column type="index" label="序号" width="70" />
            <el-table-column prop="departmentName" label="部门名称" />
            <el-table-column prop="createTime" label="创建时间" width="180" />
            <el-table-column label="操作" width="200">
              <template #default="scope">
                <el-button type="primary" size="small" @click="editDepartment(scope.row)">
                  编辑
                </el-button>
                <el-button type="danger" size="small" @click="deleteDepartment(scope.row)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- 工作室信息 -->
      <el-tab-pane label="工作室信息" name="studio">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>工作室信息设置</span>
              <el-button type="primary" @click="saveStudioInfo" :loading="studioSaving">
                <el-icon><Check /></el-icon> 保存设置
              </el-button>
            </div>
          </template>

          <el-form :model="studioForm" label-width="120px" v-loading="studioLoading">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="工作室名称">
                  <el-input v-model="studioForm.name" placeholder="请输入工作室名称" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="负责人">
                  <el-input v-model="studioForm.director" placeholder="请输入负责人姓名" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="成立时间">
                  <el-date-picker
                    v-model="studioForm.establishTime"
                    type="date"
                    placeholder="选择成立时间"
                    format="YYYY-MM-DD"
                    value-format="YYYY-MM-DD"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="团队人数">
                  <el-input-number v-model="studioForm.memberCount" :min="1" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="项目数量">
                  <el-input-number v-model="studioForm.projectCount" :min="0" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="联系电话">
                  <el-input v-model="studioForm.phone" placeholder="请输入联系电话" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="联系邮箱">
                  <el-input v-model="studioForm.email" placeholder="请输入联系邮箱" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="房间号">
                  <el-input v-model="studioForm.room" placeholder="请输入房间号" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="地址">
              <el-input v-model="studioForm.address" placeholder="请输入详细地址" />
            </el-form-item>

            <el-form-item label="获奖情况">
              <el-input
                v-model="studioForm.awards"
                type="textarea"
                :rows="4"
                placeholder="请输入获奖情况"
              />
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <!-- 常用地点管理 -->
      <el-tab-pane label="常用地点管理" name="locations">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>常用地点管理</span>
              <el-button type="primary" @click="showAddLocationDialog">
                <el-icon><Location /></el-icon> 新增地点
              </el-button>
            </div>
          </template>

          <el-table :data="locations" border stripe v-loading="locationLoading">
            <el-table-column type="index" label="序号" width="70" />
            <el-table-column prop="name" label="地点名称" width="150" />
            <el-table-column prop="description" label="地点描述" />
            <el-table-column label="坐标" width="200">
              <template #default="scope">
                {{ scope.row.lat?.toFixed(6) }}, {{ scope.row.lng?.toFixed(6) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200">
              <template #default="scope">
                <el-button type="primary" size="small" @click="editLocation(scope.row)">
                  <el-icon><Edit /></el-icon> 编辑
                </el-button>
                <el-button type="danger" size="small" @click="deleteLocationItem(scope.row)">
                  <el-icon><Delete /></el-icon> 删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 新增/编辑部门对话框 -->
    <el-dialog
      v-model="departmentDialogVisible"
      :title="departmentDialogMode === 'add' ? '新增部门' : '编辑部门'"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="departmentForm" label-width="100px">
        <el-form-item label="部门名称" required>
          <el-input v-model="departmentForm.departmentName" placeholder="请输入部门名称" />
        </el-form-item>

      </el-form>

      <template #footer>
        <el-button @click="departmentDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDepartment" :loading="departmentSaving">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 新增/编辑地点对话框 -->
    <el-dialog
      v-model="locationDialogVisible"
      :title="locationDialogMode === 'add' ? '新增地点' : '编辑地点'"
      width="800px"
      :close-on-click-modal="false"
    >
      <el-form :model="locationForm" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="地点名称" required>
              <el-input v-model="locationForm.name" placeholder="请输入地点名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="地点描述">
              <el-input v-model="locationForm.description" placeholder="请输入地点描述" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="纬度" required>
              <el-input 
                v-model="locationForm.lat" 
                placeholder="请输入纬度或在地图上点击选择"
                type="number"
                step="0.000001"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="经度" required>
              <el-input 
                v-model="locationForm.lng" 
                placeholder="请输入经度或在地图上点击选择"
                type="number"
                step="0.000001"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="地图选择">
          <div class="map-selector-container">
            <LocationMapSelector
              v-model:latitude="locationForm.lat"
              v-model:longitude="locationForm.lng"
              :radius="50"
              @map-clicked="handleMapClick"
            />
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="locationDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveLocation" :loading="locationSaving">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, Check, Edit, Delete, Location } from '@element-plus/icons-vue';
import { getStudioInfo, updateStudioInfo } from '@/api/studio';
import { getAllLocations, createLocation, updateLocation, deleteLocation } from '@/api/location';
import LocationMapSelector from '@/components/attendance/LocationMapSelector.vue';
import request from '@/utils/request';

// 活动标签页
const activeTab = ref('departments');

// 部门管理相关
const departments = ref([]);
const departmentLoading = ref(false);
const departmentDialogVisible = ref(false);
const departmentDialogMode = ref('add'); // 'add' | 'edit'
const departmentSaving = ref(false);
const departmentForm = reactive({
  id: null,
  departmentName: ''
});

// 工作室信息相关
const studioLoading = ref(false);
const studioSaving = ref(false);
const studioForm = reactive({
  id: null,
  name: '',
  establishTime: '',
  director: '',
  memberCount: 0,
  projectCount: 0,
  awards: '',
  phone: '',
  email: '',
  address: '',
  room: ''
});

// 常用地点相关
const locations = ref([]);
const locationLoading = ref(false);
const locationDialogVisible = ref(false);
const locationDialogMode = ref('add'); // 'add' | 'edit'
const locationSaving = ref(false);
const locationForm = reactive({
  id: null,
  name: '',
  lat: 29.552965,
  lng: 106.238573,
  description: ''
});

// 部门管理方法
const loadDepartments = async () => {
  departmentLoading.value = true;
  try {
    const response = await request.get('/admin/department/list');
    if (response.code === 200) {
      departments.value = response.data || [];
    }
  } catch (error) {
    console.error('获取部门列表失败:', error);
    ElMessage.error('获取部门列表失败');
  } finally {
    departmentLoading.value = false;
  }
};

const showAddDepartmentDialog = () => {
  departmentDialogMode.value = 'add';
  Object.assign(departmentForm, {
    id: null,
    departmentName: ''
  });
  departmentDialogVisible.value = true;
};

const editDepartment = (department) => {
  departmentDialogMode.value = 'edit';
  Object.assign(departmentForm, { 
    id: department.departmentId || department.id,
    departmentName: department.departmentName 
  });
  departmentDialogVisible.value = true;
};

const saveDepartment = async () => {
  if (!departmentForm.departmentName.trim()) {
    ElMessage.warning('请输入部门名称');
    return;
  }

  departmentSaving.value = true;
  try {
    const url = departmentDialogMode.value === 'add' 
      ? '/admin/department/add' 
      : '/admin/department/update';
    
    const response = await request.post(url, departmentForm);
    if (response.code === 200) {
      ElMessage.success(departmentDialogMode.value === 'add' ? '新增成功' : '更新成功');
      departmentDialogVisible.value = false;
      loadDepartments();
    } else {
      ElMessage.error(response.msg || '操作失败');
    }
  } catch (error) {
    console.error('保存部门失败:', error);
    ElMessage.error('保存失败');
  } finally {
    departmentSaving.value = false;
  }
};

const deleteDepartment = async (department) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除部门"${department.departmentName}"吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );

    const response = await request.post('/admin/department/delete', [department.departmentId || department.id]);
    if (response.code === 200) {
      ElMessage.success('删除成功');
      loadDepartments();
    } else {
      ElMessage.error(response.msg || '删除失败');
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除部门失败:', error);
      ElMessage.error('删除失败');
    }
  }
};

// 工作室信息方法
const loadStudioInfo = async () => {
  studioLoading.value = true;
  try {
    const response = await getStudioInfo();
    if (response.code === 200 && response.data) {
      Object.assign(studioForm, response.data);
    }
  } catch (error) {
    console.error('获取工作室信息失败:', error);
    ElMessage.error('获取工作室信息失败');
  } finally {
    studioLoading.value = false;
  }
};

const saveStudioInfo = async () => {
  studioSaving.value = true;
  try {
    const response = await updateStudioInfo(studioForm);
    if (response.code === 200) {
      ElMessage.success('保存成功');
    } else {
      ElMessage.error(response.msg || '保存失败');
    }
  } catch (error) {
    console.error('保存工作室信息失败:', error);
    ElMessage.error('保存失败');
  } finally {
    studioSaving.value = false;
  }
};

// 常用地点管理方法
const loadLocations = async () => {
  locationLoading.value = true;
  try {
    const response = await getAllLocations();
    if (response.code === 200) {
      locations.value = response.data || [];
    }
  } catch (error) {
    console.error('获取常用地点列表失败:', error);
    ElMessage.error('获取常用地点列表失败');
  } finally {
    locationLoading.value = false;
  }
};

const showAddLocationDialog = () => {
  locationDialogMode.value = 'add';
  Object.assign(locationForm, {
    id: null,
    name: '',
    lat: 29.552965,
    lng: 106.238573,
    description: ''
  });
  locationDialogVisible.value = true;
};

const editLocation = (location) => {
  locationDialogMode.value = 'edit';
  Object.assign(locationForm, { ...location });
  locationDialogVisible.value = true;
};

const saveLocation = async () => {
  if (!locationForm.name.trim()) {
    ElMessage.warning('请输入地点名称');
    return;
  }
  
  if (!locationForm.lat || !locationForm.lng) {
    ElMessage.warning('请选择地点位置');
    return;
  }

  locationSaving.value = true;
  try {
    let response;
    if (locationDialogMode.value === 'add') {
      response = await createLocation(locationForm);
    } else {
      response = await updateLocation(locationForm.id, locationForm);
    }
    
    if (response.code === 200) {
      ElMessage.success(locationDialogMode.value === 'add' ? '新增成功' : '更新成功');
      locationDialogVisible.value = false;
      loadLocations();
    } else {
      ElMessage.error(response.msg || '操作失败');
    }
  } catch (error) {
    console.error('保存地点失败:', error);
    ElMessage.error('保存失败');
  } finally {
    locationSaving.value = false;
  }
};

const deleteLocationItem = async (location) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除地点"${location.name}"吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );

    const response = await deleteLocation(location.id);
    if (response.code === 200) {
      ElMessage.success('删除成功');
      loadLocations();
    } else {
      ElMessage.error(response.msg || '删除失败');
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除地点失败:', error);
      ElMessage.error('删除失败');
    }
  }
};

const handleMapClick = () => {
  // 地图点击事件处理，可以添加额外逻辑
  console.log('地图已更新位置:', locationForm.lat, locationForm.lng);
};

// 初始化
onMounted(() => {
  loadDepartments();
  loadStudioInfo();
  loadLocations();
});
</script>

<style lang="scss" scoped>
.system-settings-view {
  padding: 20px;

  .page-header-card {
    margin-bottom: 20px;

    .page-title {
      h2 {
        margin: 0 0 8px 0;
        color: var(--el-text-color-primary);
      }

      .subtitle {
        margin: 0;
        color: var(--el-text-color-secondary);
        font-size: 14px;
      }
    }
  }

  .settings-tabs {
    .el-tab-pane {
      padding-top: 20px;
    }
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: bold;
  }

  .el-form {
    .el-row {
      margin-bottom: 0;
    }
  }

  .map-selector-container {
    width: 100%;
    border: 1px solid var(--el-border-color);
    border-radius: 6px;
    overflow: hidden;
    
    .location-map-selector {
      width: 100%;
      height: 400px;
    }
  }
}
</style> 