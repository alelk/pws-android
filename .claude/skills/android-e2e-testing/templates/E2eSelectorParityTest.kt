package com.example.app.architecture

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import java.io.File

/**
 * Rule (AGENTS.md "E2E"): every selector id in `e2e/config/compose.env` exists as a string literal
 * in production source, and every numbered flow declares exactly one kind in its header
 * (`tags: smoke | mutation | diagnostic`, plus `device-state` when it toggles device state).
 *
 * Why: the E2E suite does not run in `./gradlew build`. Without this lock a renamed `testTag` is
 * found at release time as "Element not found" — or never, if the suite is skipped. The runner picks
 * and orders flows by tag, so a flow without a kind is in no run at all: a test that nobody runs.
 * `diagnostic` is the one legitimate way to be outside every run; each such flow is listed in
 * [KNOWN_DIAGNOSTIC] with a reason, and the list only shrinks.
 *
 * Matching is by literal: `"action:save"` or a prefix of an interpolated literal
 * (`"song-row-$id"` matches the env value `song-row-1` through the prefix `song-row-`). Regex
 * values and system-UI ids go to [KNOWN_UNCHECKED] with a reason.
 *
 * See it red: rename a `testTag("action:save")` in production, run `./gradlew :app:test`, revert;
 * delete the `tags:` of one flow, run, revert.
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

    // Numbered flows of the root and of flavor folders; subflow folders (_helpers, _harness) are not flows.
    val flowsDir = File(root, FLOWS_DIR)
    val flows: Map<String, String> = flowsDir.walkTopDown()
      .filter { it.isFile && it.name.matches(Regex("""\d\d-.*\.yaml""")) }
      .map { it.relativeTo(flowsDir).invariantSeparatorsPath }
      .filter { path -> path.split('/').none { it.startsWith("_") } }
      .associateWith { File(flowsDir, it).readText() }

    test("guard: the scan sees the env file, the sources, a plausible number of ids and of flows") {
      envFile.exists() shouldBe true
      sources.size shouldBeGreaterThanOrEqual MIN_SOURCE_FILES
      ids.size shouldBeGreaterThanOrEqual MIN_IDS
      flows.size shouldBeGreaterThanOrEqual MIN_FLOWS
    }

    test("pair: the detector flags a missing id and accepts literal and interpolated forms") {
      val text = """Modifier.testTag("action:save") ; Modifier.testTag("song-row-${'$'}id")"""
      missing(mapOf("A" to "action:save", "B" to "song-row-1", "C" to "action:gone"), text) shouldBe listOf("C")
    }

    test("pair: the tag reader takes the header only, ignores comments and sees a missing or doubled kind") {
      val header = "appId: x\ntags:\n  - mutation   # why\n  # - device-state\n  - device-state\n---\n- tapOn: smoke\n"
      tagsOf(header) shouldBe listOf("mutation", "device-state")
      tagsOf("appId: x\n---\n- tapOn: smoke\n") shouldBe emptyList()
      tagsOf("tags:\n  - smoke\n  - diagnostic\n---\n").count { it in KINDS } shouldBe 2
    }

    test("every env selector id is a literal in production source (or a known exception)") {
      val actual = missing(ids, sourceText).toSet()
      withClue("unknown: ${actual - KNOWN_UNCHECKED.keys}\nvanished: ${KNOWN_UNCHECKED.keys - actual}") {
        actual shouldBe KNOWN_UNCHECKED.keys
      }
    }

    test("every numbered flow declares exactly one kind tag and only known tags") {
      val noSingleKind = flows.filterValues { tagsOf(it).count { tag -> tag in KINDS } != 1 }.keys
      val unknownTags = flows.mapValues { (_, text) -> tagsOf(text) - TAGS }.filterValues { it.isNotEmpty() }
      withClue("not exactly one kind: $noSingleKind\nunknown tags: $unknownTags") {
        (noSingleKind.isEmpty() && unknownTags.isEmpty()) shouldBe true
      }
    }

    test("diagnostic flows are exactly the known ones") {
      val actual = flows.filterValues { "diagnostic" in tagsOf(it) }.keys
      withClue("unknown: ${actual - KNOWN_DIAGNOSTIC.keys}\nvanished: ${KNOWN_DIAGNOSTIC.keys - actual}") {
        actual shouldBe KNOWN_DIAGNOSTIC.keys
      }
    }
  })

private const val ENV_FILE = "e2e/config/compose.env"
private const val FLOWS_DIR = "e2e/flows/compose"
private val SOURCE_DIRS = listOf("app/src", "features/src") // add composite/included build dirs that own the tags
private const val MIN_SOURCE_FILES = 20
private const val MIN_IDS = 10
private const val MIN_FLOWS = 5

/** What a flow is. Exactly one per flow; the runner places the flow by it. */
private val KINDS = setOf("smoke", "mutation", "diagnostic")

/** Every tag a flow header may carry. `device-state`: toggles airplane mode / locale / font scale, runs last. */
private val TAGS = KINDS + "device-state"

/** Env keys whose values are ids to check. Keys outside these prefixes (timeouts, test data, system ids) are skipped. */
private val ID_KEY = Regex("""^(NAV|ACTION|FIELD|LIBRARY|SONG|HOME|ONBOARDING)_[A-Z0-9_]*$""")

/** env value (regex ids, system UI, ids built at runtime) -> why it cannot be matched by literal. */
private val KNOWN_UNCHECKED: Map<String, String> = mapOf(
  // "SONG_ITEM_PREFIX" to "regex family, matched by a prefix literal in a different form",
)

/** Flows deliberately in no run (path relative to the flows folder) -> why. Keep empty unless a flow is diagnostic. */
private val KNOWN_DIAGNOSTIC: Map<String, String> = mapOf(
  // "05-history-after-open.yaml" to "dwell timing unstable, run alone while fixing",
)

private fun selectorIds(env: String): Map<String, String> =
  env.lineSequence()
    .map { it.trim() }
    .filter { it.isNotEmpty() && !it.startsWith("#") }
    .mapNotNull { Regex("""^([A-Z_][A-Z0-9_]*)="(.*)"$""").matchEntire(it) }
    .map { it.groupValues[1] to it.groupValues[2] }
    .filter { (key, _) -> ID_KEY.matches(key) }
    .toMap()

/**
 * Tags in a flow header — the block list under `tags:` above the first `---`. Same reading as `flow_tags` in
 * run-e2e.sh (bare words, `#` comments and blank lines skipped); `tags: [smoke]` is not read, on purpose.
 */
private fun tagsOf(flow: String): List<String> {
  val header = flow.lineSequence()
    .takeWhile { it.trim() != "---" }
    .map { it.substringBefore('#') }
    .filter { it.isNotBlank() }
    .toList()
  val start = header.indexOfFirst { it.trim() == "tags:" }
  if (start < 0) return emptyList()
  return header.drop(start + 1)
    .takeWhile { it.trim().startsWith("-") }
    .map { it.trim().removePrefix("-").trim().trim('"') }
}

/** Keys (not values) of ids that have neither a literal nor an interpolated-literal occurrence. */
private fun missing(ids: Map<String, String>, sourceText: String): List<String> =
  ids.filter { (_, id) ->
    // "id" exactly, or "<id minus its trailing digits/name>" as the prefix of an interpolated literal.
    val prefix = id.trimEnd { it.isLetterOrDigit() && !it.isLetter() }.ifEmpty { id }
    "\"$id\"" !in sourceText && !(prefix != id && ("\"$prefix$" in sourceText || "\"$prefix{" in sourceText))
  }.keys.toList().sorted()
