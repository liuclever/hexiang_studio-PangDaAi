<template>
  <div class="info-portal-view">
    <el-card shadow="hover" class="page-header-card">
      <div class="page-title">
        <h2>信息门户</h2>
      </div>
    </el-card>

    <el-row :gutter="20">
      <el-col :xs="24" :sm="24" :md="16" :lg="18">
        <el-card shadow="hover" class="main-content-card">
          <template #header>
            <div class="card-header">
              <h3>新闻公告</h3>
              <el-link type="primary" :underline="false" href="#/announcements">查看全部</el-link>
            </div>
          </template>
          
          <div class="announcement-grid">
            <div v-if="loading" class="loading-placeholder">
              <el-skeleton :rows="3" animated />
            </div>
            
            <div v-else-if="latestAnnouncements.length === 0" class="empty-placeholder">
              暂无公告
            </div>
            
            <div v-else class="announcement-cards">
              <el-card
                v-for="announcement in latestAnnouncements" 
                :key="announcement.id" 
                class="announcement-card"
                shadow="hover"
                @click="viewAnnouncementDetail(announcement)"
              >
                <!-- 图片区域 -->
                <div class="announcement-image">
                  <el-carousel 
                    v-if="announcement.images && announcement.images.length > 1" 
                    height="180px" 
                    indicator-position="none" 
                    arrow="hover"
                    @click.stop
                  >
                    <el-carousel-item v-for="image in announcement.images" :key="image.id">
                      <img :src="image.url" :alt="image.name" class="carousel-img" />
                    </el-carousel-item>
                  </el-carousel>
                  <img 
                    v-else-if="announcement.images && announcement.images.length === 1" 
                    :src="announcement.images[0].url" 
                    :alt="announcement.title" 
                    class="single-img" 
                  />
                  <div class="no-image" v-else>
                    <el-icon><Picture /></el-icon>
                  </div>
                </div>
                
                <!-- 内容区域 -->
                <div class="announcement-info">
                  <div class="announcement-header">
                    <el-tag size="small" :type="getTypeTagType(announcement.type)">
                      {{ getTypeLabel(announcement.type) }}
                    </el-tag>
                    <h4 class="announcement-title">{{ announcement.title }}</h4>
                  </div>
                  
                  <div class="announcement-content">
                    {{ truncateContent(announcement.content, 60) }}
                  </div>

                  <!-- 附件区域 -->
                  <div v-if="announcement.attachments && announcement.attachments.length > 0" class="announcement-card-attachments">
                    <el-divider content-position="left">附件</el-divider>
                    <div 
                      v-for="attachment in announcement.attachments" 
                      :key="attachment.id" 
                      class="attachment-item"
                      @click.stop="downloadAttachment(attachment.url)"
                    >
                      <el-icon><Document /></el-icon>
                      <span class="attachment-name">{{ attachment.name }}</span>
                    </div>
                  </div>
                  
                  <div class="announcement-footer">
                    <span class="date">{{ formatDate(announcement.publishDate) }}</span>
                    <span class="author">{{ announcement.author }}</span>
                  </div>
                </div>
              </el-card>
            </div>
          </div>
        </el-card>
        
        <el-card shadow="hover" class="main-content-card">
          <template #header>
            <div class="card-header">
              <h3>近期活动</h3>
              <span class="activity-hint">向下滑动查看更多</span>
            </div>
          </template>
          
          <div v-if="loadingActivities" class="loading-placeholder">
            <el-skeleton :rows="4" animated />
          </div>
          <div v-else-if="recentActivities.length === 0" class="empty-placeholder">
            暂无活动
          </div>
          <el-scrollbar height="450px" v-else>
            <div class="timeline-container">
              <el-timeline>
                <el-timeline-item
                  v-for="activity in recentActivities"
                  :key="activity.id"
                  :timestamp="activity.time"
                  :type="activity.type"
                  class="timeline-item"
                >
                  <h4>{{ activity.title }}</h4>
                  <p>{{ truncateContent(activity.content, 80) }}</p>
                  <p v-if="activity.location" class="activity-location">
                    <el-icon><Location /></el-icon> {{ activity.location }}
                  </p>
                  
                  <!-- 活动图片 -->
                  <div v-if="activity.images && activity.images.length > 0" class="activity-image">
                    <img :src="activity.images[0].url" :alt="activity.title" />
                  </div>
                </el-timeline-item>
              </el-timeline>
            </div>
          </el-scrollbar>
        </el-card>
      </el-col>
      
      <!-- 侧边栏保持不变 -->
      <el-col :xs="24" :sm="24" :md="8" :lg="6">
        <el-card shadow="hover" class="side-card">
          <template #header>
            <div class="card-header">
              <h3>工作室概况</h3>
            </div>
          </template>
          
          <div v-if="loadingStudio" class="loading-placeholder">
            <el-skeleton :rows="6" animated />
          </div>
          <div v-else class="studio-info">
            <div v-if="studioInfo.name" class="studio-content">
              <div class="info-item" v-if="studioInfo.name">
                <el-icon><OfficeBuilding /></el-icon>
                <span>{{ studioInfo.name }}</span>
              </div>
              <div class="info-item" v-if="studioInfo.director">
                <el-icon><Document /></el-icon>
                <span>负责人：{{ studioInfo.director }}</span>
              </div>
              <div class="info-item" v-if="studioInfo.establishTime">
                <el-icon><Document /></el-icon>
                <span>成立时间：{{ studioInfo.establishTime }}</span>
              </div>
              <div class="info-item" v-if="studioInfo.memberCount">
                <el-icon><Document /></el-icon>
                <span>团队成员：{{ studioInfo.memberCount }} 人</span>
              </div>
              <div class="info-item" v-if="studioInfo.projectCount">
                <el-icon><Document /></el-icon>
                <span>项目数量：{{ studioInfo.projectCount }} 个</span>
              </div>
              <div class="info-item" v-if="studioInfo.awards">
                <el-icon><Document /></el-icon>
                <span>获奖情况：{{ studioInfo.awards }}</span>
              </div>
              <div class="info-item" v-if="studioInfo.address">
                <el-icon><Location /></el-icon>
                <span>{{ studioInfo.address }}</span>
              </div>
              <div class="info-item" v-if="studioInfo.room">
                <el-icon><Location /></el-icon>
                <span>房间号：{{ studioInfo.room }}</span>
              </div>
            </div>
            <div v-else class="empty-placeholder">
              暂无工作室信息
            </div>
          </div>
        </el-card>
        
        <el-card shadow="hover" class="side-card">
          <template #header>
            <div class="card-header">
              <h3>联系方式</h3>
            </div>
          </template>
          
          <div v-if="loadingStudio" class="loading-placeholder">
            <el-skeleton :rows="4" animated />
          </div>
          <div v-else class="contact-info">
            <div v-if="studioInfo.phone || studioInfo.email" class="contact-content">
              <div class="info-item" v-if="studioInfo.phone">
                <el-icon><Phone /></el-icon>
                <span>{{ studioInfo.phone }}</span>
              </div>
              <div class="info-item" v-if="studioInfo.email">
                <el-icon><Message /></el-icon>
                <span>{{ studioInfo.email }}</span>
              </div>
            </div>
            <div v-else class="empty-placeholder">
              暂无联系方式
            </div>
          </div>
        </el-card>
        
        <!-- Quick Links card is now removed -->

      </el-col>
    </el-row>

    <!-- 公告详情对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="currentAnnouncement?.title" 
      width="800px"
      destroy-on-close
      class="announcement-detail-dialog"
    >
      <div v-if="currentAnnouncement" class="announcement-detail">
        <div class="announcement-detail-header">
          <el-tag :type="getTypeTagType(currentAnnouncement.type)" class="type-tag">
            {{ getTypeLabel(currentAnnouncement.type) }}
          </el-tag>
          <div class="announcement-meta">
            <span>发布人: {{ currentAnnouncement.author }}</span>
            <span>发布时间: {{ formatDate(currentAnnouncement.publishDate) }}</span>
          </div>
        </div>
        
        <!-- 图片区域 - 如果有图片，优先展示 -->
        <div v-if="currentAnnouncement.images && currentAnnouncement.images.length > 0" class="announcement-detail-images">
          <el-carousel 
            v-if="currentAnnouncement.images.length > 1" 
            :interval="4000" 
            type="card" 
            height="300px"
            indicator-position="outside"
          >
            <el-carousel-item 
              v-for="(image, index) in currentAnnouncement.images" 
              :key="'carousel-' + index"
              @click="previewImage(image)"
            >
              <div class="carousel-image-container">
                <img :src="image.url" :alt="image.name" />
              </div>
            </el-carousel-item>
          </el-carousel>
          
          <div 
            v-else 
            class="single-image-container"
            @click="previewImage(currentAnnouncement.images[0])"
          >
            <img :src="currentAnnouncement.images[0].url" :alt="currentAnnouncement.images[0].name" />
          </div>
        </div>
        
        <div class="announcement-detail-content">
          <p>{{ currentAnnouncement.content }}</p>
        </div>
        
        <!-- 图片缩略图区域 -->
        <div v-if="currentAnnouncement.images && currentAnnouncement.images.length > 0" class="announcement-images">
          <h4>图片附件:</h4>
          <div class="image-gallery">
            <div 
              v-for="(image, index) in currentAnnouncement.images" 
              :key="'img-' + index" 
              class="image-item"
              @click="previewImage(image)"
            >
              <img :src="image.url" :alt="image.name" />
              <div class="image-name">{{ image.name }}</div>
            </div>
          </div>
        </div>
        
        <!-- 附件区域 -->
        <div v-if="currentAnnouncement.attachments && currentAnnouncement.attachments.length > 0" class="announcement-attachments">
          <h4>文件附件:</h4>
          <ul class="attachment-list">
            <li v-for="(attachment, index) in currentAnnouncement.attachments" :key="'att-' + index" class="attachment-item">
              <el-link :underline="false" type="primary" :href="attachment.url" target="_blank" download>
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
      hide-on-click-modal
      :zoom-rate="1.2"
      :min-scale="0.2"
      :max-scale="7"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { format, subMonths } from 'date-fns';
