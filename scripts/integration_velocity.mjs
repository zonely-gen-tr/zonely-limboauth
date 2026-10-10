// Local H2 test: register, reject a wrong password, reconnect, login, transfer to a backend.
// Install the optional client under .test-runtime/client: npm install --prefix .test-runtime/client minecraft-protocol
import { createRequire } from 'node:module';
import { spawn } from 'node:child_process';
import { mkdtempSync, mkdirSync, copyFileSync, writeFileSync, rmSync, createWriteStream } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { createServer } from 'node:net';

const root = resolve(import.meta.dirname, '..');
const require = createRequire(join(root, '.test-runtime/client/package.json'));
const mc = require('minecraft-protocol');
const minecraftData = require('minecraft-data');
const [velocity, api, clientVersion = '1.21.11'] = process.argv.slice(2);
if (!velocity || !api) throw new Error('Usage: node scripts/integration_velocity.mjs <velocity> <limboapi> [minecraft]');
const assets = join(root, '.test-runtime');
const dir = mkdtempSync(join(tmpdir(), 'zonely-auth-integration-'));
const wait = ms => new Promise(resolve => setTimeout(resolve, ms));
const name = 'ZonelyIntTest';
const password = 'ZonelyTest!753';
let proxy, backend, output;
const clients = new Set();
let backendReached = false;

async function freePort() {
  const server = createServer();
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const port = server.address().port;
  await new Promise(resolve => server.close(resolve));
  return port;
}

async function authenticate(port, registration) {
  backendReached = false;
  return new Promise((resolve, reject) => {
    const client = mc.createClient({ host: '127.0.0.1', port, version: clientVersion, username: name, auth: 'offline' });
    clients.add(client);
    let finished = false, sent = false, wrongRejected = false, success = false;
    const finish = error => {
      if (finished) return;
      finished = true;
      clearTimeout(timeout);
      clearInterval(check);
      client.end();
      clients.delete(client);
      error ? reject(error) : resolve();
    };
    const timeout = setTimeout(() => finish(new Error('Authentication/transfer timeout')), 30000);
    const check = setInterval(() => {
      if (backendReached && success && (registration || wrongRejected)) finish();
    }, 100);
    client.on('login', () => {
      if (sent) return;
      sent = true;
      setTimeout(() => {
        if (!finished) client.chat(registration ? `/register ${password} ${password}` : '/login WrongPassword!567');
      }, 500);
    });
    const chat = packet => {
      const text = JSON.stringify(packet);
      if (process.env.DEBUG_AUTH) console.log(text);
      if (/Successfully (registered|logged in)/i.test(text)) success = true;
      if (/wrong password/i.test(text) && !registration && !wrongRejected) {
        if (backendReached) return finish(new Error('Wrong password reached backend'));
        wrongRejected = true;
        setTimeout(() => { if (!finished) client.chat(`/login ${password}`); }, 5500);
      }
    };
    client.on('system_chat', chat);
    client.on('chat', chat);
    client.on('error', finish);
    client.on('end', reason => { if (!finished) finish(new Error(`Client disconnected: ${reason}`)); });
  });
}

try {
  const port = await freePort();
  const backendPort = await freePort();
  backend = mc.createServer({ host: '127.0.0.1', port: backendPort, version: clientVersion, 'online-mode': false, motd: 'Local auth test backend' });
  backend.on('playerJoin', client => {
    const data = minecraftData(clientVersion);
    client.write('login', {
      dimension: 0, difficulty: 1, levelType: 'flat',
      ...(data.loginPacket ?? {}), entityId: client.id, isHardcore: false, gameMode: 0,
      maxPlayers: 20, reducedDebugInfo: false,
    });
    if (client.username === name) backendReached = true;
  });
  await new Promise((resolve, reject) => { backend.once('listening', resolve); backend.once('error', reject); });
  mkdirSync(join(dir, 'plugins', 'limboauth'), { recursive: true });
  copyFileSync(join(assets, `velocity-${velocity}.jar`), join(dir, 'velocity.jar'));
  copyFileSync(join(assets, `limboapi-${api}.jar`), join(dir, 'plugins', 'limboapi.jar'));
  copyFileSync(join(root, 'build/libs/limboauth-1.1.15-zonely.1.jar'), join(dir, 'plugins', 'limboauth.jar'));
  writeFileSync(join(dir, 'plugins/limboauth/config.yml'), 'main:\n  purge-cache-millis: 100\n');
  writeFileSync(join(dir, 'velocity.toml'), `config-version = "2.7"\nbind = "127.0.0.1:${port}"\nmotd = "Zonely integration test"\nonline-mode = false\nforce-key-authentication = false\nplayer-info-forwarding-mode = "none"\n[servers]\nlobby = "127.0.0.1:${backendPort}"\ntry = ["lobby"]\n[forced-hosts]\n"lobby.example.com" = ["lobby"]\n`);
  const log = join(assets, `integration-${velocity}-${clientVersion}.log`);
  output = createWriteStream(log);
  let logs = '';
  proxy = spawn(process.env.JAVA_BIN ?? 'java', ['-Xmx768m', '-Dterminal.jline=false', '-Dterminal.ansi=false', '-jar', 'velocity.jar'], { cwd: dir, stdio: ['pipe', 'pipe', 'pipe'], windowsHide: true });
  proxy.stdout.on('data', data => { logs += data.toString(); });
  proxy.stderr.on('data', data => { logs += data.toString(); });
  proxy.stdout.pipe(output, { end: false });
  proxy.stderr.pipe(output, { end: false });
  for (let i = 0; i < 120 && !/Done \(/.test(logs); i++) {
    if (proxy.exitCode !== null) throw new Error('Proxy exited at startup');
    await wait(500);
  }
  if (!/Done \(/.test(logs)) throw new Error('Proxy startup timeout');
  await authenticate(port, true);
  await wait(5500); // Expire the test session cache and the shared 5s IP command limiter.
  await authenticate(port, false);
  if (/ERROR|Exception|NoSuchMethodError/.test(logs)) throw new Error('Proxy log contains an error');
  console.log(`PASS Velocity ${velocity}, LimboAPI ${api}, Minecraft ${clientVersion}: register, wrong password rejected, reconnect/login, backend transfer`);
} catch (error) {
  console.error(`FAIL Velocity ${velocity}, LimboAPI ${api}, Minecraft ${clientVersion}: ${error.message}`);
  process.exitCode = 1;
} finally {
  for (const client of clients) client.end();
  if (proxy && proxy.exitCode === null) {
    proxy.stdin.write('shutdown\n');
    const forced = setTimeout(() => proxy.kill(), 5000);
    await new Promise(resolve => proxy.once('close', resolve));
    clearTimeout(forced);
  }
  if (backend) {
    for (const client of Object.values(backend.clients)) client.end();
    backend.close();
  }
  if (output) await new Promise(resolve => output.end(resolve));
  rmSync(dir, { recursive: true, force: true });
}
