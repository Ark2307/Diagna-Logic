/// <reference types="vitest/config" />
import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import path from "path";
import { defineConfig } from "vite";

// Backend URL is configurable so a local port conflict (see CLAUDE.md) doesn't require
// editing this file — set VITE_BACKEND_URL when the backend isn't on the standard 8080.
const backendUrl = process.env.VITE_BACKEND_URL || "http://localhost:8080";

export default defineConfig({
    plugins: [react(), tailwindcss()],
    resolve: {
        alias: {
            "@": path.resolve(__dirname, "./src"),
        },
    },
    server: {
        proxy: {
            "/api": {
                target: backendUrl,
                changeOrigin: true,
            },
        },
    },
    test: {
        environment: "jsdom",
        globals: true,
        setupFiles: ["./src/test/setup.ts"],
        css: true,
    },
});
