import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  build: {
    // 빌드 결과물을 Spring Boot static 폴더로 직접 출력
    outDir: '../backend/src/main/resources/static',
    emptyOutDir: true
  },
  server: {
    port: 51300,
    proxy: {
      '/api': {
        target: 'http://localhost:48080',
        changeOrigin: true
      },
      '/ws': {
        target: 'ws://localhost:48080',
        ws: true,
        changeOrigin: true
      }
    }
  }
})
