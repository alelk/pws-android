# KMP Architecture Checklist

## Module Configuration

- [ ] Every shared KMP module has `jvm()` and the chosen web target (`wasmJs { browser() }`)
- [ ] Exactly **one** web target is enabled — not `wasmJs` and `js` together
- [ ] `kotlin.multiplatform` plugin is used — not `kotlin.jvm` — for shared modules
- [ ] Targets come from a convention plugin, not per-module blocks
- [ ] Kotest runs on the JVM via JUnit 5; no Kotest MPP plugin unless it supports the exact Kotlin version
- [ ] wasm browser test task disabled (runner only), `compileTestKotlinWasmJs` still in `check`
- [ ] JVM-only modules (`server:*`) use `kotlin.jvm` plugin

## `commonMain` Purity

- [ ] No `import java.*` in any `commonMain` file
- [ ] No `import java.util.UUID` — use `kotlin.uuid.Uuid`
- [ ] No `import java.time.*` — use `kotlin.time.*`
- [ ] No `import java.io.File` — use `FilePath` value class
- [ ] No `import io.ktor.server.*` in `commonMain`
- [ ] No `import org.jetbrains.exposed.*` in `commonMain`
- [ ] `./gradlew :domain:compileKotlinWasmJs` passes

## `expect/actual`

- [ ] `expect` declarations are in `commonMain`
- [ ] All `expect` declarations have matching `actual` in both `jvmMain` and `wasmJsMain`
- [ ] No business logic in `actual` implementations — only platform glue
- [ ] Services with lifecycle/async are interfaces in `commonMain` + Koin-bound shell implementations
- [ ] `TransactionRunner` in `commonMain`; `NoopTransactionRunner` in `commonTest`

## Tests

- [ ] Domain tests pass on the JVM and *compile* for the web target
      (`./gradlew :domain:jvmTest :domain:compileTestKotlinWasmJs`)
- [ ] No MockK in `commonTest` — use `Fake*` implementations
- [ ] Kotest `FunSpec` / `ShouldSpec` style (not annotation-based)

## Web-specific

- [ ] Every third-party dependency reaching `commonMain` compiles under the web target
      (verified by compiling, not by assuming)
- [ ] Browser interop lives in `wasmJsMain` only — `commonMain` stays URL/DOM-agnostic
- [ ] Browser APIs are feature-detected with a readable failure message (secure context!)
- [ ] Docker builder uses a Debian-based image (not Alpine) for web builds
- [ ] `wasmJsBrowserDistribution` produces the complete distribution

