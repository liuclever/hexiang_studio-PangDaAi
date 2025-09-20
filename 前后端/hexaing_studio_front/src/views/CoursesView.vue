<template>
  <div class="courses-view">
    <el-card shadow="hover" class="page-header-card">
      <div class="page-title">
        <h2>课程管理</h2>
        <el-button type="primary" @click="showAddCourseDialog">
          <el-icon><Plus /></el-icon>新增课程
        </el-button>
      </div>
    </el-card>

    <!-- 课程筛选 -->
    <el-card shadow="hover" class="filter-card">
      <div class="filter-container">
        <el-input
          v-model="searchQuery"
          placeholder="搜索课程名称或教师"
          class="filter-item"
          clearable
          prefix-icon="Search"
        />
        <el-select v-model="statusFilter" placeholder="课程状态" class="filter-item" clearable>
          <el-option label="全部" value="all" />
          <el-option label="已发布" value="active" />
          <el-option label="草稿" value="draft" />
          <el-option label="已下架" value="paused" />
        </el-select>
        <el-select v-model="directionFilter" placeholder="培训方向" class="filter-item" clearable>
          <el-option label="全部方向" value="all" />
          <el-option 
            v-for="direction in directions" 
            :key="direction.direction_id" 
            :label="direction.direction_name" 
            :value="direction.direction_id" 
          />
        </el-select>
      </div>
    </el-card>

    <el-row :gutter="20" class="course-card-row">
      <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="course in filteredCourses" :key="course.id">
        <el-card shadow="hover" class="course-card">
          <div class="course-cover" :style="{ backgroundImage: `url(${resolveFileUrl(course.coverImage)})` }">
            <div class="course-status" :class="getCourseStatusClass(course.status)">
              {{ getCourseStatusLabel(course.status) }}
            </div>
          </div>
          <div class="course-info">
            <h3 class="course-title">{{ course.name }}</h3>
            <div class="course-meta">
              <span><el-icon><User /></el-icon> {{ course.teacher }}</span>
              <span><el-icon><Timer /></el-icon> {{ course.duration }}</span>
            </div>
            <div class="course-meta">
              <span v-if="course.location"><el-icon><Location /></el-icon> {{ course.location }}</span>
              <span><el-icon><UserFilled /></el-icon> {{ course.students || 0 }} 人</span>
            </div>
            <div class="course-meta">
              <span v-if="course.schedule"><el-icon><Calendar /></el-icon> {{ course.schedule }}</span>
            </div>
            <div class="course-category">
              <el-tag size="small" type="info">{{ getDirectionName(course.directionId) }}</el-tag>
            </div>
            <div class="course-description">{{ course.description }}</div>
            <div class="course-actions">
              <el-button type="primary" size="small" @click="viewCourse(course)">查看详情</el-button>
              <el-button type="warning" size="small" @click="editCourse(course)">编辑</el-button>
              <el-dropdown trigger="click" size="small">
                <el-button type="info" size="small">更多<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-if="course.status !== 'active'" @click="activateCourse(course.id)">发布课程</el-dropdown-item>
                    <el-dropdown-item v-if="course.status === 'active'" @click="pauseCourse(course.id)">下架课程</el-dropdown-item>
                    <el-dropdown-item divided @click="handleDeleteCourse(course)">
                      <span style="color: var(--el-color-danger)">删除</span>
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 无数据显示 -->
    <el-empty v-if="filteredCourses.length === 0" description="暂无课程" />

    <el-pagination
      v-if="totalCourses > pageSize"
      class="pagination"
      background
      layout="prev, pager, next"
      :total="totalCourses"
      :page-size="pageSize"
      v-model:current-page="currentPage"
      @current-change="handleCurrentChange"
    />

    <!-- 新建/编辑课程对话框 -->
    <el-dialog v-model="courseDialogVisible" :title="isNewCourse ? '新增课程' : '编辑课程'" width="600px">
      <el-form :model="currentCourse" label-width="100px" :rules="courseRules" ref="courseForm">
        <el-form-item label="课程名称" prop="name">
          <el-input v-model="currentCourse.name" placeholder="请输入课程名称" />
        </el-form-item>
        <el-form-item label="授课教师" prop="teacherName">
          <el-select v-model="currentCourse.teacherName" placeholder="请选择授课教师">
            <el-option
              v-for="teacher in teachers"
              :key="teacher.name"
              :label="teacher.name"
              :value="teacher.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="课程时长" prop="duration">
          <el-input v-model="currentCourse.duration" placeholder="例如：24课时" />
        </el-form-item>
        <el-form-item label="上课地点" prop="location">
          <el-select v-model="currentCourse.location" placeholder="请选择上课地点">
            <el-option
              v-for="loc in commonLocations"
              :key="loc.id"
              :label="loc.name"
              :value="loc.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="上课时间" prop="schedule">
          <el-input v-model="currentCourse.schedule" placeholder="请输入上课时间，如：每周一 14:00-16:00" />
        </el-form-item>
        <el-form-item label="课程状态" prop="status">
          <el-select v-model="currentCourse.status" placeholder="请选择课程状态">
            <el-option label="已发布" value="active" />
            <el-option label="草稿" value="draft" />
            <el-option label="已下架" value="paused" />
          </el-select>
        </el-form-item>
        <el-form-item label="培训方向" prop="directionId">
          <el-select v-model="currentCourse.directionId" placeholder="请选择培训方向">
            <el-option 
              v-for="direction in directions" 
              :key="direction.direction_id" 
              :label="direction.direction_name" 
              :value="direction.direction_id" 
            />
          </el-select>
        </el-form-item>
        <el-form-item label="封面图片" prop="coverImage">
          <el-upload
            action="/api/admin/file/upload"
            :auto-upload="false"
            :on-change="handleCoverChange"
            :on-remove="handleCoverRemove"
            :file-list="coverFileList"
            :limit="1"
            list-type="picture-card"
            accept="image/jpeg,image/png"
            :before-upload="beforeCoverUpload"
            :headers="getUploadHeaders()"
            :on-preview="handlePictureCoverPreview"
            :data="{type: 'IMAGE'}"
            :on-exceed="handleCoverExceed"
          >
            <el-icon v-if="!coverFileList.length"><Plus /></el-icon>
            <template #tip>
              <div class="el-upload__tip">课程封面仅支持单张图片，可上传JPG/PNG格式，单个图片不超过2MB，建议尺寸800x600px</div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item label="课程资料" prop="materialUrl">
          <el-upload
            action="/api/admin/file/upload"
            :auto-upload="false"
            :on-change="handleMaterialChange"
            :on-remove="handleMaterialRemove"
            :file-list="materialFileList"
            :headers="getUploadHeaders()"
            :before-upload="beforeMaterialUpload"
            :data="{type: 'DOCUMENT'}"
            multiple
          >
            <el-button type="primary">
              <el-icon><Upload /></el-icon>
              上传课程资料
            </el-button>
            <template #tip>
              <div class="el-upload__tip">
                支持PDF、Word、PPT等格式文件，大小不超过50MB，可上传多个文件
              </div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="课程描述" prop="description">
          <el-input v-model="currentCourse.description" type="textarea" :rows="4" placeholder="请输入课程描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="courseDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveCourse" :loading="saveLoading">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 课程详情对话框 -->
    <el-dialog v-model="detailsDialogVisible" title="课程详情" width="650px">
      <div v-if="currentCourse.id" class="course-details">
        <div class="course-details-header">
          <img :src="resolveFileUrl(currentCourse.coverImage)" alt="课程封面" class="course-details-image">
          <div class="course-details-meta">
            <h2>{{ currentCourse.name }}</h2>
            <p><strong>授课教师:</strong> {{ currentCourse.teacherName }}</p>
            <p><strong>课时数量:</strong> {{ currentCourse.duration }}</p>
            <p v-if="currentCourse.location"><strong>上课地点:</strong> {{ currentCourse.location }}</p>
            <p v-if="currentCourse.schedule"><strong>上课时间:</strong> {{ currentCourse.schedule }}</p>
            <p><strong>课程状态:</strong> 
              <el-tag :type="getStatusType(currentCourse.status)">
                {{ getCourseStatusLabel(currentCourse.status) }}
              </el-tag>
            </p>
            <p><strong>培训方向:</strong> 
              <el-tag size="small">{{ getDirectionName(currentCourse.directionId) }}</el-tag>
            </p>
            <p>
              <strong>学员数量:</strong> 
              <el-button type="text" @click="showStudentsDialog">
                {{ currentCourse.students || 0 }} 人
                <el-icon class="el-icon--right"><ArrowRight /></el-icon>
              </el-button>
              <span class="student-hint">(点击查看已选课学员)</span>
            </p>
            <p v-if="currentCourse.materialUrl">
              <strong>课程资料:</strong> 
              <el-link 
                type="primary" 
                @click="downloadFile(currentCourse.materialUrl)"
              >
                {{ getMaterialFileName(currentCourse.materialUrl) || '查看课程资料' }}
                <el-icon class="el-icon--right"><Document /></el-icon>
              </el-link>
            </p>
            
            <!-- 显示多个课程资料 -->
            <div v-if="currentCourse.materials && currentCourse.materials.length > 0">
              <p><strong>课程资料:</strong></p>
              <ul class="material-list">
                <li v-for="material in currentCourse.materials" :key="material.materialId">
                  <el-link 
                    type="primary" 
                    @click="downloadFile(material.filePath, material.fileName)"
                  >
                    {{ material.fileName }}
                    <el-icon class="el-icon--right"><Document /></el-icon>
                  </el-link>
                </li>
              </ul>
            </div>
          </div>
        </div>
        
        <el-divider />
        
        <div class="course-details-description">
          <h3>课程简介</h3>
          <p>{{ currentCourse.description }}</p>
        </div>
        
        <el-divider />
        
        <div class="course-details-actions">
          <el-button type="warning" @click="editCourse(currentCourse)">编辑课程</el-button>
          <el-button 
            :type="currentCourse.status === 'active' ? 'info' : 'success'"
            @click="currentCourse.status === 'active' ? pauseCourse(currentCourse.id) : activateCourse(currentCourse.id)"
          >
            {{ currentCourse.status === 'active' ? '下架课程' : '发布课程' }}
          </el-button>
          <el-button type="danger" @click="confirmDeleteFromDetails">删除课程</el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 学生管理对话框 -->
    <el-dialog v-model="studentsDialogVisible" title="课程学员管理" width="650px">
      <div class="students-management">
        <div class="students-header">
          <h3>{{ currentCourse.name }} - 学员管理</h3>
          <el-button type="primary" @click="showAddStudentDialog">
            <el-icon><Plus /></el-icon>添加学员
          </el-button>
        </div>
        
        <el-table :data="courseStudents" style="width: 100%" v-loading="studentsLoading">
          <el-table-column prop="name" label="姓名" width="120" />
          <el-table-column prop="majorClass" label="专业" />
          <el-table-column label="培训方向">
            <template #default="scope">
              <div class="direction-tags">
                <el-tag 
                  v-for="(direction, index) in scope.row.directions" 
                  :key="index" 
                  size="small" 
                  type="info" 
                  class="direction-tag"
                  effect="light"
                >
                  {{ direction.name }}
                </el-tag>
                <span v-if="!scope.row.directions || scope.row.directions.length === 0">
                  {{ scope.row.directionName || '未分类' }}
                </span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="scope">
              <el-button 
                type="danger" 
                size="small" 
                @click="removeStudent(scope.row)"
                :loading="removeStudentLoading === scope.row.id"
              >
                移除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        
        <div class="empty-placeholder" v-if="courseStudents.length === 0 && !studentsLoading">
          <el-empty description="暂无学员">
            <template #extra>
              <el-button type="primary" @click="showAddStudentDialog">
                <el-icon><Plus /></el-icon>添加学员
              </el-button>
            </template>
          </el-empty>
        </div>
      </div>
    </el-dialog>

    <!-- 添加学生对话框 -->
    <el-dialog v-model="addStudentDialogVisible" title="添加学员" width="500px">
      <el-form :model="searchStudentForm" @submit.prevent="searchStudents">
        <el-form-item>
          <el-input 
            v-model="searchStudentForm.keyword" 
            placeholder="输入学生姓名搜索" 
            clearable
          >
            <template #append>
              <el-button @click="searchStudents" :loading="searchStudentsLoading">
                <el-icon><Search /></el-icon>
              </el-button>
            </template>
          </el-input>
        </el-form-item>
      </el-form>
      
      <div class="students-header">
        <h4>{{ searchStudentForm.keyword ? '搜索结果' : '符合培训方向的可添加学员' }}</h4>
      </div>
      
      <el-table 
        :data="searchStudentResults" 
        style="width: 100%" 
        v-loading="searchStudentsLoading"
        height="300px"
      >
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="majorClass" label="专业" />
        <el-table-column label="培训方向">
          <template #default="scope">
            <div class="direction-tags">
              <el-tag 
                v-for="(direction, index) in scope.row.directions" 
                :key="index" 
                size="small" 
                type="info" 
                class="direction-tag"
                effect="light"
              >
                {{ direction.name }}
              </el-tag>
              <span v-if="!scope.row.directions || scope.row.directions.length === 0">
                {{ scope.row.directionName || '未分类' }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="scope">
            <el-button 
              type="primary" 
              size="small" 
              @click="addStudentToCourse(scope.row)"
              :loading="addStudentLoading === scope.row.id"
              :disabled="isStudentInCourse(scope.row.id)"
            >
              {{ isStudentInCourse(scope.row.id) ? '已添加' : '添加' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <div class="empty-placeholder" v-if="searchStudentResults.length === 0 && !searchStudentsLoading">
        <el-empty :description="searchStudentForm.keyword ? '未找到匹配的学生' : '暂无符合条件的学生'" />
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="addStudentDialogVisible = false">关闭</el-button>
      </div>
      </template>
    </el-dialog>
  </div>
  
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
import { ref, computed, reactive, onMounted } from 'vue';
import { User, Timer, ArrowDown, Plus, Search, Location, Calendar, ArrowRight, UserFilled, Upload, Document, Delete } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox, ElImageViewer } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import { useRoute } from 'vue-router';
import { getCourseList, getCourseDetail, addCourse, updateCourse, deleteCourse as apiDeleteCourse, updateCourseStatus, getCourseStudents, addStudentToCourse as apiAddStudentToCourse, removeStudentFromCourse, getTrainingDirections, getTeacherList, searchStudents as apiSearchStudents, getStudentList, getEnrolledStudents, getEligibleStudents } from '../utils/api/course';
import { getAllCommonLocations } from '@/api/locations';

// 新增：文件URL解析函数
const resolveFileUrl = (path: string | undefined): string => {
  if (!path) {
    // 返回默认的占位图URL
    return '/images/default-cover.svg'; 
  }
  
  // 如果路径已经是完整的URL（例如，包含http、https或是一个blob对象URL），则直接返回
  if (path.startsWith('http') || path.startsWith('blob:')) {
    return path;
  }
  
  // 确保路径以/开头
  const normalizedPath = path.startsWith('/') ? path : '/' + path;
  
  // 使用FileController的文件访问路径，与公告管理保持一致
  // 对于下载文件，使用/download路径
  if (path.endsWith('.pdf') || path.endsWith('.doc') || path.endsWith('.docx') || 
      path.endsWith('.ppt') || path.endsWith('.pptx') || path.endsWith('.xls') || 
      path.endsWith('.xlsx') || path.endsWith('.zip') || path.endsWith('.rar')) {
    return `/api/admin/file/download${normalizedPath}`;
  }
  
  // 对于图片等需要查看的文件，使用/view路径
  return `/api/admin/file/view${normalizedPath}`;
};

// 下载文件函数
const downloadFile = (filePath: string, fileName?: string) => {
  if (!filePath) {
    ElMessage.error('文件路径不存在');
    return;
  }
  
  // 确保文件路径格式正确
  let downloadUrl = '';
  
  // 检查路径是否已经是完整的URL
  if (filePath.startsWith('http://') || filePath.startsWith('https://')) {
    downloadUrl = filePath;
  } 
  // 检查路径是否已经包含 /api/admin/file/view/
  else if (filePath.startsWith('/api/admin/file/view/')) {
    downloadUrl = filePath;
  }
  // 如果是相对路径，添加前缀
  else {
    downloadUrl = `/api/admin/file/view/${filePath}`;
  }
  
  // 添加下载参数
  downloadUrl += `?download=true`;
  
  // 如果提供了文件名，添加到URL中
  if (fileName) {
    downloadUrl += `&originalName=${encodeURIComponent(fileName)}`;
  }
  
  console.log('下载文件:', downloadUrl);
  
  // 创建一个临时链接并模拟点击来下载文件
  const link = document.createElement('a');
  link.href = downloadUrl;
  link.target = '_blank';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
};

// 获取路由对象以访问URL参数
const route = useRoute();

// 添加onMounted钩子，检查URL参数
onMounted(() => {
  // 检查URL参数中是否有action=new
  if (route.query.action === 'new') {
    showAddCourseDialog();
  }
  
  // 加载课程列表
  loadCourses();
  
  // 加载培训方向
  loadTrainingDirections();

  loadCommonLocations();
});

// 课程数据接口
interface Course {
  id: number;
  name: string;
  teacher: string;
  teacherId?: number; // 添加teacherId字段 
  description: string;
  duration: string;
  status: 'draft' | 'active' | 'paused';
  coverImage: string;
  students?: number;
  directionId?: number; // 使用驼峰命名
  materialUrl?: string; // 使用驼峰命名
  location?: string;
  schedule?: string;
  teacherName?: string; // 添加 teacherName 字段
  materials?: { materialId: number; fileName: string; filePath: string }[]; // 新增 materials 字段
}

// 学生接口定义
interface Student {
  id: number;
  name: string;
  studentNumber: string;
  majorClass?: string;
  directionId?: number;
  directionName?: string;
}

// 常用地点接口定义
interface CommonLocation {
  id: number;
  name: string;
}

// 添加培训方向数据
const directions = ref([
  { direction_id: 1, direction_name: '前端开发' },
  { direction_id: 2, direction_name: '后端开发' },
  { direction_id: 3, direction_name: '移动开发' },
  { direction_id: 4, direction_name: '数据科学' },
  { direction_id: 5, direction_name: 'UI/UX设计' }
]);

// 加载培训方向列表
const loadTrainingDirections = async () => {
  try {
    const res = await getTrainingDirections();
    if (res.code === 1 && res.data) {
      directions.value = res.data;
    }
  } catch (error) {
    console.error('加载培训方向失败:', error);
  }
};

// 添加培训方向筛选
const directionFilter = ref<number|'all'>('all');

// 获取培训方向名称的方法
const getDirectionName = (directionId?: number) => {
  if (!directionId) return '未分类';
  const direction = directions.value.find(d => d.direction_id === directionId);
  return direction ? direction.direction_name : '未分类';
};

// 课程数据
const courses = ref<Course[]>([]);

// 加载课程列表
const loadCourses = async () => {
  try {
    // 构建查询参数，确保分页参数正确传递
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      name: searchQuery.value || undefined,
      status: statusFilter.value !== 'all' ? statusFilter.value : undefined,
      directionId: directionFilter.value !== 'all' ? directionFilter.value : undefined
    };
    
    console.log('课程查询参数:', params);
    
    const res = await getCourseList(params);
    console.log('课程查询响应:', res);
    
    // 接受code=200作为成功状态码
    if (res && (res.code === 1 || res.code === 200)) {
      // 转换后端数据格式为前端格式，适配records字段
      // 兼容两种后端返回结构：data直接是数组，或data是包含records和total的对象
      const rawData = res.data;
      const recordsList = Array.isArray(rawData) ? rawData : (rawData?.records || []);
      
      courses.value = recordsList.map((item: any) => ({
        id: item.courseId || null,  // 适配courseId字段
        name: item.name || '',
        teacher: item.teacherName || '未分配',
        teacherId: item.teacherId || undefined, // 新增 teacherId 映射
        description: item.description || '',
        duration: item.duration || '',
        status: getStatusFromCode(item.status),
        coverImage: item.coverImage || '',  // 适配coverImage字段
        students: item.studentCount || 0,   // 适配studentCount字段
        directionId: item.categoryId,      // 适配categoryId字段
        materialUrl: item.materialUrl || '', // 适配materialUrl字段
        location: item.location || '',
        schedule: item.schedule || '',
        materials: item.materials || [] // 新增 materials 映射
      }));
      
      totalCourses.value = (Array.isArray(rawData) ? rawData.length : (rawData?.total || recordsList.length));
      
      console.log('加载课程成功，总数:', totalCourses.value, '数据:', courses.value);
    } else {
      ElMessage.error((res && res.msg) || '获取课程列表失败');
      // 加载失败时显示空数据
      courses.value = [];
      totalCourses.value = 0;
    }
  } catch (error) {
    // 处理API错误
    handleApiError(error);
  }
};

// 处理API错误
const handleApiError = (error: any) => {
  console.error('API请求失败:', error);
  ElMessage.error('获取数据失败，请检查网络连接或联系管理员');
  
  // 显示空数据
  courses.value = [];
  totalCourses.value = 0;
};

// 状态码转换为前端状态
const getStatusFromCode = (status: number): 'active' | 'draft' | 'paused' => {
  switch (status) {
    case 1: return 'active'; // 已发布
    case 0: return 'draft';  // 草稿
    case 2: return 'paused'; // 已下架
    default: return 'draft';
  }
};

// 前端状态转换为状态码
const getStatusCode = (status: string): number => {
  switch (status) {
    case 'active': return 1; // 已发布
    case 'draft': return 0;  // 草稿
    case 'paused': return 2; // 已下架
    default: return 0;
  }
};

// 分页和筛选
const pageSize = ref(10); // 设置为10条每页，与后端默认一致
const currentPage = ref(1);
const totalCourses = ref(0);
const searchQuery = ref('');
const statusFilter = ref('all');

// 对话框状态
const courseDialogVisible = ref(false);
const detailsDialogVisible = ref(false);
const isNewCourse = ref(true);
const saveLoading = ref(false);
const courseForm = ref<FormInstance>();

// 文件列表
const coverFileList = ref<any[]>([]);
const materialFileList = ref<any[]>([]);

// 图片预览
const previewVisible = ref(false);
const previewImages = ref<string[]>([]);
const previewIndex = ref(0);

// 当前编辑的课程
const currentCourse = ref<Course>({
  id: 0,
  name: '',
  teacher: '',
  teacherId: undefined,
  description: '',
  duration: '',
  status: 'draft',
  coverImage: '',
  directionId: undefined,
  materialUrl: '',
  location: '',
  schedule: '',
  students: 0,
  teacherName: '' // 初始化 teacherName
});

// 表单验证规则
const courseRules = reactive<FormRules>({
  name: [
    { required: true, message: '请输入课程名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度应为2到50个字符', trigger: 'blur' }
  ],
  teacherName: [
    { required: true, message: '请选择授课教师', trigger: 'change' }
  ],
  duration: [
    { required: true, message: '请输入课程时长', trigger: 'blur' }
  ],
  status: [
    { required: true, message: '请选择课程状态', trigger: 'change' }
  ],
  description: [
    { required: true, message: '请输入课程描述', trigger: 'blur' },
    { min: 10, max: 500, message: '长度应为10到500个字符', trigger: 'blur' }
  ],
  directionId: [
    { required: true, message: '请选择培训方向', trigger: 'change' }
  ],
  materialUrl: [
    // { type: 'url', message: '请输入有效的URL地址', trigger: 'blur' } // 移除此行，因为通过文件上传而非手动输入URL
  ],
  location: [
    { required: true, message: '请输入上课地点', trigger: 'blur' }
  ],
  schedule: [
    { required: true, message: '请输入上课时间', trigger: 'blur' }
  ]
});

// 过滤课程列表
const filteredCourses = computed(() => {
  console.log('filteredCourses: courses.value initial:', courses.value.length, courses.value);
  let result = [...courses.value];
  
  if (searchQuery.value) {
    console.log('filteredCourses: applying searchQuery:', searchQuery.value);
    result = result.filter(course => 
      course.name.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      course.teacher.toLowerCase().includes(searchQuery.value.toLowerCase())
    );
  }
  
  if (statusFilter.value !== 'all') {
    console.log('filteredCourses: applying statusFilter:', statusFilter.value);
    result = result.filter(course => course.status === statusFilter.value);
  }
  
  if (directionFilter.value !== 'all') {
    console.log('filteredCourses: applying directionFilter:', directionFilter.value);
    result = result.filter(course => course.directionId === directionFilter.value);
  }
  
  console.log('filteredCourses: final result length:', result.length, 'final result:', result);
  return result;
});

// 获取课程状态样式
const getCourseStatusClass = (status: string) => {
  switch (status) {
    case 'active': return 'status-active';
    case 'draft': return 'status-draft';
    case 'paused': return 'status-paused';
    default: return '';
  }
};

// 获取课程状态标签
const getCourseStatusLabel = (status: string) => {
  switch (status) {
    case 'active': return '已发布';
    case 'draft': return '草稿';
    case 'paused': return '已下架';
    default: return '未知';
  }
};

// 获取状态类型（用于el-tag）
const getStatusType = (status: string) => {
  switch (status) {
    case 'active': return 'success';
    case 'draft': return 'info';
    case 'paused': return 'warning';
    default: return 'info';
  }
};

// 处理分页变化
const handleCurrentChange = (val: number) => {
  currentPage.value = val;
  loadCourses(); // 重新加载数据
};

// 查看课程详情
const detailLoading = ref(false);
const detailLoadFailed = ref(false);

const viewCourse = async (course: Course) => {
  // 先显示对话框，但内容为空或加载状态
  currentCourse.value = { id: course.id, name: course.name } as Course; // 只保留ID和名称
  detailsDialogVisible.value = true;
  detailLoading.value = true;
  detailLoadFailed.value = false;
  
  try {
    // 调用API获取详细信息
    const res = await getCourseDetail(course.id);
    if (res && (res.code === 1 || res.code === 200) && res.data) {
      // API成功后，更新课程详情数据
      const detailData = res.data;
      // 使用详情数据填充表单
      currentCourse.value = {
        id: course.id,
        name: detailData.name || course.name,
        teacher: detailData.teacherName || course.teacher,
        teacherId: detailData.teacherId || undefined, // 映射 teacherId
        description: detailData.description || '',
        duration: detailData.duration || '',
        status: getStatusFromCode(detailData.status),
        coverImage: detailData.coverImage || '',
        students: detailData.studentCount || 0,
        directionId: detailData.categoryId,
        materialUrl: detailData.materialUrl || '',
        location: detailData.location || '',
        schedule: detailData.schedule || '',
        teacherName: detailData.teacherName || '', // 映射 teacherName
        materials: detailData.materials || [] // 新增 materials 映射
      };
      // 自动根据teacherName赋值teacherId
      const teacher = teachers.value.find(t => t.name === detailData.teacherName);
      if (teacher) {
        currentCourse.value.teacherId = teacher.id;
      }
      console.log('获取课程详情成功:', detailData);
      detailLoadFailed.value = false;
    } else {
      console.error('获取课程详情失败:', res);
      detailLoadFailed.value = true;
      // 保持currentCourse的最小数据，其他属性为空或默认值
      currentCourse.value = { 
        id: course.id, 
        name: course.name,
        teacher: '', 
        description: '', 
        duration: '', 
        status: 'draft', 
        coverImage: '',
        students: 0 
      } as Course;
      ElMessage.error(res?.msg || '获取课程详情失败');
    }
  } catch (error) {
    console.error('获取课程详情出错:', error);
    detailLoadFailed.value = true;
    // 保持currentCourse的最小数据，其他属性为空或默认值
    currentCourse.value = { 
      id: course.id, 
      name: course.name,
      teacher: '', 
      description: '', 
      duration: '', 
      status: 'draft', 
      coverImage: '',
      students: 0 
    } as Course;
    ElMessage.error('获取课程详情出错');
  } finally {
    detailLoading.value = false;
  }
};

// 教师列表相关状态
const teachers = ref<{id: number; name: string}[]>([]);
const teachersLoading = ref(false);
// 编辑课程的loading状态
const editCourseLoading = ref(false);

// 加载教师列表
const loadTeachersList = async () => {
  teachersLoading.value = true;
  try {
    const res = await getTeacherList();
    console.log('教师列表API响应:', res);
    
    if (res && (res.code === 1 || res.code === 200)) {
      // 更灵活地处理不同的响应结构
      let teacherData = res.data;
      
      // 打印原始数据用于调试
      console.log('原始教师数据:', teacherData);
      
      // 转换API返回的数据格式为组件使用的格式，增加容错处理
      // 后端返回的是 List<String>，所以直接映射为name
      teachers.value = teacherData.map((item: string, index: number) => {
        return {
          id: index + 1, // 使用索引作为ID
          name: item
        };
      });
      
      console.log('处理后的教师列表:', teachers.value);
    } else {
      console.warn('教师列表数据异常:', res);
      ElMessage.warning('教师列表数据加载异常');
      teachers.value = [];
    }
  } catch (error) {
    console.error('加载教师列表失败:', error);
    ElMessage.error('教师列表加载失败，请刷新重试');
  } finally {
    // 如果API没有返回任何教师数据，添加临时测试用教师数据
    if (teachers.value.length === 0) {
      console.warn('未从API获取到教师数据，使用临时测试数据');
      teachers.value = [
        { id: 1, name: '张老师' },
        { id: 2, name: '李老师' },
        { id: 3, name: '王老师' }
      ];
    }
    
    teachersLoading.value = false;
  }
};

// 显示添加课程对话框
const showAddCourseDialog = async () => {
  isNewCourse.value = true;
  currentCourse.value = {
    id: 0,
    name: '',
    teacher: '',
    teacherId: undefined,
    description: '',
    duration: '',
    status: 'draft',
    coverImage: '',
    directionId: undefined,
    materialUrl: '',
    location: '',
    schedule: '',
    students: 0,
    teacherName: '' // 初始化 teacherName
  };
  
  // 清空文件列表
  coverFileList.value = [];
  materialFileList.value = [];
  
  // 加载教师列表
  await loadTeachersList();
  
  courseDialogVisible.value = true;
};

// 编辑课程
const editCourse = async (course: Course) => {
  // 先加载教师列表
  await loadTeachersList();
  
  editCourseLoading.value = true;
  
  try {
    // 先获取课程详情
    const res = await getCourseDetail(course.id);
    
    if (res && (res.code === 1 || res.code === 200) && res.data) {
      // 获取成功后设置当前课程数据
      const detailData = res.data;
      isNewCourse.value = false;
      
      // 使用详情数据填充表单
      currentCourse.value = {
        id: course.id,
        name: detailData.name || course.name,
        teacher: detailData.teacherName || course.teacher,
        teacherId: detailData.teacherId || undefined, // 映射 teacherId
        description: detailData.description || '',
        duration: detailData.duration || '',
        status: getStatusFromCode(detailData.status),
        coverImage: detailData.coverImage || '',
        students: detailData.studentCount || 0,
        directionId: detailData.categoryId,
        materialUrl: detailData.materialUrl || '',
        location: detailData.location || '',
        schedule: detailData.schedule || '',
        teacherName: detailData.teacherName || '', // 映射 teacherName
        materials: detailData.materials || [] // 新增 materials 映射
      };
      
      // 自动根据teacherName赋值teacherId
      const teacher = teachers.value.find(t => t.name === detailData.teacherName);
      if (teacher) {
        currentCourse.value.teacherId = teacher.id;
      }
      
      // 初始化文件列表
      if (detailData.coverImage) {
        coverFileList.value = [{
          name: '课程封面',
          url: resolveFileUrl(detailData.coverImage),
          uid: Date.now()
        }];
      } else {
        coverFileList.value = [];
      }
      
      // 处理课程资料列表
      materialFileList.value = [];
      if (detailData.materials && detailData.materials.length > 0) {
        // 使用新的materials数组
        materialFileList.value = detailData.materials.map((material, index) => ({
          name: material.fileName,
          url: resolveFileUrl(material.filePath),
          uid: Date.now() + index,
          // 保存原始资料信息，用于后续处理
          materialId: material.materialId,
          filePath: material.filePath
        }));
      } else if (detailData.materialUrl) {
        // 兼容旧版本，使用materialUrl
        materialFileList.value = [{
          name: getMaterialFileName(detailData.materialUrl),
          url: resolveFileUrl(detailData.materialUrl),
          uid: Date.now()
        }];
      }
      
      // 关闭详情页（如果正在显示）并打开编辑页
      detailsDialogVisible.value = false; 
      courseDialogVisible.value = true;
      
      console.log('获取课程详情成功，准备编辑:', currentCourse.value);
    } else {
      ElMessage.error(res?.msg || '获取课程详情失败');
    }
  } catch (error) {
    console.error('获取课程详情出错:', error);
    ElMessage.error('获取课程详情出错');
  } finally {
    editCourseLoading.value = false;
  }
};

// 保存课程
const saveCourse = async () => {
  if (!courseForm.value) return;
  
  await courseForm.value.validate(async (valid) => {
    if (valid) {
      saveLoading.value = true;
      
      try {
        // 选择的教师信息
        const selectedTeacher = teachers.value.find(t => t.name === currentCourse.value.teacherName);
        console.log('选择的教师:', selectedTeacher);
        
        // 创建 FormData 对象
        const formData = new FormData();

        // 将课程数据添加到 FormData
        // 注意：后端期望 courseDto 是一个 JSON 字符串的 RequestPart
        const courseDtoJson = JSON.stringify({
          courseId: isNewCourse.value ? undefined : currentCourse.value.id,
          name: currentCourse.value.name,
          description: currentCourse.value.description,
          status: getStatusCode(currentCourse.value.status),
          duration: currentCourse.value.duration,
          // coverImage 和 materialUrl 由文件上传设置，这里不直接添加URL
          categoryId: currentCourse.value.directionId,
          location: currentCourse.value.location || '',
          schedule: currentCourse.value.schedule || '',
          teacherName: currentCourse.value.teacherName,
          teacherId: selectedTeacher?.id // 确保 teacherId 被传递
        });
        formData.append('courseDto', new Blob([courseDtoJson], { type: 'application/json' }));

        // 处理封面图片
        if (coverFileList.value.length > 0) {
          const coverFile = coverFileList.value[0];
          
          if (coverFile.raw) {
            // 如果有原始文件对象，直接添加到FormData
            formData.append('coverImageFile', coverFile.raw, coverFile.name);
          } else if (typeof currentCourse.value.coverImage === 'string' && currentCourse.value.coverImage.startsWith('blob:')) {
            // 如果是临时Blob URL，获取Blob对象
            const blob = await fetch(currentCourse.value.coverImage).then(r => r.blob());
            formData.append('coverImageFile', blob, 'cover.png');
          }
          // 如果是已存在的图片URL，不需要重新上传
        }

        // 处理课程资料
        if (materialFileList.value.length > 0) {
          // 遍历所有课程资料文件
          for (let i = 0; i < materialFileList.value.length; i++) {
            const materialFile = materialFileList.value[i];
            
            if (materialFile.raw) {
              // 如果有原始文件对象，直接添加到FormData
              formData.append('materialFiles', materialFile.raw, materialFile.name);
            } else if (typeof materialFile.url === 'string' && materialFile.url.startsWith('blob:')) {
              // 如果是临时Blob URL，获取Blob对象
              const blob = await fetch(materialFile.url).then(r => r.blob());
              formData.append('materialFiles', blob, materialFile.name || `material_${i}.pdf`);
            }
            // 如果是已存在的资料URL，不需要重新上传
          }
        }
        
        // 如果是编辑模式，添加需要保留的资料ID列表
        if (!isNewCourse.value && currentCourse.value.materials && currentCourse.value.materials.length > 0) {
          // 获取需要保留的资料ID列表
          const keepMaterialIds = currentCourse.value.materials
            .filter(material => {
              // 检查这个资料是否在materialFileList中有对应项（通过filePath匹配）
              return !materialFileList.value.some(file => 
                file.url && file.url.includes(material.filePath)
              );
            })
            .map(material => material.materialId);
            
          // 将保留的资料ID列表添加到FormData
          if (keepMaterialIds.length > 0) {
            formData.append('keepMaterialIds', JSON.stringify(keepMaterialIds));
          }
        }

        console.log('提交的FormData数据:', formData);
        
        let result;
        if (isNewCourse.value) {
          // 添加新课程
          result = await addCourse(formData);
          if (result && (result.code === 1 || result.code === 200)) {
            ElMessage({
              type: 'success',
              message: '课程已成功添加！'
            });
            // 重新加载课程列表
            loadCourses();
          } else {
            ElMessage.error(result?.msg || '添加课程失败');
          }
        } else {
          // 更新现有课程
          result = await updateCourse(formData);
          if (result && (result.code === 1 || result.code === 200)) {
            ElMessage({
              type: 'success',
              message: '课程已成功更新！'
            });
            // 重新加载课程列表
            loadCourses();
          } else {
            ElMessage.error(result?.msg || '更新课程失败');
          }
        }
        
        courseDialogVisible.value = false;
      } catch (error) {
        ElMessage.error('操作失败，请重试');
        console.error('Save error:', error);
      } finally {
        saveLoading.value = false;
      }
    } else {
      ElMessage.warning('请正确填写表单信息');
    }
  });
};

// 删除课程
const handleDeleteCourse = (course: Course) => {
  ElMessageBox.confirm(
    `确定要删除课程"${course.name}"吗？`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const result = await apiDeleteCourse([course.id]);
      if (result.code === 200) {
        ElMessage({
          type: 'success',
          message: '课程已删除'
        });
        // 重新加载课程列表
        loadCourses();
      } else {
        ElMessage.error(result.msg || '删除课程失败');
      }
    } catch (error) {
      console.error('删除课程失败:', error);
      ElMessage.error('删除课程失败，请重试');
    }
  }).catch(() => {
    // 取消删除
  });
};

