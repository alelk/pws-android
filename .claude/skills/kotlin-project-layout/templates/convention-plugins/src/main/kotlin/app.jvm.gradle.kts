import org.gradle.api.tasks.testing.Test
import org.jlleitschuh.gradle.ktlint.KtlintExtension

// Shared configuration for JVM-only modules (server:*): Kotlin/JVM plus the
// same Detekt/ktlint wiring as the KMP convention plugin.

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("io.gitlab.arturbosch.detekt")
    id("org.jlleitschuh.gradle.ktlint")
}

extensions.configure<KtlintExtension> {
    // Same reasoning as detekt's `baseline` below: pre-existing findings a module had before
    // its own baseline was cut stay recorded, not silently re-passed. A module with no
    // `ktlint-baseline.xml` simply has none recorded.
    baseline.set(project.file("ktlint-baseline.xml"))
}

// Pinned so the build does not depend on whichever JDK happens to launch Gradle:
// Gradle provisions/selects this toolchain and every module compiles against the
// same bytecode level, locally and in CI.
kotlin {
    jvmToolchain(JVM_TOOLCHAIN_VERSION)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("detekt.yml"))
    // Tests are ~a third of the codebase; excluding them from static analysis means the
    // part most likely to accumulate copy-paste is the part nobody checks.
    source.setFrom("src/main/kotlin", "src/test/kotlin")
    // Pre-existing findings a module had before its own baseline was cut, so `./gradlew build`
    // stays a gate on *new* debt rather than a blanket amnesty. A module with no baseline file
    // simply has none recorded — detekt treats a missing path the same as an empty baseline.
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
