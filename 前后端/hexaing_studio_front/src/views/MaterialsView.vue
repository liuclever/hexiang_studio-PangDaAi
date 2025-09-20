<template>
  <div class="materials-view">
    <MaterialsHeader
      v-model:search-query="searchQuery"
      v-model:filter-category="filterCategory"
      v-model:filter-type="filterType"
      v-model:date-range="dateRange"
      :categories="categories"
      :file-types="fileTypes"
      @add="showAddMaterialDialog"
      @search="handleSearch"
    />

    <CategoryManager
      :is-admin="isAdmin"
      :categories="categories"
      @add="showAddCategoryDialog"
      @edit="editCategory"
      @delete="handleDeleteCategory"
    />
        
    <MaterialsTable
      :loading="loading"
      :materials="filteredMaterials"
      :categories="categories"
          :total="totalMaterials"
      :current-user="currentUser"
      :is-admin="isAdmin"
      v-model:page-size="pageSize"
      v-model:current-page="currentPage"
      @download="downloadMaterial"
      @edit="editMaterial"
      @delete="deleteMaterialItem"
        />

    <UploadDialog
      v-model="uploadDialogVisible" 
      :categories="categories"
      @upload="uploadMaterialFiles"
          />

    <CategoryDialog
      v-model="categoryDialogVisible"
      :is-editing="isEditingCategory"
      :initial-data="categoryForm"
      @save="saveCategory"
    />

    <EditDialog
      v-model="editDialogVisible"
      :material-data="editingMaterial"
      :categories="categories"
      @save="updateMaterialInfo"
    />


  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted, watch } from 'vue';
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus';
import { useRoute, useRouter } from 'vue-router'; // 引入 useRoute
import request, { ApiResponse } from '@/utils/request';
import { getUserId } from '@/utils/auth';
import { 
  getMaterialCategories, 
  getMaterialList, 
  uploadMaterial,
  deleteMaterial, 
  addMaterialCategory,
  updateMaterialCategory,
  deleteMaterialCategory,
  recordMaterialDownload,
  getMaterialDetail,
  updateMaterial
} from '../utils/api/material';
import { ALLOWED_EXTENSIONS, DANGEROUS_EXTENSIONS, formatFileSize } from '../utils/fileUtils';

import MaterialsHeader from '@/components/materials/MaterialsHeader.vue';
import CategoryManager from '@/components/materials/CategoryManager.vue';
import MaterialsTable from '@/components/materials/MaterialsTable.vue';
import UploadDialog from '@/components/materials/dialogs/UploadDialog.vue';
import CategoryDialog from '@/components/materials/dialogs/CategoryDialog.vue';
import EditDialog from '@/components/materials/dialogs/EditDialog.vue';

// Interface definitions
interface Material {
  id: number;
  fileName: string;
  fileType: string;
  fileSize: number | null;
  url: string;
  description: string | null;
  categoryId: number | null;
  uploadTime: string | Date;
  uploaderId: number | null;
  uploader: string;
  downloadCount: number;
  isPublic: number;
}

interface Category {
  id: number;
  name: string;
  orderId: number;
}

interface User {
  id: number;
  name: string;
  role: 'admin' | 'teacher' | 'student' | 'guest';
  roleId: number;
  positionId: number;
}

// State variables
const loading = ref(false);
const materials = ref<Material[]>([]);
const categories = ref<Category[]>([]);
const currentPage = ref(1);
const pageSize = ref(20);
const totalMaterials = ref(0);
const searchQuery = ref('');
const filterCategory = ref('');
const filterType = ref('');
const dateRange = ref<[string, string] | null>(null);

const fileTypes = ref([
  { value: '图片', label: '图片' },
  { value: '文档', label: '文档' },
  { value: '视频', label: '视频' },
  { value: '音频', label: '音频' },
  { value: '压缩包', label: '压缩包' },
  { value: '其他', label: '其他' },
]);

// Dialog control
const uploadDialogVisible = ref(false);
const categoryDialogVisible = ref(false);
const editDialogVisible = ref(false);
const isEditingCategory = ref(false);
const editingMaterial = ref<Material | null>(null);

// Current user info - 动态获取真实用户信息
const currentUser = reactive<User>({
  id: getUserId() || 0,
  name: '',
  role: 'student',
  roleId: 1,
  positionId: 1
});

const route = useRoute(); // 获取当前路由信息
const router = useRouter();

// 根据真实用户信息判断权限
const isAdmin = computed(() => {
  // 部长(3)、副部长(4)、老师(5)、主任(6)、副主任(7)、超级管理员(8) 有管理权限
  return currentUser.positionId >= 3;
});

const categoryForm = reactive({
  id: 0,
  name: '',
  orderId: 1
});

