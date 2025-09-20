<template>
  <el-dialog
    :model-value="visible"
    :title="`${user?.name || '用户'}的荣誉与证书`"
    width="80%"
    @update:model-value="$emit('update:visible', $event)"
    :before-close="handleClose"
    class="honor-certificate-dialog"
  >
    <el-tabs v-model="activeTab" type="border-card">
      <!-- 荣誉管理标签页 -->
      <el-tab-pane label="荣誉管理" name="honors">
        <div class="controls">
          <el-button type="primary" @click="openFormDialog('honor')" :icon="Plus">添加荣誉</el-button>
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
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
    
    <el-dialog v-model="previewVisible">
      <img w-full :src="previewImageUrl" alt="Preview Image" style="width: 100%;" />
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailsDialogVisible"
      :title="detailsTitle"
      width="700px"
      append-to-body
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
            <div class="attachment-preview" @click="previewAttachment">
              <img :src="getAttachmentUrl(selectedItem.attachment)" alt="附件" style="max-width: 100%; max-height: 300px; object-fit: contain; cursor: pointer; border-radius: 4px;">
              <div class="preview-hint">点击查看大图</div>
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
import { Plus, Delete, ZoomIn } from '@element-plus/icons-vue';
import request from '@/utils/request';
import { UserVo, UserHonor, UserCertificate } from './types';

type FormType = 'honor' | 'certificate';

const props = defineProps<{
  visible: boolean;
  user: UserVo | null;
}>();

const emit = defineEmits(['update:visible', 'refresh']);

const activeTab = ref<FormType>('honor');
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
    fetchData('honor');
    fetchData('certificate');
  }
});

const handleClose = () => {
  emit('update:visible', false);
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
  
  const downloadUrl = `/api/admin/file/view/${attachmentPath}?download=true&originalName=${encodeURIComponent(originalName)}`;
  
  // 使用a标签触发下载，兼容性更好
  const link = document.createElement('a');
  link.href = downloadUrl;
  link.target = '_blank'; // 在新标签页打开，避免页面跳转
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
};

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

.honor-certificate-dialog :deep(.el-dialog__body) {
  padding-top: 10px;
}
</style> 