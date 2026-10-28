package io.github.myorg.myapp.domain.architecture

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.io.File

/*
 * A `Left` rolls the transaction back; a transaction that keeps its writes on a refusal is a
 * decision with a reason — a closed list, compared by EXACT equality. A new caller is red until it
 * is named here; a vanished one is red too, so the list cannot quietly keep a place that no longer
 * needs it. The behaviour itself is pinned on a real database (RefusalSemanticsTest in server:infra).
 *
 * Lives in :domain:jvmTest (java.io is fine there); runs inside ./gradlew build.
 */
class CommitsOnLeftTest :
    FunSpec({
        val main = File("src/commonMain/kotlin/io/github/myorg/myapp/domain")
        val sources =
            main
                .walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .associate { it.relativeTo(main).invariantSeparatorsPath to it.readText() }

        // Vacuous-pass guard: a moved source root must not turn the ratchet into "0 == 0".
        test("the domain sources are where this test looks for them") {
            (sources.size > MIN_SOURCES) shouldBe true
            sources.getValue(RUNNER).contains("fun <T> inRwTransactionKeepingWrites(") shouldBe true
        }

        test("only the files of KNOWN_COMMITS_ON_LEFT keep their writes on a Left") {
            val keeping = sources.filterValues { KEEPING_CALL.containsMatchIn(it) }.keys
            withClue("inRwTransactionKeepingWrites( callers: ${keeping.sorted()}") {
                keeping shouldBe KNOWN_COMMITS_ON_LEFT.keys
            }
        }

        // Only when migrating from a runner that committed a Left: the old "atomic" escape hatch is gone.
        test("no atomic variant survives: rolling back is the default") {
            sources.filterValues { RETIRED_NAME in it }.keys.shouldBeEmpty()
        }
    })

private const val MIN_SOURCES = 20
private const val RUNNER = "common/tx/TransactionRunner.kt"

/** A call through a receiver — not the declaration, not a KDoc mention. */
private val KEEPING_CALL = Regex("""\.inRwTransactionKeepingWrites\s*\(""")

/** File → what must outlive the refusal. Every entry is a reason, not a habit. */
private val KNOWN_COMMITS_ON_LEFT: Map<String, String> =
    mapOf(
        "identity/auth/SignInWithSecret.kt" to "the failed-attempt counter and the lock — or the secret is brute-forced",
        "identity/auth/RotateRefreshSession.kt" to "the revoked chain of a replayed (stolen) refresh token",
        "assistant/AskAssistant.kt" to "the spent turn of the daily allowance; the audit row of a call that happened",
    )

/** Spelled in two halves so a grep for the removed name finds nothing at all. */
private const val RETIRED_NAME = "inAtomic" + "RwTransaction"
