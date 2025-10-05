<template>
  <div class="location-map-selector">
    <div class="map-container" ref="mapContainer">
      <div v-if="mapLoading" class="map-loading">
        <div class="loading-text">正在加载地图...</div>
      </div>
      <div v-if="mapError" class="map-error">
        <div class="error-text">{{ mapError }}</div>
        <button class="retry-btn" @click="reloadMap">重新加载</button>
      </div>
    </div>
    <div class="debug-info">
      <p>当前半径: {{ radius }}米</p>
      <button class="reset-btn" @click="reloadMap">重新加载地图</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue';
import { ElMessage } from 'element-plus';

// 腾讯地图Key - 统一使用有效的API Key
const TENCENT_MAP_KEY = 'GLVBZ-4RH6W-BC6R4-322H3-WOYW2-2NBSJ';

// 地图状态管理 - 响应式数据
const mapLoading = ref(false);
const mapError = ref('');

// 组件Props定义 - 接收父组件传递的地图参数
const props = defineProps({
  longitude: {
    type: [String, Number],
    default: 106.238573
  },
  latitude: {
    type: [String, Number],
    default: 29.552965
  },
  address: {
    type: String,
    default: '重庆机电职业技术大学'
  },
  radius: {
    type: [String, Number],
    default: 10  // 默认考勤范围10米
  }
});

// 组件事件定义 - 向父组件发送数据更新事件
const emit = defineEmits(['update:longitude', 'update:latitude', 'update:address', 'update:radius', 'map-clicked']);

// 地图容器DOM引用
const mapContainer = ref(null);

// 地图核心实例变量 - 存储地图、标记和圆形区域对象
let map = null;
let marker = null;
let circle = null;
// 事件监听器引用，用于组件卸载时清理资源，防止内存泄漏
let mapClickListener = null;

// 重庆机电职业技术大学坐标
const universityLocation = {
  lat: 29.552965,
  lng: 106.238573
};

// 加载腾讯地图脚本 - 企业级规范实现
function loadMapScript() {
  return new Promise((resolve, reject) => {
    // 检查3D地图对象是否已存在，避免重复加载
    if (window.TMap) {
      console.log('腾讯3D地图已加载，直接使用');
      resolve(window.TMap);
      return;
    }

    // 生成唯一的回调函数名，避免命名冲突
    const callbackName = 'initTencentMap_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    
    // 创建全局回调函数，用于3D地图API加载完成后的通知
    window[callbackName] = function() {
      console.log('腾讯3D地图API回调函数执行');
      // 验证3D地图对象是否正确加载
      if (window.TMap) {
        resolve(window.TMap);
      } else {
        reject(new Error('腾讯3D地图对象加载异常'));
      }
      // 清理全局回调函数，避免内存泄漏
      delete window[callbackName];
    };

    // 创建地图API脚本标签
    const script = document.createElement('script');
    script.type = 'text/javascript';
    script.async = true; // 异步加载，提高页面性能
    
    // 构建3D WebGL地图API URL - 支持3D效果和更丰富的可视化
    script.src = `https://map.qq.com/api/gljs?v=1.exp&key=${TENCENT_MAP_KEY}&callback=${callbackName}`;
    
    // 脚本加载成功处理
    script.onload = () => {
      console.log('腾讯地图脚本加载完成');
    };
    
    // 脚本加载失败处理 - 提供详细的错误信息
    script.onerror = (err) => {
      console.error('腾讯地图脚本加载失败:', err);
      // 清理回调函数
      delete window[callbackName];
      // 返回具体的错误信息，便于问题定位
      reject(new Error('腾讯地图脚本加载失败，请检查：1.网络连接 2.API Key有效性 3.域名白名单配置'));
    };
    
    // 将脚本添加到页面头部
    document.head.appendChild(script);
    
    console.log('开始加载腾讯地图脚本:', script.src);
  });
}

