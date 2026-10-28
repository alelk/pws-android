# Tools: regex, Konsist, ArchUnit, Detekt rules

| Tool                 | Best for                                                            | Trade-off                                           |
|----------------------|---------------------------------------------------------------------|-----------------------------------------------------|
| Regex over `File`s   | imports, forbidden calls, literals, file sizes, resource files      | zero deps, instant; blind to syntax (comments, strings) |
| **Konsist**          | Kotlin structure: "classes named `*ViewModel` have no Compose imports", "every use-case has one public `invoke`", "commands live in the use-case's file", "value classes for ids" | real Kotlin model (PSI), readable DSL; one more test dependency; JVM test only |
| ArchUnit             | JVM bytecode dependency rules (layer cycles across modules)          | bytecode, not source; Kotlin top-level functions and inline code are awkward |
| Detekt custom rule   | a rule that should also show up in the IDE as you type               | more ceremony (rule set module), but reaches every contributor immediately |
| `pg_catalog` queries | database structure                                                  | needs the Testcontainers DB (already there)         |
| Live routing tree    | HTTP surface                                                        | Ktor-internal `toString()` format                   |

Recommendation: start with regex (it covers most rules in a few lines); move a rule to **Konsist**
when the regex needs to understand declarations (signatures, class members, annotations on
parameters). Example:

```kotlin
// jvmTest, dependency: com.lemonappdev:konsist — illustrative; check the current DSL before copying
class KonsistConventionsTest : FunSpec({
    test("ViewModels extend ViewModel and live in features") {
        Konsist.scopeFromProject()
            .classes()
            .withNameEndingWith("ViewModel")
            .assertTrue { it.hasParentWithName("ViewModel") && it.resideInPackage("..features..") }
    }
    test("domain files import no java.*") {
        Konsist.scopeFromProduction("domain")
            .files
            .assertFalse { file -> file.hasImport { it.name.startsWith("java.") } }
    }
})
```

The DSL evolves between releases — verify names against the Konsist docs for the version you pin.
