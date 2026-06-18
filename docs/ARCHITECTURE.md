# pws-android Architecture

The Android host carries **no business logic**. Its sole job is to bootstrap `pws-core` (composite-linked KMP library), provide Android-specific capabilities, and ship signed APKs/AABs per flavor.

```text
┌────────────────────────────────────────────────────────────────────┐
│  pws-android  (this repo)                                          │
│                                                                    │
│  PwsComposeApplication                                             │
│     └─ startKoin { modules(coreModules + androidModules) }         │
│                                                                    │
│  MainActivity                                                      │
│     ├─ enableEdgeToEdge()                                          │
│     ├─ DataStore  → collectAsState → DisplaySettings               │
│     ├─ *ExternalActions impls (share, intents, file pickers)       │
│     └─ setContent { AppRoot(displaySettings, *actions, …) }        │
│                                                                    │
│  :data:db-android                                                  │
│     ├─ PwsDatabaseProvider (Room + SQLCipher)                      │
│     ├─ KeyManager (Keystore-backed passphrase)                     │
│     ├─ DbKeyConfig (XOR-obfuscated BuildConfig key)                │
│     └─ initDatabase / migrateDataFromPrevDatabase                  │
└────────────────────────────────────────────────────────────────────┘
         │
         │ (composite Gradle build)
         ▼
┌────────────────────────────────────────────────────────────────────┐
│  pws-core  (../pws-core)                                           │
│  Screen → StateScreenModel → UseCase → Repository → Room/Remote    │
│  (everything platform-agnostic lives here)                         │
└────────────────────────────────────────────────────────────────────┘
```

---

## Layers in this repo

### 1. Host layer — `:app-compose`

Single owner of:
- **Activity lifecycle** (`MainActivity`)
- **DI bootstrap** (`PwsComposeApplication` → Koin)
- **Preferences** via Jetpack DataStore (`ThemePreferences`)
- **External-action implementations** (anything that needs `Context`, `Intent`, Android APIs)
- **Backup/restore** (`BackupManager`, `BackupService`) — wraps `:portable-data` from `pws-core`
- **Flavor-specific code** (donation flows, store-specific bits)

### 2. Persistence layer — `:data:db-android`

Android-specific Room provider with two-layer encryption (see [`data-security.md`](data-security.md)):

| Concern                  | Component                                                   |
|--------------------------|-------------------------------------------------------------|
| Database open + SQLCipher| `PwsDatabaseProvider`                                       |
| Passphrase (per device)  | `KeyManager` → `EncryptedSharedPreferences` (Keystore)      |
| Asset decryption key     | `DbKeyConfig` → XOR-obfuscated `BuildConfig`                |
| First-launch init        | `initDatabase()` — decrypts asset, `sqlcipher_export` → DB  |
| Legacy migration         | `migrateDataFromPrevDatabase()` (3.2.2 → 3.3.0 user data)   |
| Legacy read              | `openReadOnlyDatabase()` (plain SQLite + future SQLCipher)  |
| Schema/DAO               | (lives in `pws-core:data:db-room`)                          |

### 3. Legacy — `:app`

View-based application. **Frozen.** Bug-fix only — no new features. Lives alongside `:app-compose` until removal is scheduled.

---

## Cross-repo composite build

`settings.gradle.kts` detects `../pws-core` on disk and links it as `includeBuild` with explicit `dependencySubstitution`. Modules wired in:

```text
io.github.alelk.pws.domain:domain                    → :domain
io.github.alelk.pws.domain:domain-test-fixtures      → :domain:domain-test-fixtures
io.github.alelk.pws.portable:portable-data           → :portable-data
io.github.alelk.pws.data:db-room                     → :data:db-room
io.github.alelk.pws.data:db-room-test-fixtures       → :data:db-room:db-room-test-fixtures
io.github.alelk.pws.data:repo-room                   → :data:repo-room
io.github.alelk.pws.features:features                → :features
io.github.alelk.pws.core:core-navigation             → :core:navigation
```

If `../pws-core` is absent, the build falls back to GitHub Packages Maven resolution.

---

## External actions pattern

`pws-core` features can't see `Context` / `Intent` / `Uri`. They depend on `*ExternalActions` interfaces; this repo provides the implementations.

**Where to look:**
- Interfaces: `pws-core/features/src/commonMain/kotlin/.../*ExternalActions.kt`
- Implementations: `app-compose/.../MainActivity.kt`
- Wiring: passed into `AppRoot(...)` and forwarded to feature ScreenModels via Koin / CompositionLocal.

**Adding a new external action:**
1. Add the method to the relevant `*ExternalActions` interface in `pws-core`.
2. Consume it in the feature's `ScreenModel`.
3. Implement it here in `MainActivity.kt`.
4. Pass through `AppRoot(...)`.

---

## DisplaySettings (preferences) flow

```text
DataStore (Preferences)
   │  ThemePreferences flows
   ▼
MainActivity { val theme by themePreferences.themeFlow.collectAsState(...) }
   │
   ▼
AppRoot(displaySettings = DisplaySettings(theme, fontScale, …), …)
   │
   ▼
Feature composables / ScreenModels read via parameters or CompositionLocal
```

**Single owner rule:** all DataStore reads happen in `MainActivity`. Don't read DataStore from features in `pws-core` — they must stay platform-agnostic.

---

## Flavors

See `AGENTS.md` § 3 for the table. Flavor source sets in `app-compose/src/<flavor>/` override `main/` resources where needed. `applicationIdSuffix` is fixed per the package column — **do not change it**.

---

## Related docs

- [`MODULES.md`](MODULES.md) — module inventory and dependency graph
- [`data-security.md`](data-security.md) — encryption layers (assets + SQLCipher)
- [`release-workflow.md`](release-workflow.md) — signing, CI, GitHub Release flow
- [`../AGENTS.md`](../AGENTS.md) — full operational runbook
- [`../../pws-core/AGENTS.md`](../../pws-core/AGENTS.md) — core library guide

Last reviewed: 2026-06-17
