# Usage — Architecture Fitness Tests

## 1. Find the rules nobody checks

Read the architecture docs, conventions, checklists and ADRs; for each "must/never" ask: *what
fails today if someone breaks it?* If the answer is "a reviewer might notice", it is a candidate.
Rank by the cost of the defect it prevents (data leak > broken contract > inconsistent naming).

## 2. Pick the cheapest honest check

| The rule is about…                          | Check against                                  | Template                     |
|---------------------------------------------|------------------------------------------------|------------------------------|
| imports / package structure / file content  | source files (regex, or Konsist)               | `ContextDependencyTest`, `UiConventionsTest` |
| the HTTP surface, per-route policies        | the **live** Ktor routing tree (+ marker nodes) | `ApiSurfaceTest`, `MarkerCoverageTest` |
| database structure                          | `pg_catalog` / `information_schema` on Testcontainers | `SchemaBaselineTest`  |
| resources                                   | resource files                                 | `ResourceKeysTest`           |
| "only X may call / build / name Y"          | source files, X as a closed `Map<file, reason>` | `CapabilityOwnershipTest`   |
| "every variant is classified"               | `sealedSubclasses` / `entries` + the classifier's source | `ClosedSetTest`     |
| enum values / error codes on the wire       | reflection over `api:mapping` imports; transport/UI source | `wire-vocabulary.md` (+ [`format-locks`](../format-locks/SKILL.md)) |

Prefer the artefact that *is* the truth (running routes, the real schema) over a list of handlers
or migration text.

## 3. Write it as a ratchet

```bash
cp <skill>/templates/domain/{ContextDependencyTest,CapabilityOwnershipTest,ClosedSetTest}.kt \
                                                        domain/src/jvmTest/kotlin/<pkg>/domain/architecture/
cp <skill>/templates/contract/ContractConventionsTest.kt api/contract/src/jvmTest/kotlin/<pkg>/contract/
cp <skill>/templates/server/{ApiSurfaceTest,MarkerCoverageTest}.kt server/app/src/test/kotlin/<pkg>/app/
cp <skill>/templates/server/RouteMarker.kt              server/transport/src/main/kotlin/<pkg>/server/transport/
cp <skill>/templates/server/SchemaBaselineTest.kt       server/infra/src/test/kotlin/<pkg>/infra/
cp <skill>/templates/features/*.kt                      features/src/jvmTest/kotlin/<pkg>/features/conventions/
```

Start each spec with its guard test (root found, floor reached, the allowed owner matched). Run it
once, paste today's violations into `KNOWN_*` **with a reason each** (`Map<item, reason>`), commit.
From now on the build gates new ones.

```kotlin
test("A3 — sizes come from AppTheme.spacing / sizing") {
    root.violations(outsideDesignSystem) { DP_LITERAL.containsMatchIn(it) } shouldBe KNOWN_DP_LITERALS
}
```

## 4. Prove it can fail

Introduce a violation (a `MaterialTheme.` in a screen, a table without RLS, a route outside
`authenticate`, an `else ->` in a classifier, a call of a guarded function from a new file), run the
test, see it red with a useful message, revert. For a protection, also write its **pair**: the
allowed owner / own role still passes.

## 5. Burn down

Fix listed violations in small changes; each removes its entry from `KNOWN_*` in the same commit.
A plan step can own "close KNOWN_X" as its acceptance criterion.

## 6. Keep them where agents see them

Mention the fitness tests in `AGENTS.md`/`CLAUDE.md` ("the build pins the API surface; a new route
means updating `api-surface.txt` deliberately") and in each relevant skill's checklist. An agent
that hits a red fitness test should update the code, not the list — say so explicitly.

```bash
./gradlew build
```

Then walk through [`checklists/fitness-checklist.md`](checklists/fitness-checklist.md).
