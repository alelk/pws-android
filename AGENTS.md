# AGENTS.md — pws-android

> Canonical operational runbook for **any** AI coding agent (Claude Code, Cursor, Aider, Copilot,
> Codex, Cody…).
> Auto-loaded by tools that follow the AGENTS.md convention. Claude Code reads `CLAUDE.md` first (a
> thin wrapper) and is pointed here.
> Most business/UI logic lives in **`../pws-core`** — this repo is Android glue.

---

## 1. Repo at a glance

- **Role:** Android host for the PWS songbook. Provides DI bootstrap, DataStore, backup, intents,
  signing, flavors.
- **Primary module:** `:app-compose` (Jetpack Compose + Voyager + Koin).
- **Legacy module:** `:app` (View-based) — **don't add features here**.
- **Cross-repo:** `../pws-core` is auto-linked as a Gradle composite build (see
  `settings.gradle.kts`). No `publishToMavenLocal` needed.
- **Toolchain:** JDK 21 · AGP 8.x · Kotlin 2.3.x · Gradle wrapper (`./gradlew`).

---

## 2. Hot paths (most common commands)

| Goal                              | Command                                           |
|-----------------------------------|---------------------------------------------------|
| Local APK (RU debug, fastest)     | `./gradlew :app-compose:assembleRuDebug`          |
| Compile a flavor of the db module | `./gradlew :data:db-android:compileRuDebugKotlin` |
| Build content-delivery module     | `./gradlew :data:content-delivery:assembleDebug`  |
| Unit tests (db module)            | `./gradlew :data:db-android:testRuDebugUnitTest`  |
| Full app build (all flavors)      | `./build.sh`                                      |
| E2E smoke (Maestro)               | `./e2e/scripts/run-local.sh --flavor ru`          |
| Compose-only convenience build    | `./build-compose.sh`                              |

**Rule of thumb:** module-scoped tasks first. Only run app-wide `assemble` to verify integration
before declaring work done.

---

## 3. Flavors

| Flavor    | Content   | Package                                       | Store       |
|-----------|-----------|-----------------------------------------------|-------------|
| `ru`      | Russian   | `io.github.alelk.pws.app.compose` *(default)* | Google Play |
| `uk`      | Ukrainian | `io.github.alelk.pws.app.compose.uk`          | Google Play |
| `full`    | Combined  | `io.github.alelk.pws.app.compose.full`        | Google Play |
| `rustore` | RuStore   | `io.github.alelk.pws.app`                     | RuStore     |

- ❌ **Do not change `applicationIdSuffix`** per flavor — users would see uninstall/reinstall
  confusion.

---

## 4. Architecture (one screen)

```text
MainActivity (Android)
  ├─ initialises Koin via PwsComposeApplication
  ├─ reads DataStore prefs (theme, font scale, …) via collectAsState
  ├─ constructs *ExternalActions impls (share, intents, file pickers)
  └─ calls AppRoot(...)             ← from pws-core :features
        ↓
     (everything below lives in pws-core)
     Screen → StateScreenModel → UseCase → Repository → Room/Remote
```

This repo only owns Android-specific glue. Anything platform-agnostic belongs in `pws-core`.

- **DI:** Koin, initialised in `PwsComposeApplication`. ScreenModels scoped to Voyager screens (in
  `pws-core`).
- **Preferences:** Jetpack DataStore (Theme, Font Scale, …) — read in `MainActivity`, passed down to
  `AppRoot`.
- **Database:** Room provider lives in `:data:db-android`, schema/DAOs/repos in `pws-core` (
  `:data:db-room`, `:data:repo-room`).
- **DB security:** SQLCipher + Android Keystore — see [
  `docs/data-security.md`](docs/data-security.md).
- **Backup/restore:** `BackupManager` + `BackupService` in `:app-compose`.

Deep dive: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md), [`docs/MODULES.md`](docs/MODULES.md).

---

## 5. Module map (canonical: `settings.gradle.kts`)

| Module                   | Purpose                                                                                           |
|--------------------------|---------------------------------------------------------------------------------------------------|
| `:app-compose`           | **Primary** — Compose host activity, DI bootstrap, DataStore, backup                              |
| `:app`                   | **Legacy View-based app — do not add features**                                                   |
| `:data:db-android`       | Android Room provider, SQLCipher integration, asset decryption, migrations                        |
| `:data:content-delivery` | Book catalog fetch (Ktor), download + verify + import of `.book.yaml.gz.enc` bundles, Koin wiring |

