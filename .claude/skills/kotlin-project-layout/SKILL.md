---
name: kotlin-project-layout
description: Use when bootstrapping or auditing the Gradle layout of a Kotlin project (JVM, Android, or KMP) — app.version as the single product-version source, gradle/libs.versions.toml version catalogue, settings.gradle.kts with TYPESAFE_PROJECT_ACCESSORS, precompiled convention plugins in an included build (kmp / jvm / compose / serialization), pinned JDK toolchain, gradle.properties tuning (Gradle and Kotlin daemon memory, G1 for the Kotlin daemon), bounded test JVMs and build-wide test slots, Testcontainers' Docker API version detected from the running daemon, Detekt + ktlint with per-module baselines (tests included), and `./gradlew build` as the one mandatory gate — with an agreed, named constrained form for memory-poor agent sandboxes. The foundation skill — apply before any other skill in this catalogue.
metadata:
  version: "2.2.0"
  requires: ""
  project-types: "any-kotlin-project"
---

# Kotlin Project Layout

## When to use

- Bootstrapping a new Kotlin/Gradle project from scratch.
- Migrating a project that hardcodes versions, copies build logic between modules, or lacks a
  version catalogue.
- Auditing a project before adding any other skill from this catalogue.

## What it sets up

- **Product version**: `app.version` at the root, read once in the root build. The release
  pipeline rewrites this file and nothing else.
- **Dependency catalogue**: `gradle/libs.versions.toml`. Every library *and plugin* version lives
  here — never inline, not even inside convention plugins.
- **Convention plugins** in an included build (`convention-plugins/`, resolved through
  `pluginManagement { includeBuild(...) }`): one precompiled script plugin per module *kind*
  (`<prefix>.kmp`, `<prefix>.jvm`, `<prefix>.compose`, `<prefix>.kmp.serialization`,
  `<prefix>.jvm.serialization`). A module's `plugins {}` block is one line.
- **Pinned JDK toolchain** (`jvmToolchain(N)`) declared once in the convention build — bytecode
  never depends on whichever JDK launched Gradle.
- **Static analysis inside the gate**: Detekt + ktlint applied by the convention plugins, over
  main *and* test sources, with a per-module baseline so the gate blocks *new* debt.
- **One gate**: `./gradlew build` = compile (all targets) + tests + Detekt + ktlint + fitness
  tests. CI runs exactly this; nobody hand-picks a subset. Where memory cannot run it whole (an
  agent sandbox), a **named** constrained form is agreed per environment →
  [`references/constrained-gate.md`](references/constrained-gate.md).
- **Bounded memory**: daemon sizes in `gradle.properties`, `maxHeapSize`/`maxParallelForks` per
  test task, `TestSlots` across modules, Testcontainers told the Docker daemon's own API version.

## Project shape

```
<root>/
├── app.version                        # product version — the only one
├── settings.gradle.kts                # includeBuild("convention-plugins"), modules, typesafe accessors
├── build.gradle.kts                   # allprojects { version; group } — nothing else, no plugins
├── gradle.properties                  # daemon heap, parallel, caching
├── detekt.yml                         # one profile for the whole repo
├── gradle/libs.versions.toml          # every version
├── gradle/wrapper/                    # pinned wrapper (committed)
├── kotlin-js-store/yarn.lock          # committed when a JS/Wasm target exists (reproducible npm)
├── convention-plugins/
│   ├── settings.gradle.kts            # imports ../gradle/libs.versions.toml as `libs`
│   ├── build.gradle.kts               # `kotlin-dsl` + plugin markers from the catalogue
│   ├── gradle.properties              # in-process compile: no second Kotlin daemon
│   └── src/main/kotlin/
│       ├── BuildConventions.kt        # JVM_TOOLCHAIN_VERSION and other build-wide constants
│       ├── TestSlots.kt               # how many test tasks run at once, build-wide
│       ├── DockerApiVersion.kt        # the running Docker daemon's API version, for Testcontainers
│       ├── app.kmp.gradle.kts         # jvm + wasmJs, Kotest (JUnit 5), Detekt, ktlint
│       ├── app.jvm.gradle.kts         # Kotlin/JVM, same analysis wiring
│       ├── app.compose.gradle.kts     # app.kmp + Compose + compose compiler + serialization
│       ├── app.kmp.serialization.gradle.kts
│       └── app.jvm.serialization.gradle.kts
└── <module>/build.gradle.kts          # plugins { id("app.kmp") } + dependencies only
```