// 从详情页删除课程
const confirmDeleteFromDetails = () => {
  detailsDialogVisible.value = false;
  handleDeleteCourse(currentCourse.value);
};

// 发布课程
const activateCourse = (id: number) => {
  const course = courses.value.find(c => c.id === id);
  if (course) {
    updateCourseStatus(id, 1).then(() => { // 1 represents 'active' status
    course.status = 'active';
    ElMessage({
      type: 'success',
      message: '课程已发布'
    });
    
    // 如果正在查看该课程的详情，更新详情中的状态
    if (detailsDialogVisible.value && currentCourse.value.id === id) {
      currentCourse.value.status = 'active';
    }
    }).catch(() => {
      ElMessage({
        type: 'error',
        message: '发布课程失败'
      });
    });
  }
};

// 下架课程
const pauseCourse = (id: number) => {
  const course = courses.value.find(c => c.id === id);
  if (course) {
    updateCourseStatus(id, 2).then(() => { // 2 represents 'paused' status  
    course.status = 'paused';
    ElMessage({
      type: 'info',
      message: '课程已下架'
    });
    
    // 如果正在查看该课程的详情，更新详情中的状态
    if (detailsDialogVisible.value && currentCourse.value.id === id) {
      currentCourse.value.status = 'paused';
    }
    }).catch(() => {
  ElMessage({
        type: 'error',
        message: '下架课程失败'
  });
    });
  }
};

