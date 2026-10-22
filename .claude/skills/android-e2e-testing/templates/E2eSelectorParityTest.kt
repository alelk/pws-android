package com.example.app.architecture

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import java.io.File

/**
 * Rule (AGENTS.md "E2E"): every selector id in `e2e/config/compose.env` exists as a string literal
 * in production source, and every numbered flow file is listed in `suite-full.yaml`.
 *
 * Why: the E2E suite does not run in `./gradlew build`. Without this lock a renamed `testTag` is
 * found at release time as "Element not found" — or never, if the suite is skipped. A flow that is
 * on disk but in no suite is a test that nobody runs.
 *
 * Matching is by literal: `"action:save"` or a prefix of an interpolated literal
 * (`"song-row-$id"` matches the env value `song-row-1` through the prefix `song-row-`). Regex
 * values and system-UI ids go to [KNOWN_UNCHECKED] with a reason.
 *
 * See it red: rename a `testTag("action:save")` in production, run `./gradlew :app:test`, revert.
 */
class E2eSelectorParityTest :
  FunSpec({
    val root = generateSequence(File(".").canonicalFile) { it.parentFile }
      .first { File(it, "settings.gradle.kts").exists() }
    val envFile = File(root, ENV_FILE)
    val sources = SOURCE_DIRS.map { File(root, it) }.filter { it.exists() }
      .flatMap { dir -> dir.walkTopDown().filter { it.isFile && it.extension == "kt" && "/build/" !in it.path }.toList() }
    val sourceText = sources.joinToString("\n") { it.readText() }
    val ids = selectorIds(envFile.readText())

    test("guard: the scan sees the env file, the sources and a plausible number of ids") {
      envFile.exists() shouldBe true
      sources.size shouldBeGreaterThanOrEqual MIN_SOURCE_FILES
      ids.size shouldBeGreaterThanOrEqual MIN_IDS
    }

    test("pair: the detector flags a missing id and accepts literal and interpolated forms") {
      val text = """Modifier.testTag("action:save") ; Modifier.testTag("song-row-${'$'}id")"""
      missing(mapOf("A" to "action:save", "B" to "song-row-1", "C" to "action:gone"), text) shouldBe listOf("C")
    }

    test("every env selector id is a literal in production source (or a known exception)") {
      val actual = missing(ids, sourceText).toSet()
      withClue("unknown: ${actual - KNOWN_UNCHECKED.keys}\nvanished: ${KNOWN_UNCHECKED.keys - actual}") {
        actual shouldBe KNOWN_UNCHECKED.keys
      }
    }

    test("every numbered flow is listed in the full suite (or declared diagnostic)") {
      val flowsDir = File(root, FLOWS_DIR)
      val onDisk = flowsDir.listFiles { f -> f.name.matches(Regex("""\d\d-.*\.yaml""")) }.orEmpty().map { it.name }.toSet()
      val listed = File(flowsDir, "suite-full.yaml").readText()
      val unlisted = onDisk.filterNot { it in listed }.toSet()
      withClue("unlisted: ${unlisted - KNOWN_UNLISTED.keys}\nvanished: ${KNOWN_UNLISTED.keys - unlisted}") {
        unlisted shouldBe KNOWN_UNLISTED.keys
      }
    }
  })

private const val ENV_FILE = "e2e/config/compose.env"
private const val FLOWS_DIR = "e2e/flows/compose"
private val SOURCE_DIRS = listOf("app/src", "features/src") // add composite/included build dirs that own the tags
private const val MIN_SOURCE_FILES = 20
private const val MIN_IDS = 10

/** Env keys whose values are ids to check. Keys outside these prefixes (timeouts, test data, system ids) are skipped. */
private val ID_KEY = Regex("""^(NAV|ACTION|FIELD|LIBRARY|SONG|HOME|ONBOARDING)_[A-Z0-9_]*$""")

/** env value (regex ids, system UI, ids built at runtime) -> why it cannot be matched by literal. */
private val KNOWN_UNCHECKED: Map<String, String> = mapOf(
  // "SONG_ITEM_PREFIX" to "regex family, matched by a prefix literal in a different form",
)

/** Flow files deliberately outside every suite -> why. Keep empty unless a flow is diagnostic. */
private val KNOWN_UNLISTED: Map<String, String> = mapOf(
  // "05-history-after-open.yaml" to "diagnostic: dwell timing unstable, run alone while fixing",
)

private fun selectorIds(env: String): Map<String, String> =
  env.lineSequence()
    .map { it.trim() }
    .filter { it.isNotEmpty() && !it.startsWith("#") }
    .mapNotNull { Regex("""^([A-Z_][A-Z0-9_]*)="(.*)"$""").matchEntire(it) }
    .map { it.groupValues[1] to it.groupValues[2] }
    .filter { (key, _) -> ID_KEY.matches(key) }
    .toMap()

/** Keys (not values) of ids that have neither a literal nor an interpolated-literal occurrence. */
private fun missing(ids: Map<String, String>, sourceText: String): List<String> =
  ids.filter { (_, id) ->
    // "id" exactly, or "<id minus its trailing digits/name>" as the prefix of an interpolated literal.
    val prefix = id.trimEnd { it.isLetterOrDigit() && !it.isLetter() }.ifEmpty { id }
    "\"$id\"" !in sourceText && !(prefix != id && ("\"$prefix$" in sourceText || "\"$prefix{" in sourceText))
  }.keys.toList().sorted()
