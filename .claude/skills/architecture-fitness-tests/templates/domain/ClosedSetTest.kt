package io.github.myorg.myapp.domain.architecture

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.io.File

/**
 * Closed sets: adding a sealed variant or an enum constant must be RED until somebody classifies it.
 *
 * The compiler already does this for a `when` without `else` — so the tests below guard the three
 * ways that guarantee is lost again:
 *
 * 1. the classification table this test knows == `sealedSubclasses` (an unknown variant and a vanished
 *    one are both named);
 * 2. the `when` behind the classification has no `else` (an `else` silently answers for the new one);
 * 3. nobody compares a value (`== Period.Daily`) or lists the values by hand (`listOf(Period.Daily, …)`)
 *    outside the one declared table: both put the new value into an implicit "otherwise".
 *
 * Mine: a ledger source added and counted as "spent" by an `else`; a third period landing in the
 * `else` of ten `if (period == Daily)` sites.
 */
class ClosedSetTest :
    FunSpec({
        test("the sources this test knows are exactly the sealed subclasses of LedgerSource") {
            val known = EXPECTED.keys.map { it::class }.toSet()
            val sealed = LedgerSource::class.sealedSubclasses.toSet()
            withClue("unknown: ${sealed - known}; vanished: ${known - sealed}") { known shouldBe sealed }
        }

        test("every source has its class") {
            EXPECTED.forEach { (source, expected) -> withClue(source::class.simpleName) { source.debitClass shouldBe expected } }
        }

        test("the when behind debitClass has no else branch") {
            val file = File("src/commonMain/kotlin/io/github/myorg/myapp/domain/ledger/Ledger.kt")
            file.isFile shouldBe true
            val body = file.readText().substringAfter("val LedgerSource.debitClass: DebitClass").substringBefore("\n/**")
            body.length shouldNotBe 0
            withClue(body) { ELSE_BRANCH.containsMatchIn(body) shouldBe false }
        }

        val sources =
            listOf(File("src/commonMain/kotlin"), File("../features/src/commonMain/kotlin"))
                .filter { it.isDirectory }
                .flatMap { root -> root.walkTopDown().filter { it.isFile && it.extension == "kt" } }
                .associate { it.invariantSeparatorsPath to it.readText() }

        test("guard: the readers of Period are where this test looks for them") {
            (sources.filterValues { "Period" in it }.size >= MIN_READERS) shouldBe true
        }

        test("nobody compares a Period: it is read by a when without else") {
            sources
                .flatMap { (path, text) ->
                    text.lines().mapIndexedNotNull { i, line -> "$path:${i + 1}".takeIf { COMPARISON.containsMatchIn(line) } }
                }.shouldBeEmpty()
        }

        test("nobody lists the periods by hand: PERIOD_VARIANTS is the list") {
            sources.filterValues { HAND_LIST.containsMatchIn(it) }.keys.shouldBeEmpty()
        }
    })

private val ELSE_BRANCH = Regex("""\belse\s*->""")
private val COMPARISON = Regex("""[!=]=\s*Period\.""")

/** `listOf(Period.Daily, …)` however wrapped — but not a pair `listOf(Period.Daily to …)`. */
private val HAND_LIST = Regex("""listOf\(\s*Period\.\w+\s*[,)]""")

private const val MIN_READERS = 5

/** One sample per variant -> its class. Constructed with dummy ids; the test is about the type, not the data. */
private val EXPECTED: Map<LedgerSource, DebitClass> =
    mapOf(
        LedgerSource.Purchase(PurchaseId("p")) to DebitClass.Spent,
        LedgerSource.Penalty(RuleId("r")) to DebitClass.Penalized,
        LedgerSource.Reversal(EntryId("e")) to DebitClass.Reversed,
    )