// 重新加载地图
function reloadMap() {
  // 清理旧资源
  cleanupMap();
  
  // 重置状态
  mapError.value = '';
  
  // 重新初始化
  setTimeout(() => {
    initMap();
  }, 100);
}

// 清理3D地图资源
function cleanupMap() {
  try {
    // 移除3D地图事件监听器
    if (map && mapClickListener) {
      try {
        map.off('click', mapClickListener);
      } catch (e) {
        console.warn('移除3D地图事件监听器失败:', e);
      }
      mapClickListener = null;
    }
    
    // 清理3D标记
    if (marker) {
      marker.setMap(null);
      marker = null;
    }
    // 清理3D圆形区域
    if (circle) {
      circle.setMap(null);
      circle = null;
    }
    // 清理3D地图实例
    if (map) {
      map = null;
    }
  } catch (error) {
    console.warn('清理3D地图资源时出错:', error);
  }
}

// 处理3D地图点击事件
function handleMapClick(evt) {
  try {
    // 获取3D地图点击位置的经纬度
    if (evt && evt.latLng) {
      const lat = evt.latLng.getLat(); // 3D地图API使用getLat()方法
      const lng = evt.latLng.getLng(); // 3D地图API使用getLng()方法
      
      // 更新标记
      updateMarker(lat, lng);
      
      // 通知父组件清空常用地点选择
      emit('map-clicked');
    }
  } catch (error) {
    console.warn('处理3D地图点击事件失败:', error);
  }
}

// 更新标记位置
function updateMarker(lat, lng) {
  try {
    // 发出更新事件
    emit('update:latitude', lat);
    emit('update:longitude', lng);
    
    if (!map || !window.TMap) {
      return;
    }
    
    // 清除旧标记和圆形区域
    if (marker) {
      marker.setMap(null);
      marker = null;
    }
    if (circle) {
      circle.setMap(null);
      circle = null;
    }
    
    // 创建新位置 - 3D地图坐标
    const position = new window.TMap.LatLng(lat, lng);
    
    // 创建3D标记点 - 使用简单的红色圆点样式
    marker = new window.TMap.MultiMarker({
      map: map,
      styles: {
        default: new window.TMap.MarkerStyle({
          width: 20,
          height: 20,
          anchor: { x: 10, y: 10 }, // 圆点中心锚点
          color: '#FF0000', // 红色
          borderColor: '#FFFFFF', // 白色边框
          borderWidth: 2,
          showBorder: true
        })
      },
      geometries: [{
        id: 'attendance-marker',
        styleId: 'default', 
        position: position,
        properties: {
          title: '考勤签到点'
        }
      }]
    });
    
    // 创建3D圆形区域 - 透明蓝色样式 (WebGL API方式)
    const currentRadius = Number(props.radius);
    
    try {
      circle = new window.TMap.MultiCircle({
        map: map,
        styles: {
          default: new window.TMap.CircleStyle({
            color: 'rgba(0, 123, 255, 0.2)', // 蓝色填充，20%透明度
            borderColor: 'rgba(0, 123, 255, 0.8)', // 蓝色边框，80%透明度  
            borderWidth: 2, // 边框宽度2px
            showBorder: true // 显示边框
          })
        },
        geometries: [{
          id: 'attendance-circle',
          styleId: 'default',
          center: position,
          radius: currentRadius
        }]
      });
    } catch (e) {
      console.warn('创建3D圆形区域失败，尝试替代方案:', e);
      // 如果MultiCircle不可用，暂时不显示圆形区域
      circle = null;
    }
    
    // 移动3D地图中心到标记位置 - 带动画效果
    try {
      map.panTo(position);
    } catch (e) {
      console.warn('移动地图中心失败:', e);
    }
  } catch (error) {
    console.error('更新标记失败:', error);
  }
}

