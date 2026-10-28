# Source-scanning tests

## Helpers (copy into each test file or a small shared test util)

```kotlin
private const val UI_SOURCE_ROOT = "src/commonMain/kotlin/io/github/myorg/myapp/features"

private fun File.ktFiles(): List<File> = walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
private fun File.relative(): String = invariantSeparatorsPath.substringAfter("$UI_SOURCE_ROOT/")

private fun File.violations(where: (String) -> Boolean, hit: (String) -> Boolean): List<String> =
    ktFiles().map { it.relative() to it.readText() }
        .filter { (path, text) -> where(path) && hit(text) }
        .map { it.first }.sorted()
```

## Regex you'll reuse

| Rule                                   | Pattern                                                                     |
|----------------------------------------|-----------------------------------------------------------------------------|
| no `MaterialTheme` in screens          | `"MaterialTheme."` outside `designsystem/`                                  |
| no raw M3 components in screens        | `^import androidx\.compose\.material3\.(Text\|Button\|Card\|…)$`            |
| no dp literals in screens              | `\b\d+(\.\d+)?\.dp\b` outside `designsystem/` and `components/`             |
| no ViewModel parameter                 | `^\s+\w+:\s+\w*ViewModel[,)]`                                               |
| no one-shot flags in state             | `^\s+val (done\|sent\|saved\|success\|completed\|navigated):\s+Boolean`     |
| no wire strings in UI                  | `"(Pending\|InProgress\|Accepted\|…)"`                                      |
| no contract DTO in composables         | file contains `@Composable` and `import <pkg>.contract.`                    |
| sealed DTO has a discriminator         | `sealed (class\|interface) X` without `@JsonClassDiscriminator("type")` in the 3 lines above |
| one JSON policy                        | files containing `Json {` across contract, client, transport = exactly one   |
| file too long                          | `readLines().size > 600` (pinned exceptions with reasons)                   |
| a call, not the declaration            | `\.name\s*\(` (through a receiver); top-level: `(?<![\w.])name\s*\(`       |
| a construction                         | `(?<![\w.])Type\(` on comment-stripped text                                 |
| a name, as a whole word                | `\bPort\b` (`PortViewModel` is not the port)                                |
| no error code literal                  | `ApiErrorDto\(\s*"` in the transport                                       |
| no comparison of a closed value        | `[!=]=\s*Period\.`; no hand list: `listOf\(\s*Period\.\w+\s*[,)]`         |

Regex over source is pragmatic and fast; it has false positives on comments/strings. Keep patterns
anchored (`^import …$`), strip KDoc/line comments when scanning literals, and document the known
false positive in the `KNOWN_*` list with its reason instead of weakening the pattern.

## Bounded-context order (domain)

```kotlin
private val ORDER = listOf("common", "identity", "planning", "motivation", "acceptance", "analytics", "delivery")
// package = <prefix>.<context>.<part>...; rank(import context) must be <= rank(file context)
```

Plus package cycles inside a context: build the graph `unit(<context>.<part>) → unit(import)`,
run Tarjan's SCC, report components of size > 1; pin today's cycles in `KNOWN_CYCLES` (shrinks only).
Template: [`../templates/domain/ContextDependencyTest.kt`](../templates/domain/ContextDependencyTest.kt).

## "Only X may do Y"

```kotlin
test("only the files of KNOWN_COMMITS_ON_LEFT keep their writes on a Left") {
    sources.filterValues { Regex("""\.inRwTransactionKeepingWrites\s*\(""").containsMatchIn(it) }.keys shouldBe
        KNOWN_COMMITS_ON_LEFT.keys
}
```

Use it for capabilities with an invariant attached (a lock, an audit write, a sealing step): the
capability is legal only inside the components that uphold it. Callers, construction, knowledge of
a port, perimeters between contexts → [`capability-ownership.md`](capability-ownership.md).
Exhaustiveness of sealed sets (`when` without `else`, no hand lists) → [`closed-sets.md`](closed-sets.md).

## Pinned surfaces from source

`ScreenSurfaceTest` collects `fun (\w+Entry)\(` across the UI module and compares with a pinned
sorted list; a new screen must be added deliberately (it forces "is it in navigation? in the docs?").
