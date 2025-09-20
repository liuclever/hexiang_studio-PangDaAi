<template>
  <el-dialog
    :title="isEdit ? '编辑任务' : '创建任务'"
    v-model="dialogVisible"
    width="75%"
    destroy-on-close
    :close-on-click-modal="false"
    class="task-form-dialog"
  >
    <el-form
      ref="formRef"
      :model="taskForm"
      :rules="rules"
      label-width="100px"
      label-position="top"
    >
      <!-- 任务基本信息部分 -->
      <el-card class="form-section-card">
        <template #header>
          <div class="card-header">
            <h3>任务基本信息</h3>
          </div>
        </template>
        <el-form-item label="任务标题" prop="title">
          <el-input v-model="taskForm.title" placeholder="请输入任务标题"></el-input>
        </el-form-item>
        <el-form-item label="任务描述" prop="description">
          <el-input
            v-model="taskForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入任务描述"
          ></el-input>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker
                v-model="taskForm.startTime"
                type="datetime"
                placeholder="选择开始时间"
                style="width: 100%"
              ></el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="截止时间" prop="endTime">
              <el-date-picker
                v-model="taskForm.endTime"
                type="datetime"
                placeholder="选择截止时间"
                style="width: 100%"
              ></el-date-picker>
            </el-form-item>
          </el-col>
        </el-row>
      </el-card>

      <!-- 子任务部分 -->
      <el-card class="form-section-card">
        <template #header>
          <div class="card-header">
            <h3>子任务配置</h3>
          </div>
        </template>

        <!-- 子任务列表 -->
        <div v-if="taskForm.subTasks.length === 0" class="empty-subtasks">
          <el-empty description="暂无子任务" />
          <el-button type="primary" @click="addSubTask" class="rounded-btn" style="padding: 8px 20px; min-width: 120px;">添加子任务</el-button>
        </div>

        <div v-else class="subtasks-list">
          <el-alert
            type="info"
            :closable="false"
            style="margin-bottom: 15px;"
          >
            <template #default>
              <div class="alert-content">
                <el-icon><InfoFilled /></el-icon>
                <span>每张卡片代表一个子任务，可以添加多个子任务</span>
              </div>
            </template>
          </el-alert>
          
          <el-row :gutter="20" class="subtask-cards">
            <el-col v-for="(subTask, index) in taskForm.subTasks" 
                   :key="index" 
                   :xs="24" 
                   :sm="24" 
                   :md="12" 
                   :lg="12" 
                   :xl="8"
                   class="subtask-col">
              <el-card class="subtask-card" shadow="hover">
                <template #header>
                  <div class="subtask-card-header">
                    <h4>子任务 {{ index + 1 }}</h4>
                    <div class="subtask-actions">
                      <div class="spacer"></div>
                      <el-button
                        type="danger"
                        size="small"
                        @click="removeSubTask(index)"
                        class="rounded-btn delete-subtask-btn"
                      >
                        删除
                      </el-button>
                    </div>
                  </div>
                </template>
                
                <div class="subtask-content">
                  <el-form-item 
                    :prop="'subTasks.' + index + '.title'"
                    :rules="{ required: true, message: '子任务标题不能为空', trigger: 'blur' }"
                    label="子任务标题"
                  >
                    <el-input 
                      v-model="subTask.title" 
                      placeholder="子任务标题"
                    ></el-input>
                  </el-form-item>
                  
                  <el-form-item :prop="'subTasks.' + index + '.description'" label="子任务说明">
                    <el-input
                      v-model="subTask.description"
                      type="textarea"
                      :rows="2"
                      placeholder="子任务说明"
                    ></el-input>
                  </el-form-item>
                  
                  <!-- 子任务成员 -->
                  <div class="members-section">
                    <div class="members-header">
                      <h4>子任务成员</h4>
                      <el-button
                        size="small"
                        type="primary"
                        @click="openMemberSelector(index)"
                        class="rounded-btn"
                        style="padding: 6px 16px; font-size: 13px; min-width: 100px;"
                      >
                        <el-icon><Plus /></el-icon> 添加成员
                      </el-button>
                    </div>
                    
                    <el-empty 
                      v-if="subTask.members.length === 0"
                      description="未分配成员" 
                      :image-size="60"
                    />
                    
                    <el-table
                      v-else
                      :data="subTask.members"
                      style="width: 100%"
                      size="small"
                      border
                      class="member-table"
                    >
                      <el-table-column label="成员" min-width="120">
                        <template #default="scope">
                          <div class="member-info">
                            <el-avatar 
                              :size="36" 
                              :src="scope.row.avatar ? `/api/admin/file/view/${scope.row.avatar}` : ''" 
                            />
                            <span>{{ scope.row.name }}</span>
                          </div>
                        </template>
                      </el-table-column>
                      <el-table-column label="角色" width="120">
                        <template #default="scope">
                          <el-select v-model="scope.row.role" placeholder="任务角色" size="small">
                            <el-option
                              v-for="item in taskRoles"
                              :key="item"
                              :label="item"
                              :value="item"
                            />
                          </el-select>
                        </template>
                      </el-table-column>
                      <el-table-column label="操作" width="160" align="center">
                        <template #default="scope">
                          <div class="button-group">
                            <el-tooltip content="备注" placement="top">
                              <el-button
                                type="primary"
                                size="small"
                                plain
                                class="rounded-btn icon-button"
                                @click="toggleMemberNote(index, scope.$index)"
                              >
                                <el-icon><Edit /></el-icon>
                              </el-button>
                            </el-tooltip>
                            
                            <el-tooltip content="删除成员" placement="top">
                              <el-button
                                type="danger"
                                size="small"
                                @click="removeMember(index, scope.$index)"
                                class="rounded-btn icon-button"
                              >
                                <el-icon><Delete /></el-icon>
                              </el-button>
                            </el-tooltip>
                          </div>
                        </template>
                      </el-table-column>
                    </el-table>
                  </div>
                  
                  <!-- 备注区域 -->
                  <div v-if="activeMemberNote.subTaskIndex === index && activeMemberNote.memberIndex !== -1" class="member-note-section">
                    <el-card shadow="hover" class="note-card">
                      <template #header>
                        <div class="note-card-header">
                          <h4>成员备注 - {{ taskForm.subTasks[activeMemberNote.subTaskIndex].members[activeMemberNote.memberIndex].name }}</h4>
                          <el-button 
                            type="text" 
                            @click="closeActiveMemberNote"
                            class="close-btn"
                          >
                            <el-icon><Close /></el-icon>
                          </el-button>
                        </div>
                      </template>
                      <div class="note-card-content">
                        <el-input
                          v-model="taskForm.subTasks[activeMemberNote.subTaskIndex].members[activeMemberNote.memberIndex].note"
                          type="textarea"
                          :rows="3"
                          placeholder="添加备注"
                        ></el-input>
                        <div class="note-card-footer">
                          <el-button size="small" @click="closeActiveMemberNote" class="rounded-btn">关闭</el-button>
                          <el-button size="small" type="primary" @click="closeActiveMemberNote" class="rounded-btn">保存</el-button>
                        </div>
                      </div>
                    </el-card>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>
          
          <div class="add-more-subtasks">
            <el-button 
              type="primary" 
              @click="addSubTask" 
              class="rounded-btn" 
              style="padding: 8px 20px; min-width: 120px; margin-top: 20px;"
            >
              <el-icon><Plus /></el-icon> 添加更多子任务
            </el-button>
          </div>
        </div>
      </el-card>

      <!-- 文件上传 -->
      <el-form-item label="附件">
        <el-upload
          v-model:file-list="fileList"
          action="#"
          :auto-upload="false"
          :before-upload="beforeUpload"
          :on-remove="handleRemove"
          multiple
          class="task-file-upload"
        >
          <el-button type="primary" :icon="Plus">添加附件</el-button>
          <template #tip>
            <div class="el-upload__tip">
              文件大小不超过20MB，可以选择多个文件
            </div>
          </template>
          <template #file="{ file }">
            <div class="custom-file-item">
              <div class="file-info" @click="downloadExistingFile(file)">
                <el-icon><Document /></el-icon>
                <span :class="{ 'clickable-filename': (file as any).attachmentId }">{{ file.name }}</span>
              </div>
              <el-icon class="remove-icon" @click.stop="handleRemove(file)"><Delete /></el-icon>
            </div>
          </template>
        </el-upload>
      </el-form-item>
    </el-form>

    <!-- 对话框底部按钮 -->
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="closeDialog" class="cancel-btn rounded-btn" style="min-width: 100px;">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting" class="submit-btn rounded-btn" style="min-width: 120px;">
          {{ isEdit ? '保存修改' : '创建任务' }}
        </el-button>
      </div>
    </template>

    <!-- 成员选择对话框 -->
    <el-dialog
      v-model="memberSelectorVisible"
      title="选择成员"
      width="50%"
      append-to-body
      destroy-on-close
      class="member-selector-dialog"
    >
      <el-input
        v-model="memberSearchQuery"
        placeholder="搜索成员"
        prefix-icon="el-icon-search"
        clearable
        @clear="searchMembers"
        @input="searchMembers"
        style="margin-bottom: 15px;"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      
      <el-table
        ref="memberTableRef"
        v-loading="loadingMembers"
        :data="availableMembers"
        style="width: 100%"
        @selection-change="handleMemberSelectionChange"
        border
        class="member-table"
        size="small"
        max-height="400px"
        @row-click="handleRowClick"
        :row-class-name="tableRowClassName"
      >
        <el-table-column label="成员" min-width="120">
          <template #default="scope">
            <div class="member-info">
              <el-avatar 
                :size="40" 
                :src="scope.row.avatar ? `/api/admin/file/view/${scope.row.avatar}` : ''" 
              />
              <span class="member-name">{{ scope.row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="role" label="角色" width="190">
           <template #default="scope">
              {{ roleMap[scope.row.role_id] || '未知' }}
           </template>
        </el-table-column>
      </el-table>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="memberSelectorVisible = false" class="cancel-btn rounded-btn" style="min-width: 100px;">取消</el-button>
          <el-button type="primary" @click="confirmMemberSelection" class="submit-btn rounded-btn" style="min-width: 120px;">确认添加</el-button>
        </div>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, InfoFilled, Delete, Edit, Search, Close, Document } from '@element-plus/icons-vue';
import { addTask, updateTask } from '@/utils/api/task';
import request from '@/utils/request';
import type { UploadFile, UploadUserFile } from 'element-plus';

// 角色ID到名称的映射表
const roleMap: Record<string, string> = {
  '0': '访客',
  '1': '学员',
  '2': '老师',
  '3': '管理员',
  '4': '超级管理员',
};

const taskRoles = ['负责人', '参与者'];

// 定义props和emits
const props = defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
  task: {
    type: Object,
    default: () => ({}),
  },
  isEdit: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(['update:visible', 'refresh']);

// 对话框显示状态
const dialogVisible = ref(props.visible);

// 监听visible属性变化
watch(() => props.visible, (val) => {
  dialogVisible.value = val;
});

// 监听对话框关闭
watch(dialogVisible, (val) => {
  if (!val) emit('update:visible', false);
});

// 表单引用
const formRef = ref();

// 表单数据
const taskForm = reactive({
  taskId: '',
  title: '',
  description: '',
  startTime: '',
  endTime: '',
  subTasks: [] as any[],
});

// 时间验证函数
const validateStartTime = (rule: any, value: any, callback: any) => {
  if (!value) {
    callback(new Error('请选择开始时间'));
    return;
  }
  
  const now = new Date();
  const startTime = new Date(value);
  
  // 检查开始时间不能早于当前时间（允许一定容错时间，比如5分钟前）
  const fiveMinutesAgo = new Date(now.getTime() - 5 * 60 * 1000);
  if (startTime < fiveMinutesAgo) {
    callback(new Error('开始时间不能早于当前时间'));
    return;
  }
  
  // 如果已经选择了结束时间，检查开始时间必须早于结束时间
  if (taskForm.endTime) {
    const endTime = new Date(taskForm.endTime);
    if (startTime >= endTime) {
      callback(new Error('开始时间必须早于结束时间'));
      return;
    }
  }
  
  callback();
};

const validateEndTime = (rule: any, value: any, callback: any) => {
  if (!value) {
    callback(new Error('请选择结束时间'));
    return;
  }
  
  const endTime = new Date(value);
  
  // 如果已经选择了开始时间，检查结束时间必须晚于开始时间
  if (taskForm.startTime) {
    const startTime = new Date(taskForm.startTime);
    if (endTime <= startTime) {
      callback(new Error('结束时间必须晚于开始时间'));
      return;
    }
    
    // 检查任务持续时间不能过短（至少1小时）
    const timeDiff = endTime.getTime() - startTime.getTime();
    const oneHour = 60 * 60 * 1000;
    if (timeDiff < oneHour) {
      callback(new Error('任务持续时间至少为1小时'));
      return;
    }
  } else {
    callback(new Error('请先选择开始时间'));
    return;
  }
  
  callback();
};

// 表单验证规则
const rules = {
  title: [{ required: true, message: '请输入任务标题', trigger: 'blur' }],
  startTime: [
    { required: true, message: '请选择开始时间', trigger: 'change' },
    { validator: validateStartTime, trigger: 'change' }
  ],
  endTime: [
    { required: true, message: '请选择截止时间', trigger: 'change' },
    { validator: validateEndTime, trigger: 'change' }
  ],
};

// 成员选择相关
const memberSelectorVisible = ref(false);
const selectedMembers = ref<any[]>([]);
const availableMembers = ref<any[]>([]);
const loadingMembers = ref(false);
const currentSubTaskIndex = ref(-1);
const memberSearchQuery = ref('');
const memberTableRef = ref(); // 为表格创建引用

// 文件上传相关
const fileList = ref<UploadUserFile[]>([]);
const keepAttachmentIds = ref<number[]>([]);

// 提交状态
const submitting = ref(false);

// 备注相关
const activeMemberNote = ref({ subTaskIndex: -1, memberIndex: -1 });

// 初始化表单数据
const initFormData = () => {
  if (props.isEdit && props.task) {
    // API返回的字段名现在是startTime/endTime
    const { 
      taskId,
      title,
      description,
      startTime,
      endTime,
      subTasks,
      memberIds = [],
      attachments = []
    } = props.task;
    
    Object.assign(taskForm, {
      taskId,
      title,
      description,
      startTime, // 直接使用startTime
      endTime,   // 直接使用endTime
      subTasks: subTasks?.map((st: any) => ({
        subTaskId: st.subTaskId,
        title: st.title,
        description: st.description,
        members: [...(st.members || [])].map((m: any) => ({
          userId: m.userId,
          name: m.name,
          avatar: m.avatar,
          role: m.role, // 确保角色在子任务中也是角色
          note: m.note
        }))
      })) || [],
    });
    
    // 填充附件数据
    if (attachments && attachments.length > 0) {
      fileList.value = attachments.map((attachment: any) => ({
        name: attachment.fileName,
        url: `/api/admin/file/view/${attachment.filePath}`,
        uid: Date.now() + attachment.attachmentId,
        status: 'success',
        attachmentId: attachment.attachmentId
      }));
      
      // 记录现有附件ID，用于更新时保留
      keepAttachmentIds.value = attachments.map((attachment: any) => 
        attachment.attachmentId
      );
    } else {
      fileList.value = [];
      keepAttachmentIds.value = [];
    }
    
    // 如果API只返回memberIds而不是子任务级别的成员分配
    // 则需要根据业务逻辑决定如何分配成员到子任务
    if (memberIds.length > 0 && (!subTasks || !subTasks.some((st: any) => st.members?.length > 0))) {
      // 这里简单地将所有成员分配给第一个子任务
      // 实际应用中可能需要更复杂的逻辑
      loadMembersFromIds(memberIds);
    }
    
    // 如果没有子任务，添加一个默认的
    if (taskForm.subTasks.length === 0) {
      addSubTask();
    }
  } else {
    resetForm();
    // 新建任务时清空附件列表
    fileList.value = [];
    keepAttachmentIds.value = [];
    // 新建任务时默认添加一个子任务
    addSubTask();
  }
};

// 根据ID加载成员信息
const loadMembersFromIds = async (memberIds: number[]) => {
  if (!memberIds.length) return;
  
  try {
    // 假设有一个API可以批量获取用户信息
    const response = await request.post('/admin/user/batch', { userIds: memberIds });
    const users = response.data || [];
    
    if (users.length > 0 && taskForm.subTasks.length > 0) {
      // 将所有成员分配给第一个子任务
      taskForm.subTasks[0].members = users.map((user: any) => ({
        ...user,
        role: '参与者',
        note: ''
      }));
    }
  } catch (error) {
    console.error('加载成员信息失败:', error);
    // 如果API不支持批量获取，则使用空对象作为占位符
    taskForm.subTasks[0].members = memberIds.map(id => ({
      userId: id,
      name: `用户${id}`,
      role: '参与者',
      note: ''
    }));
  }
};

// 重置表单
const resetForm = () => {
  Object.assign(taskForm, {
    taskId: '',
    title: '',
    description: '',
    startTime: '',
    endTime: '',
    subTasks: [],
  });
};

// 添加子任务
const addSubTask = () => {
  const newSubTask = {
    title: '',
    description: '',
    members: [],
  };
  taskForm.subTasks.push(newSubTask);
};

// 删除子任务
const removeSubTask = (index: number) => {
  ElMessageBox.confirm('确定要删除该子任务吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    taskForm.subTasks.splice(index, 1);
    if (taskForm.subTasks.length === 0) {
      addSubTask();
    }
  });
};