// 学生管理相关状态和方法
const studentsDialogVisible = ref(false);
const addStudentDialogVisible = ref(false);
const studentsLoading = ref(false);
const searchStudentsLoading = ref(false);
const removeStudentLoading = ref<number | null>(null);
const addStudentLoading = ref<number | null>(null);
const courseStudents = ref<Student[]>([]);
const searchStudentResults = ref<Student[]>([]);
const searchStudentForm = reactive({
  keyword: ''
});

// 显示学生管理对话框
const showStudentsDialog = async () => {
  studentsDialogVisible.value = true;
  await loadCourseStudents();
};

// 加载课程学生
const loadCourseStudents = async () => {
  if (!currentCourse.value.id) return;
  
  studentsLoading.value = true;
  try {
    // 调用新API获取已选课程的学生
    const result = await getEnrolledStudents(currentCourse.value.id);
    
    if (result && (result.code === 1 || result.code === 200) && result.data) {
      // 处理API返回的学生数据
      const studentsList = result.data.list || result.data.records || result.data || [];
      console.log('已选课学生数据:', studentsList);
      
      // 按学生ID分组，合并相同学生的培训方向
      const studentsMap = new Map();
      
      studentsList.forEach((item: any) => {
        if (!item) return; // 跳过null项
        
        // 统一学生ID字段，确保使用一致的ID标识
        const studentId = item.studentId || item.userId || item.id;
        if (!studentId) return; // 跳过没有ID的记录
        
        // 提取培训方向信息
        let directionName = item.directionName || item.categoryName || '';
        // 只有当 directionName 为空时才尝试通过 directionId 或 categoryId 获取
        if (!directionName && (item.directionId || item.categoryId)) {
          directionName = getDirectionName(item.directionId || item.categoryId);
        }
        
        if (studentsMap.has(studentId)) {
          // 已存在该学生，添加培训方向
          const student = studentsMap.get(studentId);
          
          // 确保directions数组已初始化
          if (!student.directions) {
            student.directions = [];
          }
          
          // 检查是否已存在相同名称的培训方向，避免重复添加
          if (directionName && !student.directions.some((d: any) => d.name === directionName)) {
            student.directions.push({
              id: item.directionId || item.categoryId || student.directions.length + 1,
              name: directionName
            });
          }
        } else {
          // 新增学生
          const directions = [];
          if (directionName) {
            directions.push({
              id: item.directionId || item.categoryId || 1,
              name: directionName
            });
          }
          
          studentsMap.set(studentId, {
            id: studentId,
            name: item.name || item.studentName || '',
            studentNumber: item.studentNumber || item.number || '',
            majorClass: item.majorClass || item.major || item.majorName || '',
            directions: directions
          });
        }
      });
      
      // 转换Map为数组
      courseStudents.value = Array.from(studentsMap.values());
      
      console.log('合并后的学生列表:', courseStudents.value);
      
      // 更新课程学生数量，确保与学生列表长度一致
      const studentCount = courseStudents.value.length;
      
      // 更新当前课程的学生数量
      currentCourse.value.students = studentCount;
      
      // 更新课程列表中的学生数量
      const courseIndex = courses.value.findIndex(c => c.id === currentCourse.value.id);
      if (courseIndex !== -1) {
        courses.value[courseIndex].students = studentCount;
      }
    } else {
      ElMessage.error((result && result.msg) || '获取学生列表失败');
      courseStudents.value = [];
    }
  } catch (error) {
    ElMessage.error('加载学生列表失败');
    console.error('Load students error:', error);
    courseStudents.value = [];
  } finally {
    studentsLoading.value = false;
  }
};

