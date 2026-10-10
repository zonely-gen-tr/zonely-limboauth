# Compatibility verification — 2026-10-10

Local Windows tests use isolated H2 databases and bind to `127.0.0.1`. No production MySQL database is used.

## Player protocol tests

Each PASS covers plugin startup, registration, wrong-password rejection, reconnect after the session cache expires, bcrypt login and transfer to a local protocol backend. The initial config deliberately omits most fields to exercise default/message expansion during upgrades.

| Velocity | Java | LimboAPI upstream commit | Minecraft client | Result |
| --- | --- | --- | --- | --- |
| 3.4.0 | Temurin 21.0.8 | `e638f4d` | 1.8.8, 1.21.11 | PASS |
| 3.5.1 | Temurin 25.0.4.1 | `013034d` | 1.21.11, 26.1 | PASS |
| 4.2.0 | Temurin 25.0.4.1 | `bfef579` | 1.21.11, 26.1 | PASS |

The persistent manual-test server additionally completed login and transfer to a real vanilla Minecraft 1.21.11 backend. It uses local H2 accounts. Connect with Minecraft 1.21.11 to `localhost:25565` while the local processes are running.

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

These checks do not certify every Minecraft protocol, every Velocity build, production MySQL constraints, premium/Floodgate login or TOTP flows. Upgrade with a test account against your actual backend/database configuration.