// 打开成员选择器
const openMemberSelector = (subTaskIndex: number) => {
  currentSubTaskIndex.value = subTaskIndex;
  memberSearchQuery.value = '';
  memberSelectorVisible.value = true;
  loadAvailableMembers();
};

// 加载可用成员
const loadAvailableMembers = async () => {
  loadingMembers.value = true;
  try {
    const params = {
      keyword: memberSearchQuery.value,
      page: 1,
      size: 1000, 
    };
    const response = await request.get('/admin/task/taskUserList', { params });
    availableMembers.value = (response.data || []).map((user: any) => ({
      ...user,
      user_id: user.userId, // 确保下划线命名存在
      role_id: user.roleId,   // 将roleId映射到role_id以供显示
    }));
  } catch (error) {
    ElMessage.error('获取成员列表失败');
    availableMembers.value = [];
  } finally {
    loadingMembers.value = false;
  }
};

// 搜索成员
const searchMembers = () => {
      loadAvailableMembers();
};

// 处理成员选择变更
const handleMemberSelectionChange = (selection: any[]) => {
  selectedMembers.value = selection;
};

// 为选中行添加高亮样式
const tableRowClassName = ({ row }: { row: any }) => {
  const isSelected = selectedMembers.value.some((m: any) => m.userId === row.userId);
  return isSelected ? 'selected-row' : '';
};

