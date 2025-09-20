<template>
  <el-dialog 
    v-model="dialogVisible" 
    :title="isEditing ? '编辑公告' : '发布公告'" 
    width="700px"
  >
    <el-form 
      ref="announcementFormRef" 
      :model="announcementForm" 
      :rules="formRules" 
      label-width="100px"
    >
      <el-form-item label="标题" prop="title">
        <el-input v-model="announcementForm.title" placeholder="请输入公告标题" />
      </el-form-item>
      
      <el-form-item label="类型" prop="type">
        <el-select v-model="announcementForm.type" placeholder="请选择公告类型" style="width: 100%">
          <el-option label="通知" :value="0" />
          <el-option label="活动" :value="1" />
          <el-option label="新闻" :value="2" />
          <el-option label="其他" :value="3" />
        </el-select>
      </el-form-item>
      
      <el-form-item label="内容" prop="content">
        <el-input 
          v-model="announcementForm.content" 
          type="textarea" 
          placeholder="请输入公告内容" 
          :rows="8"
        />
      </el-form-item>
      
      <!-- 图片上传 -->
      <el-form-item label="展示图片">
        <el-upload
          action="#"
          :auto-upload="false"
          :on-change="handleImageChange"
          :on-remove="handleImageRemove"
          :file-list="imageFileList"
          multiple
          list-type="picture-card"
          accept="image/jpeg,image/png,image/gif,image/webp"
          :on-preview="handlePictureCardPreview"
          :before-upload="validateImageUpload"
        >
          <el-icon><Plus /></el-icon>
          <template #tip>
            <div class="el-upload__tip">可上传JPG/PNG图片作为公告展示图，单个图片不超过5MB</div>
          </template>
        </el-upload>
      </el-form-item>
      
      <!-- 附件上传 -->
      <el-form-item label="附件">
        <el-upload
          action="#"
          :auto-upload="false"
          :on-change="handleFileChange"
          :on-remove="handleFileRemove"
          :file-list="fileList"
          multiple
          :before-upload="validateFileUpload"
        >
          <el-button type="primary">选择文件</el-button>
          <template #tip>
            <div class="el-upload__tip">可上传任意类型文件，单个文件不超过10MB</div>
          </template>
        </el-upload>
      </el-form-item>
      
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="announcementForm.status">
          <el-radio :value="'1'" label="立即发布" />
          <el-radio :value="'0'" label="保存为草稿" />
        </el-radio-group>
      </el-form-item>
    </el-form>
    
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="hideDialog">取消</el-button>
        <el-button type="primary" @click="saveAnnouncement">确定</el-button>
      </span>
    </template>
  </el-dialog>
  
  <!-- 图片预览 -->
  <el-image-viewer
    v-if="previewVisible"
    :url-list="previewImages"
    :initial-index="previewIndex"
    @close="previewVisible = false"
    teleported
  />
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed } from 'vue';
import { ElMessage, type FormInstance, ElImageViewer } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import { addNotice, updateNotice } from '@/utils/api/notice'; // 1. 导入API函数
import { validateFileType, DANGEROUS_EXTENSIONS, getFileExtension } from '@/utils/fileUtils';

// 定义属性
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  editData: {
    type: Object,
    default: null
  }
});

// 定义事件
const emit = defineEmits(['update:visible', 'success']);

// 对话框显示状态 - 使用计算属性实现双向绑定
const dialogVisible = computed({
  get: () => props.visible,
  set: (value) => {
    emit('update:visible', value);
  }
});

// 表单引用
const announcementFormRef = ref<FormInstance>();

// 文件列表
const fileList = ref<any[]>([]);
const imageFileList = ref<any[]>([]);

// 图片预览
const previewVisible = ref(false);
const previewImages = ref<string[]>([]);
const previewIndex = ref(0);

// 是否为编辑模式
const isEditing = ref(false);

