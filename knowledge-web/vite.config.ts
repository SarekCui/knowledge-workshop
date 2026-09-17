import { defineConfig } from 'vite';

export default defineConfig({
  resolve: { dedupe: ['react', 'react-dom'] },
  optimizeDeps: { include: ['react', 'react-dom/client', 'react-router'] },
  server: { proxy: { '/api': { target: 'http://127.0.0.1:8080', changeOrigin: true } } },
});