// 检查学生是否已在课程中
const isStudentInCourse = (studentId: number) => {
  return courseStudents.value.some(student => student.id === studentId);
};

// 添加学生到课程
const addStudentToCourse = async (student: Student) => {
  if (isStudentInCourse(student.id)) return;
  
  addStudentLoading.value = student.id;
  try {
    // 调用API添加学生到课程
    const data = {
      studentId: student.id,
      courseId: currentCourse.value.id,
      createUser: 'admin' // 可以从登录用户信息中获取
    };
    
    const result = await apiAddStudentToCourse(data);
    
    if (result && (result.code === 1 || result.code === 200)) {
      // 深拷贝学生对象，避免引用问题
      const studentCopy = JSON.parse(JSON.stringify(student));
      
      // 添加学生到课程
      courseStudents.value.push(studentCopy);
      
      // 更新课程学生数量，直接使用学生列表长度
      const studentCount = courseStudents.value.length;
      currentCourse.value.students = studentCount;
      
      // 更新课程列表中的学生数量
      const courseIndex = courses.value.findIndex(c => c.id === currentCourse.value.id);
      if (courseIndex !== -1) {
        courses.value[courseIndex].students = studentCount;
      }
      
      ElMessage.success(`已添加学生 ${student.name} 到课程`);
    } else {
      ElMessage.error((result && result.msg) || '添加学生失败');
    }
  } catch (error) {
    ElMessage.error('添加学生失败');
    console.error('Add student error:', error);
  } finally {
    addStudentLoading.value = null;
  }
};