// 表单数据
const announcementForm = reactive<{
  id?: number;
  title: string;
  content: string;
  type: number;
  status: string;
  isTop?: number;
  publishDate?: string | Date;
  images: Array<{
    id?: number;
    name: string;
    url: string;
    size: number;
    raw?: File;
  }>;
  attachments: Array<{
    id?: number;
    name: string;
    url: string;
    size: number;
    raw?: File;
  }>;
}>({
  title: '',
  content: '',
  type: 1,
  status: '1',
  isTop: 0,
  publishDate: '',
  images: [],
  attachments: []
});

// 表单验证规则
const formRules = {
  title: [
    { required: true, message: '请输入公告标题', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择公告类型', trigger: 'change' }
  ],
  content: [
    { required: true, message: '请输入公告内容', trigger: 'blur' }
  ],
  status: [
    { required: true, message: '请选择发布状态', trigger: 'change' }
  ]
};

// 验证图片上传
const validateImageUpload = (file: File) => {
  // 验证文件类型
  const extension = getFileExtension(file.name).toLowerCase();
  
  // 检查是否是危险文件类型
  if (DANGEROUS_EXTENSIONS.includes(extension)) {
    ElMessage.error(`不允许上传 ${extension} 类型的文件，该类型可能存在安全风险`);
    return false;
  }
  
  // 严格限制只允许上传图片文件
  const allowedImageTypes = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'];
  if (!allowedImageTypes.includes(extension)) {
    ElMessage.error(`展示图片只支持上传 ${allowedImageTypes.join(', ')} 格式的图片文件，不允许上传其他类型文件`);
    return false;
  }
  
  // 验证文件MIME类型
  const allowedMimeTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/bmp'];
  if (!allowedMimeTypes.includes(file.type)) {
    ElMessage.error(`文件类型不匹配，请确保上传的是图片文件`);
    return false;
  }
  
  // 验证文件大小（5MB限制）
  const maxSize = 5 * 1024 * 1024; // 5MB
  if (file.size > maxSize) {
    ElMessage.error(`图片大小不能超过5MB`);
    return false;
  }
  
  return true;
};

// 验证附件上传
const validateFileUpload = (file: File) => {
  // 验证文件类型
  const extension = getFileExtension(file.name).toLowerCase();
  
  // 检查是否是危险文件类型
  if (DANGEROUS_EXTENSIONS.includes(extension)) {
    ElMessage.error(`不允许上传 ${extension} 类型的文件，该类型可能存在安全风险`);
    return false;
  }
  
  // 验证文件大小（10MB限制）
  const maxSize = 10 * 1024 * 1024; // 10MB
  if (file.size > maxSize) {
    ElMessage.error(`附件大小不能超过10MB`);
    return false;
  }
  
  return true;
};

// 文件处理
const handleImageChange = (file: any, files: any[]) => {
  announcementForm.images = files.map(f => ({
    name: f.name,
    url: f.url || '',
    size: f.size,
    raw: f.raw,
    id: f.id // 保留已有图片的ID
  }));
  imageFileList.value = files;
};

const handleImageRemove = (file: any, files: any[]) => {
  announcementForm.images = files.map(f => ({
    name: f.name,
    url: f.url || '',
    size: f.size,
    raw: f.raw,
    id: f.id
  }));
  imageFileList.value = files;
};

const handleFileChange = (file: any, files: any[]) => {
  announcementForm.attachments = files.map(f => ({
    name: f.name,
    url: f.url || '',
    size: f.size,
    raw: f.raw,
    id: f.id // 保留已有附件的ID
  }));
  fileList.value = files;
};

const handleFileRemove = (file: any, files: any[]) => {
  announcementForm.attachments = files.map(f => ({
    name: f.name,
    url: f.url || '',
    size: f.size,
    raw: f.raw,
    id: f.id
  }));
  fileList.value = files;
};

const handlePictureCardPreview = (file: any) => {
  const imageUrls = imageFileList.value.map(f => f.url).filter(url => url);
  const findIndex = imageUrls.findIndex(url => url === file.url);
  if (findIndex !== -1) {
    previewImages.value = imageUrls;
    previewIndex.value = findIndex;
    previewVisible.value = true;
  }
};


// 重置表单
const resetForm = () => {
  Object.assign(announcementForm, {
    id: 0,
    title: '',
    content: '',
    type: 1,
    publishDate: '',
    status: '1',
    isTop: 0,
    images: [],
    attachments: []
  });
  
  fileList.value = [];
  imageFileList.value = [];
  
  // 重置表单校验
  if (announcementFormRef.value) {
    announcementFormRef.value.resetFields();
  }
};

// 提交表单
const saveAnnouncement = async () => {
  if (!announcementFormRef.value) return;

  await announcementFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (isEditing.value) {
          // 编辑模式
          await updateNotice(announcementForm);
          ElMessage.success('更新成功');
        } else {
          // 新增模式
          await addNotice(announcementForm);
          ElMessage.success('发布成功');
        }
        hideDialog();
        emit('success');
      } catch (error) {
        console.error('保存公告失败:', error);
        ElMessage.error('操作失败，请稍后重试');
      }
    }
  });
};

