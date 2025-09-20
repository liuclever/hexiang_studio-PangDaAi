<template>
  <div class="location-map-container">
    <div id="map" ref="mapContainer" class="map-container"></div>
    <div class="map-type-controls">
      <button :class="{ active: mapType === 'normal' }" @click.prevent="changeMapType('normal')">地图</button>
      <button :class="{ active: mapType === 'satellite' }" @click.prevent="changeMapType('satellite')">卫星</button>
    </div>
    <div class="location-hint" v-if="!hasMarker">请在地图上点击选择签到位置</div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue';

const props = defineProps<{
  modelValue: { lat: number; lng: number } | undefined;
  locationName: string;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: { lat: number; lng: number }): void;
  (e: 'update:locationName', value: string): void;
}>();

const mapContainer = ref<HTMLElement | null>(null);
// 重庆机电职业技术大学坐标
const universityLocation = { lat: 29.552965, lng: 106.238573 };
const coordinates = ref<{ lat: number; lng: number }>(props.modelValue || universityLocation);
const mapType = ref<string>('normal');
const hasMarker = ref<boolean>(false);

// 腾讯地图实例和控件
let map: any = null;
let marker: any = null;

// 腾讯地图API密钥
const txMapKey = "GLVBZ-4RH6W-BC6R4-322H3-WOYW2-2NBSJ";

// 监听props变化，当外部选择常用标记点时更新地图
watch(() => props.modelValue, (newValue) => {
  if (newValue && newValue.lat && newValue.lng) {
    updateMarker(newValue.lat, newValue.lng);
  
    // 将地图中心移动到选择的位置
    if (map && window.qq && window.qq.maps) {
      map.panTo(new window.qq.maps.LatLng(newValue.lat, newValue.lng));
    }
  }
}, { deep: true });

// 改变地图类型
const changeMapType = (type: string) => {
  if (!map || !window.qq || !window.qq.maps) return;
  
  mapType.value = type;
  
  if (type === 'normal') {
    map.setMapTypeId(window.qq.maps.MapTypeId.ROADMAP);
  } else {
    map.setMapTypeId(window.qq.maps.MapTypeId.SATELLITE);
  }
};

// 初始化腾讯地图
const initMap = () => {
  if (!mapContainer.value || !window.qq || !window.qq.maps) {
    console.error('腾讯地图API未加载成功');
    return;
  }
  
  // 创建地图实例
  map = new window.qq.maps.Map(mapContainer.value, {
    center: new window.qq.maps.LatLng(universityLocation.lat, universityLocation.lng),
    zoom: 17,  // 增加缩放级别，更近地查看学校
    minZoom: 16,  // 限制最小缩放级别，防止缩小过度
    maxZoom: 19,  // 允许更大的缩放级别
    mapTypeId: mapType.value === 'normal' ? 
      window.qq.maps.MapTypeId.ROADMAP : 
      window.qq.maps.MapTypeId.SATELLITE,
    disableDefaultUI: true, // 禁用默认UI控件
    draggableCursor: 'pointer' // 设置鼠标样式为指针
  });
  
  // 限制地图可拖动范围 - 仅学校区域
  const bounds = new window.qq.maps.LatLngBounds(
    new window.qq.maps.LatLng(29.548, 106.233), // 西南角 - 缩小范围至仅包括学校
    new window.qq.maps.LatLng(29.558, 106.244)  // 东北角 - 缩小范围至仅包括学校
  );
  map.setOptions({
    restriction: {
      latLngBounds: bounds,
      strictBounds: true
    }
  });
  
  // 添加点击事件处理
  window.qq.maps.event.addListener(map, 'click', function(event: any) {
    const latLng = event.latLng;
    coordinates.value = { lat: latLng.getLat(), lng: latLng.getLng() };
    updateMarker(latLng.getLat(), latLng.getLng());
    
    // 获取位置名称
    getAddressByLocation(latLng.getLat(), latLng.getLng());
    
    emit('update:modelValue', coordinates.value);
  });
  
  // 如果有初始坐标，添加标记
  if (props.modelValue && props.modelValue.lat && props.modelValue.lng) {
    updateMarker(props.modelValue.lat, props.modelValue.lng);
    hasMarker.value = true;
  }
};

