<template>
  <div class="announcements-view">
    <el-card shadow="hover" class="page-header-card">
      <div class="page-title">
        <h2>公告管理</h2>
        <div class="header-actions">
          <el-button type="danger" @click="handleBatchDelete" :disabled="selectedAnnouncements.length === 0">
            <el-icon><Delete /></el-icon>批量删除
          </el-button>
          <el-button type="primary" @click="showAddAnnouncementDialog">
            <el-icon><Plus /></el-icon>发布公告
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 搜索和筛选 -->
    <el-card shadow="hover" class="search-card">
      <div class="search-container">
        <el-input 
          v-model="searchQuery" 
          placeholder="搜索公告标题或内容" 
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
          <el-select v-model="filterType" placeholder="公告类型" clearable @change="handleSearch">
            <el-option label="全部类型" value="" />
            <el-option label="通知" value="0" />
            <el-option label="活动" value="1" />
            <el-option label="新闻" value="2" />
            <el-option label="其他" value="3" />
          </el-select>
          
          <el-select v-model="filterStatus" placeholder="状态" clearable @change="handleSearch">
            <el-option label="全部状态" value="" />
            <el-option label="已发布" value="published" />
            <el-option label="草稿" value="draft" />
          </el-select>
          
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            @change="handleSearch"
          />
        </div>
      </div>
    </el-card>

    <!-- 公告列表 -->
    <div class="announcements-container">
      <el-card shadow="hover" class="table-card">
      <el-empty v-if="announcements.length === 0" description="暂无公告" />
      
      <el-table
        v-else
        :data="announcements"
        style="width: 100%"
        @selection-change="handleSelectionChange"
        border
        stripe
      >
        <el-table-column type="selection" width="55" />
        <el-table-column label="类型" width="100">
          <template #default="scope">
            <el-tag :type="getTypeTagType(scope.row.type)" class="type-tag">
              {{ getTypeLabel(scope.row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="title" label="标题" min-width="200" />
        
        <el-table-column label="内容" min-width="300">
          <template #default="scope">
            {{ truncateContent(scope.row.content) }}
          </template>
        </el-table-column>
        
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status == 1 ? 'success' : 'info'" size="small">
              {{ scope.row.status == 1 ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="发布人" width="120" prop="publisher" />
        
        <el-table-column label="发布时间" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.publishDate) }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="viewAnnouncement(scope.row)">查看</el-button>
            <el-button type="primary" size="small" @click="editAnnouncement(scope.row)">编辑</el-button>
            <el-button type="danger" size="small" @click="deleteAnnouncement(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          background
            layout="total, sizes, prev, pager, next, jumper"
          :total="totalAnnouncements"
          :page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
          :current-page="currentPage"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
      </el-card>
    </div>

    <!-- 新增/编辑公告对话框 -->
    <announcement-dialog
      v-model:visible="dialogVisible"
      :edit-data="isEditing ? {...currentAnnouncement} : undefined"
      @success="handleAnnouncementSave"
    />

    <!-- 查看公告对话框 -->
    <el-dialog 
      v-model="viewDialogVisible" 
      :title="currentAnnouncement?.title" 
      width="700px"
      destroy-on-close
    >
      <div v-if="currentAnnouncement" class="announcement-detail">
        <div class="announcement-detail-header">
          <el-tag :type="getTypeTagType(currentAnnouncement.type)" class="type-tag">
            {{ getTypeLabel(currentAnnouncement.type) }}
          </el-tag>
          <div class="announcement-meta">
            <span>发布人: {{ currentAnnouncement.publisher }}</span>
            <span>发布时间: {{ formatDate(currentAnnouncement.publishDate) }}</span>
          </div>
        </div>
        
        <div class="announcement-detail-content">
          <p>{{ currentAnnouncement.content }}</p>
        </div>
        
        <!-- 图片区域 -->
        <div v-if="currentAnnouncement.images && currentAnnouncement.images.length > 0" class="announcement-images">
          <h4>展示图片:</h4>
          <div class="image-gallery">
            <div 
              v-for="(image, index) in currentAnnouncement.images" 
              :key="'img-' + index" 
              class="image-item"
              @click="previewImage(index)"
            >
              <img :src="getFileUrl(image.filePath)" :alt="image.imageName" />
            </div>
          </div>
        </div>
        
        <!-- 附件区域 -->
        <div v-if="currentAnnouncement.attachments && currentAnnouncement.attachments.length > 0" class="announcement-attachments">
          <h4>附件:</h4>
          <ul class="attachment-list">
            <li v-for="attachment in currentAnnouncement.attachments" :key="attachment.attachmentId" class="attachment-item">
              <el-link 
                :underline="false" 
                type="primary" 
                @click="downloadFile(attachment.filePath, attachment.name)"
              >
                <div class="attachment-info">
                  <el-icon class="attachment-icon">
                    <Document />
                  </el-icon>
                  <div class="attachment-details">
                    <span class="attachment-name">{{ attachment.name }}</span>
                    <span class="attachment-size" v-if="attachment.size">{{ formatFileSize(attachment.size) }}</span>
                  </div>
                </div>
              </el-link>
            </li>
          </ul>
        </div>
      </div>
    </el-dialog>
    
    <!-- 图片预览 -->
    <el-image-viewer
      v-if="previewVisible"
      :url-list="previewImages"
      :initial-index="previewIndex"
      @close="previewVisible = false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox, ElImageViewer, ElLoading } from 'element-plus';
import { Plus, Search, Document, Delete } from '@element-plus/icons-vue';
import { format } from 'date-fns';
import AnnouncementDialog from '../components/dialogs/AnnouncementDialog.vue';
import { useRoute, useRouter } from 'vue-router'; // 引入 useRouter
import { getNoticeList, getNoticeDetail, addNotice, updateNotice, deleteNotice, type NoticeData, type PageNoticeParams } from '../utils/api/notice';
import service from '../utils/request';

// 公告数据接口
interface Announcement {
  id: number;
  title: string;
  content: string;
  type: number; // 修正: 字符串改为数字
  publisher: string; // 发布人字段
  publishDate: Date | string;
  status: number | string; // 允许字符串类型以适配前端组件
  images?: Array<{
    id: number;
    name: string;
    url: string;
    size: number;
    raw?: File;
    filePath: string; // 新增：文件相对路径
    imageName: string; // 新增：图片名称
  }>;
  attachments?: Array<{
    id: number;
    name: string;
    url: string;
    size: number;
    raw?: File;
    attachmentId: number; // 新增：附件ID
    filePath: string; // 新增：文件相对路径
  }>;
}

interface Attachment {
  id: number;
  name: string;
  url: string;
  size: number;
  raw?: File;
}

// 表格数据和控制
const announcements = ref<Announcement[]>([]);
const totalAnnouncements = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);
const searchQuery = ref('');
const filterType = ref('');
const filterStatus = ref('');
const dateRange = ref<[string, string] | null>(null);
const selectedAnnouncements = ref<Announcement[]>([]);

// 对话框控制
const dialogVisible = ref(false);
const isEditing = ref(false);
const viewDialogVisible = ref(false);
const currentAnnouncement = ref<Announcement | null>(null);

// 图片预览相关
const previewVisible = ref(false);
const previewImages = ref<string[]>([]);
const previewIndex = ref(0);

/**
 * 根据文件相对路径生成完整的后端访问URL。
 * @param filePath 文件的相对路径
 * @param download 是否为下载链接
 * @param originalName 文件的原始名称
 */
const getFileUrl = (filePath: string, download = false, originalName?: string): string => {
  if (!filePath) return '';
  // 修正baseUrl，以匹配后端文件访问接口和Vite代理配置
  const baseUrl = '/api/admin/file/view/'; 
  let url = `${baseUrl}${filePath}`;
  if (download) {
    url = `${url}?download=true`;
    if (originalName) {
      url = `${url}&originalName=${encodeURIComponent(originalName)}`;
    }
  }
  return url;
};

/**
 * 下载文件。
 * @param filePath 文件的相对路径
 * @param originalName 文件的原始名称
 */
const downloadFile = (filePath: string, originalName: string) => {
  if (!filePath) return;
  // 通过打开带 `download=true` 和 `originalName` 参数的URL来触发下载
  window.open(getFileUrl(filePath, true, originalName), '_blank');
};


const previewImage = (startIndex: number) => {
  if (currentAnnouncement.value?.images) {
    previewImages.value = currentAnnouncement.value.images.map(img => getFileUrl(img.filePath));
    previewIndex.value = startIndex;
    previewVisible.value = true;
  }
};


const viewAnnouncement = async (announcement: Announcement) => {
  const loadingInstance = ElLoading.service({ target: '.el-dialog' });
  try {
    const response = await getNoticeDetail(announcement.id);
    const detail = response.data;
    currentAnnouncement.value = {
      id: detail.noticeId,
      title: detail.title,
      content: detail.content,
      type: detail.type,
      publisher: detail.publisher,
      publishDate: detail.publishTime,
      status: detail.status,
      images: detail.images?.map((img: any) => ({
        id: img.imageId,
        name: img.imageName || 'Image', // 使用后端的imageName
        url: getFileUrl(img.filePath), // 直接生成完整的URL
        size: img.fileSize,
        filePath: img.filePath,
        imageName: img.imageName,
        raw: undefined
      })),
      attachments: detail.attachments?.map((att: any) => ({
        id: att.attachmentId,
        attachmentId: att.attachmentId,
        name: att.fileName,
        fileName: att.fileName,
        filePath: att.filePath,
        url: getFileUrl(att.filePath, true), // 为下载生成URL
        size: att.fileSize,
        fileSize: att.fileSize,
        raw: undefined
      }))
    };
    viewDialogVisible.value = true;
  } catch (error) {
    console.error('获取公告详情失败:', error);
    ElMessage.error('获取详情失败');
  } finally {
    loadingInstance.close();
  }
};

// 获取公告列表
const fetchAnnouncements = async () => {
  try {
    const params: PageNoticeParams = {
      page: currentPage.value,
      pageSize: pageSize.value,
      title: searchQuery.value, // <--- Corrected from 'name' to 'title'
      type: filterType.value,
      // 状态筛选值转换为后端需要的字符串
      status: filterStatus.value === 'published' ? '1'
            : filterStatus.value === 'draft' ? '0'
            : undefined,
      beginTime: dateRange.value ? `${dateRange.value[0]} 00:00:00` : undefined,
      endTime: dateRange.value ? `${dateRange.value[1]} 23:59:59` : undefined,
    };
    const response = await getNoticeList(params);
    if (response.data && response.data.records) {
      announcements.value = response.data.records.map((record: any) => ({
        ...record,
        id: record.noticeId, // 将后端的 noticeId 映射到前端的 id
        publishDate: record.publishTime, // 同时映射发布时间
    }));
    totalAnnouncements.value = response.data.total;
    }
  } catch (error) {
    console.error('获取公告列表失败:', error);
    ElMessage.error('获取公告列表失败');
  }
};

// 搜索处理
const handleSearch = () => {
  currentPage.value = 1;
  fetchAnnouncements();
};

// 分页大小改变
const handleSizeChange = (val: number) => {
  pageSize.value = val;
  fetchAnnouncements();
};

// 当前页改变
const handleCurrentChange = (val: number) => {
  currentPage.value = val;
  fetchAnnouncements();
};

// 表格选择项改变
const handleSelectionChange = (val: Announcement[]) => {
  selectedAnnouncements.value = val;
};

// 新增公告
const showAddAnnouncementDialog = () => {
  isEditing.value = false;
  currentAnnouncement.value = null; // Clear edit data
  dialogVisible.value = true;
};

// 编辑公告
const editAnnouncement = async (announcement: Announcement) => {
  try {
    const loadingInstance = ElLoading.service({ fullscreen: true, text: '加载公告详情...' });
    // 获取完整的公告详情，包括图片和附件
    const response = await getNoticeDetail(announcement.id);
    const detail = response.data;
    
      isEditing.value = true;
    
    // 构建完整的公告对象，包括图片和附件
    currentAnnouncement.value = {
      id: detail.noticeId,
      title: detail.title,
      content: detail.content,
      type: detail.type,
      publisher: detail.publisher,
      publishDate: detail.publishTime,
      status: detail.status,
      // 处理图片
      images: detail.images?.map((img: any) => ({
        id: img.imageId,
        name: img.imageName || 'Image',
        url: getFileUrl(img.filePath),
        size: img.fileSize || 0,
        filePath: img.filePath
      })) || [],
      // 处理附件
      attachments: detail.attachments?.map((att: any) => ({
        id: att.attachmentId,
        name: att.fileName || att.name,
        url: getFileUrl(att.filePath),
        size: att.fileSize || 0,
        filePath: att.filePath
      })) || []
    };
    
    dialogVisible.value = true;
    loadingInstance.close();
  } catch (error) {
    console.error('获取公告详情失败:', error);
    ElMessage.error('获取公告详情失败，无法编辑');
  }
};

// 删除公告
const deleteAnnouncement = (announcement: Announcement) => {
  ElMessageBox.confirm(`确定要删除公告 "${announcement.title}" 吗？`, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      await deleteNotice([announcement.id]); // <--- Pass ID as an array
      ElMessage.success('删除成功');
      fetchAnnouncements(); // Refresh list
    } catch (error) {
      ElMessage.error('删除失败');
    }
  }).catch(() => {
    // User canceled
  });
};

