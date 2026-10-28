package com.example.app.android.architecture

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import java.io.File

/*
 * TEMPLATE — app/src/test/kotlin/.../architecture/PaymentSdkIsolationTest.kt.
 *
 * Put it in the SHARED `src/test` source set (not `src/test<Store>`): then it runs in EVERY flavor's
 * unit tests (`testPlayDebugUnitTest`, `testRustoreDebugUnitTest`, …) and a stray reference is caught
 * by whichever flavor CI builds. The scan is textual over all source sets, so it sees `src/main` even
 * when the store flavor is not the one being compiled.
 *
 * Evidence: pws-android app-compose/src/test/.../architecture/PaymentSdkIsolationTest.kt
 * (`\bru\.rustore\.` allowed only under app-compose/src/rustore/ and app-compose/src/testRustore/).
 */

/**
 * The store SDK's root package — the ONE thing to change per project. Real values:
 * RuStore `ru.rustore.` · Google Play Billing `com.android.billingclient.` · Huawei IAP
 * `com.huawei.hms.iap.`. The placeholder below matches nothing on purpose: the guard test fails until
 * it is set, instead of the rule passing vacuously.
 */
private const val STORE_SDK_PACKAGE_PREFIX = "com.example.storesdk."

/** Source sets allowed to mention the SDK (paths relative to the repository root). */
private val ALLOWED_PREFIXES: Map<String, String> =
    mapOf(
        "app/src/rustore/" to "the store flavor: the adapter and its coroutine bridge",
        "app/src/testRustore/" to "the store flavor's tests",
    )

/** Vacuity floor: roughly today's count of scanned files, rounded down. A wrong cwd or dir scans ~0. */
private const val MIN_SCANNED_FILES = 20

/** Where to scan, relative to the repository root. Include every module that could grow a dependency. */
private val SCANNED_DIRS = listOf("app/src", "data")

/**
 * Rule: the store SDK package is referenced only in the store flavor's source sets.
 * Why: the SDK ships only in that flavor (`"<store>Implementation"`); a reference from `main` or another
 * flavor breaks those builds, or — if someone "fixes" that by moving the dependency to `implementation`
 * — drags the SDK, its keep rules and its manifest auto-init into APKs that must not carry it.
 *
 * See it red: add `private val sdk = "<prefix>Foo"` to any file in app/src/main (a string literal is
 * enough — the scan is textual and literals are kept), run `./gradlew :app:testPlayDebugUnitTest`,
 * then revert.
 */
class PaymentSdkIsolationTest :
    FunSpec({
        val all = scanFiles(SCANNED_DIRS)
        val sdkRef = Regex("""\b""" + Regex.escape(STORE_SDK_PACKAGE_PREFIX))

        test("guard: the scan finds the SDK where it is allowed") {
            all.size shouldBeGreaterThanOrEqual MIN_SCANNED_FILES
            all.filter { sdkRef.containsMatchIn(it.code) }.shouldNotBeEmpty()
            ALLOWED_PREFIXES.keys.forEach { prefix ->
                withClue("allowed root $prefix holds no file — stale entry?") { all.any { it.path.startsWith(prefix) } shouldBe true }
            }
        }

        test("the store SDK is referenced only in the store flavor's source sets") {
            val actual =
                all
                    .filter { file -> sdkRef.containsMatchIn(file.code) && ALLOWED_PREFIXES.keys.none { file.path.startsWith(it) } }
                    .map { it.path }
                    .sorted()
            withClue("SDK references outside ${ALLOWED_PREFIXES.keys}:\n${actual.joinToString("\n")}") { actual shouldBe emptyList() }
        }
    })

// ---- scanning helpers (move to a shared ScannedFile.kt once a second source test needs them) ----

private class ScannedFile(val path: String, text: String) {
    /** Comments stripped (a rule must not fire on prose); string and char literals kept intact. */
    val code: String = LEXEME.replace(text) { if (it.value.startsWith("/")) " " else it.value }
}

/** Raw string, string, char literal, block comment, line comment — in priority order. */
private val LEXEME =
    Regex(
        listOf(
            "\"\"\"[\\s\\S]*?\"\"\"",
            "\"(?:\\\\.|[^\"\\\\\\n])*\"",
            "'(?:\\\\.|[^'\\\\\\n])'",
            "/\\*[\\s\\S]*?\\*/",
            "//[^\\n]*",
        ).joinToString("|"),
    )

private val SKIPPED_DIRS = setOf("build", ".gradle", ".kotlin", ".idea", ".git", "node_modules")

/** Gradle runs unit tests with the module directory as cwd, so the repository root is `..`. */
private val REPO_ROOT: File =
    File("..").canonicalFile.also {
        require(File(it, "settings.gradle.kts").isFile) { "Not the repository root: ${it.absolutePath}" }
    }

private fun scanFiles(dirs: List<String>): List<ScannedFile> =
    dirs.flatMap { dir ->
        val root = File(REPO_ROOT, dir)
        require(root.isDirectory) { "Source root not found: ${root.absolutePath}" }
        root
            .walkTopDown()
            .onEnter { it.name !in SKIPPED_DIRS }
            .filter { it.isFile && it.extension == "kt" }
            .map { ScannedFile(it.relativeTo(REPO_ROOT).invariantSeparatorsPath, it.readText()) }
            .toList()
    }