// 初始化地图
async function initMap() {
  mapLoading.value = true;
  mapError.value = '';
  
  try {
    // 加载3D地图脚本
    const TMap = await loadMapScript();
    
    if (!mapContainer.value) {
      console.error('地图容器未找到');
      throw new Error('地图容器未找到');
    }
    
    // 创建3D地图实例 - WebGL渲染支持3D效果
    map = new TMap.Map(mapContainer.value, {
      // 地图中心点 - 重庆机电职业技术大学
      center: new TMap.LatLng(universityLocation.lat, universityLocation.lng),
      // 缩放级别 - 适合显示校园范围
      zoom: 16,
      // 3D视角倾斜角度 - 产生立体效果
      pitch: 30,
      // 地图旋转角度
      rotation: 0,
      // 视图模式 - 3D模式
      viewMode: '3D'
    });
    
    // 绑定3D地图点击事件
    try {
      mapClickListener = map.on('click', handleMapClick);
    } catch (e) {
      console.warn('地图点击事件绑定失败:', e);
    }
    
    // 初始标记
    const initialLat = parseFloat(props.latitude);
    const initialLng = parseFloat(props.longitude);
    
    if (!isNaN(initialLat) && !isNaN(initialLng)) {
      updateMarker(initialLat, initialLng);
    } else {
      updateMarker(universityLocation.lat, universityLocation.lng);
    }
    
    console.log('地图初始化成功');
    ElMessage.success('地图加载成功');
  } catch (error) {
    console.error('地图初始化失败:', error);
    mapError.value = error.message || '地图加载失败';
    ElMessage.error(`地图加载失败: ${error.message}`);
  } finally {
    mapLoading.value = false;
  }
}

// 监听半径变化
watch(() => props.radius, (newRadius) => {
  // 如果3D地图和标记都存在，则更新圆形区域
  if (map && marker && circle && window.TMap) {
    try {
      // 更新3D圆形区域半径 - MultiCircle API方式
      circle.updateGeometries([{
        id: 'attendance-circle',
        styleId: 'default',
        center: circle.getGeometries()[0].center, // 保持当前中心点
        radius: Number(newRadius) // 更新半径
      }]);
    } catch (error) {
      console.warn('更新3D圆形区域失败:', error);
    }
  }
}, { immediate: false });

// 监听经纬度变化
watch([() => props.latitude, () => props.longitude], ([newLat, newLng]) => {
  // 确保3D地图已初始化
  if (map && window.TMap) {
    const lat = parseFloat(newLat);
    const lng = parseFloat(newLng);
    
    // 确保是有效的坐标
    if (!isNaN(lat) && !isNaN(lng)) {
      // 更新3D标记
      updateMarker(lat, lng);
    }
  }
}, { immediate: false });

// 组件挂载时初始化地图
onMounted(() => {
  setTimeout(() => {
    initMap();
  }, 500); // 延迟初始化地图，确保DOM已完全渲染
});

// 组件卸载时清理
onUnmounted(() => {
  cleanupMap();
});
</script>

<style scoped>
.location-map-selector {
  width: 100%;
  height: 100%;
  position: relative;
}

.map-container {
  width: 100%;
  height: 400px;
  border: 1px solid #ddd;
  position: relative;
  overflow: hidden;
}

.map-loading {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f5f5f5;
  z-index: 1000;
}

.loading-text {
  font-size: 16px;
  color: #666;
}

.map-error {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background-color: #f5f5f5;
  z-index: 1000;
}

.error-text {
  font-size: 14px;
  color: #f56c6c;
  margin-bottom: 10px;
}

.retry-btn {
  padding: 8px 16px;
  background-color: #409EFF;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.retry-btn:hover {
  background-color: #66b1ff;
}

.debug-info {
  margin-top: 10px;
  padding: 5px;
  background-color: #f5f5f5;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 12px;
  color: #666;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.reset-btn {
  padding: 4px 8px;
  background-color: #409EFF;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.reset-btn:hover {
  background-color: #66b1ff;
}
</style> 