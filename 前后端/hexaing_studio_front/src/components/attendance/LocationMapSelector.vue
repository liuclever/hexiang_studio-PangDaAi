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

// 腾讯地图Key
const TENCENT_MAP_KEY = 'YRQBZ-2NFHL-DFTP3-EJLZW-I3CGH-BKF24';

// 地图状态
const mapLoading = ref(false);
const mapError = ref('');

// 接收props
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
    default: 50
  }
});

// 定义事件
const emit = defineEmits(['update:longitude', 'update:latitude', 'update:address', 'update:radius', 'map-clicked']);

// 地图容器引用
const mapContainer = ref(null);

// 地图实例和标记
let map = null;
let marker = null;
let circle = null;
// 存储事件监听器引用，用于清理
let mapClickListener = null;

// 重庆机电职业技术大学坐标
const universityLocation = {
  lat: 29.552965,
  lng: 106.238573
};

// 加载腾讯地图脚本
function loadMapScript() {
  return new Promise((resolve, reject) => {
    if (window.qq && window.qq.maps) {
      resolve(window.qq.maps);
      return;
    }

    const script = document.createElement('script');
    script.type = 'text/javascript';
    // 使用正确的腾讯地图JavaScript API v2
    script.src = `https://map.qq.com/api/js?v=2.exp&key=${TENCENT_MAP_KEY}`;
    script.async = true;
    
    script.onload = () => {
      // 等待qq.maps对象完全加载
      if (window.qq && window.qq.maps) {
        resolve(window.qq.maps);
      } else {
        // 如果qq.maps还没准备好，等待一下
        setTimeout(() => {
          if (window.qq && window.qq.maps) {
            resolve(window.qq.maps);
          } else {
            reject(new Error('腾讯地图对象未找到'));
          }
        }, 100);
      }
    };
    script.onerror = (err) => {
      console.error('腾讯地图脚本加载失败:', err);
      reject(new Error('腾讯地图脚本加载失败，请检查网络连接或API Key'));
    };
    
    document.head.appendChild(script);
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

// 清理地图资源
function cleanupMap() {
  try {
    // 移除事件监听器
    if (map && mapClickListener && window.qq && window.qq.maps) {
      try {
        window.qq.maps.event.removeListener(mapClickListener);
      } catch (e) {
        console.warn('移除地图事件监听器失败:', e);
      }
      mapClickListener = null;
    }
    
    if (marker) {
      marker.setMap(null);
      marker = null;
    }
    if (circle) {
      circle.setMap(null);
      circle = null;
    }
    if (map) {
      map = null;
    }
  } catch (error) {
    console.warn('清理地图资源时出错:', error);
  }
}

// 处理地图点击事件
function handleMapClick(evt) {
  try {
    // 获取点击位置的经纬度
    if (evt && evt.latLng) {
      const lat = evt.latLng.lat;
      const lng = evt.latLng.lng;
      
      // 更新标记
      updateMarker(lat, lng);
      
      // 通知父组件清空常用地点选择
      emit('map-clicked');
    }
  } catch (error) {
    console.warn('处理地图点击事件失败:', error);
  }
}

// 更新标记位置
function updateMarker(lat, lng) {
  try {
    // 发出更新事件
    emit('update:latitude', lat);
    emit('update:longitude', lng);
    
    if (!map || !window.qq || !window.qq.maps) {
      return;
    }
    
    // 清除旧标记
    if (marker) {
      marker.setMap(null);
      marker = null;
    }
    if (circle) {
      circle.setMap(null);
      circle = null;
    }
    
    // 创建新位置
    const position = new window.qq.maps.LatLng(lat, lng);
    
    // 创建标记
    marker = new window.qq.maps.Marker({
        position: position,
      map: map
    });
    
    // 创建圆形区域
    const currentRadius = Number(props.radius);
    
    circle = new window.qq.maps.Circle({
        center: position,
        radius: currentRadius,
      strokeColor: 'rgba(255, 0, 0, 0.5)',
      strokeWeight: 2,
      fillColor: 'rgba(255, 0, 0, 0.2)',
      map: map
    });
    
    // 移动地图中心到标记位置
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
    // 加载地图脚本
    const qqMaps = await loadMapScript();
    
    if (!mapContainer.value) {
      console.error('地图容器未找到');
      throw new Error('地图容器未找到');
    }
    
    // 创建地图实例
    map = new qqMaps.Map(mapContainer.value, {
      center: new qqMaps.LatLng(universityLocation.lat, universityLocation.lng),
      zoom: 16
    });
    
    // 绑定地图点击事件
    try {
      mapClickListener = qqMaps.event.addListener(map, 'click', handleMapClick);
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
  // 如果地图和标记都存在，则更新圆形区域
  if (map && marker && circle && window.qq && window.qq.maps) {
    try {
      // 获取当前标记位置
      const position = marker.getPosition();
        
      // 更新圆形区域半径
      circle.setRadius(Number(newRadius));
    } catch (error) {
      console.warn('更新圆形区域失败:', error);
    }
  }
}, { immediate: false });

// 监听经纬度变化
watch([() => props.latitude, () => props.longitude], ([newLat, newLng]) => {
  // 确保地图已初始化
  if (map && window.qq && window.qq.maps) {
    const lat = parseFloat(newLat);
    const lng = parseFloat(newLng);
    
    // 确保是有效的坐标
    if (!isNaN(lat) && !isNaN(lng)) {
      // 更新标记
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