// 从课程中移除学生
const removeStudent = async (student: Student) => {
  removeStudentLoading.value = student.id;
  try {
    // 调用API从课程中移除学生
    const result = await removeStudentFromCourse(student.id, currentCourse.value.id);
    
    if (result && (result.code === 1 || result.code === 200)) {
    // 从课程中移除学生
    courseStudents.value = courseStudents.value.filter(s => s.id !== student.id);
    
    // 更新课程学生数量，直接使用学生列表长度
    const studentCount = courseStudents.value.length;
    currentCourse.value.students = studentCount;
    
    // 更新课程列表中的学生数量
    const courseIndex = courses.value.findIndex(c => c.id === currentCourse.value.id);
    if (courseIndex !== -1) {
      courses.value[courseIndex].students = studentCount;
    }
    
    ElMessage.success(`已从课程中移除学生 ${student.name}`);
    } else {
      ElMessage.error((result && result.msg) || '移除学生失败');
    }
  } catch (error) {
    ElMessage.error('移除学生失败');
    console.error('Remove student error:', error);
  } finally {
    removeStudentLoading.value = null;
  }
};

// 添加文件上传相关函数
const getUploadHeaders = () => {
  // 从localStorage获取token，添加到上传请求头中
  const token = localStorage.getItem('token');
  return {
    Authorization: token ? `Bearer ${token}` : ''
  };
};