// 处理行点击事件
const handleRowClick = (row: any) => {
  memberTableRef.value?.toggleRowSelection(row);
};

// 确认成员选择
const confirmMemberSelection = () => {
  if (currentSubTaskIndex.value !== -1) {
    const currentSubTask = taskForm.subTasks[currentSubTaskIndex.value];
    const currentMemberIds = currentSubTask.members.map((m: any) => m.userId);

    selectedMembers.value.forEach((member: any) => {
      // API返回的是userId，确保使用正确的key
      const memberId = member.userId || member.user_id;
      if (!currentMemberIds.includes(memberId)) {
        currentSubTask.members.push({
          userId: memberId,
          name: member.name,
          avatar: member.avatar,
          role_id: member.roleId, // 系统角色ID
          role: '参与者',         // 默认的任务角色
          note: ''
        });
      }
    });
  }
  memberSelectorVisible.value = false;
};

// 移除成员
const removeMember = (subTaskIndex: number, memberIndex: number) => {
  taskForm.subTasks[subTaskIndex].members.splice(memberIndex, 1);
};

// 切换成员备注
const toggleMemberNote = (subTaskIndex: number, memberIndex: number) => {
  activeMemberNote.value = { subTaskIndex, memberIndex };
};

// 关闭成员备注
const closeActiveMemberNote = () => {
  activeMemberNote.value = { subTaskIndex: -1, memberIndex: -1 };
};

