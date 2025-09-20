<template>
  <el-dialog
    :model-value="visible"
    :title="`${user?.name || '用户'}的荣誉与证书`"
    width="80%"
    @update:model-value="$emit('update:visible', $event)"
    :before-close="handleClose"
    class="honor-certificate-dialog"
    v-loading="loading.honor || loading.certificate"
  >
    <el-tabs v-model="activeTab" type="border-card">
      <!-- 荣誉管理标签页 -->
      <el-tab-pane label="荣誉管理" name="honors">
        <div class="controls">
          <el-button type="primary" @click="openFormDialog('honor')" :icon="Plus">添加荣誉</el-button>
          <span v-if="loading.honor" class="loading-text">正在加载荣誉数据...</span>
        </div>
        <el-table :data="honors" v-loading="loading.honor" empty-text="暂无荣誉信息">
          <el-table-column prop="honorName" label="荣誉名称" />
          <el-table-column prop="honorLevel" label="荣誉级别" />
          <el-table-column prop="issueOrg" label="颁发机构" />
          <el-table-column prop="issueDate" label="颁发日期" :formatter="formatDate" />
          <el-table-column label="操作">
            <template #default="{ row }">
              <el-button size="small" @click="openDetailsDialog('honor', row)">详情</el-button>
              <el-button size="small" @click="openFormDialog('honor', row)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDelete('honor', row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 证书管理标签页 -->
      <el-tab-pane label="证书管理" name="certificates">
        <div class="controls">
          <el-button type="primary" @click="openFormDialog('certificate')" :icon="Plus">添加证书</el-button>
        </div>
        <el-table :data="certificates" v-loading="loading.certificate" empty-text="暂无证书信息">
          <el-table-column prop="certificateName" label="证书名称" />
          <el-table-column prop="certificateLevel" label="证书类型" />
          <el-table-column prop="issueOrg" label="颁发机构" />
          <el-table-column prop="issueDate" label="颁发日期" :formatter="formatDate" />
          <el-table-column label="操作">
            <template #default="{ row }">
              <el-button size="small" @click="openDetailsDialog('certificate', row)">详情</el-button>
              <el-button size="small" @click="openFormDialog('certificate', row)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDelete('certificate', row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 添加/编辑的内嵌对话框 -->
    <el-dialog
      v-model="formDialogVisible"
      :title="formTitle"
      width="600px"
      append-to-body
      class="form-dialog"
    >
      <el-form :model="form" ref="formRef" :rules="formRules" label-width="100px">
        <el-form-item :label="isHonorForm ? '荣誉名称' : '证书名称'" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item :label="isHonorForm ? '荣誉级别' : '证书类型'" prop="level">
          <el-select v-model="form.level" :placeholder="`请选择${isHonorForm ? '级别' : '类型'}`" style="width: 100%;">
            <el-option v-for="item in currentLevelOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="颁发机构" prop="issueOrg">
          <el-input v-model="form.issueOrg" />
        </el-form-item>
        <el-form-item label="颁发日期" prop="issueDate">
          <el-date-picker v-model="form.issueDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input type="textarea" v-model="form.description" />
        </el-form-item>
        <el-form-item v-if="!isHonorForm" label="证书编号" prop="certificateNo">
          <el-input v-model="form.certificateNo" />
        </el-form-item>
        <el-form-item label="附件照片" prop="attachment">
          <div class="upload-container">
          <el-upload
            ref="uploadRef"
            action="#"
            list-type="picture-card"
            :limit="1"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :on-preview="handlePictureCardPreview"
            :auto-upload="false"
            :file-list="fileList"
              :on-exceed="() => ElMessage.warning('只能上传一张图片')"
              :before-upload="validateFile"
              accept=".jpg,.jpeg,.png,.gif"
            >
              <template #default>
                <div class="upload-content">
                  <el-icon class="upload-icon"><Plus /></el-icon>
                  <span class="upload-text">上传图片</span>
                </div>
              </template>
          </el-upload>
            <div class="upload-tip">
              <el-icon><InfoFilled /></el-icon>
              <span>请上传一张证书/荣誉照片，支持JPG、PNG格式，大小不超过10MB</span>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
    
    <el-dialog v-model="previewVisible" class="image-preview-dialog" append-to-body>
      <img w-full :src="previewImageUrl" alt="Preview Image" style="width: 100%;" />
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailsDialogVisible"
      :title="detailsTitle"
      width="700px"
      append-to-body
      class="details-dialog"
    >
      <div v-if="selectedItem" class="details-content">
        <el-descriptions :column="2" border>
            <el-descriptions-item :label="isHonorDetails ? '荣誉名称' : '证书名称'">{{ isHonorDetails ? selectedItem.honorName : selectedItem.certificateName }}</el-descriptions-item>
            <el-descriptions-item :label="isHonorDetails ? '荣誉级别' : '证书类型'">{{ isHonorDetails ? selectedItem.honorLevel : selectedItem.certificateLevel }}</el-descriptions-item>
            <el-descriptions-item label="颁发机构">{{ selectedItem.issueOrg }}</el-descriptions-item>
            <el-descriptions-item label="颁发日期">{{ formatDate(null, null, selectedItem.issueDate) }}</el-descriptions-item>
            <el-descriptions-item v-if="!isHonorDetails" label="证书编号">{{ selectedItem.certificateNo }}</el-descriptions-item>
            <el-descriptions-item label="描述" :span="2">{{ selectedItem.description || '无' }}</el-descriptions-item>
            <el-descriptions-item v-if="user?.directionIdNames && user.directionIdNames.length > 0" label="培训方向" :span="2">
              <el-tag 
                v-for="direction in user.directionIdNames" 
                :key="direction" 
                type="success" 
                effect="light"
                style="margin-right: 8px; margin-bottom: 5px;"
              >
                {{ direction }}
              </el-tag>
            </el-descriptions-item>
        </el-descriptions>
        
        <div style="margin-top: 20px;">
          <strong>附件:</strong>
          <div v-if="selectedItem.attachment" style="margin-top: 10px;">
            <div class="image-gallery">
              <div class="image-item" @click="previewAttachment">
                <img :src="getAttachmentUrl(selectedItem.attachment)" alt="附件">
                <div class="image-overlay">
                  <el-icon><ZoomIn /></el-icon>
                </div>
              </div>
            </div>
          </div>
          <div v-else style="color: #999; margin-top: 10px;">
            无附件
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailsDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleDownload" :disabled="!selectedItem || !selectedItem.attachment">下载附件</el-button>
      </template>
    </el-dialog>

  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, reactive, computed } from 'vue';