Composite-built dependencies from `../pws-core`:
`:domain` · `:domain:domain-test-fixtures` · `:portable-data` · `:data:db-room` (+test fixtures) ·
`:data:repo-room` · `:features` · `:core:navigation`.

---

## 6. Source layout shortcuts

```
app-compose/src/main/kotlin/io/github/alelk/pws/android/compose/
  ├ MainActivity.kt              activity + ExternalActions impls + DataStore reads
  ├ PwsComposeApplication.kt     Koin init + module wiring
  ├ ThemePreferences.kt          DataStore keys / serialisers
  ├ BackupManager.kt             backup orchestration
  └ donation/                    flavor-specific donation flow

data/db-android/src/main/kotlin/io/github/alelk/pws/database/
  ├ PwsDatabaseProvider.kt       Room.databaseBuilder() + SupportOpenHelperFactory(passphrase)
  ├ initDatabase.kt              first-launch asset extraction + sqlcipher_export
  ├ migrateDataFromPrevDatabase.kt  3.2.2 → 3.3.0 migration of user data
  ├ migrations.kt                Room schema migrations (MIGRATION_14_15, ...)
  ├ markBuiltInBooks.kt          marks built-in books as source=ASSET on first launch
  ├ PwsContentKey.kt             public wrapper around DbKeyConfig.keyHex()
  ├ openReadOnlyDatabase.kt      read-only access to legacy DB files
  ├ DbKeyConfig.kt               XOR-obfuscated BuildConfig key reconstruction
  ├ databaseCallbacks.kt         Room callbacks
  ├ security/KeyManager.kt       passphrase lifecycle (EncryptedSharedPreferences)
  ├ provider/PwsDataProvider.kt  content provider for legacy interop
  └ support/                     legacy DB schema helpers (2x/1x providers)

data/content-delivery/src/main/kotlin/io/github/alelk/pws/contentdelivery/
  ├ ContentKeyProvider.kt        fun interface — decouples encryption key from db-android
  ├ catalog/BookCatalogRepositoryImpl.kt  Ktor HTTP → parse books-catalog.json → BookCatalogEntry list
  ├ install/
  │   ├ BookImporterImpl.kt      BookBundle → Room transaction (upsert Book, Songs, Numbers, Tags)
  │   ├ BookUninstallerImpl.kt   Room transaction delete with FK order; blocks ASSET source
  │   ├ InstallBookUseCaseImpl.kt  download (Ktor+progress) → verify SHA-256 → decrypt → import
  │   └ UninstallBookUseCaseImpl.kt
  └ di/ContentDeliveryModule.kt  Koin module — accepts catalogUrl + ContentKeyProvider

e2e/                             Maestro E2E flows + reports
```

---

## 7. Canonical patterns

### External actions (Android interop)

Features in `pws-core` are platform-agnostic. Android-specific actions (share, intents, file
picker, …) are exposed via `*ExternalActions` interfaces **defined in `pws-core`** and **implemented
in `MainActivity.kt`**.

```kotlin
// in MainActivity.kt
val songDetailActions = object : SongDetailExternalActions {
    override fun shareSong(text: String) { /* Android Intent.ACTION_SEND */
    }
    override fun openUrl(url: String) { /* CustomTabsIntent / Intent.ACTION_VIEW */
    }
}

AppRoot(
    songDetailExternalActions = songDetailActions,
    settingsExternalActions = settingsActions,
    displaySettings = displaySettingsFromDataStore,
    …
)
```

If a feature needs a new Android-only capability, add the callback to the relevant `ExternalActions`
interface in `pws-core`, then implement it here.

### DataStore → AppRoot

```kotlin
val theme by themePreferences.themeFlow.collectAsState(initial = Theme.System)
val fontScale by themePreferences.fontScaleFlow.collectAsState(initial = 1f)

AppRoot(displaySettings = DisplaySettings(theme = theme, fontScale = fontScale), …)
```

### Maestro testability

```kotlin
Modifier.semantics { testTagsAsResourceId = true }
```

Apply on the shell root composable so Maestro can address Compose nodes by `testTag`.

### Edge-to-edge

`enableEdgeToEdge()` is already wired in `MainActivity`. Don't remove.

---

## 8. Hard rules

### Architecture

