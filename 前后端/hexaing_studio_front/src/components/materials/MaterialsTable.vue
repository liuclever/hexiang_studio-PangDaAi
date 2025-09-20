<template>
  <div class="materials-container">
    <el-card shadow="hover" class="table-card">
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="5" animated />
      </div>

      <el-empty v-else-if="!materials || materials.length === 0" description="暂无资料" />

      <el-table
        v-else
        :data="materials"
        border
        style="width: 100%"
      >
        <el-table-column prop="fileName" label="文件名" min-width="160">
          <template #default="{ row }">
            <div class="file-name-cell" v-if="row">
              <el-icon :size="20" class="file-icon">
                <component :is="getFileIcon(row.fileType || '')" />
              </el-icon>
              <span class="file-name">{{ row.fileName || '未命名文件' }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="category" label="分类" width="120">
          <template #default="{ row }">
            <el-tag size="small" v-if="row && row.categoryId">{{ getCategoryName(row.categoryId) }}</el-tag>
            <el-tag size="small" v-else>未分类</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="description" label="描述" min-width="150">
          <template #default="{ row }">
            <span>{{ row && row.description ? row.description : '无描述' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="fileSize" label="大小" width="100">
          <template #default="{ row }">
            {{ row ? formatFileSize(row.fileSize) : '未知' }}
          </template>
        </el-table-column>

        <el-table-column prop="uploadTime" label="上传时间" width="150">
          <template #default="{ row }">
            {{ row && row.uploadTime ? formatDate(row.uploadTime) : '未知' }}
          </template>
        </el-table-column>

        <el-table-column prop="uploader" label="上传人" width="120">
          <template #default="{ row }">
            {{ row && row.uploader ? row.uploader : '未知' }}
          </template>
        </el-table-column>

        <el-table-column prop="downloadCount" label="下载次数" width="90" align="center">
          <template #default="{ row }">
            {{ row && row.downloadCount !== undefined ? row.downloadCount : 0 }}
          </template>
        </el-table-column>

        <el-table-column label="权限" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row" :type="row.isPublic ? 'success' : 'warning'" size="small">
              {{ row.isPublic ? '公开' : '私有' }}
            </el-tag>
            <el-tag v-else type="info" size="small">未知</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <div class="operation-buttons">
              <el-button size="small" type="primary" @click="$emit('download', row)">
                下载
              </el-button>
              <el-button
                size="small"
                type="warning"
                @click="$emit('edit', row)"
              >
                编辑
              </el-button>
              <el-button
                size="small"
                type="danger"
                @click="$emit('delete', row)"
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
          :total="total"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :current-page="currentPage"
          @size-change="$emit('update:pageSize', $event)"
          @current-change="$emit('update:currentPage', $event)"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { format } from 'date-fns';
import {
  Document,
  Picture,
  VideoCamera,
  Headset,
  Files,
  Folder
} from '@element-plus/icons-vue';

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
}

const props = defineProps<{
  loading: boolean;
  materials: Material[];
  categories: Category[];
  total: number;
  pageSize: number;
  currentPage: number;
  currentUser: User;
  isAdmin: boolean;
}>();

defineEmits<{
  (e: 'download', material: Material): void;
  (e: 'edit', material: Material): void;
  (e: 'delete', material: Material): void;
  (e: 'update:pageSize', size: number): void;
  (e: 'update:currentPage', page: number): void;
}>();

// Utility functions
const getCategoryName = (categoryId: number): string => {
  const category = props.categories.find(item => item.id === categoryId);
  return category ? category.name : '未分类';
};

const formatFileSize = (size: number | null): string => {
  if (size === null) return '未知';
  if (size < 1024) return size + ' B';
  if (size < 1024 * 1024) return (size / 1024).toFixed(2) + ' KB';
  if (size < 1024 * 1024 * 1024) return (size / (1024 * 1024)).toFixed(2) + ' MB';
  return (size / (1024 * 1024 * 1024)).toFixed(2) + ' GB';
};

const formatDate = (date: string | Date): string => {
  return format(new Date(date), 'yyyy-MM-dd HH:mm');
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

const getFileIcon = (fileType: string) => {
  const type = getFileCategory(fileType);
  switch (type) {
    case 'document': return Document;
    case 'image': return Picture;
    case 'video': return VideoCamera;
    case 'audio': return Headset;
    case 'archive': return Files;
    default: return Folder;
  }
};
</script>

<style lang="scss" scoped>
.materials-container {
  .table-card {
    .loading-container {
      padding: 20px 0;
    }

    .file-name-cell {
      display: flex;
      align-items: center;
      gap: 10px;

      .file-icon {
        color: var(--el-color-primary);
      }

      .file-name {
        flex: 1;
        word-break: break-all;
      }
    }

    .operation-buttons {
      display: flex;
      flex-wrap: wrap;
      gap: 5px;
      justify-content: center;
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
  }
}
</style> 