# Web target configuration (`wasmJs`, with `js(IR)` as fallback)

## Module config

```kotlin
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

kotlin {
    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "app.js"
            }
        }
        binaries.executable()      // shells only; library modules omit this
    }
}
```

`@OptIn(ExperimentalWasmDsl::class)` is required on the `wasmJs { }` block — the *DSL* is
experimental, the target itself is production-supported by Compose Multiplatform.

Compatibility fallback (older WebViews without WasmGC):

```kotlin
js(IR) { browser() }               // + move wasmJsMain/ → jsMain/
```

## Verifying a dependency actually has a web artifact

Do this **before** writing any code against a new library — a missing wasm artifact is a
dependency swap, not a workaround. Compiling the module that uses it is the only honest
check:

```bash
./gradlew :domain:compileKotlinWasmJs :api:contract:compileKotlinWasmJs
```

Verified to publish `wasm-js` artifacts: Arrow (`arrow-core`), kotlinx-datetime,
kotlinx-serialization, kotlinx-coroutines, Ktor Client (`ktor-client-core` +
`ktor-client-js` engine), Koin (`koin-core`, `koin-compose`, `koin-compose-viewmodel`),
ktor-resources, Compose Multiplatform + Navigation 3 + lifecycle-viewmodel.

Engines and other platform artifacts are chosen per source set:

```kotlin
sourceSets {
    wasmJsMain.dependencies {
        implementation(libs.ktor.client.js)      // JS/Fetch engine — has a wasmJs artifact
        implementation(libs.kotlinx.browser)     // typed window/document/navigator interop
    }
}
```

## Browser interop from `wasmJsMain`

Two mechanisms, in this order of preference:

1. **`kotlinx-browser`** — typed `window`, `document`, `navigator`, DOM events. Use it
   whenever the API is covered.
2. **`external` declarations + `js("…")` bodies** — for APIs it doesn't cover
   (`MediaRecorder`, vendor objects):

```kotlin
private external class MediaRecorder(stream: JsAny) : JsAny {
    val mimeType: String
    var ondataavailable: (BlobEvent) -> Unit
    fun start()
    fun stop()
}

private fun hasMediaDevices(): Boolean =
    js("!!(navigator.mediaDevices && navigator.mediaDevices.getUserMedia && window.MediaRecorder)")
```

Rules for `js("…")`: the body must be a **single expression**, it cannot capture Kotlin
locals other than the function's own parameters, and the function must be top-level or
private-in-file. Values crossing the boundary are `JsAny`/`JsString`/`JsNumber` — convert
explicitly (`.toString()`, `.toInt()`), never cast blindly.

**Feature-detect before calling, and fail with a readable message.** Browser APIs that are
absent (not just failing) surface as `undefined is not an object`, which says nothing
about the real cause — most often a missing secure context (see below).

## Secure context (HTTPS) — bites during LAN testing

`navigator.mediaDevices`, clipboard APIs, service workers and notifications exist **only**
in a secure context: HTTPS, `localhost` or `127.0.0.1`. A dev server opened from a phone as
`http://<lan-ip>:8081` is *not* secure, so those objects are simply missing.

Options, cheapest first: `adb reverse tcp:8081 tcp:8081` (Android — the phone sees
`localhost`); dev-server with a self-signed cert; a real HTTPS tunnel
(`cloudflared tunnel --url http://localhost:8081`, `ngrok http 8081`) — the reliable path
on iOS Safari. Dev-server config for all three →
[`compose-web-shell`](../../compose-web-shell/SKILL.md).

## Docker gotchas for Kotlin web builds

- Use a **Debian-based** Gradle builder image — NOT Alpine. The Node.js binary downloaded
  by the Kotlin web toolchain requires glibc.
- On ARM64 (Apple Silicon), add `--platform=linux/amd64` to the builder stage.
- Use `wasmJsBrowserDistribution` (not `wasmJsBrowserProductionWebpack`) — it produces the
  full distribution: `index.html`, resources, `.wasm`, `.js`.
- In a memory-poor container (an agent sandbox, a small CI runner) the wasm optimiser
  (`wasm-opt`) is the first thing OOM-killed: run the gate without it and record that the
  bundle was not measured —
  [`kotlin-project-layout` → constrained-gate](../../kotlin-project-layout/references/constrained-gate.md).

## Test gotchas

- MockK is JVM-only — it works in neither `commonTest` nor `wasmJsTest`. Write `Fake*`
  implementations of your interfaces.
- Kotest annotation-based config (`@BeforeAll`, …) does not work off-JVM. Use the spec
  styles: `FunSpec`, `ShouldSpec`, `DescribeSpec`.
- The Kotest **multiplatform Gradle plugin** lags the Kotlin compiler: its js/native
  compiler plugin can be incompatible with the current Kotlin version, and applying it
  then breaks the whole build. When that happens, run Kotest on the JVM via JUnit 5 and
  keep the web target as a **compile-time** purity gate (`compileKotlinWasmJs`) — that is
  what the `commonMain` purity rule actually needs.
- Web tests need a browser/Node runner; don't make `wasmJsTest` a CI gate before that
  runner is configured. `compileTestKotlinWasmJs` already proves `commonTest` is portable.

## Useful commands

```bash
./gradlew :module:compileKotlinWasmJs        # purity gate — JVM-only APIs fail here
./gradlew :module:compileTestKotlinWasmJs    # commonTest is portable too
./gradlew :module:jvmTest                    # JVM tests
./gradlew :module:wasmJsBrowserDistribution  # production web distribution
```
