# CLAUDE.md — pws-android

> **Canonical agent guide is [`AGENTS.md`](AGENTS.md).** Read it first — everything below is Claude-Code-specific glue.
> Auto-loaded every session by Claude Code. Keep tight.

---

## Claude-specific reminders

### Skills (shared with `pws-core` via `.claude/skills/`)

This repo carries a copy of the `pws-core` skill catalogue. Invoke via the `Skill` tool:

| Skill                       | Apply when                                                       |
|-----------------------------|------------------------------------------------------------------|
| `kotlin-project-layout`     | Touching root Gradle layout, `app.version`, version catalog      |
| `kmp-architecture`          | Adding KMP-shaped modules / interop                              |
| `kotlin-clean-architecture` | Auditing layer boundaries                                        |
| `kotlin-domain-modeling`    | Domain refactors (mostly applies in `pws-core`)                  |
| `compose-multiplatform-ui`  | UI work that touches `pws-core:features`                         |
| `voyager-navigation`        | Navigation changes (mostly in `pws-core`)                        |
| `ktor-api-contract`         | API/DTO changes (lives in `pws-core`)                            |

Each skill's `SKILL.md` is the entrypoint. Most apply to `pws-core`; this repo is mostly Android glue.

### Subagents

- **1–2 targeted lookups** → `Bash` with `grep`/`find` directly.
- **3+ open-ended queries** → spawn `Explore` subagent (protects main context).
- For cross-repo questions, search `../pws-core/` if `grep` here returns nothing.

### Active plans (read first when relevant)

In [`docs/ai/plans/`](docs/ai/plans/):

- `2026-06-15_data-protection-consolidated_plan.md` — DB encryption + key management
- `2026-05-06_book-bundle-and-data-module_plan.md` — content packaging
- `2026-05-05_content-protection-and-delivery_plan.md` — assets pipeline

When the user references "current plan" without a name, **ask which one**.

### Don't waste tokens on

`build/` · `*/build/` · `.gradle/` · `.kotlin/` · `output/` · `*.iml` · `.idea/` · `gradle-wrapper.jar` · `kotlin-js-store/` · `app-compose/src/main/res/raw/` · `data/db-android/src/test/resources/test-db/`.

### Cross-repo

`../pws-core` is auto-linked as a composite Gradle build. Most logic lives there. If a user reports an Android symptom that touches features/domain, check both repos.

---

## Everything else

Build commands · flavors · module map · architecture · hard rules · workflows · docs index → **[`AGENTS.md`](AGENTS.md)**.