// 批量删除
const handleBatchDelete = () => {
  const ids = selectedAnnouncements.value.map(item => item.id);
  ElMessageBox.confirm(`确定要删除选中的 ${ids.length} 条公告吗？`, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      if (ids.length === 0) return;
      await deleteNotice(ids);
      ElMessage.success('批量删除成功');
      fetchAnnouncements(); // Refresh list
    } catch (error) {
      ElMessage.error('批量删除失败');
    }
  }).catch(() => {
    // User canceled
  });
};

// 公告保存成功回调
const handleAnnouncementSave = () => {
  fetchAnnouncements(); // 重新加载公告列表
};

// 截断内容
const truncateContent = (content: string, length = 50) => {
  if (content.length <= length) return content;
  return `${content.substring(0, length)}...`;
};

// 格式化日期
const formatDate = (date: Date | string) => {
  if (!date) return '';
  return format(new Date(date), 'yyyy-MM-dd HH:mm:ss');
};

// 获取公告类型标签
const getTypeLabel = (type: number): string => {
  switch (type) {
    case 0: return '通知';
    case 1: return '活动';
    case 2: return '新闻';
    case 3: return '其他';
    default: return '未知';
  }
};

// 获取公告类型标签样式
const getTypeTagType = (type: number) => {
  switch (type) {
    case 0: return 'primary';
    case 1: return 'success';
    case 2: return 'warning';
    case 3: return 'info';
    default: return 'danger';
  }
};