Rename the `app.` prefix to the project's (`acme.`, `myapp.`) — the file name *is* the plugin id.

## Hard rules

- ❌ Never hardcode the product version — read `app.version`.
- ❌ Never inline a dependency or plugin version — `libs.versions.toml` only.
- ❌ Never apply a Kotlin-family plugin with `alias(libs.plugins.kotlin.*)` in a module once
  convention plugins exist. Kotlin, serialization and Compose-compiler plugins must load from **one
  classpath**; mixing `alias(...)` and the convention build loads the Kotlin Gradle plugin twice
  and fails in confusing ways. Add a `<prefix>.<kind>` convention instead.
- ❌ Never put build logic in the root `build.gradle.kts` (no `subprojects {}` / `allprojects {}`
  plugin application) — it defeats configuration isolation and hides what a module applies.
- ❌ Never exclude test sources from Detekt — tests are a third of the code and accumulate
  copy-paste fastest. Relax rules for test *support* files (`fixtures/`, `fakes/`) in `detekt.yml`
  instead.
- ❌ Never suppress a finding to make the build green when a baseline or config change is the
  honest fix; never regenerate a baseline to hide *new* findings.
- ✅ `projects.module` (type-safe accessor), never the string `":module"`.
- ✅ Wrapper pinned to an exact Gradle version and committed.
- ✅ Every non-obvious line in a convention plugin carries a one-line *why* comment — these files
  are read by every agent that touches the build.
- ❌ Never hard-code Testcontainers' Docker API version (`-Dapi.version=1.40`) — it goes stale at the
  next engine upgrade (it did, twice, on one project). `DockerApiVersion.detected` asks the daemon.
- ❌ Never shrink the gate silently. A constrained form excludes only optimisation/packaging tasks,
  splits heavy test tasks into a second run instead of dropping them, is written in the step README,
  and what it skipped is reported as "not run" → [`references/constrained-gate.md`](references/constrained-gate.md).

## Apply this skill

Step-by-step procedure → [`USAGE.md`](USAGE.md).

## Templates (copy and adapt)

- [`templates/shared/settings.gradle.kts`](templates/shared/settings.gradle.kts)
- [`templates/shared/build.gradle.kts.root`](templates/shared/build.gradle.kts.root)
- [`templates/shared/gradle.properties`](templates/shared/gradle.properties)
- [`templates/shared/libs.versions.toml`](templates/shared/libs.versions.toml) — full catalogue; delete what you don't use
- [`templates/shared/detekt.yml`](templates/shared/detekt.yml)
- [`templates/convention-plugins/`](templates/convention-plugins/) — the whole included build

## References (load on demand)

- [`references/convention-plugins.md`](references/convention-plugins.md) — why an included build, the single-classpath rule, catalogue access, toolchain, adding a new module kind.
- [`references/module-naming.md`](references/module-naming.md) — module name patterns.
- [`references/static-analysis.md`](references/static-analysis.md) — Detekt + ktlint wiring, baselines, generated-code exclusion, commands.
- [`references/constrained-gate.md`](references/constrained-gate.md) — the gate in a memory-poor sandbox: what may be excluded, splitting, knobs, what it cannot tell.

## Verify

Walk through [`checklists/layout-checklist.md`](checklists/layout-checklist.md).
