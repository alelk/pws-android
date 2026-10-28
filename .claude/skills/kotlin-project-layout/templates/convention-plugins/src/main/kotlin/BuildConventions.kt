/**
 * Build-wide constants shared by the `app.*` convention plugins.
 *
 * The JDK the project compiles against is a build-infrastructure fact, not a
 * dependency version, so it lives here rather than in `libs.versions.toml` — but it is
 * still declared exactly once. Gradle provisions or selects a matching toolchain, so
 * the bytecode level no longer depends on whichever JDK happens to launch Gradle.
 */
const val JVM_TOOLCHAIN_VERSION = 21
