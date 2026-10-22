// JVM-only module that also needs kotlinx.serialization (persistence models /
// *Pm JSONB payloads). Kotlin-family plugins must all load from one classpath,
// so serialization is applied here through the convention plugin — never via
// `alias(...)` in the module, which would load the Kotlin Gradle plugin twice.

plugins {
    id("app.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}
