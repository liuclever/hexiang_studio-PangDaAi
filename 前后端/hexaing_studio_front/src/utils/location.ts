// 腾讯地图API密钥
export const TENCENT_MAP_KEY = 'GLVBZ-4RH6W-BC6R4-322H3-WOYW2-2NBSJ';

// 腾讯地图类型声明
declare global {
  interface Window {
    qq: {
      maps: any;
    };
    TMap: any;
  }
}

// 加载腾讯地图脚本 - WebServiceAPI
export const loadTencentMapScript = (): Promise<any> => {
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
    script.src = `https://map.qq.com/api/js?v=2.exp&key=${TENCENT_MAP_KEY}&callback=${callbackName}&libraries=geometry,geocoder`;
    script.onerror = reject;
    document.head.appendChild(script);
  });
};

// 加载腾讯地图脚本 - GL地图API
export const loadTencentGLMapScript = (): Promise<any> => {
  return new Promise((resolve, reject) => {
    if (window.TMap) {
      resolve(window.TMap);
      return;
    }
    
    const script = document.createElement('script');
    script.type = 'text/javascript';
    script.src = `https://map.qq.com/api/gljs?v=1.exp&key=${TENCENT_MAP_KEY}`;
    
    script.onload = () => {
      resolve(window.TMap);
    };
    
    script.onerror = reject;
    document.head.appendChild(script);
  });
};

// 获取当前位置
export const getCurrentPosition = (): Promise<{ lat: number; lng: number }> => {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('您的浏览器不支持地理位置功能'));
      return;
    }
    
    navigator.geolocation.getCurrentPosition(
      position => {
        resolve({
          lat: position.coords.latitude,
          lng: position.coords.longitude
        });
      },
      error => {
        let errorMsg = '获取位置失败';
        switch (error.code) {
          case error.PERMISSION_DENIED:
            errorMsg = '您拒绝了位置请求权限';
            break;
          case error.POSITION_UNAVAILABLE:
            errorMsg = '位置信息不可用';
            break;
          case error.TIMEOUT:
            errorMsg = '获取位置请求超时';
            break;
        }
        reject(new Error(errorMsg));
      },
      {
        enableHighAccuracy: true, // 尝试获取更精确的位置
        timeout: 10000,  // 10秒超时
        maximumAge: 0  // 禁用缓存
      }
    );
  });
};

// 计算两点之间的距离（使用 Haversine 公式）
export const calculateDistance = (lat1: number, lng1: number, lat2: number, lng2: number): number => {
  const R = 6371e3; // 地球半径（米）
  const φ1 = lat1 * Math.PI / 180;
  const φ2 = lat2 * Math.PI / 180;
  const Δφ = (lat2 - lat1) * Math.PI / 180;
  const Δλ = (lng2 - lng1) * Math.PI / 180;

  const a = Math.sin(Δφ / 2) * Math.sin(Δφ / 2) +
            Math.cos(φ1) * Math.cos(φ2) *
            Math.sin(Δλ / 2) * Math.sin(Δλ / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  const d = R * c;
  return d; // 单位：米
};

// 通过坐标获取地址信息
export const getAddressByLocation = async (lat: number, lng: number): Promise<string> => {
  try {
    // 加载腾讯地图API
    await loadTencentMapScript();
    
    return new Promise((resolve, reject) => {
      const geocoder = new window.qq.maps.Geocoder({
        complete: (result: any) => {
          if (result.detail.status === 0) {
            resolve(result.detail.address);
          } else {
            reject(new Error('获取地址信息失败'));
          }
        },
        error: (error: any) => {
          reject(new Error(`获取地址信息失败: ${error}`));
        }
      });
      
      const latLng = new window.qq.maps.LatLng(lat, lng);
      geocoder.getAddress(latLng);
    });
  } catch (error) {
    console.error('获取地址信息失败:', error);
    throw new Error('获取地址信息失败');
  }
}; 