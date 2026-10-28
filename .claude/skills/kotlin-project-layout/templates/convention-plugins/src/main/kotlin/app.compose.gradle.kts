import org.jetbrains.compose.ComposeBuildConfig
import org.jetbrains.compose.ComposeExtension

// KMP module that renders UI with Compose Multiplatform (`features` and the shells).
// Same reason as `app.kmp.serialization`: every Kotlin-family plugin must load from a
// single classpath, so the Compose compiler plugin is applied here and never via
// `alias(...)` in a module — that would pull the Kotlin Gradle plugin in a second time.

plugins {
    id("app.kmp")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    // Navigation 3 routes are `@Serializable` NavKeys and the back stack is saved state
    // (SavedStateConfiguration polymorphism) — every Compose feature module needs it.
    id("org.jetbrains.kotlin.plugin.serialization")
}

// The type-safe `compose` accessor only exists in scripts that apply the Compose plugin in
// their own `plugins { }` block; modules get the plugin through this convention plugin, so
// the dependency notations are read off the extension instead. The artifacts every Compose
// module needs are declared once, here.
val composeDeps = extensions.getByType<ComposeExtension>().dependencies

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(composeDeps.runtime)
            implementation(composeDeps.foundation)
            implementation(composeDeps.material3)
            implementation(composeDeps.ui)
            implementation(composeDeps.components.resources)
            // `@Preview` support (common-code, per-component previews). The `ComposeExtension`
            // accessor for this artifact is deprecated in favour of the raw coordinate, pinned to
            // the same version every other Compose artifact here already resolves to.
            implementation("org.jetbrains.compose.ui:ui-tooling-preview:${ComposeBuildConfig.composeVersion}")
        }
    }
}