// 格式化文件大小
const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
};

const route = useRoute();
const router = useRouter(); // 实例化 router

onMounted(() => {
  fetchAnnouncements();

  const { query } = route;

  // 检查并处理 action=new
  if (query.action === 'new') {
    showAddAnnouncementDialog();
    // 使用 replace 清理查询参数，避免影响 noticeId 的处理
    router.replace({ query: { ...query, action: undefined } });
  }

  // 检查并处理 noticeId
  const noticeId = query.noticeId;
  if (noticeId && !isNaN(Number(noticeId))) {
    // 模拟一个只有ID的公告对象来调用详情方法
    viewAnnouncement({ id: Number(noticeId) } as Announcement);
    // 清理 noticeId 参数
    router.replace({ query: { ...query, noticeId: undefined } });
  }
});
</script>

<style scoped>
/* Basic layout and spacing */
.announcements-view {
  padding: 20px;
}
.page-header-card, .search-card, .table-card {
  margin-bottom: 20px;
}

/* Page Header */
    .page-title {
      display: flex;
      justify-content: space-between;
      align-items: center;
}
.header-actions .el-button {
  margin-left: 10px;
}

/* Search and Filter */
    .search-container {
      display: flex;
      align-items: center;
  gap: 20px;
      flex-wrap: wrap;
}
      .search-input {
  max-width: 400px;
  min-width: 300px;
      }
      .filter-container {
        display: flex;
  align-items: center;
        gap: 15px;
}