import { ElImageViewer } from 'element-plus';
import { Document, Location, Phone, Message, OfficeBuilding, Picture } from '@element-plus/icons-vue';
import { getNoticeList, getNoticeDetail, type PageNoticeParams } from '../utils/api/notice';
import { getRecentActivityNotices } from '../utils/api/activity';
import { getStudioInfo } from '../api/studio';

// 公告数据接口
interface Announcement {
  id: number;
  title: string;
  content: string;
  type: 'notice' | 'activity' | 'news' | 'other';
  author: string;
  publishDate: Date | string;
  status: 'published' | 'draft';
  images?: Array<{
    id: number; // 添加id字段
    name: string;
    url: string;
    size: number;
  }>;
  attachments?: Array<{
    id: number; // 添加id字段
    name: string;
    url: string;
    size: number;
  }>;
}

// 活动数据接口
interface Activity {
  id: number;
  title: string;
  content: string;
  time: string;
  location?: string;
  type: 'primary' | 'success' | 'warning' | 'info' | 'danger';
  images?: Array<{
    id: number;
    name: string;
    url: string;
    size: number;
  }>;
}

// 工作室信息接口
interface StudioInfo {
  id?: number;
  name?: string;
  establishTime?: string;
  director?: string;
  memberCount?: number;
  projectCount?: number;
  awards?: string;
  phone?: string;
  email?: string;
  address?: string;
  room?: string;
}

