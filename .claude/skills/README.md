# AI Skills Catalogue

A library of reusable engineering skills for Kotlin / Gradle / Ktor / PostgreSQL / Compose
Multiplatform products built with AI coding agents, in the **Agent Skills** format
(machine-discoverable by Claude Code and other skill-aware agents).

Every rule here is field-proven in a production-shaped full-stack KMP project (JVM server +
wasm Compose client, multi-tenant PostgreSQL, AI-assisted features) and states the defect it
prevents. Project-specific decisions don't belong here — they live in each project's docs/ADRs.

Each skill is a self-contained package: `SKILL.md` (when to use, hard rules, pointers), `USAGE.md`
(apply procedure), `references/` (load-on-demand deep dives), `templates/` (copy-paste files),
`checklists/` (verification).

> **How agents find these:** every `SKILL.md` declares `name:` and a trigger-rich `description:` in
> its frontmatter. Installed under `~/.claude/skills/` (user) or `<project>/.claude/skills/`
> (project), Claude scans the descriptions and loads the matching skill on demand.
> See [`INSTALL.md`](INSTALL.md).

---

## Catalogue

Versions are `metadata.version` of each `SKILL.md`; the table is regenerated when a skill changes.

### 🧭 Process — apply to every project built with agents

| Skill | Version | What it sets up |
|-------|---------|-----------------|
| [`doc-first-agentic-workflow`](doc-first-agentic-workflow/SKILL.md) | 2.0.0 | Docs tree + statuses + maturity markers, ADRs (amends/amended_by), AGENTS/CLAUDE/PROJECT_CONTEXT layering, step plans with a mine and closed decisions, executable stages + executor protocol, orchestrated runs with a decisions log, close stage with owner questions and "not run", project-status, reviews, DoD, context economy |
| [`architecture-fitness-tests`](architecture-fitness-tests/SKILL.md) | 1.1.0 | Rules as tests inside `./gradlew build`: `KNOWN_*` ratchets as `Map<item, reason>`, vacuity floors, guard/lock/pair, capability ownership (call / construct / name / perimeter), closed-set guards, context order/cycles, contract conventions, the API surface from the live routing tree with `auth=`/`rl=`/`audit=` columns and marker nodes, wire vocabulary, schema invariants via pg_catalog, UI conventions, i18n parity |
| [`kotlin-testing-strategy`](kotlin-testing-strategy/SKILL.md) | 1.1.0 | The pyramid, contract-faithful fakes, object mothers, special forms (concurrent requests through the route, counters via HTTP, refuse-before-read, SQL == domain predicate), security-boundary forms (wall walk, fault injection by trigger, refusal leaves no row, erasure census), red-on-broken and red before the code exists |
| [`the-mine-and-locks`](the-mine-and-locks/SKILL.md) | 1.0.0 | Naming the defect a step must not ship, listing its ways, numbered locks written first (tests-only stage), red by meaning via a scaffold, beacons and floors, switching locks on, close with nothing pending |
| [`hostile-recheck`](hostile-recheck/SKILL.md) | 1.0.0 | An independent adversarial re-check at a step's close: lenses, withheld inputs, prompt templates, four outcomes per finding (fix red-first, owner question, reject, by design), triage table and recording |
| [`agent-orchestrated-step`](agent-orchestrated-step/SKILL.md) | 1.0.0 | An orchestrator runs sub-agents on one large step: file ownership map, shared files edited by the orchestrator only, briefs, model tiers by task, gate output to a file, resume after interruption, an autonomous-decisions log, numbered owner questions consolidated at close |

### 🏗️ Foundation — apply to every Kotlin/Gradle project (in order)

