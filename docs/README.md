# pws-android docs

Documentation index. Agents should start with the root [`AGENTS.md`](../AGENTS.md) and only open files here when a task needs deeper context.

## Map

| File                                          | When to open                                                             |
|-----------------------------------------------|--------------------------------------------------------------------------|
| [`ARCHITECTURE.md`](ARCHITECTURE.md)          | Android-side layering, host responsibilities, composite build, ExternalActions, DataStore flow |
| [`MODULES.md`](MODULES.md)                    | Local modules + composite dependencies from `pws-core`, dependency direction, code locations |
| [`data-security.md`](data-security.md)        | Two-layer protection: asset encryption (`.dbz.enc`/`BuildConfig`) + SQLCipher + Keystore |
| [`release-workflow.md`](release-workflow.md)  | Signing, GitHub Actions release build, RuStore / Play artifacts          |
| [`ai/plans/`](ai/plans/)                      | **In-flight execution plans** — read first when a task references current work |

## Cross-repo

Most domain/UI lives in [`../../pws-core/`](../../pws-core/). Its own `docs/` covers architecture, modules, features, data flow, sync, glossary, and core plans.

| `pws-core` doc                                      | What it covers                              |
|-----------------------------------------------------|---------------------------------------------|
| [`../../pws-core/AGENTS.md`](../../pws-core/AGENTS.md) | Canonical runbook for the core library    |
| [`../../pws-core/docs/ARCHITECTURE.md`](../../pws-core/docs/ARCHITECTURE.md) | Layer responsibilities, dependency rules |
| [`../../pws-core/docs/MODULES.md`](../../pws-core/docs/MODULES.md) | Module inventory in core              |
| [`../../pws-core/docs/FEATURES.md`](../../pws-core/docs/FEATURES.md) | Feature catalog                       |

## Conventions

- **One source of truth per topic.** Don't duplicate `AGENTS.md` content here.
- **Each file ends with `Last reviewed: YYYY-MM-DD`** — update when content meaningfully changes.
- **Plans are dated** (`YYYY-MM-DD_short-slug_plan.md`) and stay until merged into the relevant doc.
- **When a plan lands**, fold durable knowledge into the appropriate `docs/*.md` and remove the plan file.
