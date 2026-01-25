import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react-swc'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  base: '/',
  test: {
    globals: {
      vi: {
        globals: true
      }
    },
    environment: 'jsdom',
    setupFiles: ['./src/test-setup.js'],
    environmentOptions: {
      jsdom: {
        resources: 'usable'
      }
    }
  }
})
