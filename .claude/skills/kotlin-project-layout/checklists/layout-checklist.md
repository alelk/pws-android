# Project Layout Checklist

## Structure

- [ ] `app.version` exists and holds a valid semver string; the root build reads it
- [ ] `settings.gradle.kts`: `pluginManagement { includeBuild("convention-plugins") }` and
      `enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")`
- [ ] Root `build.gradle.kts` applies no plugins and holds no `subprojects {}` logic
- [ ] Every module's `plugins {}` block is a single convention id (`id("app.kmp")` …)
- [ ] No `alias(libs.plugins.kotlin.*)` / compose-compiler alias inside a module
- [ ] No inline versions anywhere — modules *and* convention build read `libs.versions.toml`
- [ ] JDK toolchain declared once (`BuildConventions.kt`) and applied by every convention
- [ ] `gradle-wrapper.properties` pins an exact Gradle version; wrapper files committed
- [ ] `kotlin-js-store/yarn.lock` committed when a JS/Wasm target exists

## Build & analysis

- [ ] `./gradlew build` succeeds on a clean checkout and is the only gate CI runs
- [ ] A constrained gate (if any) is written per environment, excludes only optimisation/packaging
      tasks, runs split-off test tasks separately, and its exclusions are reported as "not run"
- [ ] Detekt and ktlint run inside `build`, over main **and** test source sets
- [ ] Each module may carry `detekt-baseline.xml` / `ktlint-baseline.xml`; baselines only shrink
- [ ] Generated sources (Compose resources) are excluded from ktlint
- [ ] Inter-module dependencies use `projects.x`, not `":x"`

## Hygiene

- [ ] `gradle.properties`: `org.gradle.parallel=true`, `org.gradle.caching=true`,
      `kotlin.daemon.jvmargs` (not the non-existent `kotlin.daemon.jvm.options`)
- [ ] `gradle.properties`: `org.gradle.jvmargs` sets both `-Xmx` **and** `-XX:MaxMetaspaceSize`
      explicitly for the Gradle daemon itself — Gradle's auto-detected default has under-sized
      Metaspace on a GitHub Actions runner (512m heap / 384m Metaspace observed) badly enough to
      kill the daemon mid-build on a multi-module project; the job then fails with a bare
      `Error: The operation was canceled.`, not a Gradle error
- [ ] `kotlin.daemon.jvmargs` names G1 with `-XX:G1PeriodicGCInterval` — the default ParallelGC
      never returns committed heap, so the daemon holds its full `-Xmx` through the test phase
- [ ] `convention-plugins/gradle.properties` has `kotlin.compiler.execution.strategy=in-process`
      (no second Kotlin daemon for the embedded-Kotlin `kotlin-dsl` build; the log line to look
      for is `Detected multiple Kotlin daemon sessions`)
- [ ] Test tasks pass `-Dapi.version` from `DockerApiVersion.detected`, not a pinned constant
- [ ] Test JVMs are bounded per task (`maxHeapSize`, `maxParallelForks`) **and** across tasks
      (`limitConcurrentTestTasks()`); exit 137 of `Gradle Test Executor` = OOM killer, look at
      the total and at native memory, not at `-Xmx`
- [ ] `.gitignore` excludes `.gradle/`, `build/`, `.kotlin/`, `node_modules/`, local config
- [ ] No secrets or credentials in any Gradle file
