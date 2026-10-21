---
name: architecture-fitness-tests
description: Use when a rule of the codebase (layering, naming, API shape, schema invariants, UI conventions, i18n parity, "only X may do Y") is written in a document or checklist but nothing fails when it is broken — turn it into a fitness test that runs inside ./gradlew build. Covers source-scanning Kotest tests with KNOWN_* ratchets as Map<item, reason> compared by exact equality, vacuity guards with floors, guard/lock/pair triples, capability ownership (who may call, construct or even name a port; perimeters between contexts), closed-set guards (sealedSubclasses == table, when without else, no hand lists), bounded-context order and package cycles (Tarjan), contract conventions (discriminators, snake_case, request naming, client ids on creates, one JSON policy), the HTTP surface pinned from the live Ktor routing tree as a security matrix (auth provider, rate bucket, custom marker nodes like audit=), wire vocabulary and error-code locks, schema invariants via pg_catalog, UI/locale rules, Konsist/ArchUnit trade-offs.
metadata:
  version: "1.1.0"
  requires: "kotlin-project-layout"
  project-types: "any-kotlin-project"
---

# Architecture Fitness Tests

## Why

A rule that lives only in a document is a rule nobody checks. Reviewers (human or AI) look at what
changed, not at everything that *should* have changed; agents follow the nearest example, not the
convention page. A fitness test makes the rule part of the build: it fails the day the violation is
written, names the file, and cannot be argued with.

The cost is low: most rules are a regex over source files, a query over `pg_catalog`, or a walk over
Ktor's routing tree — tens of lines each, running in milliseconds inside `./gradlew build`.

## The ratchet

```kotlin
test("screens speak AppTheme, never MaterialTheme") {
    root.violations(outsideDesignSystem) { "MaterialTheme." in it } shouldBe KNOWN_MATERIAL_THEME_USES.keys
}

/** Only shrinks. Remove an entry in the same change that fixes it. */
private val KNOWN_MATERIAL_THEME_USES: Map<String, String> =
    mapOf("legacy/OldScreen.kt" to "replaced by the new editor in step 14; deleted then")
```

- Compare the **actual set** with the `KNOWN_*` keys by **exact equality**: the test fails on a new
  violation *and* on a fixed one still listed — so the list can only shrink, and it always tells the
  truth.
- Every entry carries its **reason** (`Map<item, reason>`, not a bare list): the reason is what the
  next reviewer weighs "one more" against. A burn-down entry's value can be the stage that retires it.
- Adopt a rule on a messy codebase by listing today's violations; the build then gates *new* ones.
- A list that must never grow (public routes, RLS exemptions, unlimited routes, callers allowed to
  keep writes on a refusal) is **pinned**: adding a name is a decision (ADR), with its reason.