import { ElMessage, ElMessageBox, FormInstance, UploadInstance, UploadProps, UploadFile, UploadRawFile } from 'element-plus';
import { Plus, Delete, ZoomIn, InfoFilled } from '@element-plus/icons-vue';
import request from '@/utils/request';
import { UserVo, UserHonor, UserCertificate } from './types';
import { validateFileType, DANGEROUS_EXTENSIONS, getFileExtension } from '@/utils/fileUtils';

type FormType = 'honor' | 'certificate';

const props = defineProps<{
  visible: boolean;
  user: UserVo | null;
}>();

const emit = defineEmits(['update:visible', 'refresh']);

const activeTab = ref('honors');
const loading = reactive({ honor: false, certificate: false });
const honors = ref<UserHonor[]>([]);
const certificates = ref<UserCertificate[]>([]);

const formDialogVisible = ref(false);
const isEditing = ref(false);
const currentFormType = ref<FormType>('honor');
const currentFile = ref<UploadRawFile | null>(null);

const formRef = ref<FormInstance>();
const uploadRef = ref<UploadInstance>();
const fileList = ref<any[]>([]);

const form = reactive({
  id: undefined as number | undefined,
  name: '',
  level: '',
  issueOrg: '',
  issueDate: '',
  description: '',
  certificateNo: '',
  attachment: '',
});