// 文件上传前的验证
const beforeUpload = (file: UploadFile): boolean => {
  // 文件大小验证
  const isLt20M = file.size ? file.size / 1024 / 1024 < 20 : false;
  if (!isLt20M) {
    ElMessage.error('文件大小不能超过 20MB!');
    return false;
  }

  // 文件类型黑名单验证
  const fileName = file.name || '';
  const fileExtension = fileName.slice(fileName.lastIndexOf('.') + 1).toLowerCase();
  const blacklist = ['exe', 'dll', 'bat', 'sh', 'cmd', 'jar'];

  if (blacklist.includes(fileExtension)) {
    ElMessage.error(`不允许上传 ${fileExtension} 类型的文件!`);
    return false;
  }

  return true;
};

// 文件移除处理
const handleRemove = (file: UploadFile) => {
  // 如果是已有的附件，从保留列表中移除
  const attachmentId = (file as any).attachmentId;
  if (attachmentId) {
    keepAttachmentIds.value = keepAttachmentIds.value.filter(id => id !== attachmentId);
  }
  
  // 从文件列表中移除
  fileList.value = fileList.value.filter(item => item.uid !== file.uid);
};

// 下载现有文件
const downloadExistingFile = (file: UploadUserFile) => {
  if ((file as any).attachmentId && (file as any).url) {
    const filePath = (file as any).url.replace('/api/admin/file/view/', '');
    window.open(`/api/admin/file/view/${filePath}?download=true&originalName=${encodeURIComponent(file.name)}`, '_blank');
  }
};