- ❌ **Don't add features to `:app`** (legacy View module). All new UI work goes to `:app-compose`.
- ❌ **No Android-specific calls inside `pws-core`** — expose an `ExternalActions` interface in
  `pws-core` and implement it here.
- ✅ **Domain + UI lives in `pws-core` first**; this repo only adds Android glue.
- ✅ **Use Composite build** — never edit Maven-cached `pws-core` JARs to fix Android issues; edit
  the `pws-core` source.

### Secrets / signing

- ❌ **Never commit** `local.properties`, `keystore.properties`, `*.keystore`, `*.jks`.
- ❌ **Do not change `applicationIdSuffix`** per flavor.
- ❌ **Don't disable SQLCipher integration** in `:data:db-android` — see [
  `docs/data-security.md`](docs/data-security.md).
- ❌ **Don't embed the prod DB decryption key** in source. It comes from the `DB_DECRYPT_KEY_PROD`
  GitHub Actions secret. Debug builds use the public debug key.

### Compose specifics for this host

- ✅ **`enableEdgeToEdge()`** stays in `MainActivity`.
- ✅ **`Modifier.semantics { testTagsAsResourceId = true }`** on the shell root for Maestro.
- ✅ **DataStore reads** stay in `MainActivity` (single owner) and pass down via `AppRoot`
  parameters.

---

## 9. Workflows (checklists)

### Add a feature

1. **Domain** models + use cases → `pws-core:domain`.
2. **UI state** + `ScreenModel` → `pws-core:features` (see `pws-core/AGENTS.md` § 7 for ScreenModel
   rules).
3. If an **Android-only API** is needed (share, intent, file picker, biometric…) → add the callback
   to the relevant `ExternalActions` interface in `pws-core`.
4. **Implement** that callback in `MainActivity.kt`.
5. **Test** with `./gradlew :app-compose:assembleRuDebug` and an E2E flow.

### Change DB schema

1. Update Room entities/DAOs in `pws-core` (`:data:db-room`), bump `PwsDatabase.version`.
2. Write migration `MIGRATION_N_N+1` in `:data:db-android/migrations.kt`, wire in
   `PwsDatabaseProvider` `.addMigrations(...)`.
3. Update repository implementations in `pws-core` (`:data:repo-room`).
4. If adding a TypeConverter (enum, value class, etc.) — add to `DbTypeConverters.kt` in
   `:data:db-room`.
5. Run `:data:db-android:testRuDebugUnitTest`.

### Add a flavor-specific UI override

- Use `app-compose/src/<flavor>/` source set.
- Prefer adding to `pws-core` with a flag if the override is just data, not platform code.

---

## 10. Documentation index (read on demand)

| When you need…                                          | Open                                                   |
|---------------------------------------------------------|--------------------------------------------------------|
| Doc directory overview                                  | [`docs/README.md`](docs/README.md)                     |
| Android-side architecture (host, DI, DataStore, backup) | [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)         |
| Module inventory                                        | [`docs/MODULES.md`](docs/MODULES.md)                   |
| DB encryption, asset crypto, Keystore                   | [`docs/data-security.md`](docs/data-security.md)       |
| Release / signing / CI workflow                         | [`docs/release-workflow.md`](docs/release-workflow.md) |
| **In-flight plans (read first when relevant!)**         | [`docs/ai/plans/`](docs/ai/plans/)                     |
| Cross-repo context (modules, conventions)               | [`../pws-core/AGENTS.md`](../pws-core/AGENTS.md)       |
| Claude-specific niceties (skills, etc.)                 | [`CLAUDE.md`](CLAUDE.md)                               |

---

## 11. Cross-repo navigation

Most domain/UI lives in `pws-core`. If `grep` here returns nothing, search `../pws-core/`. The
composite build means changes in either repo affect this build automatically.

---

## 12. Don't waste tokens on

These paths are noise — never grep, list, or read them unless the user explicitly references them:

- `build/`, `*/build/`, `.gradle/`, `.kotlin/`, `output/`
- `*.iml`, `.idea/`
- `gradle-wrapper.jar`, `kotlin-js-store/`
- `app-compose/src/main/res/raw/` binary assets unless explicitly relevant
- `data/db-android/src/test/resources/test-db/` test DB fixtures unless touching migrations

---

*Maintenance note: when conventions, flavors, or hard rules change, update this file. If a section
grows past one screen, split it into `docs/` and link from here.*
