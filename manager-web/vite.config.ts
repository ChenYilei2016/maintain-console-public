import {defineConfig} from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
    base: '/static/console/',
    plugins: [react()],
    build: {
        outDir: '../manager/src/main/resources/static/console',
        emptyOutDir: true,
        rollupOptions: {
            input: {
                workspace: new URL('./index.html', import.meta.url).pathname,
                admin: new URL('./admin.html', import.meta.url).pathname,
            },
        },
    },
    server: {
        proxy: {
            '/manager': 'http://localhost:9999',
        },
    },
    test: {
        environment: 'node',
    },
});
