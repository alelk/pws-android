// KMP module that also needs kotlinx.serialization (DTOs). Kotlin-family
// plugins must all load from one classpath, so serialization is applied here
// through the convention plugin — never via `alias(...)` in the module, which
// would load the Kotlin Gradle plugin a second time.

plugins {
    id("app.kmp")
    id("org.jetbrains.kotlin.plugin.serialization")
}
