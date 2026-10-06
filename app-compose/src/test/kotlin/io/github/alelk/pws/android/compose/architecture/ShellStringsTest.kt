package io.github.alelk.pws.android.compose.architecture

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Rule: no `Toast.makeText(context, "literal", ...)` in `app-compose/src/main`; user-visible text of the
 * shell comes from string resources.
 * Why: a hardcoded literal is never translated (the app ships en/pl/ru/uk) and cannot be reviewed with
 * the other strings. Ratchet entries are `file:count`, so a new literal in an already listed file is red.
 *
 * See it red: add `Toast.makeText(context, "Hello", Toast.LENGTH_SHORT).show()` to any file in
 * `app-compose/src/main` and run `./gradlew :app-compose:testRuDebugUnitTest`, then revert.
 * Fixing a KNOWN literal without updating the count is red too.
 */
class ShellStringsTest :
  FunSpec({
    val sources = scanFiles("app-compose/src/main")

    test("shell toasts use string resources") {
      val actual =
        sources
          .mapNotNull { file ->
            val count = LITERAL_TOAST.findAll(file.code).count()
            if (count > 0) "${file.relativePath.removePrefix(SHELL_ROOT)}:$count" else null
          }.sorted()
      withClue("Actual violations:\n${actual.joinToString("\n")}") { actual shouldBe KNOWN_LITERAL_TOASTS }
    }
  })

private const val SHELL_ROOT = "app-compose/src/main/kotlin/io/github/alelk/pws/android/compose/"

private val LITERAL_TOAST = Regex("""Toast\s*\.\s*makeText\(\s*[^,]+,\s*"""")

/** Only shrinks (G10). `file:count` of hardcoded toast texts. */
private val KNOWN_LITERAL_TOASTS: List<String> = listOf("MainActivity.kt:7")