// 处理封面图片上传成功
const handleCoverSuccess = (response: any) => {
  if (response.code === 1) {
    currentCourse.value.coverImage = response.data.url;
    
    // 更新文件列表
    coverFileList.value = [{
      name: '课程封面',
      url: response.data.url,
      uid: Date.now()
    }];
    
    ElMessage.success('封面图片上传成功');
  } else {
    ElMessage.error(response.msg || '封面图片上传失败');
  }
};

// 处理封面图片移除
const handleCoverRemove = () => {
  currentCourse.value.coverImage = '';
  coverFileList.value = [];
  ElMessage.success('封面图片已移除');
};

// 处理封面图片预览
const handlePictureCoverPreview = (file: any) => {
  previewImages.value = [file.url];
  previewIndex.value = 0;
  previewVisible.value = true;
};

// 上传前检查封面图片
const beforeCoverUpload = (file: File) => {
  const isImage = file.type === 'image/jpeg' || file.type === 'image/png';
  const isLt2M = file.size / 1024 / 1024 < 2;

  if (!isImage) {
    ElMessage.error('封面图片只能是JPG或PNG格式!');
    return false;
  }
  if (!isLt2M) {
    ElMessage.error('封面图片大小不能超过2MB!');
    return false;
  }

  return true;
};

