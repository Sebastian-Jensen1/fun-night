import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    // Listen on the wifi address too, so phones on the same network can join from the QR code
    host: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        // Keep the browser's Host header: the server then sees a same-origin request from any
        // address the page was opened on (localhost or the wifi address) and no CORS check applies.
        changeOrigin: false,
      },
    },
  },
  test: {
    environment: 'node',
  },
})
