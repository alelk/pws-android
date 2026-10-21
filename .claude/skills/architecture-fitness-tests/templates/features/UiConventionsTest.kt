package io.github.myorg.myapp.features.conventions

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.io.File

/**
 * Frontend rules, expressed as checks. The design-system and frontend-architecture docs state them;
 * without this file only a reviewer's attention enforces them (one project counted 119
 * `MaterialTheme.*` uses in screens after 59 steps of "please don't").
 *
 * Every `KNOWN_*` list is compared with the facts by EXACT equality: the test fails on a new
 * violation and on a vanished one still listed. The lists only shrink.
 */
class UiConventionsTest :
    FunSpec({
        val root = File(UI_SOURCE_ROOT)

        // Guard against a vacuous pass: a wrong working directory would make every check below
        // green while checking nothing.
        test("source root is where the test thinks it is") {
            root.isDirectory shouldBe true
            root.ktFiles().size shouldNotBe 0
        }

        test("A1 — screens speak AppTheme, never MaterialTheme") {
            root.violations(outsideDesignSystem) { it.contains("MaterialTheme.") }.shouldBeEmpty()
        }

        test("A2 — screens use design-system wrappers, not raw Material 3 components") {
            root
                .violations(outsideDesignSystem) { text ->
                    RAW_M3_IMPORT.containsMatchIn(text)
                }.shouldBeEmpty()
        }

        test("A3 — sizes come from AppTheme.spacing / AppTheme.sizing") {
            root.violations(outsideDesignSystemOrComponents) { DP_LITERAL.containsMatchIn(it) }.shouldBeEmpty()
        }

        test("A5 — the design system is a leaf: it knows neither features nor the client") {
            root
                .ktFiles()
                .filter { it.relative().startsWith("designsystem/") }
                .filterNot { it.relative().startsWith("designsystem/markup/") }
                .filter { file ->
                    file.readLines().any { line ->
                        line.startsWith("import io.github.myorg.myapp.client.") ||
                            (
                                line.startsWith("import io.github.myorg.myapp.features.") &&
                                    "designsystem" !in line &&
                                    "generated" !in line
                            )
                    }
                }.map { it.relative() }
                .sorted()
                .shouldBeEmpty()
        }

        test("E1 — the UI does not decide by wire strings") {
            root.violations({ true }) { WIRE_STRING.containsMatchIn(it) }.shouldBeEmpty()
        }

        test("E3 — a composable never sees a contract DTO") {
            root
                .ktFiles()
                .filter { file ->
                    val text = file.readText()
                    "@Composable" in text && "import io.github.myorg.myapp.contract." in text
                }.map { it.relative() }
                .sorted()
                .shouldBeEmpty()
        }

        test("B3 — a composable never takes a ViewModel as a parameter") {
            root.violations({ true }) { VIEWMODEL_PARAMETER.containsMatchIn(it) }.shouldBeEmpty()
        }

        test("D4 — the module has no submitting/saving field; the name is busy everywhere") {
            root.violations({ true }) { SUBMITTING_OR_SAVING_FIELD.containsMatchIn(it) }.shouldBeEmpty()
        }

        test("G20 — no screen file grows past 600 lines") {
            root
                .ktFiles()
                .filter { it.readLines().size > MAX_FILE_LINES }
                .map { it.relative() }
                .sorted() shouldBe KNOWN_LARGE_FILES
        }
    })

private const val UI_SOURCE_ROOT = "src/commonMain/kotlin/io/github/myorg/myapp/features"
private const val MAX_FILE_LINES = 600

/** `designsystem/` is the only layer allowed to use Material 3: there it is the engine. */
private val outsideDesignSystem: (String) -> Boolean = { path -> !path.startsWith("designsystem/") }

/** Subject widgets own their one-off drawing measures; screens and app chrome use theme tokens. */
private val outsideDesignSystemOrComponents: (String) -> Boolean = { path ->
    !path.startsWith("designsystem/") && !path.startsWith("components/")
}

private val RAW_M3_IMPORT =
    Regex(
        """^import androidx\.compose\.material3\.(Text|TextButton|Button|OutlinedButton|OutlinedTextField|""" +
            """Surface|Card|AlertDialog|Checkbox|Switch|FilterChip|DropdownMenu|DropdownMenuItem|IconButton|""" +
            """HorizontalDivider|LinearProgressIndicator|CircularProgressIndicator|ModalBottomSheet)$""",
        RegexOption.MULTILINE,
    )

private val DP_LITERAL = Regex("""\b\d+(\.\d+)?\.dp\b""")

/** Wire values the UI must not decide by — list your enums' serialized names. */
private val WIRE_STRING =
    Regex(
        """"(Pending|InProgress|Done|Cancelled|Owner|Member|Admin)"""",
    )

private val VIEWMODEL_PARAMETER = Regex("""^\s+\w+:\s+\w*ViewModel[,)]""", RegexOption.MULTILINE)

/** D4 — `submitting`/`saving` collapsed into one name, `busy`, module-wide. */
private val SUBMITTING_OR_SAVING_FIELD = Regex("""val (submitting|saving)\s*:""")

private fun File.ktFiles(): List<File> = walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

/** Path relative to the UI source root — the unit of every list below. */
private fun File.relative(): String = invariantSeparatorsPath.substringAfter("$UI_SOURCE_ROOT/")

private fun File.violations(
    where: (String) -> Boolean,
    hit: (String) -> Boolean,
): List<String> =
    ktFiles()
        .map { it.relative() to it.readText() }
        .filter { (path, text) -> where(path) && hit(text) }
        .map { it.first }
        .sorted()

/**
 * G20 — files longer than 600 lines. Each permanent exception is written here WITH its reason
 * (a token dictionary whose value is being one file; a cohesive ViewModel that cannot be split
 * without widening visibility), never implied.
 */
private val KNOWN_LARGE_FILES =
    listOf<String>(
        // "designsystem/AppTheme.kt",   // the token dictionary: its value is that it is one
    )