// 更新标记位置
const updateMarker = (lat: number, lng: number) => {
  if (!map || !window.qq || !window.qq.maps) return;
  
  // 检查坐标是否在限定范围内
  const bounds = new window.qq.maps.LatLngBounds(
    new window.qq.maps.LatLng(29.548, 106.233), // 西南角
    new window.qq.maps.LatLng(29.558, 106.244)  // 东北角
  );
  
  // 确保位置不超出范围
  let validLat = Math.max(bounds.getSouthWest().getLat(), Math.min(bounds.getNorthEast().getLat(), lat));
  let validLng = Math.max(bounds.getSouthWest().getLng(), Math.min(bounds.getNorthEast().getLng(), lng));
  
  const position = new window.qq.maps.LatLng(validLat, validLng);
  
  // 如果已有标记，移除
  if (marker) {
    marker.setMap(null);
  }
  
  // 创建新标记
  marker = new window.qq.maps.Marker({
    position: position,
    map: map,
    animation: window.qq.maps.MarkerAnimation.DROP
  });
  
  // 更新坐标值
  coordinates.value = { lat: validLat, lng: validLng };
  
  // 添加拖拽功能
  marker.setDraggable(true);
  window.qq.maps.event.addListener(marker, 'dragend', function() {
    const newPos = marker.getPosition();
    let newLat = newPos.getLat();
    let newLng = newPos.getLng();
    
    // 确保拖拽不超出范围
    newLat = Math.max(bounds.getSouthWest().getLat(), Math.min(bounds.getNorthEast().getLat(), newLat));
    newLng = Math.max(bounds.getSouthWest().getLng(), Math.min(bounds.getNorthEast().getLng(), newLng));
    
    if (newLat !== newPos.getLat() || newLng !== newPos.getLng()) {
      // 如果位置被调整，重新设置标记位置
      const validPosition = new window.qq.maps.LatLng(newLat, newLng);
      marker.setPosition(validPosition);
    }
    
    coordinates.value = { lat: newLat, lng: newLng };
  
    // 获取位置名称
    getAddressByLocation(newLat, newLng);
    
    emit('update:modelValue', coordinates.value);
  });
  
  hasMarker.value = true;
  
  // 获取初始位置名称
  getAddressByLocation(validLat, validLng);
};

// 根据坐标获取地址
const getAddressByLocation = (lat: number, lng: number) => {
  if (!window.qq || !window.qq.maps) return;
  
  const geocoder = new window.qq.maps.Geocoder({
    complete: function(result: any) {
      if (result.detail.address) {
        const address = result.detail.address;
        emit('update:locationName', address);
      } else {
        emit('update:locationName', `位置(${lat.toFixed(6)},${lng.toFixed(6)})`);
      }
    },
    error: function() {
      emit('update:locationName', `位置(${lat.toFixed(6)},${lng.toFixed(6)})`);
    }
  });
  
  geocoder.getAddress(new window.qq.maps.LatLng(lat, lng));
};

// 加载腾讯地图脚本
const loadTencentMapScript = () => {
  return new Promise((resolve, reject) => {
    // 如果已经加载，直接返回
    if (window.qq && window.qq.maps) {
      resolve(window.qq.maps);
    return;
  }
  
    // 创建回调函数名称
    const callbackName = 'initTencentMap' + Date.now();
    (window as any)[callbackName] = function() {
      resolve(window.qq.maps);
      delete (window as any)[callbackName];
    };
      
    // 创建script标签
    const script = document.createElement('script');
    script.type = 'text/javascript';
    script.src = `https://map.qq.com/api/js?v=2.exp&key=${txMapKey}&callback=${callbackName}&libraries=geometry,geocoder`;
    script.onerror = reject;
    document.head.appendChild(script);
  });
};

// 声明qq地图全局变量类型
declare global {
  interface Window {
    qq: {
      maps: any;
    };
  }
}

// 组件挂载时初始化地图
onMounted(async () => {
  try {
    await loadTencentMapScript();
    initMap();
  } catch (error) {
    console.error('加载腾讯地图失败:', error);
  }
});

// 组件卸载时销毁地图
onUnmounted(() => {
  if (marker) {
    marker.setMap(null);
  }
  map = null;
  marker = null;
});
</script>

<style scoped>
.location-map-container {
  width: 100%;
  position: relative;
  height: 400px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  overflow: hidden;
}

.map-container {
  width: 100%;
  height: 100%;
}

.map-type-controls {
  position: absolute;
  top: 10px;
  right: 10px;
  display: flex;
  background-color: white;
  border-radius: 4px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  z-index: 1000;
}

.map-type-controls button {
  padding: 6px 12px;
  border: none;
  background: white;
  cursor: pointer;
  font-size: 14px;
  border-right: 1px solid #eee;
}

.map-type-controls button:last-child {
  border-right: none;
}

.map-type-controls button.active {
  background-color: #409EFF;
  color: white;
}

.location-hint {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background-color: rgba(0, 0, 0, 0.6);
  color: white;
  padding: 8px 16px;
  border-radius: 4px;
  font-size: 14px;
  pointer-events: none;
  z-index: 1000;
}
</style> 