// 处理封面图片超出限制
const handleCoverExceed = () => {
  ElMessage.warning('课程封面仅限上传1张图片，请先删除已有图片');
};

// 处理封面图片上传变化
const handleCoverChange = (file: any, fileList: any[]) => {
  // 确保只有一个文件
  if (fileList.length > 1) {
    coverFileList.value = [fileList[fileList.length - 1]];
  } else {
    // 保存文件对象到列表中
    coverFileList.value = fileList;
  }
  
  // 如果有文件，创建临时预览URL
  if (file.raw) {
    currentCourse.value.coverImage = URL.createObjectURL(file.raw);
  } else if (file.url) {
    // 如果已经有URL（例如编辑时），直接使用
    currentCourse.value.coverImage = file.url;
  } else {
    // 没有文件时清空
    currentCourse.value.coverImage = '';
  }
};

// 处理课程资料上传成功
const handleMaterialSuccess = (response: any) => {
  if (response.code === 1) {
    currentCourse.value.materialUrl = response.data.url;
    
    // 更新文件列表
    materialFileList.value = [{
      name: response.data.name || '课程资料',
      url: response.data.url,
      uid: Date.now()
    }];
    
    ElMessage.success('课程资料上传成功');
  } else {
    ElMessage.error(response.msg || '课程资料上传失败');
  }
};

// 处理课程资料移除
const handleMaterialRemove = (file: any) => {
  // 从文件列表中移除指定文件
  materialFileList.value = materialFileList.value.filter(item => item.uid !== file.uid);
  
  console.log('移除课程资料:', file);
  ElMessage.success('课程资料已移除');
};

// 上传前检查课程资料
const beforeMaterialUpload = (file: File) => {
  const isLt50M = file.size / 1024 / 1024 < 50;

  if (!isLt50M) {
    ElMessage.error('课程资料大小不能超过50MB!');
    return false;
  }

  return true;
};

// 处理课程资料上传变化
const handleMaterialChange = (file: any, fileList: any[]) => {
  // 保存文件对象到列表中
  materialFileList.value = fileList;
  
  // 更新UI显示，但不需要修改currentCourse.materialUrl，因为我们现在使用materials数组
  console.log('课程资料变更:', fileList);
};

// 删除课程资料
const removeMaterial = () => {
  ElMessageBox.confirm('确定要删除已上传的课程资料吗?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    currentCourse.value.materialUrl = '';
    ElMessage.success('课程资料已删除');
  }).catch(() => {});
};

// 搜索学生
const searchStudents = async () => {
  if (!searchStudentForm.keyword.trim()) {
    // 如果关键词为空，则加载符合条件的学生
    await searchEligibleStudents();
    return;
  }
  
  searchStudentsLoading.value = true;
  try {
    // 调用API搜索学生
    const result = await apiSearchStudents(searchStudentForm.keyword);
    
    if (result && (result.code === 1 || result.code === 200) && result.data) {
      // 处理API返回的学生数据
      const studentsList = result.data.list || result.data.records || result.data || [];
      console.log('搜索学生结果:', studentsList);
      
      // 过滤掉已经在课程中的学生
      const filteredStudents = studentsList.filter((student: any) => {
        const studentId = student.studentId || student.userId || student.id;
        return !isStudentInCourse(studentId);
      });
      
      searchStudentResults.value = filteredStudents.map((item: any) => {
        // 提取培训方向信息
        let directionName = item.directionName || item.categoryName || '';
        if (!directionName && (item.directionId || item.categoryId)) {
          directionName = getDirectionName(item.directionId || item.categoryId);
        }
        
        const directions = [];
        if (directionName) {
          directions.push({
            id: item.directionId || item.categoryId || 1,
            name: directionName
          });
        }
        
        return {
          id: item.studentId || item.userId || item.id,
          name: item.name || item.studentName || '',
          studentNumber: item.studentNumber || item.number || '',
          majorClass: item.majorClass || item.major || item.majorName || '',
          directions: directions
        };
      });
    } else {
      ElMessage.error((result && result.msg) || '搜索学生失败');
      searchStudentResults.value = [];
    }
  } catch (error) {
    ElMessage.error('搜索学生失败');
    console.error('Search students error:', error);
    searchStudentResults.value = [];
  } finally {
    searchStudentsLoading.value = false;
  }
};

// 显示添加学生对话框
const showAddStudentDialog = async () => {
  addStudentDialogVisible.value = true;
  searchStudentForm.keyword = '';
  searchStudentResults.value = [];
  
  // 直接加载符合条件的学生
  await searchEligibleStudents();
};

