# AGENTS.md

Working guide for AI agents. Full context: **[`docs/PROJECT_CONTEXT.md`](docs/PROJECT_CONTEXT.md)**.
Current phase: "Project status" in [`CLAUDE.md`](CLAUDE.md) — read it before each task.

## What this is

<One paragraph.> Built **documentation-first**: code only from `stable` docs.

## Stack (short)

<Kotlin x.y · JDK N · Ktor · Exposed + PostgreSQL + Flyway · Compose Multiplatform (wasmJs) · Arrow · Kotest + Testcontainers>.
Versions only in `gradle/libs.versions.toml`; product version only in `app.version`.

## Key directories

- `domain/` — entities, ports, use-cases (KMP, pure Kotlin).
- `api/contract|mapping|client` — DTOs, mapping, HTTP client.
- `features/` — Compose UI. `server/transport|infra|di|app` — routes, adapters, wiring, entrypoint.
- `docs/` — all documentation. `.claude/skills/` — how to write code (installed from the skills registry).

## Commands

```bash
docker compose up -d postgres
./gradlew build                 # THE gate
./gradlew :server:app:run
```

## Rules

- Dependencies point inward; only `server:di` knows ports and adapters.
- Tenant from the token; every repository call takes the tenant id.
- Ids are value classes; errors are `Either<DomainError, T>`.
- Canonical names from `docs/20-domain/glossary.md`.
- Bug → failing test first.
- A red fitness test (conventions, API surface, schema) means: fix the code. `KNOWN_*` lists only shrink.

## Never

- Inline versions; change the version outside `app.version`.
- `java.*` in `commonMain`; frameworks in `domain`.
- Weaken, disable or delete tests to get green.
- Implement from a `draft` document.
- Invent deploy commands — they are in `docs/50-delivery/runbook.md` (or: "deployment does not
  exist yet" — re-check this line at every step close).
- Query tenant data without the tenant, or bypass RLS.
