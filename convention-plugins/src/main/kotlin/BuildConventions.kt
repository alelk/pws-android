import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.api.JavaVersion
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension

/**
 * Build-wide constants shared by the `pws.android.*` convention plugins. They are build-infrastructure
 * facts (not dependency versions), so they live here and are declared exactly once.
 */
const val JVM_TOOLCHAIN_VERSION = 21
const val ANDROID_COMPILE_SDK = 37
const val ANDROID_MIN_SDK = 23

/** `--add-opens` flags that Robolectric / Kotest-on-Android need on JDK 21 for unit tests. */
private val UNIT_TEST_ADD_OPENS = listOf(
  "--add-opens=java.base/java.lang=ALL-UNNAMED",
  "--add-opens=java.base/java.util=ALL-UNNAMED",
  "--add-opens=java.base/java.io=ALL-UNNAMED",
  "--add-opens=java.base/java.net=ALL-UNNAMED",
  "--add-opens=java.base/java.security=ALL-UNNAMED",
  "--add-opens=java.base/java.text=ALL-UNNAMED",
  "--add-opens=java.base/java.nio=ALL-UNNAMED",
  "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
  "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
  "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
  "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
)

/**
 * Configuration common to every Android module (application and library): SDK levels, Java 21,
 * the JDK toolchain, JUnit Platform with the `--add-opens` flags, and the `localSeed` build type
 * (it must exist in every module so variant resolution matches across `:app-compose`,
 * `:data:db-android` and `:data:content-delivery`; modules configure their own `localSeed`).
 */
internal fun Project.configureAndroidCommon(android: CommonExtension) {
  android.compileSdk = ANDROID_COMPILE_SDK
  android.defaultConfig.minSdk = ANDROID_MIN_SDK

  android.compileOptions.apply {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }

  android.buildTypes.create("localSeed")

  android.testOptions.unitTests.isIncludeAndroidResources = true

  extensions.configure<KotlinBaseExtension> {
    // Pinned so the bytecode does not depend on whichever JDK launched Gradle.
    jvmToolchain(JVM_TOOLCHAIN_VERSION)
  }

  tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    jvmArgs(UNIT_TEST_ADD_OPENS)
  }
}
