# Static Analysis: Detekt + ktlint

Both are applied by the convention plugins (never by the root script) and run inside
`./gradlew build`.

## Wiring (inside `app.kmp` / `app.jvm`)

```kotlin
detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("detekt.yml"))
    // KMP: per-target source sets. Tests are included on purpose.
    source.setFrom(
        "src/commonMain/kotlin", "src/jvmMain/kotlin", "src/wasmJsMain/kotlin",
        "src/commonTest/kotlin", "src/jvmTest/kotlin", "src/wasmJsTest/kotlin",
    )
    // Findings that pre-date the module's baseline stay recorded, so the gate blocks NEW debt
    // instead of granting a blanket amnesty. A missing file = empty baseline.
    baseline = project.file("detekt-baseline.xml")
}

extensions.configure<KtlintExtension> {
    baseline.set(project.file("ktlint-baseline.xml"))
    filter {
        // Compose resource accessors are generated; Gradle's output dir may live outside the
        // project, so match both patterns.
        exclude { it.file.path.contains("/generated/compose/") || it.file.path.contains("/build/generated/") }
    }
}
```

## `detekt.yml` — project profile

See [`../templates/shared/detekt.yml`](../templates/shared/detekt.yml). Principles:

- `LongParameterList.functionThreshold: 8` — use-cases take several ports.
- `MagicNumber` off — value classes and named constants carry meaning.
- `FunctionNaming` off — `operator fun invoke` and Compose `PascalCase` functions.
- `ReturnCount.excludeGuardClauses: true` — guard clauses are the clearest precondition; counting
  them pushes code towards nested `if`s.
- Test *support* (`fixtures/`, `fakes/`, `Fake*.kt`) is excluded from `LongParameterList`,
  `ReturnCount`, `MatchingDeclarationName` — object mothers take one parameter per field on
  purpose. Real test logic is still analysed.

## Commands

```bash
./gradlew build            # includes detekt + ktlintCheck
./gradlew ktlintFormat     # auto-format before committing
./gradlew :module:detektBaseline   # ONLY when adopting detekt on an existing module
```

## Rules

- A `@Suppress` in source must carry a one-line reason (`// exhaustive map over a sealed type`).
- Baselines only shrink. Regenerating one to absorb new findings is hiding debt.
- Detekt 1.23.x analyses PSI without full type resolution, so it keeps working on newer Kotlin
  compilers; check release notes before moving to a new major (config keys change).
