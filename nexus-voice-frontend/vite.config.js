import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  // server:{
  //   '/api':{
  //     //开发环境下处理跨域问题
  //     target: 'http://localhost:8081',
  //     changeOrigin: true,
  //     rewrite: (path)=> path
  //   }
  // },
  plugins: [vue()],
})