// 提交表单
const handleSubmit = async () => {
  formRef.value.validate(async (valid: boolean) => {
    if (!valid) return;
    
    // 验证子任务数据
    if (taskForm.subTasks.length === 0) {
      ElMessage.warning('请至少添加一个子任务');
      return;
    }
    
    for (const subTask of taskForm.subTasks) {
      if (!subTask.title.trim()) {
        ElMessage.warning('子任务标题不能为空');
        return;
      }
    }
    
    // 验证时间
    if (new Date(taskForm.endTime) <= new Date(taskForm.startTime)) {
      ElMessage.warning('截止时间必须晚于开始时间');
      return;
    }
    
    try {
    submitting.value = true;
    
      // 构建FormData对象
      const formDataObj = new FormData();
      
      // 准备任务数据JSON
      const taskData: any = {
        ...taskForm,
      subTasks: taskForm.subTasks.map(subTask => ({
          ...subTask,
          subTaskId: subTask.subTaskId || undefined
      }))
    };
    
      // 如果是编辑模式，添加保留的附件ID列表
      if (props.isEdit) {
        taskData.keepAttachmentIds = keepAttachmentIds.value;
        formDataObj.append('taskUpdateDto', new Blob([JSON.stringify(taskData)], { type: 'application/json' }));
      } else {
        formDataObj.append('taskAddDto', new Blob([JSON.stringify(taskData)], { type: 'application/json' }));
      }
      
      // 添加新上传的文件
      fileList.value.forEach(file => {
        // 只添加新上传的文件，已有的附件不需要重新上传
        if (file.raw && !(file as any).attachmentId) {
          formDataObj.append('files', file.raw);
        }
      });
      
      // 发送请求
    const apiCall = props.isEdit ? 
        updateTask(formDataObj) : 
        addTask(formDataObj);
    
      const response: any = await apiCall;
      if (response.code === 200 || response.code === 1) {
      ElMessage.success(props.isEdit ? '任务更新成功' : '任务创建成功');
      emit('refresh');
        closeDialog();
      } else {
        ElMessage.error(response.msg || (props.isEdit ? '任务更新失败' : '任务创建失败'));
      }
    } catch (error: any) {
      console.error('任务操作失败:', error);
      ElMessage.error(props.isEdit ? '任务更新失败' : '任务创建失败');
    } finally {
      submitting.value = false;
    }
  });
};

