plugins {
    `kotlin-dsl`
}

repositories {
    google()
    gradlePluginPortal()
    mavenCentral()
}

// Precompiled convention plugins apply these plugins by id; the Gradle plugins
// they wrap must therefore be on this build's classpath. Versions still come
// from the catalogue — `toDep()` turns a plugin marker into its artifact.
dependencies {
    implementation(libs.plugins.kotlin.multiplatform.toDep())
    implementation(libs.plugins.kotlin.jvm.toDep())
    implementation(libs.plugins.kotlin.serialization.toDep())
    // `compose` is both a leaf alias and the prefix of `compose-compiler`, so the leaf is
    // reached through `asProvider()` (a plain `libs.plugins.compose` returns the group).
    implementation(libs.plugins.compose.asProvider().toDep())
    implementation(libs.plugins.compose.compiler.toDep())
    implementation(libs.plugins.detekt.toDep())
    implementation(libs.plugins.ktlint.toDep())
}

fun Provider<PluginDependency>.toDep() = map {
    "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}"
}
