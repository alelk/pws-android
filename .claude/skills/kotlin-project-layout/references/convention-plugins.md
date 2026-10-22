# Convention plugins in an included build

## Why an included build (not `buildSrc`, not `subprojects {}`)

- `pluginManagement { includeBuild("convention-plugins") }` makes each precompiled script plugin
  resolvable by id (`id("app.kmp")`) exactly like a published plugin.
- Unlike `buildSrc`, a change to a convention does not invalidate the whole build's classpath.
- Unlike `subprojects { apply(...) }` in the root script, every module *declares* what it is;
  the root stays free of logic and configuration isolation keeps working.

## The single-classpath rule

Kotlin Gradle plugins (`kotlin.multiplatform`, `kotlin.jvm`, `plugin.serialization`,
`plugin.compose`) must all come from **one** classloader. The convention build puts them on its
own classpath; a module that *also* writes `alias(libs.plugins.kotlin.serialization)` loads the
Kotlin plugin a second time. Symptoms range from "extension already registered" to silently wrong
compiler options.

Therefore: every combination a module needs is a convention of its own —
`app.kmp.serialization` = `app.kmp` + serialization; `app.compose` = `app.kmp` + Compose + compose
compiler + serialization (Navigation 3 routes are `@Serializable`).

## Catalogue access inside the convention build

```kotlin
// convention-plugins/settings.gradle.kts
dependencyResolutionManagement {
    versionCatalogs { create("libs") { from(files("../gradle/libs.versions.toml")) } }
}
```

```kotlin
// convention-plugins/build.gradle.kts — plugin *markers* become implementation deps
dependencies {
    implementation(libs.plugins.kotlin.multiplatform.toDep())
    implementation(libs.plugins.compose.asProvider().toDep()) // leaf alias that is also a prefix
}
fun Provider<PluginDependency>.toDep() = map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" }
```

Inside a precompiled script the type-safe `libs` accessor is **not** available; apply plugins by
id and read Compose artifacts from the extension
(`extensions.getByType<ComposeExtension>().dependencies`) or the raw coordinate pinned to
`ComposeBuildConfig.composeVersion`.

## Build-wide constants

Values that are *build infrastructure*, not dependency versions (the JDK toolchain), live in
`BuildConventions.kt` in the convention build — still declared exactly once:

```kotlin
const val JVM_TOOLCHAIN_VERSION = 21
```

## What each convention owns

| Convention                 | Applies                                  | Configures                                                                  |
|----------------------------|------------------------------------------|-----------------------------------------------------------------------------|
| `app.kmp`                  | kotlin.multiplatform, detekt, ktlint     | `jvm()`, `wasmJs { browser { testTask { enabled = false } } }`, toolchain, JUnit 5, Detekt sources incl. tests, baselines, generated-code exclusion |
| `app.jvm`                  | kotlin.jvm, detekt, ktlint               | toolchain, JUnit 5, Detekt sources incl. tests, baselines                   |
| `app.kmp.serialization`    | app.kmp + plugin.serialization           | —                                                                           |
| `app.jvm.serialization`    | app.jvm + plugin.serialization           | —                                                                           |
| `app.compose`              | app.kmp + compose + plugin.compose + serialization | common Compose deps (runtime, foundation, material3, ui, resources, preview) |

## Test-task wiring every convention carries

```kotlin
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    // docker-java's built-in API version is rejected by a current Docker Engine; a pinned
    // replacement (1.40) goes stale at the next upgrade. Ask the running daemon (DockerApiVersion.kt).
    DockerApiVersion.detected?.let { jvmArgs("-Dapi.version=$it") }
    maxHeapSize = "1g"      // a forked JVM's auto-sized heap is a "smart default" too
    maxParallelForks = 1    // forks within ONE task
}
limitConcurrentTestTasks()  // test TASKS across modules (TestSlots.kt, `app.maxParallelTestTasks`)
```

`maxHeapSize` bounds only the Java heap. Native memory a test allocates (Skia images of a Compose
gallery, direct buffers) is outside it and is freed only when a GC happens to run — see
[`compose-design-system`](../../compose-design-system/references/wrappers-and-review.md#previews-and-gallery).
A worker at `-Xmx1g` was measured at 2.5 GB RSS before such a leak was closed.

The convention build itself gets `convention-plugins/gradle.properties` with
`kotlin.compiler.execution.strategy=in-process`: `kotlin-dsl` uses Gradle's embedded Kotlin, so an
out-of-process compile starts a second Kotlin daemon that idles through the whole build.

## Adding a module kind

1. New file `app.<kind>.gradle.kts` composing existing conventions (`plugins { id("app.kmp") ... }`).
2. If it wraps a new Gradle plugin: add the plugin to `[plugins]` in the catalogue and its marker
   to `convention-plugins/build.gradle.kts`.
3. Never copy another module's configuration block.