// 获取当前用户信息
const loadCurrentUser = async () => {
  try {
    const response: ApiResponse = await request.get('/admin/user/profile');
    if (response?.code === 200 && response?.data) {
      const userData = response.data;
      currentUser.id = userData.userId || userData.user_id || 0;
      currentUser.name = userData.name || '未知用户';
      currentUser.roleId = userData.roleId || userData.role_id || 1;
      currentUser.positionId = userData.positionId || userData.position_id || 1;
      
      // 根据positionId判断角色
      if (currentUser.positionId >= 6) {  // 主任、副主任、超级管理员
        currentUser.role = 'admin';
      } else if (currentUser.positionId === 5) {  // 老师
        currentUser.role = 'teacher';
      } else {
        currentUser.role = 'student';  // 学生及以下
      }
      
      console.log('当前用户信息:', currentUser);
    }
  } catch (error) {
    console.error('获取用户信息失败:', error);
    ElMessage.error('获取用户信息失败');
  }
};

// Watch for pagination changes
watch([currentPage, pageSize], () => {
  loadMaterials();
});

// Load material data
const loadMaterials = async () => {
  loading.value = true;
  try {
    const categoryRes = await getMaterialCategories();
    if (categoryRes.code === 200 || categoryRes.code === 1) {
      categories.value = categoryRes.data || [];
    } else {
      ElMessage.error(categoryRes.msg || '获取资料分类失败');
    }
    
    const queryParams: any = {
      page: currentPage.value,
      pageSize: pageSize.value,
      name: searchQuery.value || undefined,
      categoryId: filterCategory.value ? parseInt(filterCategory.value) : undefined,
      startDate: dateRange.value ? dateRange.value[0] : undefined,
      endDate: dateRange.value ? dateRange.value[1] : undefined
    };
    
    // 处理文件类型筛选：将分类名称转换为扩展名数组
    if (filterType.value) {
      const fileTypes = getFileTypesByCategory(filterType.value);
      if (fileTypes.length > 0) {
        queryParams.fileTypes = fileTypes;
      }
    }
    
    const res = await getMaterialList(queryParams);
    if (res.code === 200 || res.code === 1) {
      materials.value = res.data.records || [];
      totalMaterials.value = res.data.total || 0;
    } else {
      ElMessage.error(res.msg || '获取资料列表失败');
      materials.value = [];
      totalMaterials.value = 0;
    }
  } catch (error) {
    console.error('加载资料数据出错:', error);
    ElMessage.error('加载资料数据失败，请刷新页面重试');
    materials.value = [];
    totalMaterials.value = 0;
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  currentPage.value = 1;
  loadMaterials();
};

const getFileCategory = (fileType: string): string => {
  const documentTypes = ['pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'txt', 'md'];
  const imageTypes = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg'];
  const videoTypes = ['mp4', 'avi', 'mov', 'wmv', 'flv', 'mkv', 'webm'];
  const audioTypes = ['mp3', 'wav', 'ogg', 'flac', 'aac'];
  const archiveTypes = ['zip', 'rar', '7z', 'tar', 'gz'];
  
  fileType = fileType.toLowerCase();
  
  if (documentTypes.includes(fileType)) return 'document';
  if (imageTypes.includes(fileType)) return 'image';
  if (videoTypes.includes(fileType)) return 'video';
  if (audioTypes.includes(fileType)) return 'audio';
  if (archiveTypes.includes(fileType)) return 'archive';
  
  return 'other';
};

// 将分类名称转换为文件扩展名数组
const getFileTypesByCategory = (category: string): string[] => {
  const typeMap: Record<string, string[]> = {
    '图片': ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg'],
    '文档': ['pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'txt', 'md'],
    '视频': ['mp4', 'avi', 'mov', 'wmv', 'flv', 'mkv', 'webm'],
    '音频': ['mp3', 'wav', 'ogg', 'flac', 'aac'],
    '压缩包': ['zip', 'rar', '7z', 'tar', 'gz'],
    '其他': ['other']
  };
  
  return typeMap[category] || [];
};

const filteredMaterials = computed(() => {
  if (!materials.value || !Array.isArray(materials.value)) {
    return [];
  }
  return materials.value; // Server-side filtering is now the authority
});

const getFileUrl = (filePath: string | undefined): string => {
  if (!filePath) return '';
  // The URL from the backend should be the correct access path.
  // No need to prepend any prefix here.
  return filePath;
};

const showAddMaterialDialog = () => {
  uploadDialogVisible.value = true;
};

const uploadMaterialFiles = async (formData: FormData) => {
      const loadingInstance = ElLoading.service({
        lock: true,
        text: '正在上传文件，请稍候...',
        background: 'rgba(0, 0, 0, 0.7)'
      });
      try {
        const res = await uploadMaterial(formData);
        if (res.code === 200 || res.code === 1) {
          ElMessage.success('文件上传成功');
          uploadDialogVisible.value = false;
          loadMaterials();
        } else {
          // 显示后端返回的具体错误信息，包括权限提示
          ElMessage.error(res.msg || '上传失败');
        }
      } catch (error: any) {
        console.error('上传文件出错:', error);
        // 如果是权限错误，显示具体的权限提示
        if (error?.response?.data?.msg) {
          ElMessage.error(error.response.data.msg);
        } else {
        ElMessage.error('上传文件失败，请重试');
        }
      } finally {
        loadingInstance.close();
      }
};

const downloadMaterial = (material: Material) => {
  if (!material.url) return;
  
  // 使用统一的URL构建方法，添加 /api 前缀，这与Vite代理配置匹配
  const baseUrl = '/api/admin/file/view/';
  const url = `${baseUrl}${material.url}?download=true&materialId=${material.id}&originalName=${encodeURIComponent(material.fileName)}&fileType=material`;
  
  window.open(url, '_blank');
  // We can optimistically update the download count on the frontend
  material.downloadCount++;
  ElMessage.success(`开始下载：${material.fileName}`);
};



const deleteMaterialItem = async (material: Material) => {
  try {
    await ElMessageBox.confirm(`确定要删除文件"${material.fileName}"吗？`, '删除确认', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    });
    const deleteRes = await deleteMaterial(material.id);
    if (deleteRes.code === 200 || deleteRes.code === 1) {
      ElMessage.success('删除成功');
      loadMaterials();
    } else {
      // 显示后端返回的具体错误信息，包括权限提示
      ElMessage.error(deleteRes.msg || '删除失败');
    }
  } catch (error: any) {
    // 如果不是用户取消操作，则显示错误信息
    if (error !== 'cancel' && error?.response?.data?.msg) {
      ElMessage.error(error.response.data.msg);
    }
  }
};

const showAddCategoryDialog = () => {
  isEditingCategory.value = false;
  categoryForm.id = 0;
  categoryForm.name = '';
  categoryForm.orderId = categories.value.length + 1;
  categoryDialogVisible.value = true;
};

const editCategory = (category: Category) => {
  isEditingCategory.value = true;
  categoryForm.id = category.id;
  categoryForm.name = category.name;
  categoryForm.orderId = category.orderId;
  categoryDialogVisible.value = true;
};

const saveCategory = async (category: { id?: number; name: string; orderId: number; }) => {
      try {
        const action = isEditingCategory.value ? updateMaterialCategory : addMaterialCategory;
    const res = await action(category as any); // Use `as any` to bypass strict type checking for the union type action
        if (res.code === 200 || res.code === 1) {
          ElMessage.success(isEditingCategory.value ? '分类更新成功' : '分类添加成功');
          categoryDialogVisible.value = false;
      loadMaterials();
        } else {
          // 显示后端返回的具体错误信息，包括权限提示
          ElMessage.error(res.msg || (isEditingCategory.value ? '更新失败' : '添加失败'));
        }
      } catch (error: any) {
        console.error('保存分类出错:', error);
        // 如果是权限错误，显示具体的权限提示
        if (error?.response?.data?.msg) {
          ElMessage.error(error.response.data.msg);
        } else {
        ElMessage.error('操作失败，请重试');
        }
      }
};

const handleDeleteCategory = async (category: Category) => {
  try {
    await ElMessageBox.confirm(`确定要删除分类"${category.name}"吗？`, '删除确认', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    });
    const deleteRes = await deleteMaterialCategory(category.id);
    if (deleteRes.code === 200 || deleteRes.code === 1) {
      ElMessage.success('删除成功');
      loadMaterials();
    } else {
      // 显示后端返回的具体错误信息，包括权限提示
      ElMessage.error(deleteRes.msg || '删除失败');
    }
  } catch (error: any) {
    // 如果不是用户取消操作，则显示错误信息
    if (error !== 'cancel' && error?.response?.data?.msg) {
      ElMessage.error(error.response.data.msg);
    }
  }
};

const editMaterial = (material: Material) => {
  editingMaterial.value = material;
  editDialogVisible.value = true;
};

const updateMaterialInfo = async (updateData: any) => {
      try {
        const res = await updateMaterial(updateData);
        if (res.code === 200 || res.code === 1) {
          ElMessage.success('资料信息更新成功');
          editDialogVisible.value = false;
      loadMaterials();
        } else {
          // 显示后端返回的具体错误信息，包括权限提示
          ElMessage.error(res.msg || '更新失败');
        }
      } catch (error: any) {
        console.error('更新资料信息出错:', error);
        // 如果是权限错误，显示具体的权限提示
        if (error?.response?.data?.msg) {
          ElMessage.error(error.response.data.msg);
        } else {
        ElMessage.error('更新资料信息失败，请重试');
        }
      }
};



onMounted(async () => {
  // 先加载用户信息，然后加载资料
  await loadCurrentUser();
  loadMaterials();

  // 检查路由中是否有 'action=upload' 参数
  if (route.query.action === 'upload') {
    showAddMaterialDialog();
    // 可选: 清理URL中的查询参数，避免刷新页面时再次触发
    router.replace({ query: {} });
  }
});
</script>

<style lang="scss" scoped>
.materials-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.materials-view :deep(.el-table) {
  font-size: 15px;
}
</style>