const isHonorForm = computed(() => currentFormType.value === 'honor');
const formTitle = computed(() => `${isEditing.value ? '编辑' : '添加'}${isHonorForm.value ? '荣誉' : '证书'}`);
const formRules = {
  name: [{ required: true, message: '名称不能为空', trigger: 'blur' }],
};

const honorLevelOptions = ref(['国家级', '省级', '市级', '校级']);
const certificateTypeOptions = ref(['文化能力相关证书', '专业能力相关证书']);

const currentLevelOptions = computed(() =>
  isHonorForm.value ? honorLevelOptions.value : certificateTypeOptions.value
);

const previewImageUrl = ref('');
const previewVisible = ref(false);

// 详情对话框状态
const detailsDialogVisible = ref(false);
const selectedItem = ref<any>(null); // any to accommodate both types
const currentDetailsType = ref<FormType>('honor');

const isHonorDetails = computed(() => currentDetailsType.value === 'honor');
const detailsTitle = computed(() => `${isHonorDetails.value ? '荣誉' : '证书'}详情`);

const fetchData = async (type: FormType) => {
  if (!props.user?.userId) return;
  const endpoint = type === 'honor' ? 'honors' : 'certificates';
  loading[type] = true;
  try {
    const response = await request.get(`/admin/achievement/${endpoint}`, {
      params: { userId: props.user.userId }
    });
    if (type === 'honor') {
      honors.value = response.data || [];
    } else {
      certificates.value = response.data || [];
    }
  } catch (error) {
    ElMessage.error(`获取${isHonorForm.value ? '荣誉' : '证书'}列表失败`);
    if (type === 'honor') {
      honors.value = [];
    } else {
      certificates.value = [];
    }
  } finally {
    loading[type] = false;
  }
};

watch(() => props.visible, (newVal) => {
  if (newVal) {
    // 重置为荣誉标签页
    activeTab.value = 'honors';
    // 加载数据
    fetchData('honor');
    fetchData('certificate');
  }
});

const handleClose = () => {
  emit('update:visible', false);
};

// 验证文件类型和大小
const validateFile = (file: File) => {
  // 验证文件类型
  const extension = getFileExtension(file.name).toLowerCase();
  
  // 检查是否是危险文件类型
  if (DANGEROUS_EXTENSIONS.includes(extension)) {
    ElMessage.error(`不允许上传 ${extension} 类型的文件，该类型可能存在安全风险`);
    return false;
  }
  
  // 只允许上传图片文件
  const allowedTypes = ['jpg', 'jpeg', 'png', 'gif'];
  if (!allowedTypes.includes(extension)) {
    ElMessage.error(`只支持上传 ${allowedTypes.join(', ')} 格式的图片`);
    return false;
  }
  
  // 验证文件大小（10MB限制）
  const maxSize = 10 * 1024 * 1024; // 10MB
  if (file.size > maxSize) {
    ElMessage.error(`文件大小不能超过10MB`);
    return false;
  }
  
  return true;
};

const handleFileChange: UploadProps['onChange'] = (uploadFile, uploadFiles) => {
  if (uploadFile.status === 'ready' && uploadFile.raw) {
    currentFile.value = uploadFile.raw;
  }
};

const handlePictureCardPreview = (file: UploadFile) => {
  previewImageUrl.value = file.url!;
  previewVisible.value = true;
};

const openFormDialog = (type: FormType, item?: UserHonor | UserCertificate) => {
  currentFormType.value = type;
  isEditing.value = !!item;
  currentFile.value = null;
  fileList.value = [];
  
  if (item) {
    form.id = item.id;
    form.name = type === 'honor' ? (item as UserHonor).honorName : (item as UserCertificate).certificateName;
    form.level = type === 'honor' ? (item as UserHonor).honorLevel || '' : (item as UserCertificate).certificateLevel || '';
    form.issueOrg = item.issueOrg || '';
    form.issueDate = item.issueDate ? new Date(item.issueDate).toISOString().split('T')[0] : '';
    form.description = item.description || '';
    form.attachment = item.attachment || '';
    if (!isHonorForm.value) {
        form.certificateNo = (item as UserCertificate).certificateNo || '';
    }
    if (item.attachment) {
      const previewUrl = getAttachmentUrl(item.attachment);
      fileList.value = [{ name: 'attachment', url: previewUrl, uid: 1 }];
    }
  } else {
    Object.keys(form).forEach(key => (form as any)[key] = undefined);
    form.name = '';
    form.level = '';
    form.issueOrg = '';
    form.issueDate = '';
    form.description = '';
    form.certificateNo = '';
    form.attachment = '';
  }
  formDialogVisible.value = true;
};