// 数据和控制
const latestAnnouncements = ref<Announcement[]>([]);
const recentActivities = ref<Activity[]>([]);
const studioInfo = ref<StudioInfo>({});
const dialogVisible = ref(false);
const currentAnnouncement = ref<Announcement | null>(null);
const loading = ref(false);
const loadingActivities = ref(false);
const loadingStudio = ref(false);

// 获取公告类型标签
const getTypeLabel = (type: string): string => {
  switch (type) {
    case 'notice': return '通知';
    case 'activity': return '活动';
    case 'news': return '新闻';
    case 'other': return '其他';
    default: return '未知';
  }
};

// 获取公告类型标签样式
const getTypeTagType = (type: string): string => {
  switch (type) {
    case 'notice': return 'primary';
    case 'activity': return 'success';
    case 'news': return 'warning';
    case 'other': return 'info';
    default: return 'info';
  }
};

// 截断内容
const truncateContent = (content: string, maxLength = 100): string => {
  if (content.length <= maxLength) return content;
  return content.substring(0, maxLength) + '...';
};

// 格式化日期
const formatDate = (date: Date | string): string => {
  if (!date) return '';
  return format(new Date(date), 'yyyy-MM-dd HH:mm');
};

// 查看公告详情
const viewAnnouncementDetail = (announcement: Announcement) => {
  loading.value = true;
  
  // 调用获取详情API
  getNoticeDetail(announcement.id).then(res => {
    if (res.data) {
      // 将后端数据转换为前端格式
      const detail = res.data;
      currentAnnouncement.value = {
        id: detail.noticeId,
        title: detail.title,
        content: detail.content,
        type: mapTypeToFrontend(detail.type),
        author: detail.publisherName || detail.publisher,
        publishDate: detail.publishTime,
        status: detail.status === '1' ? 'published' : 'draft',
        images: processImages(detail.images),
        attachments: processAttachments(detail.attachments)
      };
    } else {
      // 如果API未返回数据，使用列表中的数据
      currentAnnouncement.value = announcement;
    }
    
    dialogVisible.value = true;
  }).catch(error => {
    console.error('获取公告详情失败:', error);
    // 如果API调用失败，使用列表中的数据
    currentAnnouncement.value = announcement;
    dialogVisible.value = true;
  }).finally(() => {
    loading.value = false;
  });
};

