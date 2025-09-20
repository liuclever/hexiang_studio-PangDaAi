// pages/attendance/check-in.js
const { checkLoginStatus, getUserInfo } = require('../../utils/auth');
const { http } = require('../../utils/request');

Page({
  /**
   * 页面的初始数据
   */
  data: {
    // 用户信息
    userInfo: null,
    // 可用考勤计划列表
    availablePlans: [],
    // 当前选中的考勤计划索引
    currentPlanIndex: 0,
    // 当前选中的考勤计划
    currentPlan: null,
    // 当前位置
    location: {
      latitude: 0,
      longitude: 0,
      address: ''
    },
    // 签到目标位置
    targetLocation: {
      latitude: 0,
      longitude: 0,
      name: '',
      address: '',
      radius: 200 // 单位: 米
    },
    // 签到状态
    checkInStatus: {
      // 是否在签到范围内
      inRange: false,
      // 与目标位置的距离 (米)
      distance: 0,
      // 是否已签到
      checked: false,
      // 签到时间
      checkTime: ''
    },
    // 页面状态
    pageStatus: {
      // 是否正在获取位置
      gettingLocation: true,
      // 是否加载完成
      loaded: false,
      // 是否提交中
      submitting: false,
      // 定位错误信息
      locationError: ''
    },
    // 地图设置
    mapSetting: {
      scale: 16,
      showLocation: true,
      showScale: true,
      enableZoom: true,
      enableScroll: true
    },
    // 地图标记
    markers: [],
    // 地图圆圈
    circles: []
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    // 检查登录状态
    if (!checkLoginStatus()) {
      return;
    }
    
    // 获取用户信息
    this.setData({
      userInfo: getUserInfo()
    });
    
    // 获取当前位置
    this.getCurrentLocation();
    
    // 获取可用考勤计划
    this.getAvailablePlans();
  },

  /**
   * 获取可用考勤计划列表
   */
  getAvailablePlans() {
    wx.showLoading({
      title: '加载考勤信息...'
    });

    http.get('/wx/attendance/current-plans')
      .then(res => {
        if (res.code === 200 && res.data) {
          const plans = Array.isArray(res.data) ? res.data : [];
          
          // 处理考勤计划数据
          const processedPlans = plans.map(plan => {
            // 格式化时间范围 - 使用新的时间格式化方法
            let timeRange = '--';
            try {
              if (plan.startTime && plan.endTime) {
                console.log('原始时间数据:', plan.startTime, plan.endTime);
                
                // 处理ISO格式时间，转换为小程序兼容的格式
                let startTimeStr = plan.startTime.toString().replace('T', ' ').replace(/\.\d{3}Z?$/, '');
                let endTimeStr = plan.endTime.toString().replace('T', ' ').replace(/\.\d{3}Z?$/, '');
                
                console.log('处理后时间字符串:', startTimeStr, endTimeStr);
                
                // 使用新的时间范围格式化方法
                timeRange = this.formatTimeRange(startTimeStr, endTimeStr);
                console.log('格式化后时间:', timeRange);
              }
            } catch (e) {
              console.error('时间格式化失败:', e);
            }
            
            return {
              ...plan,
              timeRange: timeRange
            };
          });

          this.setData({
            availablePlans: processedPlans,
            currentPlanIndex: 0
          });

          // 如果有可用计划，设置第一个为当前计划
          if (processedPlans.length > 0) {
            this.setCurrentPlan(0);
          }
        }
      })
      .catch(err => {
        console.error('获取考勤计划失败:', err);
        wx.showToast({
          title: '获取考勤信息失败',
          icon: 'none'
        });
      })
      .finally(() => {
        wx.hideLoading();
      });
  },

  /**
   * 格式化时间显示
   */
  formatTime(date) {
    if (!date) return '--:--';
    
    // 确保是Date对象
    if (typeof date === 'string') {
      date = new Date(date);
    }
    
    // 检查日期是否有效
    if (isNaN(date.getTime())) {
      return '--:--';
    }
    
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    return `${hours}:${minutes}`;
  },

  /**
   * 格式化考勤时间范围
   */
  formatTimeRange(startDate, endDate) {
    if (!startDate || !endDate) return '--';
    
    const start = new Date(startDate);
    const end = new Date(endDate);
    
    if (isNaN(start.getTime()) || isNaN(end.getTime())) {
      return '--';
    }
    
    // 检查是否同一天
    const isSameDay = start.toDateString() === end.toDateString();
    
    if (isSameDay) {
      // 同一天：只显示时间
      return `${this.formatTime(start)}-${this.formatTime(end)}`;
    } else {
      // 跨天：显示日期+时间
      const startStr = `${start.getMonth() + 1}/${start.getDate()} ${this.formatTime(start)}`;
      const endStr = `${end.getMonth() + 1}/${end.getDate()} ${this.formatTime(end)}`;
      return `${startStr} - ${endStr}`;
    }
  },

  /**
   * 设置当前考勤计划
   */
  setCurrentPlan(index) {
    const plan = this.data.availablePlans[index];
    if (!plan) return;

    this.setData({
      currentPlanIndex: index,
      currentPlan: plan,
      'targetLocation.latitude': plan.locationLat || 0,
      'targetLocation.longitude': plan.locationLng || 0,
      'targetLocation.name': plan.name,
      'targetLocation.address': plan.location || '',
      'targetLocation.radius': plan.radius || 200
    });

    // 查询当前考勤计划的签到状态
    this.getAttendanceStatus(plan.planId);

    // 如果已有当前位置，重新计算距离
    if (this.data.location.latitude && this.data.location.longitude) {
      this.calculateDistance();
    }

    // 更新地图标记
    this.updateMapMarkers();
    // 更新地图圆圈
    this.updateMapCircles();
  },

  /**
   * 卡片切换事件处理
   */
  onPlanChange(e) {
    const newIndex = e.detail.current;
    this.setCurrentPlan(newIndex);
  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow() {
    // 设置TabBar选中状态为签到（索引2）
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().updateSelected(2);
    }
    
    // 如果已初始化，刷新考勤计划
    if (this.data.userInfo) {
      this.getAvailablePlans();
    }
  },

  /**
   * 获取当前位置
   */
  getCurrentLocation() {
    this.setData({
      'pageStatus.gettingLocation': true
    });
    
    wx.getLocation({
      type: 'gcj02',
      success: (res) => {
        const { latitude, longitude } = res;
        
        // 更新当前位置
        this.setData({
          'location.latitude': latitude,
          'location.longitude': longitude,
          'pageStatus.gettingLocation': false
        });
        
        // 获取地址信息
        this.getAddressInfo(latitude, longitude);
        
        // 如果已有目标位置，计算距离
        if (this.data.targetLocation.latitude && this.data.targetLocation.longitude) {
          this.calculateDistance();
        }
        
        // 更新地图标记
        this.updateMapMarkers();
        // 更新地图圆圈
        this.updateMapCircles();
      },
      fail: (err) => {
        console.error('获取位置失败:', err);
        this.setData({
          'pageStatus.gettingLocation': false,
          'pageStatus.locationError': '获取位置失败，请检查位置权限设置'
        });
      }
    });
  },
  
  /**
   * 获取地址信息
   */
  getAddressInfo(latitude, longitude) {
    // 微信小程序逆地址解析
    wx.request({
      url: 'https://apis.map.qq.com/ws/geocoder/v1/',
      data: {
        location: `${latitude},${longitude}`,
        key: 'YOUR_MAP_KEY', // 需要替换成实际的地图API密钥
        get_poi: 0
      },
      success: (res) => {
        if (res.data && res.data.result) {
          const address = res.data.result.address;
          this.setData({
            'location.address': address
          });
        }
      },
      fail: (err) => {
        console.error('获取地址失败:', err);
      }
    });
  },

  /**
   * 获取签到点信息
   * 已废弃，现在使用getAvailablePlans方法
   */
  /*
  getCheckInLocations() {
    // TODO: 从服务器获取签到点信息
    
    // 模拟签到点数据
        const mockLocation = {
      latitude: 29.553017,
      longitude: 106.237538,
      name: '工作室',
      address: '重庆邮电大学某栋某楼',
          radius: 200
        };
        
        this.setData({
          'targetLocation.latitude': mockLocation.latitude,
          'targetLocation.longitude': mockLocation.longitude,
          'targetLocation.name': mockLocation.name,
          'targetLocation.address': mockLocation.address,
          'targetLocation.radius': mockLocation.radius
        });
        
        // 如果已有当前位置，计算距离
        if (this.data.location.latitude && this.data.location.longitude) {
          this.calculateDistance();
        }
        
        // 更新地图标记
        this.updateMapMarkers();
        // 更新地图圆圈
        this.updateMapCircles();
  },
  */

  /**
   * 计算当前位置与目标位置的距离
   */
  calculateDistance() {
    const { latitude: lat1, longitude: lng1 } = this.data.location;
    const { latitude: lat2, longitude: lng2, radius } = this.data.targetLocation;
    
    // 计算两点之间的距离（米）
    const distance = this.getDistance(lat1, lng1, lat2, lng2);
    
    // 判断是否在签到范围内
    const inRange = distance <= radius;
    
    this.setData({
      'checkInStatus.distance': Math.round(distance),
      'checkInStatus.inRange': inRange
    });
  },

  /**
   * 计算两个坐标点之间的距离（米）
   */
  getDistance(lat1, lng1, lat2, lng2) {
    const R = 6371000; // 地球半径，单位米
    const dLat = this.deg2rad(lat2 - lat1);
    const dLng = this.deg2rad(lng2 - lng1);
    const a = 
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(this.deg2rad(lat1)) * Math.cos(this.deg2rad(lat2)) * 
      Math.sin(dLng / 2) * Math.sin(dLng / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    const distance = R * c;
    return distance;
  },

  /**
   * 将角度转换为弧度
   */
  deg2rad(deg) {
    return deg * (Math.PI / 180);
  },

  /**
   * 更新地图标记
   */
  updateMapMarkers() {
    const markers = [];
    
    // 目标位置标记（签到点）
    if (this.data.targetLocation.latitude && this.data.targetLocation.longitude) {
      markers.push({
        id: 1,
        latitude: this.data.targetLocation.latitude,
        longitude: this.data.targetLocation.longitude,
        iconPath: '/images/icons/marker-target.png',
        width: 32,
        height: 32,
        callout: {
          content: '签到点',
          color: '#ffffff',
          fontSize: 14,
          bgColor: '#ff4757',
          borderRadius: 4,
          padding: 6,
          display: 'ALWAYS'
        }
      });
    }
    
    // 当前位置标记
    if (this.data.location.latitude && this.data.location.longitude) {
      markers.push({
        id: 2,
        latitude: this.data.location.latitude,
        longitude: this.data.location.longitude,
        iconPath: '/images/icons/marker-current.png',
        width: 24,
        height: 24,
        callout: {
          content: '我的位置',
          color: '#ffffff',
          fontSize: 14,
          bgColor: '#1890ff',
          borderRadius: 4,
          padding: 6,
          display: 'ALWAYS'
        }
      });
    }
    
    this.setData({
      markers
    });
  },

  /**
   * 更新地图圆圈
   */
  updateMapCircles() {
    const circles = [];
    if (this.data.targetLocation.latitude && this.data.targetLocation.longitude) {
      circles.push({
        latitude: this.data.targetLocation.latitude,
        longitude: this.data.targetLocation.longitude,
        color: 'rgba(24, 144, 255, 0.5)', // 蓝色边框
        fillColor: 'rgba(24, 144, 255, 0.1)', // 蓝色填充
        radius: this.data.targetLocation.radius,
        strokeWidth: 2
      });
    }
    this.setData({
      circles
    });
  },

  /**
   * 获取考勤计划的签到状态
   */
  getAttendanceStatus(planId) {
    http.get(`/wx/attendance/plan/${planId}/status`)
      .then(res => {
        if (res.code === 200 && res.data) {
          this.setData({
            'checkInStatus.checked': res.data.checked || false,
            'checkInStatus.checkTime': res.data.checkTime || ''
          });
        } else {
          // 默认为未签到状态
          this.setData({
            'checkInStatus.checked': false,
            'checkInStatus.checkTime': ''
          });
        }
      })
      .catch(err => {
        console.error('获取签到状态失败:', err);
        // 默认为未签到状态
        this.setData({
          'checkInStatus.checked': false,
          'checkInStatus.checkTime': ''
        });
      });
  },

  /**
   * 刷新位置
   */
  refreshLocation() {
    this.getCurrentLocation();
  },



  /**
   * 提交签到
   */
  submitCheckIn() {
    // 检查是否有当前考勤计划
    if (!this.data.currentPlan) {
      wx.showToast({
        title: '请选择考勤计划',
        icon: 'none'
      });
      return;
    }
    
    // 检查是否在签到范围内
    if (!this.data.checkInStatus.inRange) {
      wx.showToast({
        title: '请移动到签到范围内',
        icon: 'none'
      });
      return;
    }
    
    this.setData({
      'pageStatus.submitting': true
    });
    
    // 准备签到数据
    const checkInData = {
      planId: this.data.currentPlan.planId,
      studentId: this.data.userInfo.userId,
      latitude: this.data.location.latitude,
      longitude: this.data.location.longitude,
      location: this.data.location.address || '未知位置'
    };

    // 提交签到
    http.post('/wx/attendance/check-in', checkInData)
      .then(res => {
        if (res.code === 200) {
          // 签到成功
          const now = new Date();
          const timeString = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}`;
          
          this.setData({
            'checkInStatus.checked': true,
            'checkInStatus.checkTime': timeString
          });

        wx.showToast({
            title: res.data.message || '签到成功',
          icon: 'success'
        });
        } else {
          wx.showToast({
            title: res.message || '签到失败',
            icon: 'none'
          });
        }
      })
      .catch(err => {
        console.error('签到失败:', err);
        wx.showToast({
          title: '签到失败，请重试',
          icon: 'none'
        });
      })
      .finally(() => {
        this.setData({
          'pageStatus.submitting': false
        });
      });
  },

  /**
   * 查看签到记录
   */
  viewCheckInRecords() {
    wx.navigateTo({
      url: '/pages/attendance/records/index'
    });
  }
});