const handleDelete = (type: FormType, id: number) => {
  ElMessageBox.confirm('确定删除吗？', '提示', { type: 'warning' })
    .then(async () => {
      const endpoint = type === 'honor' ? 'honor' : 'certificate';
      await request.delete(`/admin/achievement/${endpoint}/${id}`);
      ElMessage.success('删除成功');
      fetchData(type);
      emit('refresh');
    })
    .catch(() => {});
};

const handleSubmit = async () => {
  await formRef.value?.validate();
  
  const formData = new FormData();

  const dto: any = {
    userId: props.user?.userId,
    issueOrg: form.issueOrg,
    issueDate: form.issueDate,
    description: form.description,
  };

  if (isEditing.value) {
    dto.id = form.id;
  }

  if (isHonorForm.value) {
    dto.honorName = form.name;
    dto.honorLevel = form.level;
    if (isEditing.value) {
      dto.honorId = form.id;
    }
  } else {
    dto.certificateName = form.name;
    dto.certificateLevel = form.level;
    dto.certificateNo = form.certificateNo;
    if (isEditing.value) {
      dto.certificateId = form.id;
    }
  }

  const blob = new Blob([JSON.stringify(dto)], { type: 'application/json' });
  formData.append(isHonorForm.value ? 'honor' : 'certificate', blob);

  if (currentFile.value) {
    formData.append('file', currentFile.value);
  }

  const endpoint = currentFormType.value;
  const url = isEditing.value ? `/admin/achievement/${endpoint}/update` : `/admin/achievement/${endpoint}/add`;

  try {
    await request.post(url, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    ElMessage.success('操作成功');
    formDialogVisible.value = false;
    fetchData(endpoint);
    emit('refresh');
  } catch (error) {
    console.error('操作失败', error);
    ElMessage.error('操作失败');
  }
};

const handleFileRemove: UploadProps['onRemove'] = (uploadFile, uploadFiles) => {
  currentFile.value = null;
};

const formatDate = (row: any, column: any, cellValue: string) => {
  if (!cellValue) return 'N/A';
  return new Date(cellValue).toLocaleDateString();
};

const getAttachmentUrl = (attachmentPath: string) => {
  if (!attachmentPath) return '';
  // 幂等处理：如果已经是完整URL或以 /api/admin/file/view/ 开头，直接返回
  if (
    attachmentPath.startsWith('http') ||
    attachmentPath.startsWith('/api/admin/file/view/')
  ) {
    return attachmentPath;
  }
  return `/api/admin/file/view/${attachmentPath}`;
};

const openDetailsDialog = (type: FormType, item: UserHonor | UserCertificate) => {
  currentDetailsType.value = type;
  selectedItem.value = item;
  detailsDialogVisible.value = true;
};

const handleDownload = () => {
  if (!selectedItem.value || !selectedItem.value.attachment) {
    ElMessage.warning('没有可下载的附件');
    return;
  }
  
  const item = selectedItem.value;
  const attachmentPath = item.attachment;
  const itemName = currentDetailsType.value === 'honor' ? item.honorName : item.certificateName;
  const originalName = `${itemName}_${attachmentPath.split('/').pop()}`;
  // 用幂等函数处理
  const downloadUrl = getAttachmentUrl(attachmentPath) + `?download=true&originalName=${encodeURIComponent(originalName)}`;
  
  // 使用a标签触发下载，兼容性更好
  const link = document.createElement('a');
  link.href = downloadUrl;
  link.target = '_blank'; // 在新标签页打开，避免页面跳转
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
};

// 统一的图片预览处理函数
const previewAttachment = () => {
  if (selectedItem.value && selectedItem.value.attachment) {
    previewImageUrl.value = getAttachmentUrl(selectedItem.value.attachment);
    previewVisible.value = true;
  }
};
</script>

<style lang="scss" scoped>
.controls {
  margin-bottom: 16px;
  text-align: right;
}

.el-table {
  min-height: 200px; /* 确保表格在无数据时也有最小高度 */
}

.honor-certificate-dialog {
  :deep(.el-dialog__body) {
  padding-top: 10px;
  }
  
  :deep(.el-dialog__header) {
    background: linear-gradient(135deg, #409EFF, #67C23A);
    padding: 15px 20px;
    border-radius: 8px 8px 0 0;
    margin-right: 0;
  }
  
  :deep(.el-dialog__title) {
    color: white;
    font-weight: bold;
    font-size: 18px;
  }
  
  :deep(.el-dialog__headerbtn .el-dialog__close) {
    color: white;
  }
  
  :deep(.el-tabs) {
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  }
  
  :deep(.el-tabs__header) {
    margin-bottom: 0;
    background-color: #f5f7fa;
  }
  
  :deep(.el-tabs__item) {
    height: 50px;
    line-height: 50px;
    font-size: 16px;
    font-weight: 500;
    transition: all 0.3s;
    
    &.is-active {
      color: #409EFF;
      font-weight: bold;
      background-color: white;
    }
    
    &:hover {
      color: #409EFF;
    }
  }
  
  :deep(.el-tab-pane) {
    padding: 20px;
    background-color: white;
  }
  
  :deep(.el-table) {
    border-radius: 8px;
    overflow: hidden;
    margin-top: 15px;
    box-shadow: 0 4px 16px 0 rgba(0, 0, 0, 0.1);
    border: 1px solid #e0e0e0;
    
    th {
      background-color: #edf2fc;
      color: #303133;
      font-weight: bold;
      font-size: 15px;
      padding: 14px 0;
      border-bottom: 2px solid #dcdfe6;
    }
    
    td {
      font-size: 14px;
      padding: 14px 0;
      border-bottom: 1px solid #ebeef5;
    }
    
    .el-table__row {
      transition: all 0.3s;
      
      &:hover {
        background-color: #f5f7fa;
      }
      
      &:nth-child(even) {
        background-color: #fafafa;
      }
    }
    
    .el-button {
      padding: 8px 15px;
      font-weight: 500;
      
      &+.el-button {
        margin-left: 10px;
      }
    }
  }
}

.controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  
  .el-button {
    padding: 10px 20px;
    font-weight: 500;
  }
}

.loading-text {
  margin-left: 15px;
  color: #909399;
  font-size: 14px;
  display: flex;
  align-items: center;
  
  &::before {
    content: '';
    display: inline-block;
    width: 14px;
    height: 14px;
    border: 2px solid #409EFF;
    border-radius: 50%;
    border-top-color: transparent;
    margin-right: 8px;
    animation: loading-rotate 0.8s linear infinite;
  }
}

@keyframes loading-rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.details-dialog, .form-dialog {
  :deep(.el-dialog__header) {
    background: linear-gradient(135deg, #67C23A, #409EFF);
    padding: 15px 20px;
    border-radius: 8px 8px 0 0;
    margin-right: 0;
  }
  
  :deep(.el-dialog__title) {
    color: white;
    font-weight: bold;
    font-size: 18px;
  }
  
  :deep(.el-dialog__headerbtn .el-dialog__close) {
    color: white;
  }
  
  :deep(.el-dialog__body) {
    padding: 25px;
  }
}

.form-dialog {
  :deep(.el-form-item__label) {
    font-weight: 500;
    color: #606266;
  }
  
  :deep(.el-input__inner), :deep(.el-textarea__inner) {
    border-radius: 4px;
    border: 1px solid #dcdfe6;
    transition: all 0.3s;
    
    &:focus {
      border-color: #409EFF;
      box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
    }
  }
  
  :deep(.el-select) {
    width: 100%;
  }
}

// 上传组件样式
.upload-container {
  display: flex;
  flex-direction: column;
  gap: 10px;
  
  :deep(.el-upload--picture-card) {
    width: 120px;
    height: 120px;
    border-radius: 8px;
    border: 2px dashed #d9d9d9;
    background-color: #fafafa;
    transition: all 0.3s;
    
    &:hover {
      border-color: #409EFF;
      background-color: #f0f9ff;
    }
  }
  
  :deep(.el-upload-list--picture-card) {
    .el-upload-list__item {
      width: 120px;
      height: 120px;
      border-radius: 8px;
      overflow: hidden;
      border: 2px solid #dcdfe6;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      margin-right: 16px;
      
      &:hover {
        border-color: #409EFF;
        transform: translateY(-3px);
        box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
      }
    }
  }
  
  .upload-content {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    height: 100%;
    
    .upload-icon {
      font-size: 24px;
      color: #8c939d;
      margin-bottom: 8px;
    }
    
    .upload-text {
      font-size: 14px;
      color: #606266;
    }
  }
  
  // 统一图片预览效果
  :deep(.el-upload-list__item-thumbnail) {
    object-fit: cover;
    transition: transform 0.3s ease;
  }
  
  :deep(.el-upload-list__item:hover .el-upload-list__item-thumbnail) {
    transform: scale(1.1);
  }
  
  .upload-tip {
    display: flex;
    align-items: center;
    gap: 6px;
    color: #909399;
    font-size: 13px;
    margin-top: 5px;
    padding: 8px 12px;
    background-color: #f0f9ff;
    border-radius: 4px;
    border-left: 3px solid #409EFF;
    
    .el-icon {
      color: #409EFF;
      font-size: 16px;
    }
  }
}

.details-content {
  /* 图片画廊样式 */
  .image-gallery {
    display: flex;
    flex-wrap: wrap;
    gap: 15px;
  }
  
  .image-item {
    width: 120px;
    height: 120px;
    border-radius: 8px;
    overflow: hidden;
    cursor: pointer;
    border: 1px solid #ddd;
    position: relative;
    
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform 0.3s ease;
    }
    
    .image-overlay {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background-color: rgba(0, 0, 0, 0.3);
      display: flex;
      align-items: center;
      justify-content: center;
      opacity: 0;
      transition: opacity 0.3s ease;
      
      .el-icon {
        font-size: 24px;
        color: white;
      }
    }
    
    &:hover {
      img {
        transform: scale(1.1);
      }
      
      .image-overlay {
        opacity: 1;
      }
    }
  }
  
  .el-descriptions {
    margin-bottom: 20px;
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
    
    :deep(.el-descriptions__header) {
      margin-bottom: 15px;
    }
    
    :deep(.el-descriptions__label) {
      font-weight: bold;
      color: #606266;
      background-color: #f5f7fa;
    }
    
    :deep(.el-descriptions__content) {
      padding: 12px 15px;
      line-height: 1.6;
    }
  }
  
  .attachment-preview {
    position: relative;
    display: inline-block;
    border-radius: 8px;
    overflow: hidden;
    width: 120px;
    height: 120px;
    border: 1px solid #ddd;
    cursor: pointer;
    margin: 10px 5px;
    
    &:hover {
      img {
        transform: scale(1.1);
      }
      
      .preview-hint {
        opacity: 1;
      }
    }
    
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform 0.3s ease;
    }
    
    .preview-hint {
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      background: rgba(0, 0, 0, 0.5);
      color: white;
      padding: 5px;
      text-align: center;
      opacity: 0;
      transition: opacity 0.3s ease;
      font-size: 12px;
    }
  }
}

// 图片预览对话框样式
:deep(.image-preview-dialog) {
  z-index: 3000 !important; // 确保在最顶层

  .el-dialog__body {
    padding: 0;
  }
  
  img {
    display: block;
    max-width: 100%;
    max-height: 80vh;
    margin: 0 auto;
  }
}
</style> 