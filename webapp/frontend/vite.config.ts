import { VitePWA } from 'vite-plugin-pwa';
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { resolve } from 'path';

// https://vitejs.dev/config/
export default defineConfig({
    plugins: [
        VitePWA({
            registerType: 'autoUpdate',
            injectRegister: false,

            pwaAssets: {
                disabled: false,
                config: true,
            },

            manifest: {
                name: 'JTasks',
                short_name: 'JTasks',
                description: 'Time tracking application',
                theme_color: '#223775',
            },

            workbox: {
                globPatterns: ['**/*.{js,css,html,svg,png,ico}'],
                cleanupOutdatedCaches: true,
                clientsClaim: true,
                maximumFileSizeToCacheInBytes: 15 * 1024 ** 2, // 5 MB or set to something else
            },

            devOptions: {
                enabled: false,
                navigateFallback: 'index.html',
                suppressWarnings: true,
                type: 'module',
            },
        }),
        react({
            babel: {
                plugins: [
                    [
                        'babel-plugin-styled-components',
                        {
                            displayName: true,
                            fileName: false,
                        },
                    ],
                ],
            },
        }),
    ],
    server: {
        port: 5174,
        open: 'http://demo.jtasks.local:5174',
        host: '127.0.0.1', // Ensures Vite binds strictly to the loopback
        allowedHosts: true,
        proxy: {
            '/_ui': {
                target: 'http://localhost:9091/',
                changeOrigin: false,
            },
            '/api': {
                target: 'http://localhost:9091/',
                changeOrigin: false,
            },
            '/_resources': {
                target: 'http://localhost:9091/',
                changeOrigin: false,
            },
            '/internal/keycloak-redirect': {
                target: 'http://localhost:9091/',
                changeOrigin: false,
            },
            '/websocket': {
                // Proxy requests starting with /websocket
                target: 'http://localhost:9091/', // Target backend server for WebSocket
                ws: true, // Enable WebSocket proxying
                changeOrigin: true, // Change the origin header to the target URL
            },
        },
    },
    build: {
        sourcemap: true,
        target: 'esnext',
        chunkSizeWarningLimit: 10000,
        rollupOptions: {
            onwarn(warning, warn) {
                if (warning.code === 'MODULE_LEVEL_DIRECTIVE') {
                    return;
                }
                if (
                    warning.message &&
                    warning.message.indexOf(
                        'Error when using sourcemap for reporting an error'
                    ) != -1
                ) {
                    return;
                }
                warn(warning);
            },
            input: {
                app: './index.html', // default
            },
            output: {
                manualChunks(id) {
                    if (id.includes('node_modules')) {
                        return 'vendor'; // Group all node_modules into a 'vendor' chunk
                    }
                    if (
                        id.includes('/web-admin/') ||
                        id.includes('/web-core/') ||
                        id.includes('/webpeer/')
                    ) {
                        return 'platform'; // Group all node_modules into a 'vendor' chunk
                    }
                },
            },
        },
    },
    resolve: {
        alias: {
            'webpeer-core': resolve(
                import.meta.dirname,
                '../../submodules/elsa/submodules/webpeer/web/core/src/index'
            ), // Базовый алиас для папки src
            'web-admin': resolve(
                import.meta.dirname,
                '../../submodules/elsa/platform/web-admin/src/index'
            ), // Базовый алиас для папки src
            admin: resolve(
                import.meta.dirname,
                '../../submodules/elsa/platform/web-admin'
            ), // Базовый алиас для папки src
            jtasks: import.meta.dirname,
        },
        dedupe: [
            'react',
            'react-dom',
            'antd',
            '@types/react',
            '@ant-design/icons',
            'use-breakpoint',
            'styled-components',
            'lodash',
            'dayjs',
        ],
    },

    css: {
        preprocessorOptions: {
            scss: {
                silenceDeprecations: [
                    'import',
                    'mixed-decls',
                    'color-functions',
                    'global-builtin',
                ],
            },
        },
    },
});
