import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Dev: gọi /api/... qua proxy sang backend để khỏi cấu hình CORS.
    // Đổi đích bằng biến môi trường VITE_DEV_BACKEND (mặc định backend Java local).
    proxy: {
      '/api': {
        target: process.env.VITE_DEV_BACKEND ?? 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
