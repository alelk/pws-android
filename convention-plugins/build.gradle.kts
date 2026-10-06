plugins {
  `kotlin-dsl`
}

// Precompiled convention plugins apply these plugins by id, so the Gradle plugins they wrap must be
// on this build's classpath. Versions still come from the catalogue: `toDep()` turns a plugin
// marker into its implementation artifact.
dependencies {
  implementation(libs.plugins.android.application.toDep())
  // Kotlin Gradle plugin: AGP's built-in Kotlin exposes the `kotlin` extension (jvmToolchain).
  implementation(libs.plugins.kotlin.multiplatform.toDep())
  implementation(libs.plugins.detekt.toDep())
  implementation(libs.plugins.ktlint.toDep())
}

fun Provider<PluginDependency>.toDep() = map {
  "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}"
}