// 收集所有子任务的成员ID，去重
const getAllMemberIds = () => {
  // 此函数不再需要，因为成员信息已在子任务中提交
  return []; 
};

// 关闭对话框
const closeDialog = () => {
  dialogVisible.value = false;
  resetForm();
};

// 初始化
onMounted(() => {
  initFormData();
});

// 监听task属性变化
watch(() => props.task, () => {
  if (props.visible) {
    initFormData();
  }
}, { deep: true });
</script>

<style lang="scss" scoped>
.task-form-dialog {
  :deep(.el-dialog__body) {
    padding: 20px;
  }
  
  :deep(.el-dialog__header) {
    padding: 20px;
    border-bottom: 1px solid #ebeef5;
    margin-right: 0;
    background-color: #f8f9fa;
    
    .el-dialog__title {
      font-weight: 600;
      font-size: 18px;
    }
  }
  
  .form-section-card {
    margin-bottom: 25px;
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
    border: 1px solid #ebeef5;
    
    &:last-child {
      margin-bottom: 0;
    }
    
    :deep(.el-card__header) {
      padding: 15px 20px;
      border-bottom: 1px solid rgba(0, 0, 0, 0.05);
      background-color: #f8f9fa;
    }
    
    :deep(.el-card__body) {
      padding: 20px;
    }
    
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      h3 {
        margin: 0;
        font-size: 16px;
        font-weight: 600;
        color: #303133;
      }
      
      .add-subtask-btn {
        border-radius: 20px;
        padding: 8px 20px;
        border: none;
        background: linear-gradient(90deg, #409eff, #64b5f6);
        color: white;
        box-shadow: 0 2px 6px rgba(64, 158, 255, 0.3);
        min-width: 120px;
        
        .el-icon {
          margin-right: 4px;
        }
      }
    }
  }
  
  .alert-content {
    display: flex;
    align-items: center;
    gap: 8px;
    
    .el-icon {
      font-size: 16px;
    }
  }
  
  .submit-btn, .cancel-btn {
    border-radius: 20px;
    padding: 10px 24px;
    font-size: 14px;
  }
  
  .submit-btn {
    background: linear-gradient(90deg, #409eff, #64b5f6);
    border: none;
    box-shadow: 0 2px 6px rgba(64, 158, 255, 0.3);
    
    &:hover, &:focus {
      background: linear-gradient(90deg, #66b1ff, #90caf9);
      box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
    }
  }
  
  .cancel-btn {
    border: 1px solid #dcdfe6;
    
    &:hover, &:focus {
      background: #f5f7fa;
    }
  }
  
  .rounded-btn {
    border-radius: 20px !important;
    
    &.el-button--primary {
      background: linear-gradient(90deg, #409eff, #64b5f6);
      border: none;
      box-shadow: 0 2px 6px rgba(64, 158, 255, 0.3);
      
      &:hover, &:focus {
        background: linear-gradient(90deg, #66b1ff, #90caf9);
        box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
      }
      
      &.is-plain {
        background: transparent;
        border: 1px solid #409eff;
        color: #409eff;
        box-shadow: none;
        
        &:hover, &:focus {
          background: rgba(64, 158, 255, 0.1);
          border-color: #409eff;
          color: #409eff;
        }
      }
    }
    
    &.el-button--danger {
      background: linear-gradient(90deg, #f56c6c, #ff8a80);
      border: none;
      box-shadow: 0 2px 6px rgba(245, 108, 108, 0.3);
      
      &:hover, &:focus {
        background: linear-gradient(90deg, #f78989, #ffab91);
        box-shadow: 0 4px 12px rgba(245, 108, 108, 0.4);
      }
      
      &.is-plain {
        background: transparent;
        border: 1px solid #f56c6c;
        color: #f56c6c;
        box-shadow: none;
        
        &:hover, &:focus {
          background: rgba(245, 108, 108, 0.1);
          border-color: #f56c6c;
          color: #f56c6c;
        }
      }
    }
  }
  
  .empty-subtasks {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 20px 0;
    gap: 15px;
  }
  
  .subtasks-list {
    .subtask-cards {
      margin: 0 -10px;
      
      .subtask-col {
        margin-bottom: 25px;
        padding: 0 10px;
      }
      
            .subtask-card {
      height: 100%;
      border-radius: 12px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
      transition: transform 0.3s, box-shadow 0.3s;
      border: 1px solid #e0e0e0;
      background-color: #ffffff;
        
        &:hover {
          transform: translateY(-5px);
          box-shadow: 0 8px 15px rgba(0, 0, 0, 0.1);
          border-color: #c0c4cc;
        }
        
        :deep(.el-card__header) {
          padding: 15px;
          background-color: #f0f2f5;
          border-bottom: 2px solid #e6e6e6;
        }
        
        .subtask-card-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          
          h4 {
            margin: 0;
            font-size: 16px;
            font-weight: 600;
            color: #303133;
            position: relative;
            padding-left: 10px;
            
            &:before {
              content: '';
              position: absolute;
              left: 0;
              top: 50%;
              transform: translateY(-50%);
              width: 4px;
              height: 16px;
              background: linear-gradient(to bottom, #409eff, #64b5f6);
              border-radius: 2px;
            }
          }
          
          .subtask-actions {
            display: flex;
            align-items: center;
            flex: 1;
            justify-content: flex-end;
            margin-left: 20px;
            
            .spacer {
              flex: 1;
            }
            
            .delete-subtask-btn {
              padding: 6px 15px;
              font-size: 12px;
            }
          }
        }
        
        .subtask-content {
          padding: 10px 0;
        }
      }
    }
    
    .add-more-subtasks {
      display: flex;
      justify-content: center;
      margin-top: 20px;
    }
    
    .members-section {
      margin-top: 20px;
      padding-top: 20px;
      border-top: 1px dashed #ebeef5;
      
      .members-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 15px;
        
        h4 {
          margin: 0;
          font-size: 15px;
          font-weight: 600;
          color: #606266;
        }
      }
      
      .member-info {
        display: flex;
        align-items: center;
        gap: 16px;
        
        .member-name {
          font-size: 14px;
          color: #303133;
        }
      }
    }
  }
  
  .member-table {
    border-radius: 8px;
    overflow: hidden;
    
    :deep(.el-table__header) {
      background-color: #f8f9fa;
      
      th {
        background-color: #f8f9fa;
        color: #606266;
        font-weight: 600;
      }
    }
  }
  
  .dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 15px;
    padding-top: 10px;
  }
}

.icon-button {
  padding: 6px !important;
  min-width: auto !important;
}

.button-group {
  display: flex;
  align-items: center;
  gap: 5px;
  justify-content: center;
}

.member-note-section {
  margin-top: 15px;
  border-top: 1px dashed #ebeef5;
  padding-top: 15px;
}

.note-card {
  margin-bottom: 15px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08) !important;
  
  .note-card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    h4 {
      margin: 0;
      font-size: 14px;
      font-weight: 600;
      color: #303133;
    }
    
    .close-btn {
      padding: 2px;
      font-size: 16px;
      color: #909399;
      
      &:hover {
        color: #409EFF;
      }
    }
  }
  
  .note-card-content {
    padding: 10px 0;
  }
  
  .note-card-footer {
    margin-top: 15px;
    display: flex;
    justify-content: flex-end;
    gap: 10px;
  }
}

.member-table {
  .member-info {
    display: flex;
    align-items: center;
    gap: 10px;

    .member-name {
      font-size: 14px;
    }
  }
}

.member-selector-dialog {
  :deep(.el-dialog__body) {
    padding: 20px;
  }
  
  :deep(.el-dialog__header) {
    padding: 20px;
    border-bottom: 1px solid #ebeef5;
    margin-right: 0;
    background-color: #f8f9fa;
    
    .el-dialog__title {
      font-weight: 600;
      font-size: 18px;
    }
  }
  
  :deep(.el-table) {
    .el-table__row {
      cursor: pointer;
      
      &:hover {
        background-color: #f0f7ff !important;
      }
      
      td {
        padding: 12px 0;
      }
      
      .el-table-column--selection {
        .cell {
          padding-left: 14px;
        }
      }
    }
  }

  .member-table {
    .member-info {
      display: flex;
      align-items: center;
      gap: 12px; // 在头像和名字之间增加间距

      .member-name {
        font-size: 14px; // 调大名字的字体
        font-weight: 500;
      }
    }

    // 选中行的高亮样式
    :deep(.el-table__row.selected-row) {
      background-color: #ecf5ff !important;
    }
  }
}

.clickable-filename {
  color: #409EFF;
  cursor: pointer;
  
  &:hover {
    text-decoration: underline;
  }
}

.file-name-container {
  display: flex;
  align-items: center;
  gap: 5px;
  cursor: pointer;
}

.custom-file-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 5px;
  flex-grow: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-info .clickable-filename {
  color: #409EFF;
  cursor: pointer;
}

.file-info .clickable-filename:hover {
  text-decoration: underline;
}

.remove-icon {
  cursor: pointer;
  color: #F56C6C;
  margin-left: 10px;
}

.task-file-upload {
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
  padding: 20px;
  background-color: #f8f9fa;
  
  &:hover {
    border-color: #409EFF;
    background-color: rgba(64, 158, 255, 0.05);
  }
  
  .el-upload__tip {
    margin-top: 10px;
    font-size: 14px;
    color: #606266;
  }
}
</style>