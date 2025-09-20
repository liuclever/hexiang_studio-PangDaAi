import { defineStore } from 'pinia';
import { ref, reactive } from 'vue';

interface UserInfo {
  id: number;
  username: string;
  name: string;
  role: string;
  avatar?: string;
}

export const useUserStore = defineStore('user', () => {
  const userInfo = ref<UserInfo | null>(null);
  const token = ref<string | null>(null);
  const isLoggedIn = ref(false);

  // 初始化用户信息
  function initUserInfo() {
    const storedToken = localStorage.getItem('token');
    if (storedToken) {
      token.value = storedToken;
      isLoggedIn.value = true;
      
      // 从localStorage获取用户信息
      const storedUserInfo = localStorage.getItem('userInfo');
      if (storedUserInfo) {
        try {
          userInfo.value = JSON.parse(storedUserInfo);
        } catch (error) {
          console.error('Failed to parse user info:', error);
        }
      }
    }
  }

  // 设置用户信息
  function setUserInfo(info: UserInfo) {
    userInfo.value = info;
    localStorage.setItem('userInfo', JSON.stringify(info));
  }

  // 设置token
  function setToken(newToken: string) {
    token.value = newToken;
    isLoggedIn.value = true;
    localStorage.setItem('token', newToken);
  }

  // 清除用户信息
  function clearUserInfo() {
    userInfo.value = null;
    token.value = null;
    isLoggedIn.value = false;
    localStorage.removeItem('token');
    localStorage.removeItem('userInfo');
  }

  return {
    userInfo,
    token,
    isLoggedIn,
    initUserInfo,
    setUserInfo,
    setToken,
    clearUserInfo
  };
}); 