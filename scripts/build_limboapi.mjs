// Builds pinned upstream LimboAPI; only build tooling changes, no runtime source patches.
import { spawn } from 'node:child_process';
import { existsSync, mkdirSync, readFileSync, writeFileSync, copyFileSync } from 'node:fs';
import { resolve, join } from 'node:path';

const root = resolve(import.meta.dirname, '..');
const matrix = JSON.parse(readFileSync(join(root, 'compatibility/limboapi.json'), 'utf8'));
const velocity = process.argv[2];
const entry = matrix[velocity];
if (!entry) throw new Error('Usage: node scripts/build_limboapi.mjs <3.4.0|3.5.1|4.2.0>');
const assets = join(root, '.test-runtime');
const source = process.env.LIMBOAPI_SOURCE ?? join(assets, `source-${velocity}`);
const cache = process.env.LIMBOAPI_DATA_CACHE ?? join(assets, 'minecraft-cache');
const dist = join(assets, 'dist');
mkdirSync(dist, { recursive: true });

function run(command, args, cwd = root) {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, { cwd, stdio: 'inherit', windowsHide: true });
    child.on('error', reject);
    child.on('close', code => code === 0 ? resolve() : reject(new Error(`${command} exited ${code}`)));
  });
}

if (!existsSync(source)) {
  await run('git', ['clone', '--no-checkout', 'https://github.com/Elytrium/LimboAPI.git', source]);
  await run('git', ['checkout', '--detach', entry.commit], source);
}
// Verify the existing checkout without discarding local build caches or changes.
await run('git', ['diff', '--exit-code', entry.commit, 'HEAD'], source);
await run('git', ['diff', '--exit-code', '--', 'plugin/src', 'api/src'], source);
const buildFile = join(source, 'plugin/build.gradle');
let build = readFileSync(buildFile, 'utf8');
build = build.replace(
  /project\.ext\.dataDirectory = new File\([^\n]+\)/,
  `project.ext.dataDirectory = new File("${resolve(cache).replaceAll('\\', '/').replaceAll('"', '\\"')}")`,
);
// Upstream's waitFor() can deadlock on Windows when the data generator fills stdout.
build = build.replace('commandLine.execute([], parent).waitFor()',
  'Process generator = commandLine.execute([], parent)\n  generator.waitForProcessOutput(System.out, System.err)\n  if (generator.exitValue() != 0) { throw new GradleException("Minecraft data generator failed") }');
writeFileSync(buildFile, build);
console.log(`Building LimboAPI ${entry.commit} for Velocity ${velocity}; use Java ${entry.java}.`);
if (process.platform === 'win32') {
  await run('cmd.exe', ['/d', '/c', 'gradlew.bat --no-daemon :plugin:shadowJar --console=plain'], source);
} else {
  await run('bash', ['./gradlew', '--no-daemon', ':plugin:shadowJar', '--console=plain'], source);
}
copyFileSync(join(source, 'plugin/build/libs/limboapi-1.1.27-SNAPSHOT.jar'), join(dist, entry.artifact));
await run('git', ['archive', '--format=zip', `--output=${join(dist, `limboapi-source-${entry.commit.slice(0, 7)}.zip`)}`, entry.commit], source);
console.log(`Packaged ${join(dist, entry.artifact)} (AGPL-3.0; source archive included).`);
