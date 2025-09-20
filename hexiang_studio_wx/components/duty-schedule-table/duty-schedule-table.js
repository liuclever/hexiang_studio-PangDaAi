// components/duty-schedule-table/duty-schedule-table.js
const { http } = require('../../utils/request');
const { getUserInfo } = require('../../utils/auth');

Component({
  /**
   * 组件的属性列表
   */
  properties: {
    // 初始显示的周（可选，默认为当前周）
    initialWeek: {
      type: String,
      value: ''
    },
    // 是否显示底部操作栏
    showActions: {
      type: Boolean,
      value: true
    }
  },

  /**
   * 组件的初始数据
   */
  data: {
    // 当前显示的周信息
    currentWeek: null,
    weekInfo: {
      startDate: '',
      endDate: '',
      year: '',
      weekNumber: ''
    },
    isCurrentWeek: true,
    
    // 工作日数据
    weekdays: [
      { name: '周一', date: '', dateStr: '', isToday: false },
      { name: '周二', date: '', dateStr: '', isToday: false },
      { name: '周三', date: '', dateStr: '', isToday: false },
      { name: '周四', date: '', dateStr: '', isToday: false },
      { name: '周五', date: '', dateStr: '', isToday: false }
    ],
    
    // 时间段配置
    timeSlots: [
      { slot: '08:30-10:00', period: '上午\n第一节' },
      { slot: '10:20-11:50', period: '上午\n第二节' },
      { slot: '14:00-15:30', period: '下午\n第一节' },
      { slot: '15:50-17:20', period: '下午\n第二节' },
      { slot: '18:30-20:00', period: '晚上\n值班' }
    ],
    
    // 值班数据 {date: {timeSlot: {students: [], isActive: boolean, ...}}}
    dutyData: {},
    
    // 用户信息
    currentUser: null,
    
    // 页面状态
    loading: false
  },

  /**
   * 组件生命周期
   */
  lifetimes: {
    attached() {
      this.initComponent();
    }
  },

  /**
   * 组件方法
   */
  methods: {
    /**
     * 初始化组件
     */
    initComponent() {
      const userInfo = getUserInfo();
      this.setData({ currentUser: userInfo });
      
      // 设置初始周
      const initialWeek = this.properties.initialWeek;
      if (initialWeek) {
        this.setCurrentWeek(new Date(initialWeek));
      } else {
        this.setCurrentWeek(new Date());
      }
      
      this.loadDutyData();
    },

    /**
     * 设置当前显示的周
     */
    setCurrentWeek(date) {
      const monday = this.getMonday(date);
      const friday = this.getFriday(monday);
      
      this.setData({
        currentWeek: monday,
        weekInfo: {
          startDate: this.formatDate(monday, 'MM-DD'),
          endDate: this.formatDate(friday, 'MM-DD'),
          year: monday.getFullYear(),
          weekNumber: this.getWeekNumber(monday)
        },
        isCurrentWeek: this.isSameWeek(monday, new Date())
      });
      
      this.updateWeekdays(monday);
    },

    /**
     * 更新工作日数据
     */
    updateWeekdays(monday) {
      const today = new Date();
      const todayStr = this.formatDate(today, 'YYYY-MM-DD');
      
      const weekdays = this.data.weekdays.map((day, index) => {
        const currentDate = new Date(monday);
        currentDate.setDate(monday.getDate() + index);
        const dateStr = this.formatDate(currentDate, 'YYYY-MM-DD');
        
        return {
          ...day,
          date: this.formatDate(currentDate, 'DD'),
          dateStr: dateStr,
          isToday: dateStr === todayStr
        };
      });
      
      this.setData({ weekdays });
    },

    /**
     * 加载值班数据
     */
    async loadDutyData() {
      if (!this.data.currentWeek) return;
      
      this.setData({ loading: true });
      
      try {
        const monday = this.data.currentWeek;
        const friday = this.getFriday(monday);
        
        const startDate = this.formatDate(monday, 'YYYY-MM-DD');
        const endDate = this.formatDate(friday, 'YYYY-MM-DD');
        
        const response = await http.get('/admin/duty-schedule/weekly-table', {
          startDate,
          endDate
        });
        
        console.log('API响应:', response);
        
        if (response.code === 200) {
          console.log('原始数据:', response.data);
          const dutyData = this.processDutyData(response.data);
          console.log('处理后的数据:', dutyData);
          this.setData({ dutyData });
        } else {
          console.error('API调用失败:', response);
          wx.showToast({
            title: response.message || '加载值班数据失败',
            icon: 'error'
          });
        }
      } catch (error) {
        console.error('加载值班数据失败:', error);
        wx.showToast({
          title: '网络异常',
          icon: 'error'
        });
      } finally {
        this.setData({ loading: false });
      }
    },

    /**
     * 处理值班数据
     */
            processDutyData(rawData) {
      const dutyData = {};
      const currentUser = this.data.currentUser;
      const now = new Date();
      
      console.log('开始处理数据，rawData结构:', rawData);
      
      // 构建考勤状态映射表
      const statusMap = {};
      if (rawData.statusData) {
        rawData.statusData.forEach(status => {
          const key = `${status.duty_date}_${status.time_slot}_${status.student_id}`;
          statusMap[key] = status.attendance_status;
        });
      }
      console.log('考勤状态映射表:', statusMap);
      
      // 适配 /admin/duty-schedule/weekly-table 返回的数据格式
      if (rawData.tableData && rawData.tableData.dutyData) {
        const duties = rawData.tableData.dutyData;
        console.log('找到值班数据，条数:', duties.length);
        
        duties.forEach((duty, index) => {
          const date = duty.duty_date;
          const timeSlot = duty.time_slot;
          
          if (!date || !timeSlot) {
            console.warn('值班数据缺少日期或时间段:', duty);
            return;
          }
          
          if (!dutyData[date]) {
            dutyData[date] = {};
          }
          
          // 解析开始和结束时间
          const startTime = new Date(duty.start_time);
          const endTime = new Date(duty.end_time);
          
          // 判断状态
          const isActive = now >= startTime && now <= endTime;
          const isUpcoming = now < startTime;
          const isCompleted = now > endTime;
          
          // 处理学生数据，合并考勤状态
          const students = (duty.students || []).map(student => {
            const isCurrentUser = currentUser && student.studentId === currentUser.studentId;
            
            // 从statusMap中获取真实的考勤状态
            const statusKey = `${date}_${timeSlot}_${student.studentId}`;
            const attendanceStatus = statusMap[statusKey] || 'pending';
            
            console.log(`学生 ${student.studentName} (${student.studentId}) 状态: ${attendanceStatus}`);
            
            return {
              studentId: student.studentId,
              studentName: student.studentName || student.name || '未知',
              attendanceStatus: attendanceStatus,
              isCurrentUser
            };
          });
          
          dutyData[date][timeSlot] = {
            students,
            isActive,
            isUpcoming,
            isCompleted,
            scheduleId: duty.schedule_id,
            location: duty.location || '工作室'
          };
          
          console.log(`✅ 成功处理: ${date} ${timeSlot}, 学生数: ${students.length}`);
        });
      } else {
        console.warn('未找到tableData.dutyData，rawData:', rawData);
      }
      
      console.log('最终的dutyData结构:', dutyData);
      console.log('dutyData的日期键:', Object.keys(dutyData));
      
      // 检查当前weekdays的日期范围
      const weekdays = this.data.weekdays;
      console.log('当前weekdays日期范围:', weekdays.map(d => d.dateStr));
      
      // 详细检查数据匹配情况
      weekdays.forEach(day => {
        if (dutyData[day.dateStr]) {
          console.log(`📅 ${day.dateStr} 有数据:`, Object.keys(dutyData[day.dateStr]));
        } else {
          console.log(`❌ ${day.dateStr} 无数据`);
        }
      });
      
      return dutyData;
    },



    /**
     * 格式化时间为 HH:MM 格式
     */
    formatTime(date) {
      const hours = String(date.getHours()).padStart(2, '0');
      const minutes = String(date.getMinutes()).padStart(2, '0');
      return `${hours}:${minutes}`;
    },

    /**
     * 上一周
     */
    prevWeek() {
      const currentWeek = new Date(this.data.currentWeek);
      currentWeek.setDate(currentWeek.getDate() - 7);
      this.setCurrentWeek(currentWeek);
      this.loadDutyData();
    },

    /**
     * 下一周
     */
    nextWeek() {
      const currentWeek = new Date(this.data.currentWeek);
      currentWeek.setDate(currentWeek.getDate() + 7);
      this.setCurrentWeek(currentWeek);
      this.loadDutyData();
    },

    /**
     * 回到当前周
     */
    goToCurrentWeek() {
      this.setCurrentWeek(new Date());
      this.loadDutyData();
    },

    /**
     * 刷新数据
     */
    refreshData() {
      wx.showLoading({ title: '刷新中...' });
      this.loadDutyData().finally(() => {
        wx.hideLoading();
        wx.showToast({
          title: '刷新成功',
          icon: 'success',
          duration: 1500
        });
      });
    },

    /**
     * 单元格点击事件
     */
    onCellTap(e) {
      const { day, slot } = e.currentTarget.dataset;
      const dutyInfo = this.data.dutyData[day] && this.data.dutyData[day][slot];
      
      if (dutyInfo && dutyInfo.students.length > 0) {
        // 显示值班详情
        this.showDutyDetail(day, slot, dutyInfo);
      }
    },

    /**
     * 显示值班详情
     */
    showDutyDetail(day, slot, dutyInfo) {
      const studentNames = dutyInfo.students.map(s => s.studentName).join('、');
      const currentUserInDuty = dutyInfo.students.some(s => s.isCurrentUser);
      
      let actionItems = ['查看详情'];
      if (currentUserInDuty && (dutyInfo.isActive || dutyInfo.isUpcoming)) {
        actionItems.unshift('快速签到');
      }
      
      wx.showActionSheet({
        itemList: actionItems,
        success: (res) => {
          if (res.tapIndex === 0 && currentUserInDuty) {
            // 快速签到
            this.quickSignIn(dutyInfo);
          } else {
            // 查看详情
            this.viewDutyDetail(dutyInfo);
          }
        }
      });
    },

    /**
     * 快速签到
     */
    async quickSignIn(dutyInfo) {
      try {
        // 获取当前位置
        const location = await this.getCurrentLocation();
        
        // 执行签到
        const response = await http.post('/wx/attendance/check-in', {
          planId: dutyInfo.planId,
          latitude: location.latitude,
          longitude: location.longitude,
          location: location.address || '工作室'
        });
        
        if (response.success) {
          wx.showToast({
            title: '签到成功',
            icon: 'success'
          });
          this.loadDutyData(); // 刷新数据
        } else {
          wx.showToast({
            title: response.message || '签到失败',
            icon: 'error'
          });
        }
      } catch (error) {
        console.error('签到失败:', error);
        wx.showToast({
          title: '签到失败',
          icon: 'error'
        });
      }
    },

    /**
     * 获取当前位置
     */
    getCurrentLocation() {
      return new Promise((resolve, reject) => {
        wx.getLocation({
          type: 'gcj02',
          success: resolve,
          fail: reject
        });
      });
    },

    /**
     * 查看值班详情
     */
    viewDutyDetail(dutyInfo) {
      // 触发自定义事件，让父组件处理
      this.triggerEvent('dutydetail', { dutyInfo });
    },

    /**
     * 前往我的值班
     */
    goToMyDuties() {
      wx.navigateTo({
        url: '/pages/attendance/duty/index'
      });
    },

    // 工具方法
    getMonday(date) {
      const d = new Date(date);
      const day = d.getDay();
      const diff = d.getDate() - day + (day === 0 ? -6 : 1);
      return new Date(d.setDate(diff));
    },

    getFriday(monday) {
      const friday = new Date(monday);
      friday.setDate(monday.getDate() + 4);
      return friday;
    },

    formatDate(date, format = 'YYYY-MM-DD') {
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      
      return format
        .replace('YYYY', year)
        .replace('MM', month)
        .replace('DD', day);
    },

    isSameWeek(date1, date2) {
      const monday1 = this.getMonday(date1);
      const monday2 = this.getMonday(date2);
      return monday1.getTime() === monday2.getTime();
    },

    getWeekNumber(date) {
      const start = new Date(date.getFullYear(), 0, 1);
      const days = Math.floor((date - start) / (24 * 60 * 60 * 1000));
      return Math.ceil((days + start.getDay() + 1) / 7);
    }
  }
}); 