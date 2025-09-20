<template>
    <div class="attendance-plan-form">
      <el-form 
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="100px"
        v-loading="loading"
      >
        <el-form-item label="考勤名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入考勤名称"></el-input>
        </el-form-item>
  
        <el-form-item label="考勤类型" prop="type">
          <el-radio-group v-model="formData.type">
            <el-radio label="activity">活动考勤</el-radio>
            <el-radio label="course">课程考勤</el-radio>
          </el-radio-group>
        </el-form-item>
  
        <template v-if="formData.type === 'course'">
          <el-form-item label="关联课程" prop="courseId">
            <el-select
              v-model="formData.courseId"
              filterable
              placeholder="请选择关联课程"
              :loading="courseSearchLoading"
              clearable
            >
              <el-option
                v-for="item in courseOptions"
                :key="item.id"
                :label="`${item.name} (${item.categoryName || '未分类'})`"
                :value="item.id"
              ></el-option>
            </el-select>
          </el-form-item>
        </template>
  
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            v-model="formData.startTime"
            type="datetime"
            placeholder="选择开始时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
          ></el-date-picker>
        </el-form-item>
  
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
            v-model="formData.endTime"
            type="datetime"
            placeholder="选择结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
          ></el-date-picker>
        </el-form-item>
  
        <el-form-item label="常用地点" prop="commonLocationId">
          <el-select
            v-model="formData.commonLocationId"
            filterable
            placeholder="请选择常用地点"
            clearable
            @change="handleCommonLocationChange"
          >
            <el-option
              v-for="item in commonLocations"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            ></el-option>
          </el-select>
          <div class="location-tip">
            <el-alert
              title="选择常用地点后将自动填充地点信息，或在地图上点击选择位置"
              type="info"
              :closable="false"
              show-icon
            ></el-alert>
          </div>
        </el-form-item>
  
        <el-form-item label="位置坐标" required>
          <div class="coordinate-inputs">
            <el-form-item prop="locationLng" class="coordinate-item">
              <el-input v-model.number="formData.locationLng" placeholder="经度" type="number"></el-input>
            </el-form-item>
            <el-form-item prop="locationLat" class="coordinate-item">
              <el-input v-model.number="formData.locationLat" placeholder="纬度" type="number"></el-input>
            </el-form-item>
          </div>
          <div class="map-selector">
            <location-map-selector 
              v-model:longitude="formData.locationLng" 
              v-model:latitude="formData.locationLat"
              v-model:address="formData.location"
              v-model:radius="formData.radius"
              @map-clicked="handleMapClicked"
            />
          </div>
        </el-form-item>
  
        <el-form-item label="有效半径" prop="radius">
          <el-input-number 
            v-model="formData.radius" 
            :min="1" 
            :max="50"
            :step="1"
          ></el-input-number>
          <span class="radius-unit">米</span>
        </el-form-item>
  
                <el-form-item label="备注" prop="note">
          <el-input 
            v-model="formData.note" 
            type="textarea" 
            placeholder="请输入备注信息"
            :rows="3"
          ></el-input>
        </el-form-item>

        <!-- 活动考勤时显示学生选择 -->
        <template v-if="formData.type === 'activity'">
          <el-divider content-position="left">
            <span style="color: #606266; font-size: 14px;">参与学生</span>
          </el-divider>
          
          <el-form-item label="预约学生" required>
            <div class="student-selection-container">
              <div class="student-selection-tip">
                <el-alert
                  title="请选择参与学生（必填），创建考勤计划后将自动为选中学生创建预约"
                  type="warning"
                  :closable="false"
                  show-icon
                />
              </div>
              
              <div class="student-transfer-wrapper">
                <el-transfer
                  v-model="selectedStudents"
                  :data="studentList"
                  :titles="['全部学生', '参与学生']"
                  :props="{
                    key: 'studentId',
                    label: 'studentName'
                  }"
                  :filter-placeholder="'搜索学生姓名'"
                  filterable
                  class="student-transfer"
                >
                  <template #default="{ option }">
                    <div class="student-item">
                      <span class="student-name">{{ option.studentName }}</span>
                      <span class="student-info" v-if="option.grade">{{ option.grade }}</span>
                    </div>
                  </template>
                </el-transfer>
              </div>
              
              <div class="selection-summary" v-if="selectedStudents.length > 0">
                <el-tag type="success" size="small">
                  已选择 {{ selectedStudents.length }} 名学生
                </el-tag>
              </div>
            </div>
          </el-form-item>
        </template>

        <el-form-item>
          <el-button type="primary" @click="submitForm" :loading="props.loading">
            {{ isEdit ? '更新' : '创建' }}
          </el-button>
          <el-button @click="cancel">取消</el-button>
        </el-form-item>
      </el-form>
    </div>
  </template>
  
  <script setup>
  import { ref, reactive, onMounted, watch } from 'vue';
  import { ElMessage } from 'element-plus';
  import LocationMapSelector from '@/components/attendance/LocationMapSelector.vue';
  import { searchCoursesApi, getDutySchedulesApi, getAllStudents } from '@/api/attendance';
  import { getAllCommonLocations } from '@/api/locations';
  import { formatDateTime } from '@/utils/date';
  
  const props = defineProps({
    initialData: {
        type: Object,
        default: () => ({
            name: '',
            type: 'activity',
            startTime: '',
            endTime: '',
            location: '',
            locationLat: 29.552965,
            locationLng: 106.238573,
            radius: 50,
            remark: '',
            courseId: null,
            commonLocationId: null
        })
    },
    isEdit: {
      type: Boolean,
      default: false
    },
    attendanceType: {
      type: String,
      required: true,
      validator: (value) => ['activity', 'course'].includes(value)
    },
    loading: {
      type: Boolean,
      default: false
    }
  });
  
  const emit = defineEmits(['submit', 'cancel']);
  
  const formRef = ref(null);
  
  // 使用reactive复制一份props.initialData而不是直接引用
  const formData = reactive({ 
    name: props.initialData.name || '',
    type: props.initialData.type || 'activity',
    startTime: props.initialData.startTime || '',
    endTime: props.initialData.endTime || '',
    location: props.initialData.location || '',
    locationLat: props.initialData.locationLat || 29.552965,
    locationLng: props.initialData.locationLng || 106.238573,
    radius: props.initialData.radius || 50,
    note: props.initialData.note || '',
    courseId: props.initialData.courseId || null,
    commonLocationId: props.initialData.commonLocationId || null,
    scheduleId: props.initialData.scheduleId || null,
    status: props.initialData.status || 1
  });
  
  // 监听props变化，同步到formData
  watch(() => props.initialData, (newVal) => {
    if (newVal) {
      Object.keys(newVal).forEach(key => {
        if (key in formData) {
          formData[key] = newVal[key];
        }
      });
    }
  }, { deep: true });
  
  // Since the component is re-mounted each time, we handle the 'duty' type logic on mount.
  onMounted(() => {
    if (formData.type === 'duty') {
        formData.type = 'activity';
    }
    fetchCommonLocations();
    
    // 如果是课程考勤，需要加载课程列表
    if (formData.type === 'course') {
      fetchCourses();
    }
    
    // 如果是活动考勤，需要加载学生列表
    if (formData.type === 'activity') {
      fetchStudentList();
    }
  });
  
  // 监听考勤类型变化，当类型为course时加载课程列表，为activity时加载学生列表
  watch(() => formData.type, (newType) => {
    if (newType === 'course') {
      fetchCourses();
    } else if (newType === 'activity') {
      fetchStudentList();
      // 清空之前的学生选择
      selectedStudents.value = [];
    }
  });
  
  // 数据加载状态
  const loading = ref(false);
  const courseSearchLoading = ref(false);
  const scheduleLoading = ref(false);
  
  // 课程选项
  const courseOptions = ref([]);
  
  // 值班排班选项
  const scheduleOptions = ref([]);
  
  // 常用地点选项
  const commonLocations = ref([]);
  
  // 学生选择相关
  const studentList = ref([]);
  const selectedStudents = ref([]);
  const studentLoading = ref(false);
  
  // 时间段选项 - 根据后端设置
  const timeSlots = [
    '08:30-10:00',
    '10:20-11:50',
    '14:00-15:30',
    '15:50-17:20',
    '18:30-20:00'
  ];
  
  // 表单校验规则
  const rules = reactive({
    name: [
      { required: true, message: '请输入考勤名称', trigger: 'blur' },
      { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
    ],
    type: [
      { required: true, message: '请选择考勤类型', trigger: 'change' }
    ],
    courseId: [
      { 
        validator: (rule, value, callback) => {
          if (formData.type === 'course' && !value) {
            callback(new Error('请选择关联课程'));
          } else {
            callback();
          }
        }, 
        trigger: 'change' 
      }
    ],
    scheduleId: [
      { required: false, message: '请选择关联排班', trigger: 'change' }
    ],
    startTime: [
      { required: true, message: '请选择开始时间', trigger: 'change' }
    ],
    endTime: [
      { required: true, message: '请选择结束时间', trigger: 'change' },
      {
        validator: (rule, value, callback) => {
          if (value && props.initialData.startTime && new Date(value) <= new Date(props.initialData.startTime)) {
            callback(new Error('结束时间必须大于开始时间'));
          } else {
            callback();
          }
        },
        trigger: 'change'
      }
    ],
    location: [
      { required: false, message: '请输入地点', trigger: 'blur' }
    ],
    locationLng: [
      { required: true, message: '请输入经度', trigger: 'blur' },
      { type: 'number', message: '经度必须为数字' }
    ],
    locationLat: [
      { required: true, message: '请输入纬度', trigger: 'blur' },
      { type: 'number', message: '纬度必须为数字' }
    ],
    radius: [
      { required: true, message: '请设置有效半径', trigger: 'change' },
      { type: 'number', min: 1, max: 50, message: '半径范围在 1 到 50 米', trigger: 'change' },
      { 
        validator: (rule, value, callback) => {
          if (value < 1 || value > 50) {
            callback(new Error('有效半径必须在1到50米之间'));
          } else {
            callback();
          }
        }, 
        trigger: 'change' 
      }
    ]
  });
  
  // 获取课程列表
  const fetchCourses = async () => {
    courseSearchLoading.value = true;
    try {
      const res = await searchCoursesApi(''); // 空字符串获取所有课程
      if (res && (res.code === 1 || res.code === 200)) {
        // 适配后端返回的数据结构
        courseOptions.value = (res.data || []).map((item) => ({
          id: item.courseId || item.id,
          name: item.name,
          courseCode: item.courseId || item.id,
          categoryName: item.categoryName // 添加课程分类名称
        }));
      } else {
        ElMessage.error(res?.msg || '获取课程列表失败');
      }
    } catch (error) {
      console.error('获取课程列表失败:', error);
      ElMessage.error('获取课程列表失败');
    } finally {
      courseSearchLoading.value = false;
    }
  };
  
  // 获取值班排班
  const fetchDutySchedules = async () => {
    scheduleLoading.value = true;
    try {
      const res = await getDutySchedulesApi();
      if (res && res.code === 200) {
        scheduleOptions.value = res.data || [];
      } else {
        ElMessage.error(res?.msg || '获取值班排班失败');
      }
    } catch (error) {
      console.error('获取值班排班失败:', error);
      ElMessage.error('获取值班排班失败');
    } finally {
      scheduleLoading.value = false;
    }
  };
  
  // 获取常用地点
  const fetchCommonLocations = async () => {
    try {
      const res = await getAllCommonLocations(); // 使用正确的API函数
      
      // 判断返回码是200或1都视为成功
      if (res && (res.code === 1 || res.code === 200)) {
        // 适配后端返回的数据结构
        commonLocations.value = (res.data || []).map(item => {
          return {
            id: item.id,
            name: item.name || `地点${item.id}`,
            longitude: item.lng !== undefined ? item.lng : item.longitude,
            latitude: item.lat !== undefined ? item.lat : item.latitude,
            radius: item.radius || item.defaultRadius || 50
          };
        });
      } else {
        ElMessage.error(res?.msg || '获取常用地点失败');
      }
    } catch (error) {
      ElMessage.error('获取常用地点失败');
    }
  };

  // 获取学生列表
  const fetchStudentList = async () => {
    if (studentList.value.length > 0) return; // 避免重复加载
    
    try {
      studentLoading.value = true;
      const res = await getAllStudents();
      if (res && res.code === 200) {
        studentList.value = (res.data || []).map(student => ({
          studentId: student.studentId || student.id,
          studentName: student.studentName || student.name,
          grade: student.grade || '',
          major: student.major || ''
        }));
        console.log('学生列表加载成功:', studentList.value);
      } else {
        ElMessage.error('获取学生列表失败');
        studentList.value = [];
      }
    } catch (error) {
      console.error('获取学生列表失败:', error);
      ElMessage.error('获取学生列表失败');
      studentList.value = [];
    } finally {
      studentLoading.value = false;
    }
  };
  
  // 处理地图点击事件
  const handleMapClicked = () => {
    // 清空常用地点选择，避免冲突
    formData.commonLocationId = null;
  };
  
  // 处理选择常用地点
  const handleCommonLocationChange = (locationId) => {
    if (!locationId) return;
    
    // 尝试查找匹配的地点
    let selectedLocation = commonLocations.value.find(item => {
      return String(item.id) === String(locationId);
    });
    
    if (selectedLocation) {
      // 确保使用正确的属性名
      formData.location = selectedLocation.name;
      formData.locationLng = Number(selectedLocation.longitude || selectedLocation.lng);
      formData.locationLat = Number(selectedLocation.latitude || selectedLocation.lat);
      formData.radius = Number(selectedLocation.radius || selectedLocation.defaultRadius || 50);
    } else {
      ElMessage.warning('未找到匹配的常用地点');
    }
  };
  
  // 处理选择时间段
  const handleTimeSlotChange = (timeSlot) => {
    if (!timeSlot) return;
    
    // 解析时间段
    const [startTimeStr, endTimeStr] = timeSlot.split('-');
    
    // 获取当前日期
    const today = new Date();
    const year = today.getFullYear();
    const month = today.getMonth();
    const date = today.getDate();
    
    // 设置开始时间和结束时间
    const [startHour, startMinute] = startTimeStr.split(':').map(Number);
    const [endHour, endMinute] = endTimeStr.split(':').map(Number);
    
    const startTime = new Date(year, month, date, startHour, startMinute);
    const endTime = new Date(year, month, date, endHour, endMinute);
    
    // 格式化为字符串
    formData.startTime = formatDateTime(startTime);
    formData.endTime = formatDateTime(endTime);
  };
  
  // 提交表单
  const submitForm = () => {
    console.log('开始验证表单', formData);
    formRef.value.validate((valid) => {
      if (valid) {
        console.log('表单验证通过，准备提交数据:', formData);
        
        // 获取用户ID
        const userId = localStorage.getItem('userId');
        
        // 确保日期格式正确
        let submitData = {
          ...formData,
          name: formData.name?.trim(),
          type: formData.type || 'activity',
          location: formData.location || '默认地点',
          locationLng: Number(formData.locationLng),
          locationLat: Number(formData.locationLat),
          radius: Number(formData.radius),
          status: formData.status || 1,
          createUser: userId ? parseInt(userId) : 1, // 确保创建用户ID被设置
          selectedStudents: formData.type === 'activity' ? selectedStudents.value : [] // 添加选中的学生
        };
        
        console.log('最终提交数据:', submitData);
        console.log('选中的学生:', selectedStudents.value);
        emit('submit', submitData);
      } else {
        console.error('表单验证失败');
        ElMessage.error('表单验证失败，请检查输入');
      }
    });
  };
  
  // 取消
  const cancel = () => {
    emit('cancel');
  };
  
  // 导出方法供父组件调用
  defineExpose({
    fetchCommonLocations
  });
  </script>
  
  <style scoped>
  .attendance-plan-form {
    max-width: 100%;
  }
  
  .time-slot-tip,
  .location-tip {
    margin-top: 8px;
  }
  
  .coordinate-inputs {
    display: flex;
    gap: 10px;
    margin-bottom: 10px;
  }
  
  .coordinate-item {
    flex: 1;
    margin-bottom: 0;
  }
  
  .map-selector {
    margin-top: 10px;
    height: 400px;
    width: 100%;
    overflow: hidden;
  }
  
  .radius-unit {
    margin-left: 10px;
  }

  </style>