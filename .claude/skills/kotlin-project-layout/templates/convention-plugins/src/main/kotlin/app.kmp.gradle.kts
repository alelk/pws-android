import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jlleitschuh.gradle.ktlint.KtlintExtension

// Shared configuration for every Kotlin Multiplatform module: jvm + wasmJs(browser) targets,
// Kotest on the JVM, Detekt/ktlint. Add an android target here (not per module) when an
// Android shell appears.
//
// One web target: wasmJs is the recommended Compose Multiplatform web target. js(IR) is a
// fallback for WebViews without WasmGC — one web target at a time, never both.

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("io.gitlab.arturbosch.detekt")
    id("org.jlleitschuh.gradle.ktlint")
}

// The Kotest *multiplatform Gradle plugin* is deliberately NOT applied: its js/native compiler
// plugin historically lags the Kotlin compiler and breaks the build on upgrade. Kotest runs on
// the JVM via JUnit 5 (jvmTest deps); the web target only has to COMPILE commonMain and
// commonTest — that is what keeps them free of java.* (the purity gate is compilation).

kotlin {
    // Pinned so the bytecode does not depend on whichever JDK launched Gradle.
    jvmToolchain(JVM_TOOLCHAIN_VERSION)

    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            // No wasm browser test runner is configured, and Gradle 9 fails a test task that
            // discovers no tests. Disable only the runner: compileTestKotlinWasmJs stays in the
            // lifecycle graph and keeps commonTest platform-pure. Do NOT use
            // failOnNoDiscoveredTests = false instead — that hides a genuinely missing runner.
            testTask {
                enabled = false
            }
        }
    }
}

extensions.configure<KtlintExtension> {
    filter {
        // Compose resource accessors are generated; Gradle's output dir may live outside the
        // project, so matching only /build/generated/ is not enough.
        exclude { element ->
            element.file.path.contains("/generated/compose/") || element.file.path.contains("/build/generated/")
        }
    }
    // Pre-existing findings stay recorded, so the build gates NEW debt. Missing file = none.
    baseline.set(project.file("ktlint-baseline.xml"))
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("detekt.yml"))
    // Tests are included on purpose: they are where copy-paste accumulates fastest.
    source.setFrom(
        "src/commonMain/kotlin",
        "src/jvmMain/kotlin",
        "src/wasmJsMain/kotlin",
        "src/commonTest/kotlin",
        "src/jvmTest/kotlin",
        "src/wasmJsTest/kotlin",
    )
    baseline = project.file("detekt-baseline.xml")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    // docker-java (Testcontainers' client) falls back to a stale built-in API version that a
    // current Docker Engine rejects; a pinned replacement goes stale at the next engine upgrade.
    // Ask the running daemon instead (DockerApiVersion.kt); no docker on PATH → no override.
    DockerApiVersion.detected?.let { jvmArgs("-Dapi.version=$it") }
    // Unset, each forked test JVM gets its own auto-detected heap — the same "smart
    // default" that under-sizes the Gradle daemon on a constrained CI runner (see
    // gradle.properties) can just as easily over-size a test worker instead, and a
    // GitHub-hosted runner on a private repo is only 2 vCPU / ~7 GB total. Bound it so the
    // total footprint (Gradle daemon + Kotlin daemon + test workers + Testcontainers) is
    // predictable instead of discovered by an OS-level OOM kill.
    maxHeapSize = "1g"
    maxParallelForks = 1
}

// Test JVMs of different modules queue for a slot (TestSlots.kt).
limitConcurrentTestTasks()