| # | Skill | Version | What it sets up |
|---|-------|---------|-----------------|
| 1 | [`kotlin-project-layout`](kotlin-project-layout/SKILL.md) | 2.2.0 | `app.version`, version catalogue, convention plugins in an included build, JDK toolchain, Detekt + ktlint with baselines, one gate; Docker API version asked from the daemon; the gate in a memory-poor sandbox |
| 2 | [`kotlin-clean-architecture`](kotlin-clean-architecture/SKILL.md) | 2.1.0 | Hexagonal layers and module rules, bounded contexts as ranked packages, ports & adapters, `Unconfigured*` adapters, error design, cross-context reactions (reverse ports, ordered effect funnel) |
| 3 | [`kotlin-domain-modeling`](kotlin-domain-modeling/SKILL.md) | 4.0.0 | Verb use-cases + commands, Either + Raise DSL, value-class ids, idempotent create, TransactionRunner + `TenantContext.Tenant` where **a `Left` rolls back** (closed `KNOWN_COMMITS_ON_LEFT` exception list), outbox events, injected Clock, keyset pages |
| 4 | [`kmp-architecture`](kmp-architecture/SKILL.md) | 2.1.0 | jvm + wasmJs targets, commonMain purity via compilation, Kotest on JVM, interfaces + DI over expect/actual, browser interop |

### ⚙️ Backend — JVM server with PostgreSQL

| Skill | Version | What it sets up |
|-------|---------|-----------------|
| [`ktor-backend`](ktor-backend/SKILL.md) | 2.1.0 | Testable `module()`, plugin order, Hoplite config + fail-fast secrets, JWT (SSE-safe 401), rate buckets (`default` on every route), body limit before reading, security headers/CSP, exhaustive error mapping with `details.reason`, health, SSE, lifecycle-safe background services |
| [`exposed-postgres`](exposed-postgres/SKILL.md) | 3.0.0 | Exposed 1.x, provisioning-only Flyway with pinned settings, `catchingDb` + SQLSTATE mapping, a runner that rolls back a `Left`, `*Pm` JSONB, CAS versions, lock order as an artefact, schema conventions, Testcontainers |
| [`postgres-multitenancy-rls`](postgres-multitenancy-rls/SKILL.md) | 2.0.0 | Three isolation layers, RLS (ENABLE + FORCE, fail closed), DB roles, jobs discovery role with column-level grants, identity policies, exemption criterion (a property, not a list), definer functions as a controlled privilege, isolation suite |
| [`outbox-and-background-jobs`](outbox-and-background-jobs/SKILL.md) | 1.1.0 | Transactional outbox + relay, per-tenant seq, table-as-queue with claim/call/settle and a finite lease, process-as-state runners, one discovery gate, sweeps, local-hour planners, live sync over SSE |
| [`notification-channels`](notification-channels/SKILL.md) | 1.0.0 | Push/e-mail/chat delivery behind channel ports: delivery row as the journal, address as proof, quiet hours and dedup, reminders, pointer-not-copy payloads, deliverability as a deploy condition |
| [`ktor-api-contract`](ktor-api-contract/SKILL.md) | 2.1.0 | Contract-first KMP DTOs, one JSON policy, `@Resource` routes, discriminators, `OptionalFieldDto`, `ApiErrorCodes` + `details.reason` vocabulary, idempotent creates, cursor pages, `Either<ApiError,T>` client |
| [`ktor-auth-sessions`](ktor-auth-sessions/SKILL.md) | 2.0.0 | Account/Member identity, JWT + rotating refresh sessions with a reuse grace window, cookie on `Path=/v1/auth`, Argon2id, PIN + pepper + lockout, one-shot tokens, enumeration safety, ownership/invitations/acting authority, a device principal, the tenant-state wall |
| [`ledger-and-wallet`](ledger-and-wallet/SKILL.md) | 1.0.0 | Append-only ledger with source keys, balance derived never stored, debits under a wallet row lock, holds at request time, reversals as rows, one file owns the debit (guard tests), concurrent-debit tests |
| [`format-locks`](format-locks/SKILL.md) | 1.0.0 | Golden samples per stored JSONB variant, a JSONB registry, `wire-vocabulary.txt`, stored `CHECK` lists == Kotlin names, error-code and reason literal locks |

### 🔐 Privacy & security