// 类型映射函数
const mapTypeToFrontend = (type: number): 'notice' | 'activity' | 'news' | 'other' => {
  switch (type) {
    case 0: return 'notice';
    case 1: return 'activity';
    case 2: return 'news';
    case 3: return 'other';
    default: return 'other';
  }
};

// 加载数据
const loadData = () => {
  // 加载最新公告
  loading.value = true;
  
  const now = new Date();
  const oneMonthAgo = subMonths(now, 1);
  
  const params: PageNoticeParams = {
    page: 1,
    pageSize: 5,  // 获取最新的5条
    status: '1',    // 只获取已发布的公告
    type: 2, // 2 is for '新闻' (news) according to the DB schema
    beginTime: format(oneMonthAgo, 'yyyy-MM-dd HH:mm:ss'),
    endTime: format(now, 'yyyy-MM-dd HH:mm:ss'),
  };
  
  getNoticeList(params).then(res => {
    // 将后端数据转换为前端所需格式
    const { records } = res.data;
    latestAnnouncements.value = records.map((item: any) => ({
      id: item.noticeId,
      title: item.title,
      content: item.content,
      type: mapTypeToFrontend(item.type),
      author: item.publisherName || item.publisher,
      publishDate: item.publishTime,
      status: item.status === '1' ? 'published' : 'draft',
      images: processImages(item.images),
      attachments: processAttachments(item.attachments)
    }));
    console.log('公告数据:', latestAnnouncements.value);
  }).catch(error => {
    console.error('获取公告列表失败:', error);
    // 加载失败时使用空数组
    latestAnnouncements.value = [];
  }).finally(() => {
    loading.value = false;
  });
  
  // 加载近期活动
  loadingActivities.value = true;
  getRecentActivityNotices().then((res: any) => {
    if (res && res.data) {
      // 将后端数据转换为前端所需格式
      recentActivities.value = res.data.map((item: any) => {
        // 根据活动类型设置不同的颜色
        let type: 'primary' | 'success' | 'warning' | 'info' | 'danger' = 'primary';
        if (item.activityType) {
          switch (item.activityType) {
            case 1: type = 'success'; break;
            case 2: type = 'warning'; break;
            case 3: type = 'info'; break;
            case 4: type = 'danger'; break;
            default: type = 'primary';
          }
        }
        
        return {
          id: item.noticeId || item.id,
          title: item.title,
          content: item.content,
          time: formatDate(item.activityTime || item.publishTime),
          location: item.location || '',
          type,
          images: processImages(item.images)
        };
      });
      console.log('活动数据:', recentActivities.value);
    } else {
      console.error('获取活动列表失败:', '未知错误');
      // 加载失败时使用空数组
      recentActivities.value = [];
    }
  }).catch((error: any) => {
    console.error('获取活动列表失败:', error);
    // 加载失败时使用空数组
    recentActivities.value = [];
  }).finally(() => {
    loadingActivities.value = false;
  });

  // 加载工作室信息
  loadingStudio.value = true;
  getStudioInfo().then((res: any) => {
    if (res && res.data) {
      studioInfo.value = res.data;
      console.log('工作室信息:', studioInfo.value);
    } else {
      console.error('获取工作室信息失败:', '未知错误');
      studioInfo.value = {};
    }
  }).catch((error: any) => {
    console.error('获取工作室信息失败:', error);
    studioInfo.value = {};
  }).finally(() => {
    loadingStudio.value = false;
  });
};

