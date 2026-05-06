import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import { VitePWA } from "vite-plugin-pwa";

export default defineConfig(({ mode }) => ({
  base: mode === "production" ? "/record-viewer/" : "/",
  plugins: [
    vue(),
    VitePWA({
      registerType: "autoUpdate",
      includeAssets: ["favicon.ico", "favicon.png", "RecordViewer_favicon.png"],
      manifest: {
        name: "Record Viewer",
        short_name: "RecordViewer",
        description: "Record Viewer Application",
        theme_color: "#ffffff",
        icons: [
          {
            src: "favicon.png",
            sizes: "192x192",
            type: "image/png",
          },
          {
            src: "RecordViewer_favicon.png",
            sizes: "512x512",
            type: "image/png",
          },
        ],
      },
    }),
  ],
  resolve: {
    alias: {
      "@": new URL("./src", import.meta.url).pathname,
    },
  },
  server: {
    proxy: {
      "^/record-viewer/api": {
        target: "https://localhost:8443",
        changeOrigin: true,
      },
    },
  },
  devServer: {
    proxy: {
      "^/record-viewer/api": {
        target: "https://10.0.200.64:8443",
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: "dist",
  },
}));
