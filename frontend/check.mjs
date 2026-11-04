import { readdir, readFile, access } from 'node:fs/promises';
import { spawnSync } from 'node:child_process';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('../src/main/resources/static/', import.meta.url));
const files = (await readdir(path.join(root, 'assets'))).filter(name => name.endsWith('.js'));
for (const name of files) {
    const source = await readFile(path.join(root, 'assets', name), 'utf8');
    const check = spawnSync(process.execPath, ['--input-type=module', '--check'], { input: source, encoding: 'utf8' });
    if (check.status !== 0) throw new Error(`${name}: ${check.stderr}`);
    for (const [, imported] of source.matchAll(/from\s+['"](\.\/[^'"]+)['"]/g)) await access(path.join(root, 'assets', imported));
}
for (const file of ['index.html', 'favicon.svg', 'assets/styles.css']) await access(path.join(root, file));
for (const file of ['serve.mjs', 'check.mjs']) {
    const check = spawnSync(process.execPath, ['--check', fileURLToPath(new URL(file, import.meta.url))], { encoding: 'utf8' });
    if (check.status !== 0) throw new Error(check.stderr);
}
console.log(`OK: ${files.length} browser modules, local imports, HTML/CSS/assets and development tools.`);