// 处理图片数据
const processImages = (images: any[] | undefined): Array<{id: number, name: string, url: string, size: number}> => {
  if (!images || !Array.isArray(images) || images.length === 0) return [];
  
  return images.map(img => {
    const path = img.imagePath || img.image_path || img.filePath;
    return {
      id: img.imageId || img.id || 0,
      name: img.imageName || img.fileName || img.name || '图片',
      url: getFileUrl(path),
      size: img.imageSize || img.fileSize || img.size || 0
    };
  });
};

// 处理附件数据
const processAttachments = (attachments: any[] | undefined): Array<{id: number, name: string, url: string, size: number}> => {
  if (!attachments || !Array.isArray(attachments) || attachments.length === 0) return [];
  
  return attachments.map(att => {
    const path = att.attachmentPath || att.filePath;
    return {
      id: att.attachmentId || att.id || 0,
      name: att.attachmentName || att.fileName || att.name || '附件',
      url: getFileUrl(path, true, att.attachmentName || att.fileName || att.name),
      size: att.attachmentSize || att.fileSize || att.size || 0
    };
  });
};

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

// 格式化文件大小
const formatFileSize = (bytes: number): string => {
  if (!bytes) return '未知大小';
  const units = ['B', 'KB', 'MB', 'GB'];
  let size = bytes;
  let unitIndex = 0;
  
  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024;
    unitIndex++;
  }
  
  return `${size.toFixed(2)} ${units[unitIndex]}`;
};

// 预览图片
const previewImage = (image: any) => {
  if (currentAnnouncement.value?.images) {
    previewImages.value = currentAnnouncement.value.images.map(img => img.url);
    previewIndex.value = currentAnnouncement.value.images.findIndex(img => img.url === image.url);
    previewVisible.value = true;
  }
};

const downloadAttachment = (url: string) => {
  window.open(url, '_blank');
};

// 图片预览相关
const previewVisible = ref(false);
const previewImages = ref<string[]>([]);
const previewIndex = ref(0);

// 初始化
onMounted(() => {
  loadData();
});
</script>

