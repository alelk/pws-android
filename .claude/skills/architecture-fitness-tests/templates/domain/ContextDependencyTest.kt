package io.github.myorg.myapp.domain.architecture

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.File

/**
 * Guards the bounded-context order: an import never points to a context further downstream
 * (later in ORDER), and no new package cycle appears inside a context. Both KNOWN lists may only
 * shrink. Edit ORDER to your contexts; document the order where the contexts are described.
 */
class ContextDependencyTest :
    FunSpec({
        val sources = domainSources()

        test("imports never point to a context further downstream") {
            val actual =
                sources
                    .flatMap { file ->
                        file.imports
                            .filter { rank(contextOf(it)) > rank(contextOf(file.pkg)) }
                            .map { "${file.relativePath} -> ${unitOf(it)}" }
                    }.toSortedSet()
            withClue("Actual violations:\n${actual.joinToString("\n")}") {
                actual shouldBe KNOWN_VIOLATIONS.toSortedSet()
            }
        }

        test("no new package cycles inside a context") {
            val actual = cyclesInsideContexts(sources)
            withClue("Actual cycles:\n${actual.joinToString("\n")}") {
                actual shouldBe KNOWN_CYCLES.toSortedSet()
            }
        }
    })

/** Packages are nested under contexts: a unit is <context>.<part>. */
private const val LAYOUT_DEPTH = 2

private const val ROOT = "src/commonMain/kotlin/io/github/myorg/myapp/domain"
private const val PREFIX = "io.github.myorg.myapp.domain."

private val ORDER = listOf("common", "identity", "planning", "motivation", "acceptance", "analytics", "delivery")

private val KNOWN_VIOLATIONS: Set<String> = emptySet()

private val KNOWN_CYCLES: Set<String> =
    setOf(
        // "planning.task ↔ planning.time",   // today's cycles, pinned; remove as they are broken
    )

private data class SourceFile(
    val relativePath: String,
    val pkg: String,
    val imports: Set<String>,
)

private val PACKAGE_LINE = Regex("""^package\s+${Regex.escape(PREFIX)}([\w.]+)""", RegexOption.MULTILINE)
private val IMPORT_LINE = Regex("""^import\s+${Regex.escape(PREFIX)}([\w.]+)""", RegexOption.MULTILINE)

private fun domainSources(): List<SourceFile> {
    val root = File(ROOT)
    require(root.isDirectory) { "Run from the :domain module directory; not found: ${root.absolutePath}" }
    return root
        .walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .map { file ->
            val text = file.readText()
            SourceFile(
                relativePath = file.relativeTo(root).invariantSeparatorsPath,
                pkg = PACKAGE_LINE.find(text)?.groupValues?.get(1) ?: error("no package in $file"),
                imports = IMPORT_LINE.findAll(text).map { it.groupValues[1] }.toSet(),
            )
        }.toList()
}

/** The package part of a package or import path: segments before the first capitalised one. */
private fun packageSegments(path: String): List<String> = path.split('.').takeWhile { it.first().isLowerCase() }

private fun unitOf(path: String): String = packageSegments(path).take(LAYOUT_DEPTH).joinToString(".")

private fun contextOf(path: String): String = packageSegments(path).first()

private fun rank(context: String): Int = ORDER.indexOf(context).also { require(it >= 0) { "unknown context $context" } }

private fun cyclesInsideContexts(sources: List<SourceFile>): Set<String> {
    val edges = mutableMapOf<String, MutableSet<String>>()
    for (file in sources) {
        val from = unitOf(file.pkg)
        for (import in file.imports) {
            val to = unitOf(import)
            if (to != from && contextOf(import) == contextOf(file.pkg)) {
                edges.getOrPut(from) { mutableSetOf() }.add(to)
            }
        }
    }
    return stronglyConnected(edges)
        .filter { it.size > 1 }
        .map { component -> component.sorted().joinToString(" ↔ ") }
        .toSortedSet()
}

/** Tarjan's algorithm over the unit graph. */
private fun stronglyConnected(edges: Map<String, Set<String>>): List<Set<String>> {
    val nodes = (edges.keys + edges.values.flatten()).sorted()
    val index = mutableMapOf<String, Int>()
    val low = mutableMapOf<String, Int>()
    val stack = ArrayDeque<String>()
    val onStack = mutableSetOf<String>()
    val result = mutableListOf<Set<String>>()
    var counter = 0

    fun visit(node: String) {
        index[node] = counter
        low[node] = counter
        counter++
        stack.addLast(node)
        onStack += node
        for (next in edges[node].orEmpty()) {
            if (next !in index) {
                visit(next)
                low[node] = minOf(low.getValue(node), low.getValue(next))
            } else if (next in onStack) {
                low[node] = minOf(low.getValue(node), index.getValue(next))
            }
        }
        if (low[node] == index[node]) {
            val component = mutableSetOf<String>()
            do {
                val member = stack.removeLast()
                onStack -= member
                component += member
            } while (member != node)
            result += component
        }
    }

    nodes.filter { it !in index }.forEach(::visit)
    return result
}
