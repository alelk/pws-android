package io.github.alelk.pws.android.compose.architecture

import java.io.File

/**
 * Source-scanning helpers of the fitness tests (pure JVM: no Robolectric, no Android classes).
 * Gradle runs the unit tests with the module directory (`app-compose`) as the working directory, so
 * the repository root is `..`; all paths below are relative to the repository root.
 */
internal class ScannedFile(val relativePath: String, val text: String) {
  /**
   * Source with KDoc, block and line comments removed (a rule must not fire on prose). String and char
   * literals are kept intact, so a literal such as `"* / *"` or `"http://x"` is never mistaken for a comment.
   */
  val code: String by lazy { LEXEME.replace(text) { if (it.value.startsWith("/")) " " else it.value } }
}

/** Alternatives in priority order: raw string, string, char literal, block comment, line comment. */
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

private val SKIPPED_DIRS =
  setOf("build", ".gradle", ".kotlin", ".idea", ".git", "output", "local-repo", "kotlin-js-store", "node_modules")

/** Repository root (`pws-android`), verified so that a wrong working directory fails loudly. */
internal val REPO_ROOT: File =
  File("..").canonicalFile.also {
    require(File(it, "settings.gradle.kts").isFile && File(it, "app-compose").isDirectory) {
      "Not the pws-android root: ${it.absolutePath} (tests must run with the app-compose directory as cwd)"
    }
  }

/**
 * All files with one of [extensions] under [dirs] (relative to [REPO_ROOT]), with paths relative to
 * [REPO_ROOT]. Non-vacuity guard: every directory must exist and the result must not be empty.
 */
internal fun scanFiles(vararg dirs: String, extensions: Set<String> = setOf("kt")): List<ScannedFile> {
  val files =
    dirs.flatMap { dir ->
      val root = File(REPO_ROOT, dir)
      require(root.isDirectory) { "Source root not found: ${root.absolutePath}" }
      root
        .walkTopDown()
        .onEnter { it.name !in SKIPPED_DIRS }
        .filter { it.isFile && it.extension in extensions }
        .map { ScannedFile(it.relativeTo(REPO_ROOT).invariantSeparatorsPath, it.readText()) }
        .toList()
    }
  require(files.isNotEmpty()) { "No files under ${dirs.toList()}: the scan would pass vacuously" }
  return files
}

/** A rule predicate over one scanned file. */
internal typealias Hit = (ScannedFile) -> Boolean

/** Sorted, distinct relative paths of files for which [hit] is true. */
internal fun List<ScannedFile>.violations(hit: Hit) = filter(hit).map { it.relativePath }.distinct().sorted()