<style lang="scss" scoped>
.info-portal-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
  
  .page-header-card {
    .page-title {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      h2 {
        margin: 0;
        font-size: 1.5rem;
        color: var(--el-text-color-primary);
        position: relative;
        padding-left: 15px;
        
        &::before {
          content: '';
          position: absolute;
          left: 0;
          top: 50%;
          transform: translateY(-50%);
          width: 4px;
          height: 20px;
          background-color: var(--el-color-primary);
          border-radius: 2px;
        }
      }
    }
  }
  
  .el-row {
    margin-bottom: 0;
  }
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    h3 {
      margin: 0;
      font-size: 18px;
      font-weight: 600;
      color: var(--el-text-color-primary);
      position: relative;
      padding-left: 12px;
      
      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 50%;
        transform: translateY(-50%);
        width: 3px;
        height: 16px;
        background-color: var(--el-color-primary);
        border-radius: 2px;
      }
    }
    
    .activity-hint {
      font-size: 14px;
      color: var(--el-color-info);
      margin-left: 20px;
    }
  }
  
  .main-content-card {
    margin-bottom: 20px;
    transition: box-shadow 0.3s;
    
    &:hover {
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
    }
    
    &:has(.timeline-container) {
      padding-bottom: 20px;
    }
  }
  
  .side-card {
    margin-bottom: 20px;
    transition: box-shadow 0.3s;
    
    &:hover {
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
    }
  }
  
  .loading-placeholder,
  .empty-placeholder {
    padding: 20px;
    text-align: center;
    color: var(--el-text-color-secondary);
  }
  
  .announcement-grid {
    padding: 10px 0;
    
    .announcement-cards {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 20px;
      
      .announcement-card {
        cursor: pointer;
        transition: transform 0.3s, box-shadow 0.3s;
        height: 100%;
        
        &:hover {
          transform: translateY(-5px);
          box-shadow: 0 10px 15px rgba(0, 0, 0, 0.1);
        }
        
        .announcement-image {
          height: 180px;
          overflow: hidden;
          margin-bottom: 15px;
          border-radius: 4px;
          background-color: #f5f7fa;
          display: flex;
          justify-content: center;
          align-items: center;

          .el-carousel {
            width: 100%;
            height: 100%;
          }
          
          .carousel-img, .single-img {
            width: 100%;
            height: 100%;
            object-fit: contain;
          }
          
          &.no-image {
            color: var(--el-text-color-secondary);
            
            .el-icon {
              font-size: 48px;
              opacity: 0.5;
            }
          }
        }
        
        .announcement-info {
          padding: 0 5px;
        }
        
        .announcement-header {
          margin-bottom: 10px;
          
          .announcement-title {
            margin: 8px 0 0;
            font-size: 16px;
            font-weight: 600;
            line-height: 1.4;
            overflow: hidden;
            text-overflow: ellipsis;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
          }
        }
        
        .announcement-content {
          color: var(--el-text-color-regular);
          margin-bottom: 15px;
          line-height: 1.5;
          overflow: hidden;
          text-overflow: ellipsis;
          display: -webkit-box;
          -webkit-line-clamp: 3;
          -webkit-box-orient: vertical;
          min-height: 4.5em; // 保持最小高度以对齐
        }
        
        .announcement-card-attachments {
          margin-top: 10px;
          .el-divider {
            margin: 10px 0;
          }
          .attachment-item {
            display: flex;
            align-items: center;
            gap: 5px;
            font-size: 13px;
            color: var(--el-color-primary);
            cursor: pointer;
            padding: 2px 5px;
            border-radius: 4px;
            transition: background-color 0.2s;

            &:hover {
              background-color: var(--el-color-primary-light-9);
            }

            .attachment-name {
              white-space: nowrap;
              overflow: hidden;
              text-overflow: ellipsis;
            }
          }
        }
        
        .announcement-footer {
          display: flex;
          justify-content: space-between;
          color: var(--el-text-color-secondary);
          font-size: 13px;
          padding-top: 10px;
          border-top: 1px solid var(--el-border-color-lighter);
        }
      }
    }
  }
  
  .studio-info, .contact-info {
    .info-item {
      display: flex;
      margin-bottom: 12px;
      
      .el-icon {
        margin-right: 8px;
        color: var(--el-color-primary);
      }
      
      .label {
        width: 80px;
        color: var(--el-text-color-secondary);
      }
      
      .value {
        flex: 1;
        color: var(--el-text-color-primary);
      }
    }
  }
  
  .quick-links {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  
  .activity-location {
    margin-top: 5px;
    color: var(--el-text-color-secondary);
    display: flex;
    align-items: center;
    
    .el-icon {
      margin-right: 5px;
    }
  }
  
  .el-timeline {
    padding: 10px 10px 10px 0;
    
    .el-timeline-item {
      margin-bottom: 15px;
      padding-left: 0; /* 删除额外的内边距 */
      
      h4 {
        margin: 0 0 8px;
        font-size: 16px;
        font-weight: 600;
        color: var(--el-text-color-primary);
      }
      
      p {
        margin: 0 0 5px;
        color: var(--el-text-color-regular);
        line-height: 1.5;
      }
      
      &:last-child {
        margin-bottom: 0;
      }
    }
  }
  
  .timeline-container {
    padding: 0 20px 0 30px; /* 增加左侧内边距 */
    margin-left: 10px; /* 整体向右移动 */
    
    :deep(.el-timeline-item__node) {
      z-index: 1;
      left:0px; /* 调整小圆圈的位置 */
      top: 0px; /* 调整小圆圈的位置 */
    }
    
    :deep(.el-timeline-item__tail) {
      left: 4px; /* 调整时间线的位置 */
    }
    
    :deep(.el-timeline-item__wrapper) {
      padding-left: 20px; /* 内容区域向右移 */
      padding-right: 20px; /* 内容区域向右移 */
    }
    
    :deep(.el-timeline-item__content) {
      .activity-image {
        margin-top: 12px;
        border-radius: 6px;
        overflow: hidden;
        max-width: 100%;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        
        img {
          width: 100%;
          max-height: 220px;
          object-fit: contain;
          background-color: #f5f7fa;
        }
      }
    }
  }
  
  .el-scrollbar {
    --el-scrollbar-opacity: 0.3;
    --el-scrollbar-hover-opacity: 0.5;
    --el-scrollbar-bg-color: var(--el-border-color-lighter);
    
    &:hover {
      --el-scrollbar-opacity: 0.5;
    }
  }
  
  .announcement-detail {
    .announcement-detail-header {
      margin-bottom: 20px;
      
      .announcement-meta {
        margin-top: 10px;
        display: flex;
        gap: 20px;
        color: var(--el-text-color-secondary);
        font-size: 14px;
      }
    }
    
    .announcement-detail-content {
      line-height: 1.8;
      white-space: pre-line;
      margin-bottom: 20px;
    }
    
    .announcement-detail-images {
      margin-bottom: 20px;
      .carousel-image-container {
        width: 100%;
        height: 300px;
        display: flex;
        justify-content: center;
        align-items: center;
        border-radius: 6px;
        overflow: hidden;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
        cursor: pointer;
        transition: transform 0.3s;

        &:hover {
          transform: scale(1.02);
        }

        img {
          width: 100%;
          height: 100%;
          object-fit: contain;
        }
      }
      .single-image-container {
        width: 100%;
        height: 300px;
        display: flex;
        justify-content: center;
        align-items: center;
        border-radius: 6px;
        overflow: hidden;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
        cursor: pointer;
        transition: transform 0.3s;

        &:hover {
          transform: scale(1.02);
        }

        img {
          width: 100%;
          height: 100%;
          object-fit: contain;
        }
      }
    }
    
    .announcement-images {
      margin-top: 20px;
      padding-top: 15px;
      border-top: 1px solid var(--el-border-color-lighter);
      
      h4 {
        margin-top: 0;
        margin-bottom: 15px;
      }
      
      .image-gallery {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
        gap: 20px;
        
        .image-item {
          cursor: pointer;
          border-radius: 6px;
          overflow: hidden;
          height: 180px;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
          transition: transform 0.3s;
          display: flex;
          flex-direction: column;
          align-items: center;
          text-align: center;
          
          &:hover {
            transform: scale(1.03);
          }
          
          img {
            width: 100%;
            height: 100%;
            object-fit: contain;
            background-color: #f5f7fa;
          }
          .image-name {
            margin-top: 8px;
            font-size: 14px;
            color: var(--el-text-color-secondary);
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            width: 100%;
            padding: 0 5px;
          }
        }
      }
    }
    
    .announcement-attachments {
      padding-top: 15px;
      border-top: 1px solid var(--el-border-color-lighter);
      
      h4 {
        margin-top: 0;
        margin-bottom: 10px;
      }
      
      .attachment-list {
        list-style: none;
        padding: 0;
        margin: 0;
        
        .attachment-item {
          margin-bottom: 8px;
          padding: 8px 12px;
          border: 1px solid var(--el-border-color-lighter);
          border-radius: 4px;
          display: flex;
          align-items: center;
          gap: 10px;
          cursor: pointer;
          transition: background-color 0.3s;
          
          &:hover {
            background-color: var(--el-color-primary-light-9);
            border-color: var(--el-color-primary-light-7);
          }
          
          .attachment-info {
            display: flex;
            align-items: center;
            gap: 8px;
            
            .attachment-icon {
              color: var(--el-color-primary);
            }
            
            .attachment-details {
              display: flex;
              align-items: baseline;
              gap: 5px;
              
              .attachment-name {
                font-weight: 600;
                color: var(--el-text-color-primary);
              }
              
              .attachment-size {
                font-size: 13px;
                color: var(--el-text-color-secondary);
              }
            }
          }
        }
      }
    }
  }
  
  .announcement-detail-dialog {
    .el-dialog__body {
      padding: 20px;
    }
  }

  @media (max-width: 768px) {
    .announcement-grid {
      .announcement-cards {
        grid-template-columns: 1fr;
      }
    }
  }
}
</style>