| Skill | Version | What it sets up |
|-------|---------|-----------------|
| [`security-hardening-baseline`](security-hardening-baseline/SKILL.md) | 1.0.0 | Review → hardening step: text policy mirrored in the schema, tenant ceilings, paid calls (cheap checks first, a capability off is a person), perimeter as tested text (proxy, compose, CI, deploy), runtime-role startup check, "a protection that locks out its own" |
| [`tenant-data-lifecycle`](tenant-data-lifecycle/SKILL.md) | 1.0.0 | Deletion as a state with a grace period and a wall, erasure order (files, then rows in one definer function), one definition of a member's own, account deletion and leaving, export as a one-time archive link, import of structure, the superuser erasure census |
| [`audit-journal-at-the-door`](audit-journal-at-the-door/SKILL.md) | 1.0.0 | A tenant journal and an account journal written by the transport after the answer, in their own transaction, remembering refusals; a closed `AuditKind` pinned as an `audit=` column of the API surface; fail-open with fault-injection locks; retention sweeps |
| [`safe-file-uploads`](safe-file-uploads/SKILL.md) | 1.0.0 | Refuse before reading, one allow-list with signature checks, no server-side decoding, one multipart reader, per-route body limit, quotas asked twice, domain-built keys, bytes then row, blob-first orphan sweep, download headers |

### 🤖 AI — assistive features over a deterministic core

| Skill | Version | What it sets up |
|-------|---------|-----------------|
| [`llm-integration`](llm-integration/SKILL.md) | 2.0.0 | Trust modes resolved over layers, narrow ports, named providers with a mandatory data boundary and routes, `Unconfigured*`, strict structured output, versioned prompts, injection defence, one retry ladder, `llm_call` audit, speech as a recording state, a context snapshot of what the user sees |
| [`ai-egress-gateway`](ai-egress-gateway/SKILL.md) | 1.0.0 | A sealed outbound text type built only by the gateway: reversible labels for personal data, mask → moderate → restore, refusal as a domain outcome, perimeter providers not masked, construction and boundary locks |
| [`ai-spend-and-entitlement`](ai-spend-and-entitlement/SKILL.md) | 1.0.0 | Durable spend per day and per tenant (calls and money), reserve outside the lock order, a per-member allowance in the tenant's day, AI access until a date, per-person reach (consent), access decided at the door, every paid port budgeted (DI scan) |

### 🖥️ Frontend — Compose Multiplatform

| Skill | Version | What it sets up |
|-------|---------|-----------------|
| [`compose-multiplatform-ui`](compose-multiplatform-ui/SKILL.md) | 3.1.0 | `features` + thin shells, three in-module layers, Entry/Content, `Async` + `launchRequest`, Channel effects, session-scoped stores, shell chrome, a shared-device zone, a closed markup renderer, interaction patterns, i18n parity, canvas-web behaviour |
| [`compose-design-system`](compose-design-system/SKILL.md) | 1.2.0 | M3 as engine, own semantic tokens, wrappers, skins, reserved colours, sheets that name their height, first-time hints, bundled fonts on wasm, contrast, gallery review |
| [`ui-wording-discipline`](ui-wording-discipline/SKILL.md) | 1.0.0 | One concept = one wording function, a words glossary, voice rules, UX rules a non-technical user needs (one primary action, state in words, undo not confirm), enforced by tests |
| [`compose-navigation3`](compose-navigation3/SKILL.md) | 2.1.0 | Navigation 3 routes/back stack, `switchTo` without empty stacks, navigation lambdas, URL-fragment address codec, safe top-of-stack interception |
| [`compose-web-shell`](compose-web-shell/SKILL.md) | 2.1.0 | wasm shell: `ComposeViewport`, `shell.js` under a strict CSP, error overlay and throttled error reports, "new version" row, service worker that never caches the API, dev proxy (`changeOrigin = false`), HTTPS on devices, bundle measurement |
| [`telegram-miniapp`](telegram-miniapp/SKILL.md) | 2.0.0 ⚠️ unverified | Telegram Mini App shell on wasmJs, exchange `initData` once for app sessions, theme sync into tokens, dev mode — templates not yet built in a project |

### 🔧 Delivery