// 关闭对话框
const hideDialog = () => {
  dialogVisible.value = false;
};


// 初始化表单
watch(() => props.editData, (newData) => {
  if (newData) {
    isEditing.value = true;
    console.log('编辑公告数据:', newData);
    
    // 创建一个新对象，避免直接修改props
    const formData = {...newData};
    
    // 确保状态值正确转换为字符串格式
    if (formData.status !== undefined && formData.status !== null) {
      const statusStr = String(formData.status);
      // 明确地将 '1' 或 'published' 视为发布状态，其他所有情况视为草稿
      formData.status = (statusStr === '1' || statusStr === 'published') ? '1' : '0';
      console.log('设置表单状态值:', formData.status);
    } else {
      // 如果传入的状态是undefined或null，默认为草稿
      formData.status = '0';
    }
    
    // 将处理后的数据应用到表单
    Object.assign(announcementForm, formData);
    
    // 设置附件文件列表
    if (newData.attachments && newData.attachments.length > 0) {
      fileList.value = newData.attachments.map((attachment: any) => ({
        name: attachment.name || attachment.fileName || '',
        url: attachment.url || '',
        id: attachment.id || attachment.attachmentId || undefined,
        size: attachment.size || attachment.fileSize || 0,
        uid: attachment.id || attachment.attachmentId || Date.now() + Math.random().toString(36).substring(2, 9)
      }));
      
      // 更新表单中的附件数据
      announcementForm.attachments = newData.attachments.map((attachment: any) => ({
        name: attachment.name || attachment.fileName || '',
        url: attachment.url || '',
        id: attachment.id || attachment.attachmentId || undefined,
        size: attachment.size || attachment.fileSize || 0
      }));
      
      console.log('设置附件列表:', fileList.value);
    } else {
      fileList.value = [];
      announcementForm.attachments = [];
    }
    
    // 设置图片列表
    if (newData.images && newData.images.length > 0) {
      imageFileList.value = newData.images.map((image: any) => ({
        name: image.name || image.imageName || '',
        url: image.url || '',
        id: image.id || image.imageId || undefined,
        size: image.size || image.fileSize || 0,
        uid: image.id || image.imageId || Date.now() + Math.random().toString(36).substring(2, 9)
      }));

      // 更新表单中的图片数据
      announcementForm.images = newData.images.map((image: any) => ({
        name: image.name || image.imageName || '',
        url: image.url || '',
        id: image.id || image.imageId || undefined,
        size: image.size || image.fileSize || 0
      }));
      
      // 设置预览图片
      previewImages.value = imageFileList.value.map(img => img.url).filter(url => url);
      
      console.log('设置图片列表:', imageFileList.value);
    } else {
      imageFileList.value = [];
      announcementForm.images = [];
      previewImages.value = [];
    }
  } else {
    isEditing.value = false;
    resetForm();
  }
}, { immediate: true, deep: true });
</script>

<style lang="scss" scoped>
// 对话框样式继承自Element Plus，这里可以添加一些自定义样式
.el-upload__tip {
  line-height: 1.2;
  margin-top: 5px;
  color: var(--el-text-color-secondary);
}
</style> 