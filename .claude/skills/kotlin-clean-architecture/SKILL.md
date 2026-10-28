---
name: kotlin-clean-architecture
description: Use when designing or auditing the layering of a Kotlin backend or full-stack KMP project — Hexagonal / Clean Architecture with strict module boundaries (domain / api:contract / api:mapping / api:client / features / shells / server:transport / infra / di / app), ports & adapters, bounded contexts as ranked packages with a shared kernel, reverse ports for synchronous cross-context reactions with an ordered effect funnel, a test that forbids upstream imports and package cycles, Unconfigured* adapters for optional external services, and where validation and error mapping live. Apply before ktor-backend, exposed-postgres or any other backend skill.
metadata:
  version: "2.1.0"
  requires: "kotlin-project-layout"
  project-types: "any-kotlin-project-with-business-logic"
---

# Kotlin Clean Architecture

## When to use

- Designing the module structure of a new backend or KMP project.
- Auditing for layer violations (domain importing Ktor, transport with business logic, a screen
  importing another screen, one context reaching "downstream").
- Adding a feature and unsure where a port, use-case or adapter goes.

## Layer model (dependencies point inward)

```
shells (webapp, tgminiapp, androidApp)        ← entrypoint + platform glue only
   ↓
features  (Compose UI + ViewModels)
   ↓
api:client (+ api:client:di) · api:mapping · api:contract
   ↓
domain  (entities, value types, ports, use-cases — pure Kotlin, KMP)
   ↑ calls use-cases            ↑ implements ports
server:transport              server:infra
            ↖   server:di (the only module that knows ports AND adapters)   ↗
                          server:app (entrypoint)
```

**Dependency rule:** inner layers know nothing of outer layers. `domain` never imports
infrastructure; adapters import `domain`. Full allowed/forbidden table →
[`references/module-dependencies.md`](references/module-dependencies.md).

## Per-layer responsibilities

| Layer              | Contains                                                                      | Forbidden                                        |
|--------------------|-------------------------------------------------------------------------------|--------------------------------------------------|
| `domain`           | Entities, value objects, sealed hierarchies, **ports**, use-cases, policies   | Ktor, Exposed, serialization, Koin, any I/O, `java.*` |
| `server:infra`     | Port adapters, Exposed tables, `*Pm` models, jobs, provider clients           | `api:contract` / `api:mapping`                   |
| `server:transport` | Routing: DTO → command, call use-case, fold `Either` → HTTP                    | Any business `if/when`, role checks              |
| `server:di`        | Koin modules binding ports to adapters, one module per context                | Business logic                                   |
| `server:app`       | Entrypoint, config loading, plugin install, background-service lifecycle      | Anything else                                    |
| `features`         | Screens, ViewModels, design system                                            | `server:*`, platform APIs                        |
| shells             | `main()`, Koin start, platform implementations of `features` interfaces       | Screens, direct `domain`/`api:*` use             |

## Bounded contexts are packages, ordered

Inside `domain` (and mirrored in `server:infra` / `server:di`) the first package segment is a
**context**, the second its part: `domain.<context>.<part>`.

```
domain/
  common/        shared kernel: ids, DomainError, OptionalField, Actor, tx/, event/, policy/
  identity/      account · auth · tenant · member
  <core>/        the main aggregates of the product (e.g. order · catalog · booking)
  <rewards>/     what is built on top of the core (e.g. billing · loyalty)
  <review>/      workflows that act on the core (e.g. approval · moderation)
  analytics/     reports · digests
  delivery/      notification · mail · sync
```

- Contexts have a **declared order** (`common → identity → <core> → <rewards> → … →
  delivery`). An import may point to the same context or one *earlier* in the order — never later.
- `common` is the shared kernel: pure values and functions, no ports that belong to one context.
  A type that several contexts *carry* but one context *interprets* (a policy) lives in `common`,
  otherwise the carrier would depend on a context further right.
- No new package cycles *inside* a context.
- **A synchronous reaction across contexts is a reverse port**: the upstream context declares it
  (`TransitionEffect`, `MemberProvisioning`), the downstream one implements it, `server:di` wires
  it. Handlers are `when` without `else`; a funnel of effects is an ordered list whose order is the
  lock order, pinned by a test on the real DI wiring. Defect prevented: a cycle per reaction, or a
  deadlock from a reversed effect order →
  [`references/cross-context-reactions.md`](references/cross-context-reactions.md).
- Context order and cycles are enforced by a test in the domain module (`ContextDependencyTest`,
  template [`architecture-fitness-tests/templates/domain/ContextDependencyTest.kt`](../architecture-fitness-tests/templates/domain/ContextDependencyTest.kt))
  — not by review. Rules that no test checks yet are labelled "enforced on review" in the project's
  module doc, never presented as tested.
- A context becomes its own Gradle module only when build time or ownership forces it.

`api:contract` is organised by API resource, `features` by screen, `server:transport` and
`api:client` are flat — those modules hold no rules, only translation.

## Ports & Adapters

- **Port** = interface in `domain` for what the domain needs: `*Repository`, `*Port`
  (`ReviewerPort`, `SpeechToTextPort`), `*Sender`, `*Hasher`, `TransactionRunner`, `EventOutbox`.
- **Adapter** = implementation in `server:infra`, named by technology: `Exposed*`, `Smtp*`,
  `OpenRouter*`, `Argon2*`, `Disk*`.
- **Optional/paid services get an `Unconfigured*` adapter** that returns a typed, visible
  degradation (`DomainError.Upstream`, capability flag false). A deployment without an API key
  is a valid configuration forever; `server:di` picks the adapter from config. Never inject a
  nullable port and scatter `?.let` through use-cases.
- **Decorators** for cross-cutting concerns on a port (`BudgetedLlm` wraps `OpenRouterLlm`):
  budget, audit, metrics — the use-case never knows.

## Hard rules

- ❌ No `io.ktor.*`, `org.jetbrains.exposed.*`, `kotlinx.serialization.*`, `org.koin.*` in `domain`.
- ❌ No `api:contract` in `server:infra` — the DB model (`*Pm`) and the wire model (DTO) are separate.
- ❌ No business errors as exceptions — `Either<DomainError, T>`.
- ❌ No business logic or authorisation decisions in transport or DI; role checks live in use-cases.
- ❌ No screen importing another screen; navigation crosses the composition root (`app/`).
- ✅ Only `server:di` knows ports and adapters at the same time.
- ✅ Every external service (DB, HTTP, LLM, STT, mail, storage, clock) is reached through a port;
  the clock is injected (`kotlin.time.Clock`), never read from `Clock.System` in a use-case.

## References (load on demand)

- [`references/module-dependencies.md`](references/module-dependencies.md) — per-module allowed/forbidden table and the quick checks.
- [`references/validation-boundaries.md`](references/validation-boundaries.md) — structural (mapping) vs business (use-case) validation.
- [`references/error-handling.md`](references/error-handling.md) — `DomainError` design, adapter exception mapping, transport mapping.
- [`references/cross-context-reactions.md`](references/cross-context-reactions.md) — reverse ports, the ordered effect funnel, kernel policies.

## Apply this skill

Step-by-step → [`USAGE.md`](USAGE.md).

## Verify

Walk through [`checklists/architecture-checklist.md`](checklists/architecture-checklist.md).