- Prefer a **property** over a list whenever a criterion exists ("a table may skip RLS only if it
  turns an identifier into a tenant") — then the list is the property's current extension.

## Hard rules

- ✅ Every spec starts with a **guard** test: the scanned root exists, holds at least a floor of
  files/routes/tables (`sources.size > 500`), and the pattern finds the allowed owner itself. A wrong
  working directory or a regex that matches nothing otherwise turns every check green.
- ✅ Tests run on the JVM — `jvmTest` in KMP modules (wasm has no `java.io.File`/reflection), plain
  `src/test` in JVM-only server modules; Gradle sets the module directory as the working directory, so
  relative paths are deterministic.
- ✅ Each test's KDoc says **which rule** it pins, **which defect** it prevents and where the rule is
  documented, so the failure message leads to the reasoning.
- ✅ Failure output lists the offenders, both directions where it matters (`unknown: …; vanished: …`).
- ✅ A new fitness test is itself checked **on a deliberately broken input** (introduce a violation,
  see red, revert) — a guard that can't fail is decoration.
- ✅ A protection gets **two tests: "a stranger is refused" and "one's own is not"** (guard / lock /
  pair, [`references/principles.md`](references/principles.md)) — a lock alone ships a door that
  refuses everybody.
- ✅ Pin **surfaces from the artefact** (the live routing tree, `pg_catalog`, `sealedSubclasses`,
  reflection over the classes `api:mapping` imports), not from a list of what we meant to have.
- ✅ A cross-cutting per-route policy leaves a **marker node** in the routing tree and becomes a
  column of `api-surface.txt` — otherwise nothing fails when a new route forgets it.
- ❌ Don't encode taste. Pin rules that prevent a class of defects or keep the codebase navigable
  for the next reader; each should have a documented reason.
- ❌ Don't let exemption lists grow silently; don't add `@Suppress`-style escape hatches to the test.

## Catalogue (templates in `templates/`)

| Area       | Test                          | Pins                                                                                   |
|------------|-------------------------------|----------------------------------------------------------------------------------------|
| Domain     | `ContextDependencyTest`       | imports never point to a later bounded context; no new package cycles inside a context |
| Domain     | `CapabilityOwnershipTest`     | who may **call** `x(`, **construct** `T(`, or **name** a port at all — a closed `Map<file, reason>`; perimeters between contexts |
| Domain     | `ClosedSetTest`               | the classification table == `sealedSubclasses`; the classifier's `when` has no `else`; nobody compares or hand-lists a closed value |
| Contract   | `ContractConventionsTest`     | discriminators, snake_case `@SerialName`, `<Verb><Noun>Request`, no dual-role DTOs, `version` naming, one `Json {}`, list envelopes, creates carry the client's `id`, client interface shape |
| API        | `ApiSurfaceTest`              | the live route table = `api-surface.txt` (`auth=` provider, `rl=` buckets, marker columns); public / device / unlimited routes pinned with reasons; `default` bucket first; kebab-case; one param name per position; no duplicates |
| API        | `MarkerCoverageTest`          | a closed domain list (audit kinds) vs the marker column of the live tree, both ways   |
| API        | `RouteMarker.kt` (transport)  | the transparent `RouteSelector` that makes a per-route policy visible to the two tests above |
| Wire       | `WireVocabularyTest`, `ErrorCodeLiteralTest`, `ErrorReasonTest` | enum values on the wire pinned in `wire-vocabulary.txt`; no literal error code; no screen reads `message` — snippets in [`references/wire-vocabulary.md`](references/wire-vocabulary.md), full recipe in [`format-locks`](../format-locks/SKILL.md) |
| Schema     | `SchemaBaselineTest`          | RLS flags, composite tenant keys and FKs, indexed cascading FKs, no DB-clock timestamps, domains vs repeated CHECKs, journal and column grants, uuid columns are FKs, text columns have ceilings |
| UI         | `UiConventionsTest`           | no `MaterialTheme`/raw M3 in screens, no dp literals, design system is a leaf, no wire strings, no contract DTO in composables, no ViewModel params, one `busy` name, file size |
| UI         | `ScreenSurfaceTest`           | the set of screen entries is pinned; every Entry has a Content; no one-shot Boolean flags |
| UI         | `SheetHeightTest`, `HintRulesTest`, `ShellChromeBoundaryTest` | sheet height named per call; a closed hint list, one per screen; shell chrome stays in the shell — owned by [`compose-design-system`](../compose-design-system/references/wrappers-and-review.md) and [`compose-multiplatform-ui`](../compose-multiplatform-ui/references/shell-chrome.md) |
| i18n       | `ResourceKeysTest`            | every locale has the same keys; no glyphs the bundled font can't draw                  |

References: [`references/principles.md`](references/principles.md) (equality, reasons, guards,
guard/lock/pair, red before the code exists) ·
[`references/source-scanning.md`](references/source-scanning.md) ·
[`references/capability-ownership.md`](references/capability-ownership.md) ·
[`references/closed-sets.md`](references/closed-sets.md) ·
[`references/api-surface.md`](references/api-surface.md) (route-surface pinning, marker nodes) ·
[`references/wire-vocabulary.md`](references/wire-vocabulary.md) ·
[`references/schema-baseline.md`](references/schema-baseline.md) ·
[`references/tools.md`](references/tools.md) (Konsist, ArchUnit, detekt custom rules).

## Related

- [`kotlin-testing-strategy`](../kotlin-testing-strategy/SKILL.md) — behaviour tests over the same
  artefacts (the wall walk over the live routing tree, fault injection, census after erasure).
- [`ktor-api-contract`](../ktor-api-contract/SKILL.md) — the contract rules `ContractConventionsTest` pins.
- [`format-locks`](../format-locks/SKILL.md) — wire vocabulary and stored JSONB shapes pinned by golden files.
- [`the-mine-and-locks`](../the-mine-and-locks/SKILL.md) — naming a step's defect and the locks that prove it absent.

## Apply this skill

Step-by-step → [`USAGE.md`](USAGE.md).

## Verify

Walk through [`checklists/fitness-checklist.md`](checklists/fitness-checklist.md).
