package io.github.alelk.pws.android.compose.architecture

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

/**
 * Rule (G13, AGENTS.md section 8 "Build / R8"): no `-keep class <package>.** { *; }` and no
 * `-keep class **` in any `app-compose/proguard-rules*.pro`.
 * Why: such a rule disables shrinking and obfuscation for a whole package tree (the DEX once grew to
 * 36.8 MB and Google Play rated the app "App optimization: Low"). Keep rules are targeted and commented.
 * A rule with `allowshrinking` is not flagged: it still lets R8 drop unused classes.
 *
 * See it red: add `-keep class com.example.** { *; }` or `-keep class ** { *; }` to
 * `proguard-rules.pro` and run `./gradlew :app-compose:testRuDebugUnitTest`, then revert.
 * Removing the (deliberate) `net.zetetic.database.**` keep without clearing KNOWN is red too.
 */
class ProguardRulesTest :
  FunSpec({
    val files =
      scanFiles("app-compose", extensions = setOf("pro"))
        .filter { it.relativePath.matches(Regex("""app-compose/proguard-rules[^/]*\.pro""")) }

    test("scan sees both proguard files") {
      files.map { it.relativePath } shouldContainAll
        listOf("app-compose/proguard-rules-rustore.pro", "app-compose/proguard-rules.pro")
    }

    test("no wildcard-package -keep rules") {
      val actual =
        files
          .flatMap { file ->
            val rules = file.text.lines().joinToString("\n") { it.substringBefore('#') }
            KEEP_DIRECTIVE.findAll(rules).mapNotNull { m ->
              val modifiers = m.groupValues[1]
              val target = m.groupValues[2]
              val wildcard = target.endsWith("**")
              if (wildcard && "allowshrinking" !in modifiers) {
                val members = m.groupValues[3].trim().replace(WHITESPACE, " ")
                "${file.relativePath.substringAfterLast('/')}: -keep class $target $members".trim()
              } else {
                null
              }
            }
          }.distinct()
          .sorted()
      withClue("Actual violations:\n${actual.joinToString("\n")}") { actual shouldBe KNOWN_WIDE_KEEPS }
    }
  })

private val WHITESPACE = Regex("""\s+""")

/** `-keep[,modifiers] class <target> [{ members }]`; `-keepnames`, `-keepclassmembers` etc. do not match. */
private val KEEP_DIRECTIVE =
  Regex("""-keep((?:,\w+)*)\s+(?:@\S+\s+)?(?:public\s+)?(?:class|interface)\s+(\S+)([^-]*)""")

/** Only shrinks. The one deliberate wide keep: sqlcipher's native code looks these classes up by name via JNI. */
private val KNOWN_WIDE_KEEPS: List<String> =
  listOf(
    "proguard-rules.pro: -keep class net.zetetic.database.** { *; }",
  )
