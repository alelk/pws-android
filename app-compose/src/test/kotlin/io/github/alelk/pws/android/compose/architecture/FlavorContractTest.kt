package io.github.alelk.pws.android.compose.architecture

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Rule: every flavor source set (`src/{ru,uk,full,rustore}`) has `flavor/FlavorIntegration.kt` and
 * declares the same public top-level names: `MONETIZATION`, `flavorKoinModules`, `flavorStartupTasks`,
 * `flavorShowPaywall` (internal/private helpers are not part of the contract).
 * Why: `main` calls these names without knowing the flavor; a flavor that drifts compiles only when
 * that flavor is built, so a rename in one flavor is found late. This test finds it on any flavor build.
 *
 * See it red: rename `flavorShowPaywall` in `src/ru/.../FlavorIntegration.kt` (and run
 * `./gradlew :app-compose:testRuDebugUnitTest`), or add a public top-level `fun extra()` to one
 * flavor, or move a file out of `flavor/`. Revert afterwards.
 */
class FlavorContractTest :
  FunSpec({
    val sources = scanFiles("app-compose/src").filter { it.relativePath.endsWith("/FlavorIntegration.kt") }

    test("every flavor has flavor/FlavorIntegration.kt") {
      val actual = sources.map { it.relativePath.removePrefix("app-compose/src/").substringBefore('/') }.sorted()
      withClue("Found: ${sources.map { it.relativePath }}") { actual shouldBe FLAVORS.sorted() }
      sources.forEach { it.relativePath.contains("/kotlin/$FLAVOR_PACKAGE_PATH/FlavorIntegration.kt") shouldBe true }
    }

    test("every flavor declares the same public top-level names") {
      sources.forEach { file ->
        val names =
          TOP_LEVEL_DECLARATION
            .findAll(file.code)
            .filter { "internal" !in it.groupValues[1] && "private" !in it.groupValues[1] }
            .map { it.groupValues[2] }
            .toSet()
        withClue(file.relativePath) { names shouldBe CONTRACT_NAMES }
      }
    }
  })

private val FLAVORS = listOf("full", "ru", "rustore", "uk")

private const val FLAVOR_PACKAGE_PATH = "io/github/alelk/pws/android/compose/flavor"

private val CONTRACT_NAMES = setOf("MONETIZATION", "flavorKoinModules", "flavorStartupTasks", "flavorShowPaywall")

/** A top-level (column 0) `val` / `fun` with its modifiers (group 1) and name (group 2). */
private val TOP_LEVEL_DECLARATION =
  Regex(
    """^((?:(?:public|internal|private|suspend|const|inline)\s+)*)(?:val|fun)\s+(?:<[^>]*>\s*)?(?:[\w.]+\.)?(\w+)""",
    RegexOption.MULTILINE,
  )
