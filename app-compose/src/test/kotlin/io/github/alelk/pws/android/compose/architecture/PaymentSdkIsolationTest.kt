package io.github.alelk.pws.android.compose.architecture

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe

/**
 * Rule: the RuStore SDK (`ru.rustore.*`) is referenced only under `app-compose/src/rustore/` and
 * `app-compose/src/testRustore/`.
 * Why: the payment SDK ships only in the `rustore` flavor; a reference from `main` (or any other flavor)
 * breaks the other flavors' builds and drags the SDK and its keep rules into APKs that must not carry it.
 *
 * See it red: add `private val sdk = "ru.rustore.sdk.Foo"` to any file in `app-compose/src/main` (the scan
 * is textual, so a string literal is enough and the file still compiles) and run
 * `./gradlew :app-compose:testRuDebugUnitTest`, then revert.
 */
class PaymentSdkIsolationTest :
  FunSpec({
    val all = scanFiles("app-compose/src", "data")
    val sdkRef = Regex("""\bru\.rustore\.""")

    test("scan finds the SDK where it is allowed (guard)") {
      all.filter { sdkRef.containsMatchIn(it.code) }.shouldNotBeEmpty()
    }

    test("RuStore SDK is referenced only in the rustore flavor") {
      val actual =
        all.violations { file ->
          sdkRef.containsMatchIn(file.code) &&
            ALLOWED_PREFIXES.none { file.relativePath.startsWith(it) }
        }
      withClue("Actual violations:\n${actual.joinToString("\n")}") { actual shouldBe emptyList() }
    }
  })

private val ALLOWED_PREFIXES = listOf("app-compose/src/rustore/", "app-compose/src/testRustore/")
