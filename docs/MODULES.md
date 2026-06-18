# pws-android Modules

Current modules from `settings.gradle.kts` and their responsibilities.

## Local modules

| Module             | Purpose                                                                    |
|--------------------|----------------------------------------------------------------------------|
| `:app-compose`     | **Primary host.** Compose Activity, DI bootstrap, DataStore, backup, donations |
| `:app`             | **Legacy** View-based app. Frozen — bug-fix only, no new features          |
| `:data:db-android` | Android Room provider, SQLCipher integration, asset decryption, migrations |

## Composite-build dependencies (from `../pws-core`)

| Maven coordinate                                | Mapped project                          |
|-------------------------------------------------|-----------------------------------------|
| `io.github.alelk.pws.domain:domain`             | `:domain`                               |
| `io.github.alelk.pws.domain:domain-test-fixtures` | `:domain:domain-test-fixtures`        |
| `io.github.alelk.pws.portable:portable-data`    | `:portable-data`                        |
| `io.github.alelk.pws.data:db-room`              | `:data:db-room`                         |
| `io.github.alelk.pws.data:db-room-test-fixtures`| `:data:db-room:db-room-test-fixtures`   |
| `io.github.alelk.pws.data:repo-room`            | `:data:repo-room`                       |
| `io.github.alelk.pws.features:features`         | `:features`                             |
| `io.github.alelk.pws.core:core-navigation`      | `:core:navigation`                      |

Substitution happens automatically when `../pws-core` exists. Otherwise the Maven coordinates resolve via GitHub Packages.

## Dependency direction

```text
:app-compose ──► :data:db-android
            │     └► pws-core :data:repo-room ─► pws-core :data:db-room ─► pws-core :domain
            │
            ├► pws-core :features ─► pws-core :domain, :core:navigation, :core:ui
            ├► pws-core :portable-data ─► pws-core :domain
            └► pws-core :api:client (DI-selected, currently unused on Android)

:app  (legacy, isolated)
```

Direction rule: `:app-compose` and `:data:db-android` depend on `pws-core` modules. **Never the other way around.** `pws-core` must not import Android types.

## Key code locations

### `:app-compose`
```
app-compose/src/main/kotlin/io/github/alelk/pws/android/compose/
  MainActivity.kt                  activity + ExternalActions impls + DataStore reads
  PwsComposeApplication.kt         Koin init + module wiring
  ThemePreferences.kt              DataStore keys / serialisers
  BackupManager.kt                 backup orchestration
  donation/                        flavor-specific donation flow
app-compose/src/<flavor>/          flavor-specific overrides (ru / uk / full / rustore)
```

### `:data:db-android`
```
data/db-android/src/main/kotlin/io/github/alelk/pws/database/
  PwsDatabaseProvider.kt           Room.databaseBuilder() + SupportOpenHelperFactory(passphrase)
  initDatabase.kt                  first-launch asset extraction + sqlcipher_export
  migrateDataFromPrevDatabase.kt   3.2.2 → 3.3.0 migration of user data
  openReadOnlyDatabase.kt          read-only access to legacy DB files
  DbKeyConfig.kt                   XOR-obfuscated BuildConfig key reconstruction
  databaseCallbacks.kt             Room callbacks
  security/KeyManager.kt           passphrase lifecycle (EncryptedSharedPreferences)
  provider/PwsDataProvider.kt      content provider for legacy interop
  support/                         legacy DB schema helpers (1x/2x providers)
```

### `:app` (legacy)
Don't open unless explicitly asked. Frozen.

### E2E
```
e2e/
  flows/        Maestro flow YAMLs
  scripts/      run-local.sh launcher
  config/       test config
  reports/      generated reports (ignore)
```

## Module-task quick reference

| Task                                | Command                                              |
|-------------------------------------|------------------------------------------------------|
| Compile `:app-compose` (RU debug)   | `./gradlew :app-compose:assembleRuDebug`             |
| Compile `:data:db-android`          | `./gradlew :data:db-android:compileRuDebugKotlin`    |
| Test `:data:db-android`             | `./gradlew :data:db-android:testRuDebugUnitTest`     |
| Full app build (all flavors)        | `./build.sh`                                         |
| E2E smoke (Maestro, RU flavor)      | `./e2e/scripts/run-local.sh --flavor ru`             |

Last reviewed: 2026-06-17
