// Offline, localhost-only smoke test for a pre-downloaded Velocity server and LimboAPI.
// Run: node scripts/smoke_velocity.mjs 3.4.0
import { createServer, connect } from 'node:net';
import { spawn } from 'node:child_process';
import { createWriteStream, mkdtempSync, mkdirSync, copyFileSync, writeFileSync, readFileSync, rmSync, existsSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';

const version = process.argv[2];
const apiVersion = process.argv[3] ?? '1.1.26';
if (!['3.4.0', '3.5.1', '4.2.0'].includes(version)) {
  throw new Error('Usage: node scripts/smoke_velocity.mjs <3.4.0|3.5.1|4.2.0>');
}

const root = resolve(import.meta.dirname, '..');
const assets = join(root, '.test-runtime');
const files = [
  join(assets, `velocity-${version}.jar`),
  join(assets, `limboapi-${apiVersion}.jar`),
  join(root, 'build', 'libs', 'limboauth-1.1.15-zonely.1.jar'),
];
files.forEach(file => { if (!existsSync(file)) throw new Error(`Missing: ${file}`); });

const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
function varint(number) {
  const bytes = [];
  do {
    let value = number & 127;
    number >>>= 7;
    if (number) value |= 128;
    bytes.push(value);
  } while (number);
  return Buffer.from(bytes);
}
function packet(data) { return Buffer.concat([varint(data.length), data]); }
function mcString(str) { const value = Buffer.from(str); return Buffer.concat([varint(value.length), value]); }

async function getPort() {
  const server = createServer();
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const port = server.address().port;
  await new Promise(resolve => server.close(resolve));
  return port;
}

async function attemptStatus(port) {
  return new Promise(resolve => {
    const socket = connect(port, '127.0.0.1');
    let finished = false;
    const finish = ok => { if (!finished) { finished = true; socket.destroy(); resolve(ok); } };
    socket.setTimeout(2500, () => finish(false));
    socket.on('error', () => finish(false));
    socket.on('connect', () => {
      const portBytes = Buffer.alloc(2);
      portBytes.writeUInt16BE(port);
      const handshake = Buffer.concat([Buffer.from([0]), varint(340), mcString('localhost'), portBytes, varint(1)]);
      socket.write(Buffer.concat([packet(handshake), packet(Buffer.from([0]))]));
    });
    socket.on('data', data => finish(data.length > 2));
    socket.on('end', () => finish(false));
  });
}

const dir = mkdtempSync(join(tmpdir(), `zonely-velocity-${version}-`));
let proc;
try {
  mkdirSync(join(dir, 'plugins'));
  copyFileSync(files[0], join(dir, 'velocity.jar'));
  copyFileSync(files[1], join(dir, 'plugins', 'limboapi.jar'));
  copyFileSync(files[2], join(dir, 'plugins', 'limboauth.jar'));
  const port = await getPort();
  writeFileSync(join(dir, 'velocity.toml'), `config-version = "2.7"\nbind = "127.0.0.1:${port}"\nmotd = "Zonely local test"\nonline-mode = false\nforce-key-authentication = false\nplayer-info-forwarding-mode = "none"\n[servers]\nlobby = "127.0.0.1:25569"\ntry = ["lobby"]\n[forced-hosts]\n"lobby.example.com" = ["lobby"]\n`);
  const log = join(assets, `smoke-${version}.log`);
  const output = createWriteStream(log);
  proc = spawn(process.env.JAVA_BIN ?? 'java', ['-Xmx768m', '-Dterminal.jline=false', '-Dterminal.ansi=false', '-jar', 'velocity.jar'], { cwd: dir, stdio: ['pipe', 'pipe', 'pipe'], windowsHide: true });
  proc.stdout.pipe(output);
  proc.stderr.pipe(output);
  let status = false;
  for (let tick = 0; tick < 70; ++tick) {
    if (proc.exitCode !== null) break;
    if (await attemptStatus(port)) { status = true; break; }
    await delay(400);
  }
  await delay(1500);
  proc.kill();
  await new Promise(resolve => { if (proc.exitCode !== null) resolve(); else proc.once('close', resolve); });
  await new Promise(resolve => output.end(resolve));
  const logs = readFileSync(log, 'utf8');
  const errors = logs.split(/\r?\n/).filter(line => /ERROR|Exception|NoSuchMethodError/.test(line));
  const loaded = /Loaded plugin limboapi\s/i.test(logs) && /Loaded plugin limboauth\s/i.test(logs);
  const passed = status && loaded && errors.length === 0;
  console.log(`Velocity ${version}, LimboAPI ${apiVersion}: ${passed ? 'PASS' : 'FAIL'} | status=${status}, plugins=${loaded}, errorLines=${errors.length}`);
  if (!passed) console.log(logs.split(/\r?\n/).filter(line => line.trim() && !/^\s*>\s*(>\s*){5,}$/.test(line)).slice(-45).map(line => line.slice(0, 350)).join('\n'));
  console.log(`Log: ${log}`);
  process.exitCode = passed ? 0 : 1;
} finally {
  if (proc && proc.exitCode === null) proc.kill();
  rmSync(dir, { recursive: true, force: true });
}