// 搜索符合条件的学生
const searchEligibleStudents = async () => {
  if (!currentCourse.value.id) return;
  
  searchStudentsLoading.value = true;
  try {
    // 调用新API获取符合条件的学生
    const result = await getEligibleStudents(currentCourse.value.id);
    
    if (result && (result.code === 1 || result.code === 200) && result.data) {
      // 处理API返回的学生数据
      const studentsList = result.data.list || result.data.records || result.data || [];
      console.log('符合条件的学生数据:', studentsList);
      
      // 按学生ID分组，合并相同学生的培训方向
      const studentsMap = new Map();
      
      studentsList.forEach((item: any) => {
        if (!item) return; // 跳过null项
        
        // 统一学生ID字段，确保使用一致的ID标识
        const studentId = item.studentId || item.userId || item.id;
        if (!studentId) return; // 跳过没有ID的记录
        
        // 提取培训方向信息
        let directionName = item.directionName || item.categoryName || '';
        // 只有当 directionName 为空时才尝试通过 directionId 或 categoryId 获取
        if (!directionName && (item.directionId || item.categoryId)) {
          directionName = getDirectionName(item.directionId || item.categoryId);
        }
        
        if (studentsMap.has(studentId)) {
          // 已存在该学生，添加培训方向
          const student = studentsMap.get(studentId);
          
          // 确保directions数组已初始化
          if (!student.directions) {
            student.directions = [];
          }
          
          // 检查是否已存在相同名称的培训方向，避免重复添加
          if (directionName && !student.directions.some((d: any) => d.name === directionName)) {
            student.directions.push({
              id: item.directionId || item.categoryId || student.directions.length + 1,
              name: directionName
            });
          }
        } else {
          // 新增学生
          const directions = [];
          if (directionName) {
            directions.push({
              id: item.directionId || item.categoryId || 1,
              name: directionName
            });
          }
          
          studentsMap.set(studentId, {
            id: studentId,
            name: item.name || item.studentName || '',
            studentNumber: item.studentNumber || item.number || '',
            majorClass: item.majorClass || item.major || item.majorName || '',
            directions: directions
          });
        }
      });
      
      // 转换Map为数组
      searchStudentResults.value = Array.from(studentsMap.values());
      
      console.log('符合条件的学生列表:', searchStudentResults.value);
    } else {
      ElMessage.error((result && result.msg) || '获取符合条件的学生列表失败');
      searchStudentResults.value = [];
    }
  } catch (error) {
    ElMessage.error('加载符合条件的学生列表失败');
    console.error('Load eligible students error:', error);
    searchStudentResults.value = [];
  } finally {
    searchStudentsLoading.value = false;
  }
};

const commonLocations = ref<CommonLocation[]>([]);
const loadCommonLocations = async () => {
  const res = await getAllCommonLocations();
  if (res && (res.code === 200 || res.code === 1) && res.data) {
    commonLocations.value = res.data;
  }
};

// 获取课程资料文件名的函数
const getMaterialFileName = (url: string | undefined): string => {
  if (!url) return '课程资料';
  
  // 尝试从URL中提取文件名
  // 先处理URL中可能包含的查询参数
  const urlWithoutQuery = url.split('?')[0];
  
  // 然后从路径中提取文件名
  const pathParts = urlWithoutQuery.split('/');
  const filename = pathParts[pathParts.length - 1];
  
  // 尝试解码URL编码的文件名
  try {
    // 如果文件名是UUID格式，则显示为"课程资料"
    if (/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.[a-z]+$/i.test(filename)) {
      return '课程资料';
    }
    return decodeURIComponent(filename) || '课程资料';
  } catch (e) {
    return filename || '课程资料';
  }
};

// 定义API响应类型
interface ApiResponse<T = any> {
  code: number;
  data?: T;
  msg?: string;
  message?: string;
}
</script>

<style lang="scss" scoped>
.courses-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
  
  .page-header-card, .filter-card {
    .page-title {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      h2 {
        margin: 0;
        font-size: 1.5rem;
        color: var(--el-text-color-primary);
      }
    }
  }
  
  .filter-container {
    display: flex;
    gap: 15px;
    
    .filter-item {
      width: 220px;
    }
  }
  
  .course-card-row {
    margin-bottom: 10px;
  }
  
  .course-card {
    height: 100%;
    display: flex;
    flex-direction: column;
    transition: all 0.3s ease;
    margin-bottom: 20px;
    
    &:hover {
      transform: translateY(-5px);
      box-shadow: 0 6px 12px rgba(0,0,0,0.1);
    }
    
    .course-cover {
      height: 150px;
      background-size: cover;
      background-position: center;
      position: relative;
      border-top-left-radius: var(--el-border-radius-base);
      border-top-right-radius: var(--el-border-radius-base);
      
      .course-status {
        position: absolute;
        top: 10px;
        right: 10px;
        padding: 4px 8px;
        border-radius: 4px;
        font-size: 12px;
        color: white;
        
        &.status-active {
          background-color: var(--el-color-success);
        }
        
        &.status-draft {
          background-color: var(--el-color-info);
        }
        
        &.status-paused {
          background-color: var(--el-color-warning);
        }
      }
    }
    
    .course-info {
      padding: 15px;
      flex-grow: 1;
      display: flex;
      flex-direction: column;
      
      .course-title {
        margin-top: 0;
        margin-bottom: 10px;
        font-size: 16px;
        font-weight: 600;
      }
      
      .course-meta {
        display: flex;
        justify-content: space-between;
        margin-bottom: 10px;
        color: var(--el-text-color-secondary);
        font-size: 13px;
        
        span {
          display: flex;
          align-items: center;
          gap: 5px;
        }
      }
      
      .course-category {
        margin-bottom: 10px;
      }
      
      .course-description {
        color: var(--el-text-color-regular);
        font-size: 14px;
        line-height: 1.4;
        margin-bottom: 15px;
        overflow: hidden;
        text-overflow: ellipsis;
        display: -webkit-box;
        -webkit-box-orient: vertical;
        -webkit-line-clamp: 3;
        flex-grow: 1;
      }
      
      .course-actions {
        display: flex;
        gap: 10px;
        margin-top: auto;
      }
    }
  }
  
  .pagination {
    display: flex;
    justify-content: center;
    margin: 20px 0;
  }
  
  // 课程详情弹窗样式
  .course-details {
    &-header {
      display: flex;
      gap: 20px;
      margin-bottom: 20px;
    }
    
    &-image {
      width: 200px;
      height: 150px;
      object-fit: cover;
      border-radius: 8px;
    }
    
    &-meta {
      h2 {
        margin-top: 0;
        margin-bottom: 15px;
      }
      
      p {
        margin: 8px 0;
      }
    }
    
    &-description {
      h3 {
        font-size: 18px;
        margin-bottom: 10px;
      }
      
      p {
        line-height: 1.6;
      }
    }
    
    &-actions {
      display: flex;
      justify-content: flex-end;
      gap: 10px;
      margin-top: 20px;
    }
  }

  .students-management {
    .students-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 10px;
    }
  }

  // 文件上传相关样式
  .el-upload__tip {
    line-height: 1.2;
    margin-top: 5px;
    color: var(--el-text-color-secondary);
  }
}

.direction-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.direction-tag {
  margin-right: 0;
}

.material-list {
  padding-left: 20px;
  margin-top: 5px;
  
  li {
    margin-bottom: 8px;
    
    .el-link {
      display: inline-flex;
      align-items: center;
    }
  }
}

.student-hint {
  font-size: 12px;
  color: #909399;
  margin-left: 5px;
}

.students-header {
  margin-bottom: 15px;
}

.students-header h4 {
  margin: 0;
  padding: 0;
  font-size: 14px;
  color: #606266;
}
</style> 