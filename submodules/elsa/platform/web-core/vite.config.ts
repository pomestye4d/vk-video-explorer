import { UserConfig, ConfigEnv } from 'vite';
import { resolve } from 'path';
import dts from 'vite-plugin-dts';

// https://vitejs.dev/config/
const defineConfig = ({ mode, command }: ConfigEnv): UserConfig => {
    const definedConfig = {};

    if (command === 'build') {
        const config = {
            plugins: [
                dts({
                    exclude: ['src/main.tsx'],
                }),
            ],
            build: {
                lib: {
                    entry: resolve('src', 'websocket.ts'),
                    name: 'websocket',
                    fileName: (format) => `index.js`,
                },
                rollupOptions: {
                    // external: ['react', 'react-dom'],
                    // output: {
                    //     globals: {
                    //         react: 'React',
                    //     },
                    // },
                },
                sourcemap: true,
                // Reduce bloat from legacy polyfills.
                target: 'esnext',
                // Leave minification up to applications.
                minify: false,
            },
        };
        return config;
    } else if (command === 'serve') {
        const config = {
            publicDir: 'example',
        };
        return config;
    }

    return definedConfig;
};
export default defineConfig;