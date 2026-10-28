package io.github.myorg.myapp.features.conventions

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldNotBe
import java.io.File
import java.lang.Character.MATH_SYMBOL
import java.lang.Character.OTHER_SYMBOL
import java.lang.Character.getType

/**
 * Every string the product draws exists in **both** locales.
 *
 * `values/` is English and is the legal fallback for any locale without a folder of its own,
 * which is why a missing translation produces no error, no warning and no red test — it simply
 * renders in English. That is how a whole screen can reach Russian users in English: keys added to
 * `values/` and never to `values-ru/`, unnoticed for weeks.
 *
 * The check is symmetric on purpose. A key only in `values-ru/` is the rarer half of the same
 * defect — a translation for a string that no longer exists, kept alive by nobody reading it.
 */
class ResourceKeysTest :
    FunSpec({
        val root = File("src/commonMain/composeResources")

        fun keysOf(locale: String): Set<String> {
            val file = File(root, "$locale/strings.xml")
            val text = file.takeIf { it.isFile }?.readText()
            // A wrong working directory would otherwise make this test pass by finding nothing
            // in either folder — the one way a set-equality check can be vacuously green.
            text shouldNotBe null
            return NAME_ATTRIBUTE.findAll(text.orEmpty()).map { it.groupValues[1] }.toSet()
        }

        test("values and values-ru declare the same keys") {
            val en = keysOf("values")
            val ru = keysOf("values-ru")
            (en - ru).sorted().shouldBeEmpty()
            (ru - en).sorted().shouldBeEmpty()
        }

        test("common UI source does not use font-dependent symbol glyphs as icons") {
            val commonMain = File("src/commonMain")
            commonMain.isDirectory shouldNotBe false

            val occurrences = mutableListOf<String>()
            commonMain
                .walkTopDown()
                .filter { it.isFile && it.extension in setOf("kt", "xml") }
                .forEach { file ->
                    file.readLines().forEachIndexed { index, line ->
                        line.codePoints().forEach { codePoint ->
                            if (getType(codePoint) == OTHER_SYMBOL.toInt()) {
                                val location = "${file.invariantSeparatorsPath}:${index + 1}: "
                                occurrences += location + String(Character.toChars(codePoint))
                            }
                        }
                    }
                }

            occurrences.shouldBeEmpty()
        }

        /*
         * A maths symbol like `⟳` (U+27F3, category `Sm`) passes the `So` check above and draws as a
         * box ("tofu") on a canvas when no bundled font has it. Maths symbols are allowed in UI text
         * only where the bundled font subset has a glyph; anything else is a drawn icon. A formula
         * renderer that maps TeX to maths glyphs on purpose has its own fallback and is excluded.
         */
        test("UI text does not carry maths symbols the bundled font cannot draw") {
            val commonMain = File("src/commonMain")
            commonMain.isDirectory shouldNotBe false

            val occurrences = mutableListOf<String>()
            commonMain
                .walkTopDown()
                .filter { it.isFile && it.extension in setOf("kt", "xml") }
                .filterNot { it.invariantSeparatorsPath.contains("designsystem/markup/") }
                .forEach { file ->
                    file.readLines().forEachIndexed { index, line ->
                        val text = if (file.extension == "kt") uiLiterals(line) else line
                        text.codePoints().forEach { codePoint ->
                            if (getType(codePoint) == MATH_SYMBOL.toInt() && !fontDrawsSymbol(codePoint)) {
                                val location = "${file.invariantSeparatorsPath}:${index + 1}: "
                                occurrences += location + String(Character.toChars(codePoint))
                            }
                        }
                    }
                }

            occurrences.shouldBeEmpty()
        }
    })

/** String literals of a Kotlin line; KDoc and line comments are prose, not UI. */
private fun uiLiterals(line: String): String {
    val trimmed = line.trimStart()
    if (trimmed.startsWith("*") || trimmed.startsWith("//") || trimmed.startsWith("/*")) return ""
    return STRING_LITERAL.findAll(line).joinToString("") { it.value }
}

private fun fontDrawsSymbol(codePoint: Int): Boolean =
    codePoint <= LATIN_1_END || codePoint in ARROWS_IN_SUBSET || codePoint in MATHS_IN_SUBSET

private const val LATIN_1_END = 0xFF
private val ARROWS_IN_SUBSET = 0x2190..0x2195
private val MATHS_IN_SUBSET = setOf(0x2212, 0x2248, 0x2260, 0x2264, 0x2265)
private val STRING_LITERAL = Regex(""""(?:[^"\\]|\\.)*"""")

private val NAME_ATTRIBUTE = Regex("""<(?:string|plurals)\s+name="([^"]+)"""")
