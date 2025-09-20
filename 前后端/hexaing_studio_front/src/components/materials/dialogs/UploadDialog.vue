<template>
  <el-dialog
    :model-value="modelValue"
    title="上传资料"
    width="600px"
    @update:modelValue="$emit('update:modelValue', $event)"
    @close="resetForm"
  >
    <el-form
      ref="materialFormRef"
      :model="materialForm"
      :rules="formRules"
      label-width="100px"
    >
      <el-form-item label="资料分类" prop="categoryId">
        <el-select v-model="materialForm.categoryId" placeholder="请选择资料分类" style="width: 100%">
          <el-option
            v-for="category in categories"
            :key="category.id"
            :label="category.name"
            :value="category.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="资料描述" prop="description">
        <el-input
          v-model="materialForm.description"
          type="textarea"
          placeholder="请输入资料描述"
          :rows="3"
        />
      </el-form-item>

      <el-form-item label="权限设置" prop="isPublic">
        <el-radio-group v-model="materialForm.isPublic">
          <el-radio :label="true">公开</el-radio>
          <el-radio :label="false">私有</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="文件">
        <el-upload
          ref="uploadRef"
          action="#"
          :auto-upload="false"
          :on-change="handleFileChange"
          :before-upload="validateFile"
          :limit="5"
          multiple
          :accept="allowedFileExtensions"
        >
          <template #trigger>
            <el-button type="primary">选择文件</el-button>
          </template>

          <template #tip>
            <div class="el-upload__tip">
              支持上传的文件类型：图片、文档、视频、音频、压缩包等，单个文件不超过100MB。
              <div class="el-upload__warning">
                注意：不允许上传可执行文件（如.exe、.bat、.py等）和脚本文件，这些文件可能存在安全风险。
              </div>
            </div>
          </template>

          <template #file="{ file }">
            <div class="upload-file-item">
              <el-icon class="file-icon">
                <component :is="getFileIcon(getExtension(file.name))" />
              </el-icon>
              <span class="file-name">{{ file.name }}</span>
              <span class="file-size">{{ formatFileSize(file.size) }}</span>
            </div>
          </template>
        </el-upload>
      </el-form-item>
    </el-form>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="$emit('update:modelValue', false)">取消</el-button>
        <el-button type="primary" @click="submitUpload">上传</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed } from 'vue';
import { ElMessage } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import { Document, Picture, VideoCamera, Headset, Files, Folder } from '@element-plus/icons-vue';
import { validateFileType, ALLOWED_EXTENSIONS, DANGEROUS_EXTENSIONS, FILE_TYPES, formatFileSize as formatSize, getFileTypeCategory, getFileExtension as getExtension } from '@/utils/fileUtils';

interface Category {
  id: number;
  name: string;
  orderId: number;
}

const props = defineProps<{
  modelValue: boolean;
  categories: Category[];
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void;
  (e: 'upload', formData: FormData): void;
}>();

const materialFormRef = ref<FormInstance>();
const uploadRef = ref<any>(null);

const materialForm = reactive({
  categoryId: 0,
  description: '',
  isPublic: true,
  files: [] as any[],
});

const formRules = reactive<FormRules>({
  categoryId: [{ required: true, message: '请选择资料分类', trigger: 'change' }],
  description: [{ required: true, message: '请输入资料描述', trigger: 'blur' }],
});

// 计算允许的文件扩展名，用于el-upload的accept属性
const allowedFileExtensions = computed(() => {
  return ALLOWED_EXTENSIONS.map(ext => `.${ext}`).join(',');
});

watch(() => props.modelValue, (newValue) => {
  if (newValue) {
    resetForm();
    if (props.categories.length > 0) {
      materialForm.categoryId = props.categories[0].id;
    }
  }
});

// 验证文件类型和大小
const validateFile = (file: File) => {
  // 验证文件类型
  const extension = getExtension(file.name);
  
  // 检查是否是危险文件类型
  if (DANGEROUS_EXTENSIONS.includes(extension)) {
    ElMessage.error(`不允许上传 ${extension} 类型的文件，该类型可能存在安全风险`);
    return false;
  }
  
  // 检查是否在允许的文件类型列表中
  const isValidType = validateFileType(file);
  if (!isValidType) {
    ElMessage.error(`不支持上传 ${extension} 类型的文件`);
    return false;
  }
  
  // 验证文件大小（100MB限制）
  const maxSize = 100 * 1024 * 1024; // 100MB
  if (file.size > maxSize) {
    ElMessage.error(`文件大小不能超过100MB`);
    return false;
  }
  
  return true;
};

const handleFileChange = (file: any, fileList: any[]) => {
  materialForm.files = fileList;
};

const submitUpload = async () => {
  if (!materialFormRef.value) return;

  await materialFormRef.value.validate(async (valid) => {
    if (valid) {
      if (materialForm.files.length === 0) {
        ElMessage.error('请选择要上传的文件');
        return;
      }

      const formData = new FormData();
      formData.append('categoryId', materialForm.categoryId.toString());
      formData.append('isPublic', materialForm.isPublic ? '1' : '0');
      if (materialForm.description && materialForm.description.trim() !== '') {
        formData.append('description', materialForm.description);
      }
      materialForm.files.forEach((file: any) => {
        formData.append('files', file.raw);
      });

      emit('upload', formData);
    } else {
      ElMessage.error('请完善表单信息');
    }
  });
};

const resetForm = () => {
  materialFormRef.value?.resetFields();
  materialForm.files = [];
  uploadRef.value?.clearFiles();
};

// 使用从fileUtils导入的getExtension函数

const formatFileSize = (size: number | null): string => {
  if (size === null) return '未知';
  return formatSize(size);
};

const getFileIcon = (fileType: string) => {
  const category = getFileTypeCategory(fileType);
  
  switch (category) {
    case 'IMAGE':
      return Picture;
    case 'VIDEO':
      return VideoCamera;
    case 'AUDIO':
      return Headset;
    case 'ARCHIVE':
      return Folder;
    case 'DOCUMENT':
  return Document;
    default:
      return Files;
  }
};

</script>

<style lang="scss" scoped>
.upload-file-item {
  display: flex;
  align-items: center;
  padding: 5px 0;

  .file-icon {
    margin-right: 8px;
    color: var(--el-color-primary);
  }

  .file-name {
    flex: 1;
    margin-right: 15px;
  }
}

.el-upload__warning {
  color: var(--el-color-danger);
  margin-top: 5px;
  font-size: 12px;
  line-height: 1.4;
}
</style> 