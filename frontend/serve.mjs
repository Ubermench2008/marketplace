import http from 'node:http';
import https from 'node:https';
import { createReadStream } from 'node:fs';
import { stat, realpath } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('../src/main/resources/static/', import.meta.url));
const backend = new URL(process.env.MARKETPLACE_BACKEND_URL || 'http://127.0.0.1:8080');
const port = Number(process.env.MARKETPLACE_FRONTEND_PORT || 5173);
if (!['http:', 'https:'].includes(backend.protocol) || backend.username || backend.password) throw new Error('Invalid backend URL');
if (!Number.isInteger(port) || port < 1 || port > 65535) throw new Error('Invalid frontend port');
const mime = { '.html': 'text/html; charset=utf-8', '.css': 'text/css; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.svg': 'image/svg+xml', '.webp': 'image/webp', '.png': 'image/png', '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg' };
const hopHeaders = new Set(['connection', 'keep-alive', 'transfer-encoding', 'te', 'trailer', 'upgrade', 'proxy-authorization', 'proxy-authenticate']);
const endToEnd = headers => Object.fromEntries(Object.entries(headers).filter(([name]) => !hopHeaders.has(name.toLowerCase())));

const server = http.createServer(async (request, response) => {
    let url;
    try { url = new URL(request.url, `http://127.0.0.1:${port}`); }
    catch { response.writeHead(400).end(); return; }
    if (url.pathname.startsWith('/api/') || url.pathname.startsWith('/media/')) {
        const transport = backend.protocol === 'https:' ? https : http;
        const upstream = transport.request(new URL(url.pathname + url.search, backend), {
            method: request.method,
            headers: { ...endToEnd(request.headers), host: backend.host },
            timeout: 30000
        }, result => {
            response.writeHead(result.statusCode, endToEnd(result.headers));
            result.pipe(response);
        });
        upstream.on('timeout', () => upstream.destroy(new Error('Backend timeout')));
        upstream.on('error', () => {
            if (!response.headersSent) response.writeHead(502, { 'Content-Type': 'application/json; charset=utf-8' });
            if (!response.writableEnded) response.end(JSON.stringify({ message: 'Бэкенд недоступен. Запустите Spring-приложение на порту 8080.' }));
        });
        request.on('aborted', () => upstream.destroy());
        request.pipe(upstream);
        return;
    }
    if (!['GET', 'HEAD'].includes(request.method)) { response.writeHead(405, { Allow: 'GET, HEAD' }).end(); return; }
    try {
        const name = decodeURIComponent(url.pathname === '/' ? '/index.html' : url.pathname);
        const filename = await realpath(path.resolve(root, '.' + name));
        const relative = path.relative(root, filename);
        if (relative.startsWith('..') || path.isAbsolute(relative) || !(await stat(filename)).isFile()) { response.writeHead(404).end(); return; }
        response.writeHead(200, {
            'Content-Type': mime[path.extname(filename)] || 'application/octet-stream',
            'Cache-Control': 'no-store',
            'X-Content-Type-Options': 'nosniff',
            'Content-Security-Policy': "default-src 'self'; img-src 'self' blob:; object-src 'none'; base-uri 'self'; frame-ancestors 'none'; form-action 'self'"
        });
        if (request.method === 'HEAD') response.end();
        else createReadStream(filename).on('error', () => response.destroy()).pipe(response);
    } catch { response.writeHead(404).end('Not found'); }
});
server.on('error', error => { console.error(`Frontend server: ${error.message}`); process.exitCode = 1; });
server.listen(port, '127.0.0.1', () => {
    console.log(`Рядом: http://127.0.0.1:${port}`);
    console.log(`API/media proxy: ${backend.origin}`);
});
process.on('SIGINT', () => server.close(() => process.exit(0)));
process.on('SIGTERM', () => server.close(() => process.exit(0)));
