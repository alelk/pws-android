package com.example.app.architecture

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import java.io.File

/**
 * Rule (AGENTS.md "Build / R8"): no `-keep class <package>.** { *; }`, no `-keep class ** { *; }`
 * and no `-keep class <package>.* …` in any `app/proguard-rules*.pro`.
 *
 * Why: such a rule forbids R8 to shrink, obfuscate and optimize a whole package tree. On a real app
 * eleven of them held the DEX at 36.8 MB and Play rated "App optimization: Low" (18/18/19%).
 * Keep rules are targeted (a named class/member, `-keepnames`, `-keepclassmembers`, `-if`) and
 * commented. A `-keep,allowshrinking` is not flagged: R8 may still drop unused classes.
 *
 * The everyday gate (`./gradlew build`) never runs R8 when release signing is absent; this test is the
 * cheap lock that does run there. The DEX-size gate in the release job is the artifact lock.
 *
 * See it red: add `-keep class com.example.** { *; }` to `proguard-rules.pro`, run
 * `./gradlew :app:testFreeDebugUnitTest`, then revert. Deleting a KNOWN rule without clearing the
 * entry is red too (the list only shrinks and always tells the truth).
 */
class ProguardRulesTest :
  FunSpec({
    // Unit tests run with the module directory as working directory.
    val moduleDir = File(".").canonicalFile
    val ruleFiles =
      moduleDir
        .listFiles { f -> f.isFile && f.name.matches(RULE_FILE) }
        .orEmpty()
        .sortedBy { it.name }

    test("guard: the scan sees every rule file") {
      // A wrong working directory or a renamed file must not turn the check vacuously green.
      ruleFiles.map { it.name } shouldContainAll EXPECTED_RULE_FILES
    }

    test("pair: the detector flags a wildcard keep and spares targeted rules") {
      wideKeeps("x.pro", WIDE_SAMPLE) shouldBe
        listOf(
          "x.pro: -keep class com.example.** { *; }",
          "x.pro: -keep class com.other.* { *; }",
          "x.pro: -keep,allowobfuscation class ** { *; }",
        )
      wideKeeps("x.pro", TARGETED_SAMPLE) shouldBe emptyList()
    }

    test("no wildcard-package -keep rules beyond the known, justified ones") {
      val actual = ruleFiles.flatMap { wideKeeps(it.name, it.readText()) }.distinct().sorted()
      withClue(
        "unknown: ${actual - KNOWN_WIDE_KEEPS.keys}\nvanished: ${KNOWN_WIDE_KEEPS.keys - actual.toSet()}",
      ) {
        actual.toSet() shouldBe KNOWN_WIDE_KEEPS.keys
      }
    }
  })

/** `proguard-rules.pro`, `proguard-rules-<flavor>.pro`. Add consumer-rules.pro of library modules if any. */
private val RULE_FILE = Regex("""proguard-rules[^/]*\.pro""")

private val EXPECTED_RULE_FILES = listOf("proguard-rules.pro", "proguard-rules-storeB.pro")

/**
 * Only shrinks. Key = normalized rule as reported, value = why it must stay wide.
 * Remove an entry in the same change that narrows or deletes the rule.
 */
private val KNOWN_WIDE_KEEPS: Map<String, String> =
  mapOf(
    "proguard-rules.pro: -keep class com.example.nativelib.** { *; }" to
      "JNI: the native library calls these classes and methods by name",
  )

/**
 * `-keep[,modifiers] [@Annotation] [public] class|interface <target> [extends|implements X] [{ members }]`.
 * `-keepnames`, `-keepclassmembers`, `-keepclasseswithmembers` do not match (`-keep` must be followed by
 * `,` or whitespace). Group 1 = modifiers, 2 = target, 3 = rest up to the next directive.
 */
private val KEEP_DIRECTIVE =
  Regex("""-keep((?:,\w+)*)\s+(?:@\S+\s+)?(?:public\s+)?(?:class|interface|enum)\s+(\S+)([^-]*)""")

private val WHITESPACE = Regex("""\s+""")
private val NARROWING = Regex("""^\s*(extends|implements)\b""")

/** Wide `-keep` rules of one file, normalized as `<file>: -keep[,mods] class <target> <members>`. */
private fun wideKeeps(fileName: String, text: String): List<String> {
  val rules = text.lines().joinToString("\n") { it.substringBefore('#') } // comments are prose
  return KEEP_DIRECTIVE.findAll(rules).mapNotNull { m ->
    val modifiers = m.groupValues[1]
    val target = m.groupValues[2]
    val rest = m.groupValues[3]
    val packageWildcard = target.endsWith(".**") || target.endsWith(".*")
    // `*`/`**` alone is wide unless narrowed by extends/implements (e.g. `* extends RoomDatabase`).
    val bareWildcard = (target == "**" || target == "*") && !NARROWING.containsMatchIn(rest)
    if ((packageWildcard || bareWildcard) && "allowshrinking" !in modifiers) {
      val members = rest.trim().replace(WHITESPACE, " ")
      "$fileName: -keep$modifiers class $target $members".trim()
    } else {
      null
    }
  }.toList()
}

private val WIDE_SAMPLE =
  """
  -keep class com.example.** { *; }
  -keep class com.other.* { *; }
  -keep,allowobfuscation class ** { *; }
  """.trimIndent()

private val TARGETED_SAMPLE =
  """
  # -keep class commented.out.** { *; }
  -keep class * extends androidx.room.RoomDatabase { <init>(); }
  -keep,allowshrinking class com.example.dto.** { *; }
  -keepnames class * implements com.example.app.navigation.Screen
  -keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { <fields>; }
  -keep class com.example.app.NativeBridge { native <methods>; }
  """.trimIndent()