| Skill | Version | What it sets up |
|-------|---------|-----------------|
| [`gradle-release-system`](gradle-release-system/SKILL.md) | 3.2.0 | Conventional Commits + semantic-release, CI = the local gate, profiles (library, webapp, docker, assets, full-stack), single-workflow release image, release-artifact verification, CI hardening |
| [`gradle-docker-build`](gradle-docker-build/SKILL.md) | 2.1.0 | Multi-stage images, BuildKit secrets, digest-pinned bases, exec entrypoint, migrate job, JVM container flags, compose hardening, what an image owes a stand, Kotlin web gotchas |
| [`pre-deploy-schema-policy`](pre-deploy-schema-policy/SKILL.md) | 1.0.0 | Baseline migrations edited in place until data cannot be lost, a schema fingerprint in the image and on `/health`, a deploy gate and a two-halves reset, pinned Flyway settings, the flip to versioned migrations |
| [`single-host-stand-deploy`](single-host-stand-deploy/SKILL.md) | 1.0.0 | One host, compose + a TLS proxy on one origin, a deploy script (lock, `.env` checks, digest memory, backup, gate, wait-ready, rollback), egress check, smoke through the product, rehearsal and a runbook that says what was rehearsed |
| [`backup-restore-drill`](backup-restore-drill/SKILL.md) | 1.0.0 | Backups that were restored: dump under the right role (RLS trap), read-back counts, off-host encrypted snapshot tagged with the app version, restore into a scratch environment, RTO measured, erasure-journal replay, periodic drill |

### 📱 Android — native app distribution & monetization

| Skill | Version | What it sets up |
|-------|---------|-----------------|
| [`android-release-build`](android-release-build/SKILL.md) | 1.0.0 | productFlavors by dimension, per-flavor signing configs from Gradle properties (never committed), the buildType-overrides-flavor signing trap, certificate verification of the built APK/AAB, versionCode vs semantic app.version, R8/ProGuard targeted keep rules (why wildcard `-keep` defeats shrink/obfuscate/optimize), a DEX-size gate, a fitness test forbidding wildcard keeps |
| [`android-store-billing`](android-store-billing/SKILL.md) | 1.0.0 | Store SDK (RuStore Pay, Play Billing, …) behind a store-agnostic PaymentProvider port scoped to one flavor, an SDK-isolation fitness test, a MonetizationMode axis keeping the payment stack out of DI until sales are on, an offline-first entitlement repository, a monotonic purchase sync that may only grant/extend (never revoke), carrying a paid flag across an update, refunds as an explicit decision |

### Planned (later wave, not written yet)

`consent-and-policy-versioning`, `operator-access-and-break-glass`, `virtual-recurrence-slots`,
`rule-as-of-date-counting`, `digest-pipeline`, `compose-adaptive-shell`, `designer-pass-process`. Skills mention them by plain name; a mention becomes a link when the skill exists.

---

## Dependency graph

```
doc-first-agentic-workflow ─┬── the-mine-and-locks ── hostile-recheck      (process, any project)
architecture-fitness-tests ─┤
kotlin-testing-strategy ────┘

kotlin-project-layout
        │
        ├── kmp-architecture
        ▼
kotlin-clean-architecture
        │
        ▼
kotlin-domain-modeling
        │
        ├── ktor-backend ─┬── exposed-postgres ── postgres-multitenancy-rls
        │                 │        ├── ledger-and-wallet
        │                 │        └── format-locks            (+ ktor-api-contract, fitness tests)
        │                 ├── outbox-and-background-jobs
        │                 ├── ktor-api-contract
        │                 ├── ktor-auth-sessions
        │                 ├── llm-integration ─┬── ai-egress-gateway
        │                 │                    └── ai-spend-and-entitlement
        │                 └── privacy & security (need postgres-multitenancy-rls):
        │                       security-hardening-baseline   (+ ktor-auth-sessions)
        │                       tenant-data-lifecycle         (+ ktor-auth-sessions, outbox-and-background-jobs)
        │                       audit-journal-at-the-door     (+ architecture-fitness-tests)
        │                       safe-file-uploads
        │
        └── compose-multiplatform-ui ─┬── compose-design-system
                                      ├── compose-navigation3
                                      ├── compose-web-shell   (web)
                                      ├── telegram-miniapp    (Telegram; unverified)
                                      └── android-store-billing   (+ architecture-fitness-tests)

gradle-release-system · gradle-docker-build ── pre-deploy-schema-policy ── single-host-stand-deploy   (delivery)
kotlin-project-layout ── android-release-build   (Android build; sibling of gradle-release-system)
```

