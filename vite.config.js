import { defineConfig } from 'vite'

export default defineConfig({
  root: './educaweb/frontend',
  server: {
    port: 5173,
    host: true
  },
  build: {
    outDir: '../dist',
    emptyOutDir: true
  }
})
