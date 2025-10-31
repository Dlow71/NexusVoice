import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  
  build: {
    // 代码分割优化
    rollupOptions: {
      output: {
        // 手动分包，将大型依赖拆分到单独的chunk
        manualChunks: {
          // Vue核心库
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
          // Element Plus UI库
          'element-plus': ['element-plus'],
          // Markdown渲染
          'markdown': ['marked', 'dompurify'],
          // 工具库
          'utils': ['axios'],
        },
        // 更好的chunk命名
        chunkFileNames: 'js/[name]-[hash].js',
        entryFileNames: 'js/[name]-[hash].js',
        assetFileNames: '[ext]/[name]-[hash].[ext]',
      }
    },
    // 调整chunk大小警告阈值（可选，默认500KB）
    chunkSizeWarningLimit: 1000,
    // 启用CSS代码分割
    cssCodeSplit: true,
    // 使用 esbuild 压缩（更快）
    minify: 'esbuild'
  },
  
  // 生产环境优化
  esbuild: {
    // 移除生产环境的console和debugger
    drop: ['console', 'debugger'],
  }
})
