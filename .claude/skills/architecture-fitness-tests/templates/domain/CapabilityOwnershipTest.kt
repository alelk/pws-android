package io.github.myorg.myapp.domain.architecture

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.io.File

/**
 * "Only these files may do Y": a capability that carries an invariant (a lock, a sealing step, a
 * transaction that keeps its writes on a refusal, a port that must never be reached from a use-case)
 * is legal only inside the components that uphold it. Three forms, copy the one you need per capability:
 *
 * 1. CALLERS — the files that CALL `x(` equal a closed `Map<file, reason>`. Exact equality: a new
 *    caller is red until named, a vanished one is red so the map never keeps a stale place.
 * 2. CONSTRUCTION — a type is built (`Type(`) only under one package (the gateway that seals it).
 *    `internal` stops other modules; this stops the rest of the module.
 * 3. KNOWLEDGE — a port is NAMED (import, parameter, KDoc) only by allowed roots across the whole
 *    repository's main sources. Stronger than "called": a file that knows the port can start calling it.
 *
 * Every form has a guard first (the scan sees what it should, with a numeric floor) and a proof that
 * the pattern matches something (the allowed owner itself is found) — otherwise a broken regex is green.
 */
class CapabilityOwnershipTest :
    FunSpec({
        val main = File("src/commonMain/kotlin/io/github/myorg/myapp/domain")
        val sources =
            main
                .walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .associate { it.relativeTo(main).invariantSeparatorsPath to it.readText() }

        test("guard: the domain sources are where this test looks for them") {
            (sources.size > MIN_DOMAIN_SOURCES) shouldBe true
            sources.getValue(RUNNER).contains("fun <T> inRwTransactionKeepingWrites(") shouldBe true
        }

        // Form 1. `\.name\s*\(` — a call through a receiver; the declaration (`fun name(`) and a KDoc
        // mention (`[name]`) do not match.
        test("only the files of KNOWN_COMMITS_ON_LEFT keep their writes on a Left") {
            val callers = sources.filterValues { KEEPING_CALL.containsMatchIn(it) }.keys
            withClue("inRwTransactionKeepingWrites( callers: ${callers.sorted()}") {
                callers shouldBe KNOWN_COMMITS_ON_LEFT.keys
            }
        }

        // Form 2. Comments stripped, `(?<![\w.])` so `Other.SealedText(` and `XSealedText(` don't count.
        test("SealedText( is constructed only under common/privacy") {
            val builders =
                sources
                    .filterValues { CONSTRUCTION.containsMatchIn(withoutComments(it)) }
                    .keys
            builders.filterNot { it.startsWith(GATEWAY) }.shouldBeEmpty()
            builders.any { it.startsWith(GATEWAY) } shouldBe true // the pattern is not matching nothing
        }
    })

/** Form 3 lives in the module that sees the whole repository (e.g. `server/app/src/test`). */
class PortKnowledgeTest :
    FunSpec({
        val sources = repositoryMainSources(File("../.."))

        test("guard: the scan sees the main sources of every module the rule names") {
            withClue("main sources found: ${sources.size}") { (sources.size > MIN_REPO_SOURCES) shouldBe true }
            ALLOWED_ROOTS.keys.forEach { root -> withClue(root) { sources.keys.any { it.startsWith(root) } shouldBe true } }
        }

        test("in the domain only the port's own file names it") {
            sources.filter { (path, text) -> path.startsWith("domain/") && PORT_WORD.containsMatchIn(text) }.keys shouldBe
                setOf(PORT_FILE)
        }

        test("outside the domain only the allowed roots name it, and each of them does") {
            val knowing = sources.filter { (_, text) -> PORT_WORD.containsMatchIn(text) }.keys - PORT_FILE
            withClue("files naming the port: ${knowing.sorted()}") {
                knowing.filterNot { path -> ALLOWED_ROOTS.keys.any { path.startsWith(it) } }.shouldBeEmpty()
                ALLOWED_ROOTS.keys.forEach { root -> knowing.any { it.startsWith(root) } shouldBe true }
            }
        }
    })

private const val MIN_DOMAIN_SOURCES = 100
private const val RUNNER = "common/tx/TransactionRunner.kt"
private val KEEPING_CALL = Regex("""\.inRwTransactionKeepingWrites\s*\(""")

/** File -> what must outlive the refusal. Closed: a new entry is a reviewed decision. */
private val KNOWN_COMMITS_ON_LEFT: Map<String, String> =
    mapOf(
        "identity/auth/SignInWithPin.kt" to "the wrong-PIN counter and the lock, or a PIN is guessed in an evening",
        "identity/auth/RotateRefreshSession.kt" to "the revoked chain of a reused (stolen) refresh token",
    )

private const val GATEWAY = "common/privacy/"
private val CONSTRUCTION = Regex("""(?<![\w.])SealedText\(""")

private val BLOCK_COMMENT = Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL)
private val LINE_COMMENT = Regex("""//[^\n]*""")

private fun withoutComments(source: String): String = source.replace(BLOCK_COMMENT, "").replace(LINE_COMMENT, "")

/** Whole word: `AuditJournalViewModel` is not the port. */
private val PORT_WORD = Regex("""\bAuditJournal\b""")
private const val PORT_FILE = "domain/src/commonMain/kotlin/io/github/myorg/myapp/domain/common/audit/AuditJournal.kt"
private const val MIN_REPO_SOURCES = 500

/** Root -> why it may know the port. */
private val ALLOWED_ROOTS: Map<String, String> =
    mapOf(
        "server/transport/src/main/" to "the route wrapper writes after the answer, in its own transaction",
        "server/infra/src/main/" to "the adapter and the retention sweep",
        "server/di/src/main/" to "wiring",
    )

/** Every `*.kt` under any `src/<set>Main` of the repository, by path from its root; build output skipped. */
private fun repositoryMainSources(start: File): Map<String, String> {
    val root = start.canonicalFile
    return root
        .walkTopDown()
        .onEnter { dir -> dir == root || (dir.name != "build" && !dir.name.startsWith(".")) }
        .filter { it.isFile && it.extension == "kt" }
        .map { it.relativeTo(root).invariantSeparatorsPath to it }
        .filter { (path, _) -> MAIN_SET.containsMatchIn(path) }
        .associate { (path, file) -> path to file.readText() }
}

private val MAIN_SET = Regex("""/src/\w*[mM]ain/""")
