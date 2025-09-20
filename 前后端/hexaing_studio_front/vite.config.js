import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': '/src'
    }
  },
  server: {
    hmr: {
      overlay: false  // 禁用错误覆盖层
    },
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8044',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      },
      '/admin': {
        target: 'http://127.0.0.1:8044',
        changeOrigin: true
      }
    }
  }
}) 