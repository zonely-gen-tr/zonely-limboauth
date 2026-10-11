# Compatibility verification — 2026-10-11

The automated compatibility suite uses isolated H2 databases and binds to `127.0.0.1`. A separate, explicitly authorized shared-MySQL test is documented below; connection credentials and account backups remain outside Git.

## Player protocol tests

Each PASS covers plugin startup, registration, wrong-password rejection, reconnect after the session cache expires, bcrypt login and transfer to a local protocol backend. The initial config deliberately omits most fields to exercise default/message expansion during upgrades.

| Velocity | Java | LimboAPI upstream commit | Minecraft client | Result |
| --- | --- | --- | --- | --- |
| 3.4.0 | Temurin 21.0.8 | `e638f4d` | 1.8.8, 1.21.11 | PASS |
| 3.5.1 | Temurin 25.0.4.1 | `013034d` | 1.21.11, 26.1 | PASS |
| 4.2.0 | Temurin 25.0.4.1 | `bfef579` | 1.21.11, 26.1 | PASS |

The persistent manual-test server additionally completed login and transfer to a real vanilla Minecraft 1.21.11 backend. It uses local H2 accounts. Connect with Minecraft 1.21.11 to `localhost:25565` while the local processes are running.

## Shared MySQL / Minecraft 1.20.4 verification

Velocity 3.4.0, matched LimboAPI `e638f4d`, Java 21 and a real Paper 1.20.4 build 499 backend were tested with a Minecraft 1.20.4 protocol client on loopback. The plugin binary is the published `v1.1.15-zonely.2` release, SHA-256 `5695b0a408299f531cf1e105753624c7f3374b99c33c5ba9866a11513bb7fca2`.

The shared `userslist` schema and rows were backed up privately before startup. LimboAuth added eight missing mapped columns. Tests used one generated account and deleted it afterward; existing user credentials were not changed.

- Registration persisted a bcrypt account in the shared MySQL table and transferred to Paper.
- After the session cache expired, an incorrect password was rejected before backend entry; the correct password reached Paper.
- Updating only the generated account’s bcrypt password directly in MySQL closed its active game session within the configured check interval.
- Reconnection with the new password succeeded and reached Paper.

This verifies database sharing and external password-change handling. A protocol client exercised the server flow; no rendered Minecraft screenshots were captured. It does not certify the panel’s own password-reset UI, TOTP, premium or Floodgate flows.

## Proxy binaries tested

Version labels alone do not uniquely identify Velocity snapshots/forks. These are the exact SHA-256 values of the binaries used:

```text
fb599cbda6a6d01decce5e281f71f51cae7cacffcfafca32a09601f407b0583e  velocity-3.4.0.jar
b4e3164df5377346854dc6cb9e6a78022b1946ff69e89676313f5f6f1c6f0fb3  velocity-3.5.1.jar
35a5596a5468a035d8a32c8de5ebb0dc6b8d8f0cc3ff5169d514aca762af8aa8  velocity-4.2.0.jar
```

The maximum proxy protocol IDs are 774, 776 and 777 respectively. LimboAPI 1.1.26 fails on these binaries; the latest upstream dev-build also fails on the older proxies. Matching upstream commits are recorded in `compatibility/limboapi.json`.

## Build and storage checks

- Java 21: `gradlew --no-daemon test shadowJar checkstyleMain checkstyleTest` passes.
- Unit tests verify bcrypt `$2a$`, `$2b$`, `$2y$`, `BCRYPT$`, UTF-8 passwords, incorrect passwords and malformed hashes.
- An H2 schema test reads `userslist` using the shared AuthMe column mapping without renaming fields.
- Local proxy startup exercises fresh H2 table creation/migration; restart/login tests exercise persisted player data.
- `scripts/build_limboapi.mjs` builds the pinned upstream revision, shares a Minecraft data cache and drains generator output to avoid the Windows pipe-buffer deadlock. Runtime LimboAPI source is unchanged.

These checks do not certify every Minecraft protocol, every Velocity build, all MySQL schemas/constraints, premium/Floodgate login or TOTP flows. Upgrade with a test account against your actual backend/database configuration.
