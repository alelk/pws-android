# CLAUDE.md

> **Start here.** Project context: [`docs/PROJECT_CONTEXT.md`](docs/PROJECT_CONTEXT.md); cross-agent
> guide: [`AGENTS.md`](AGENTS.md). This file adds the current phase and the doc-first workflow.

## Project status

<!-- Budget: the latest step in ≤ ~10 lines. When the next step closes, move this paragraph to
project-status.md — do not keep older steps here. -->

**Latest step:** NN — <title> (`docs/70-development/plans/step-NN-….md`, ADR NNNN; **<built |
walked | accepted>**). <Two sentences: what exists now.> **The mine:** <one line, its locks>.
**Not run:** <stand rehearsal, hand walk, measure — or "nothing">. Open owner questions:
`plans/step-NN/NN.N-close.md`.

**Next:** <step or "none in progress">. Open seams: <links>.

Full history: [`docs/70-development/project-status.md`](docs/70-development/project-status.md) — read
it before implementation work (not needed for a doc edit or a point fix); `grep -n "Step NN"` it
rather than reading it whole.

## Gate

`./gradlew build` (see PROJECT_CONTEXT §Commands).

## Skills

Engineering conventions live in `.claude/skills/` and load by description. Don't restate them in
prompts; reference the domain/architecture doc instead.

## Doc-first workflow

1. Docs reach `stable`. 2. Implement the slice from the docs + skills. 3. `./gradlew build` +
skill checklists. 4. Divergence → fix the doc first. 5. Conventional Commits, when asked (or as
pre-authorised in the run's log).

## Domain names most often confused

- <Term A> vs <Term B> — see `docs/20-domain/glossary.md`.
