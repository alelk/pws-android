---
name: kmp-architecture
description: Use when configuring Kotlin Multiplatform modules — choosing targets (jvm, wasmJs, js, android), keeping commonMain free of java.* (the purity gate is compileKotlinWasmJs inside ./gradlew build, not a browser test run), disabling the unavailable wasm browser test task under Gradle 9, Kotest on the JVM via JUnit 5 instead of the lagging Kotest MPP plugin, interfaces + Koin over expect/actual for services, wasmJs/js browser config, browser interop (kotlinx-browser, external + js()), secure-context gotchas, and Docker gotchas for Kotlin web builds. Apply after kotlin-project-layout for any KMP-shaped project.
metadata:
  version: "2.1.0"
  requires: "kotlin-project-layout"
  project-types: "kmp-library, full-stack-kmp-app"
---

# KMP Architecture

## When to use

- Starting a new KMP module or converting a JVM-only module to KMP.
- Hitting a JVM-only API leak in `commonMain` (the JS build fails to compile).
- Deciding whether a module should be `kotlin.multiplatform` or `kotlin.jvm`.

## Plugin & target decision

| Module             | Plugin          | Targets              | Why                                |
|--------------------|-----------------|----------------------|------------------------------------|
| `domain`           | `multiplatform` | jvm, wasmJs (+android¹) | Shared business logic           |
| `api:contract`     | `multiplatform` | jvm, wasmJs          | DTOs used on both platforms        |
| `api:mapping`      | `multiplatform` | jvm, wasmJs          | Mapping needed on server and in UI |
| `api:client`       | `multiplatform` | jvm, wasmJs          | HTTP client used on both platforms |
| `api:client:di`    | `multiplatform` | jvm, wasmJs          | DI wiring per platform             |
| `features`         | `multiplatform` | jvm, wasmJs (+android¹) | Compose UI shared across shells |
| `server:infra`     | `jvm`           | jvm                  | Exposed, Flyway — JVM only         |
| `server:transport` | `jvm`           | jvm                  | Ktor Server — JVM only             |
| `server:di`        | `jvm`           | jvm                  | Server DI                          |
| `server:app`       | `jvm`           | jvm                  | Application entrypoint             |
| `webapp`           | `multiplatform` | wasmJs               | Web shell (Compose canvas)         |
| `tgminiapp`        | `multiplatform` | wasmJs *or* js       | Telegram shell — see web target below |

¹ **Add `android` only together with an Android shell** (`androidApp`) — a target nobody runs
is build time and a third set of dependency artifacts to keep resolvable. Until then the
convention plugin declares `jvm()` + `wasmJs()` and says so in a comment. An offline store
(`data:db-room`, `data:repo-room` — Room, `android` + `jvm`) is likewise added only when an
offline-capable shell exists and its ADR is accepted, not scaffolded ahead.

### Which web target: `wasmJs` or `js(IR)`

**One web target, not both.** `wasmJs { browser() }` is the default: it is the recommended
Compose Multiplatform web target and every mainstream Kotlin library ships a wasm-js
artifact (Arrow, kotlinx-*, Ktor Client, Koin — verified end to end, see
[`references/web-target.md`](references/web-target.md)). `js(IR) { browser() }` is a
**compatibility fallback**, not a second production target: pick it only for a shell that
must run in a WebView without WasmGC (Chrome < 119 / Safari < 18.2 — relevant for older
Telegram/Android WebViews). Shipping both doubles build time and debugging surface.

Switching between them is cheap **if** platform glue stays thin: the interop APIs
(`window`, `document`, `MediaRecorder`, …) are the same, only the source set moves
(`wasmJsMain` ↔ `jsMain`). Keep that glue small enough that the swap is a file move.

## Module build file

Targets are configured **once**, in the `app.kmp` / `app.compose` convention plugins
([`kotlin-project-layout`](../kotlin-project-layout/SKILL.md)); a module only lists dependencies.
Templates: [`templates/shared/build.gradle.kts.kmp-module`](templates/shared/build.gradle.kts.kmp-module),
[`templates/shared/build.gradle.kts.kmp-compose-module`](templates/shared/build.gradle.kts.kmp-compose-module).

### The wasm test task under Gradle 9

With no wasm browser test runner configured, Gradle 9 fails `wasmJsBrowserTest` when
`commonTest` exists but no tests are discovered. Disable **only the runner** in the convention:

```kotlin
wasmJs { browser { testTask { enabled = false } } }
```

`compileTestKotlinWasmJs` stays in the lifecycle and keeps `commonTest` portable. Don't use
`failOnNoDiscoveredTests = false` — it hides a genuinely missing runner.

## Source set layout

```
src/
├── commonMain/kotlin/      # shared — must compile on all targets
├── commonTest/kotlin/      # shared tests — run on all targets
├── jvmMain/kotlin/         # JVM-specific `actual` implementations
├── jvmTest/kotlin/         # JVM-specific tests (MockK, Testcontainers)
├── wasmJsMain/kotlin/      # web-specific `actual` implementations + browser interop
└── wasmJsTest/kotlin/      # web-specific tests (rare)
```

(`jsMain`/`jsTest` instead of `wasmJsMain`/`wasmJsTest` when the module targets `js(IR)`.)

## Hard rules

- ❌ Never add `org.jetbrains.exposed:*` to any `commonMain` dependency.
- ❌ Never add `io.ktor:ktor-server-*` to any `commonMain` dependency.
- ❌ Never use `java.*` imports in `commonMain`. Substitution table →
  [kotlin-domain-modeling / value-types.md](../kotlin-domain-modeling/references/value-types.md).
- ❌ Never use MockK in `commonTest`/`wasmJsTest` — write `Fake*` implementations.
- ❌ Never enable both `wasmJs` and `js` "just in case" — pick one web target per shell.
- ✅ Run `./gradlew :<module>:compileKotlinWasmJs` to catch JVM-only API leaks
  immediately — it is the cheapest purity gate and needs no test runner.
- ✅ Platform services with lifecycle or async (storage, history, microphone, clipboard) are a
  **`commonMain` interface + an implementation bound in the shell's Koin module** — fakeable in
  tests. `expect/actual` only for tiny, stateless glue (an engine factory, a constant).
- ✅ Run Kotest on the JVM via JUnit 5 (`kotest-runner-junit5` in `jvmTest`). Don't apply the
  Kotest multiplatform Gradle plugin unless its compiler plugin supports your exact Kotlin
  version — it historically lags and breaks the build on upgrade. The web target is a
  **compile-time** purity gate.
- ✅ The purity gate runs inside `./gradlew build` (every KMP module's `compileKotlinWasmJs` is in
  `check`), so nobody has to remember a separate command.

## Apply this skill

Step-by-step (including JVM-to-KMP migration recipe) → [`USAGE.md`](USAGE.md).

## References (load on demand)

- [`references/expect-actual.md`](references/expect-actual.md) — pattern + HTTP client example, when (not) to use.
- [`references/web-target.md`](references/web-target.md) — `wasmJs`/`js` browser config, browser interop, verifying a dependency has a web artifact, Docker gotchas, test caveats.

## Verify

```bash
./gradlew :<module>:compileKotlinWasmJs    # catches JVM-only API leaks
grep -rn "import java\." src/commonMain/   # should print nothing
```

Then walk through [`checklists/kmp-checklist.md`](checklists/kmp-checklist.md).
