package io.github.myorg.myapp.features.conventions

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.io.File

/**
 * The screen surface: how many screens there are, how they are shaped, how they deliver one-shot
 * events. The client's counterpart of ApiSurfaceTest: there the reference is the live Ktor routing
 * tree, here the list of entry composables taken from source. A new screen missing from the list
 * fails the build — it is either forgotten in navigation or undocumented.
 */
class ScreenSurfaceTest :
    FunSpec({
        val root = File(UI_SOURCE_ROOT)

        test("the set of navigation entries is the pinned one") {
            root.entryNames() shouldBe EXPECTED_ENTRIES
        }

        test("B1 — an entry has a pure Content layer") {
            val contents = root.contentNames()
            root.entryNames().filterNot { "${it.removeSuffix("Entry")}Content" in contents }.shouldBeEmpty()
        }

        test("B4 — one-shot events travel by Channel, not by a flag in state") {
            root.violations { ONE_SHOT_FLAG.containsMatchIn(it) } shouldBe KNOWN_ONE_SHOT_FLAGS
        }
    })

/** The pinned entries. A new screen is added here deliberately (navigation, docs, tests). */
private val EXPECTED_ENTRIES =
    listOf(
        "LoginEntry",
        "TodayEntry",
        "TaskDetailEntry",
    )

/**
 * B4 — persisted fields that merely share a name with a one-shot flag (e.g. a checklist item's
 * `done`), each with the reason the regex cannot tell them apart.
 */
private val KNOWN_ONE_SHOT_FLAGS = listOf<String>()

private const val UI_SOURCE_ROOT = "src/commonMain/kotlin/io/github/myorg/myapp/features"

private val ENTRY_DECLARATION = Regex("""^fun (\w+Entry)\(""", RegexOption.MULTILINE)
private val CONTENT_DECLARATION = Regex("""^(?:internal )?fun (\w+Content)\(""", RegexOption.MULTILINE)
private val ONE_SHOT_FLAG =
    Regex(
        """^\s+val (done|sent|saved|success|succeeded|completed|navigated|finished):\s+Boolean""",
        RegexOption.MULTILINE,
    )

private fun File.entryNames(): List<String> = declarations(ENTRY_DECLARATION)

private fun File.contentNames(): List<String> = declarations(CONTENT_DECLARATION)

private fun File.declarations(pattern: Regex): List<String> =
    ktFiles()
        .flatMap { file -> pattern.findAll(file.readText()).map { it.groupValues[1] }.toList() }
        .distinct()
        .sorted()

private fun File.violations(hit: (String) -> Boolean): List<String> =
    ktFiles()
        .filter { hit(it.readText()) }
        .map { it.relative() }
        .sorted()

private fun File.ktFiles(): List<File> = walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

private fun File.relative(): String = invariantSeparatorsPath.substringAfter("$UI_SOURCE_ROOT/")