/* Table and Pagination */
.announcements-container {
  margin-top: 20px;
}
.type-tag {
  cursor: default;
}
          .pagination-container {
      margin-top: 20px;
      display: flex;
      justify-content: center;
}

/* Announcement Detail Dialog */
.announcement-detail {
  padding: 0 10px;
}
.announcement-detail-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}
      .announcement-meta {
  font-size: 13px;
  color: #999;
}
.announcement-meta span {
  margin-left: 20px;
}
    .announcement-detail-content {
      line-height: 1.8;
  margin-bottom: 25px;
  white-space: pre-wrap; /* Renders newlines */
}

/* Images and Attachments in Dialog */
.announcement-images h4, .announcement-attachments h4 {
  margin-bottom: 15px;
  font-size: 16px;
  color: #333;
}
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
}
.image-item img {
            width: 100%;
  height: 100%;
            object-fit: cover;
  transition: transform 0.3s ease;
}
.image-item:hover img {
  transform: scale(1.1);
      }
      
      .attachment-list {
        list-style: none;
        padding: 0;
}
        .attachment-item {
  margin-bottom: 10px;
}
          .attachment-info {
            display: flex;
            align-items: center;
}
            .attachment-icon {
  margin-right: 10px;
  font-size: 20px;
            }
            .attachment-details {
              display: flex;
  flex-direction: column;
}
.attachment-name {
  font-size: 14px;
}
.attachment-size {
  font-size: 12px;
  color: #999;
}

/* Hide delete icon in image viewer */
.announcements-view :deep(.el-image-viewer__actions__divider + span) {
  display: none !important;
}

.table-card :deep(.el-table) {
  font-size: 15px;
}
</style>