Each skill lists its prerequisites in `metadata.requires`.

---

## Picking skills for a project type

| Project type                           | Skills                                                                                                                                                  |
|----------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| Any project built with agents          | `doc-first-agentic-workflow`, `architecture-fitness-tests`, `kotlin-testing-strategy`, `the-mine-and-locks`, `hostile-recheck`, `agent-orchestrated-step`                          |
| KMP library                            | + `kotlin-project-layout`, `kmp-architecture`, `kotlin-domain-modeling`, `gradle-release-system`                                                         |
| Ktor backend                           | + `kotlin-project-layout`, `kotlin-clean-architecture`, `kotlin-domain-modeling`, `ktor-backend`, `exposed-postgres`, `ktor-api-contract`, `format-locks`, `gradle-docker-build`, `gradle-release-system` |
| Multi-tenant SaaS backend              | + `postgres-multitenancy-rls`, `outbox-and-background-jobs`, `ktor-auth-sessions`, `security-hardening-baseline`, `tenant-data-lifecycle`, `audit-journal-at-the-door` |
| Points, credits or balances            | + `ledger-and-wallet`                                                                                                                                   |
| User files (photos, audio, documents)  | + `safe-file-uploads`                                                                                                                                   |
| AI-assisted product                    | + `llm-integration`, `ai-egress-gateway`, `ai-spend-and-entitlement`                                                                                    |
| Self-hosted on one server, pre-launch  | + `pre-deploy-schema-policy`, `single-host-stand-deploy`                                                                                                |
| Full-stack KMP (server + web)          | all foundation + backend + `compose-multiplatform-ui`, `compose-design-system`, `compose-navigation3`, `compose-web-shell`                              |
| Compose app without backend            | `kotlin-project-layout`, `kmp-architecture`, `kotlin-domain-modeling`, `compose-multiplatform-ui`, `compose-design-system`, `compose-navigation3`       |
| Telegram Mini App                      | the Compose set + `telegram-miniapp` (+ `ktor-auth-sessions` for the initData exchange) — the skill is unverified                                        |
| Native Android app, store distribution | + `android-release-build`; add `android-store-billing` if it sells premium features                                                                     |

---

## Skill file layout (convention)

```
<skill-name>/
├── SKILL.md       # frontmatter + when to use + hard rules + pointers (target ≤ 200 lines)
├── USAGE.md       # step-by-step apply procedure (no theory duplication)
├── references/    # deep dives — loaded on demand
├── templates/     # copy-paste-ready files (generic package io.github.myorg.myapp, prefix app.)
├── checklists/    # verification — the only place "done criteria" live
└── examples/      # optional
```

### SKILL.md frontmatter (Agent Skills spec)

```yaml
---
name: <kebab-case, equals the directory>
description: <one line, ≤ 1024 chars, trigger-rich: "Use when … — covers X, Y, Z." Name technologies verbatim.>
metadata:
  version: "<semver>"
  requires: "<skill>, <skill>"
  project-types: "<type>, <type>"
---
```

Only `name`, `description`, `license`, `allowed-tools`, `metadata`, `compatibility` are top-level
keys in the spec; custom data goes under `metadata` (string values).

---

## How to use

1. Pick skills (table above).
2. Read `SKILL.md`, follow `USAGE.md`, copy from `templates/`.
3. Walk `checklists/` before declaring done.
4. Keep project-specific overrides in the project's docs/ADRs, not here.

## Governance

- Semantic versioning per skill (`metadata.version`); a breaking convention change bumps the major
  and updates dependent skills in the same change.
- `SKILL.md`, references, templates and checklists change together.
- Every skill has `SKILL.md`, `USAGE.md` and at least one checklist.
- Keep `SKILL.md` ≤ ~200 lines; details → `references/`, code → `templates/`.
- Every hard rule names the defect it prevents; rules that can be checked mechanically point to the
  fitness test that checks them.
- Versions in templates are a set verified together (see `kotlin-project-layout` catalogue header);
  alpha artifacts (Navigation 3) are re-verified on every bump.
- When a project discovers a better practice, update the skill here first, then re-install.
