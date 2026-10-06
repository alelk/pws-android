import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.tasks.BaseKtLintCheckTask

// Detekt + ktlint for every Kotlin module, over main AND test sources of every source set.
// Pre-existing findings are recorded per module in `detekt-baseline.xml` / `ktlint-baseline.xml`
// (a missing file = nothing recorded), so `check` blocks only NEW debt. Baselines only shrink:
// never regenerate one to absorb new findings.

plugins {
  id("io.gitlab.arturbosch.detekt")
  id("org.jlleitschuh.gradle.ktlint")
}

extensions.configure<DetektExtension> {
  buildUponDefaultConfig = true
  config.setFrom(rootProject.file("detekt.yml"))
  // Every `src/<sourceSet>/kotlin` (commonMain, iosMain, jvmTest, ru, androidTest, ...), so a new
  // source set is analysed without touching this file. Generated code lives under build/, not src/.
  source.setFrom(
    layout.projectDirectory.dir("src").asFile
      .listFiles { dir -> dir.isDirectory }.orEmpty()
      .map { File(it, "kotlin") }
      .filter { it.isDirectory }
      .sortedBy { it.path },
  )
  baseline = file("detekt-baseline.xml")
}

extensions.configure<KtlintExtension> {
  baseline.set(file("ktlint-baseline.xml"))
  // The ktlint plugin creates per-source-set tasks only from the Kotlin Gradle plugin; with AGP's
  // built-in Kotlin it sees none and would lint nothing but *.gradle.kts. Feed every Kotlin source
  // (all flavors, main and test) to the "Kotlin scripts" task instead.
  kotlinScriptAdditionalPaths {
    include(fileTree("src") { include("**/*.kt") })
  }
  filter {
    // Generated sources (Compose resource accessors, KSP, Room): Gradle's output dir may live
    // outside the project, so match the generated-path patterns rather than only /build/.
    exclude { element ->
      val path = element.file.path
      path.contains("/generated/") || path.contains("/build/")
    }
  }
}

// ktlint's source set includes generated source dirs (i18n4k, Compose resources). They are
// filtered out above, but Gradle still sees them as inputs, so declare the ordering explicitly.
tasks.withType<BaseKtLintCheckTask>().configureEach {
  // Resolved by name at graph time: `tasks.matching { }` would realize every task, including
  // Kotlin/Native ones that cannot be created on unsupported hosts.
  mustRunAfter(provider { tasks.names.filter { it.startsWith("generate") }.map { tasks.named(it) } })
}
