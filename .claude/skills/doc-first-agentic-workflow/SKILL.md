---
name: doc-first-agentic-workflow
description: Use when organising a software project so AI coding agents (Claude Code, Codex, Junie) implement it reliably — documentation-first with doc statuses (todo/draft/stable) and maturity markers (now/seam/later), a numbered docs tree, ADRs (immutable, superseded or amended), layered agent entry files (AGENTS.md, CLAUDE.md with a size-budgeted current phase, PROJECT_CONTEXT.md, skills), step plans with closed decisions (reversible at discretion, irreversible asked), the step's mine with locks written first and proven red, stages as executable files with an executor protocol (strict order, no decisions beyond the plan, no drive-by fixes, notes, green gate per stage, commits on request or pre-authorised), orchestrated runs (orchestrator + sub-agents, file ownership, model tiers, resume, autonomous-decisions log), numbered owner questions, a close stage (scenario, acceptance numbers, honest not-run list, hostile re-check), project-status, reviews, Definition of Done, context economy.
metadata:
  version: "2.0.0"
  requires: ""
  project-types: "any-project-built-with-ai-agents"
---

# Doc-first, agent-executed development

## Why

Agents are fast and literal. They implement what is written, follow the nearest example, and make
up the missing decision if nobody made it. The workflow below moves every decision **before** the
code, into a document an agent reads, makes every stage end in a state the build verifies, and
makes the end of a step say honestly what was proven and what was not.

```
idea → draft doc → discussion → stable doc (+ ADR for each fork)
     → step plan (decisions closed, the mine profiled, locks named, scope in/out, acceptance)
     → x.0 docs + inventory → x.1 locks (tests only, red where the defect lives)
     → x.2 … stages (one executable file each; executor or orchestrator + sub-agents)
     → each stage: green build + DoD + notes → commit (on request or pre-authorised)
     → close stage: scenario, acceptance numbers, hostile re-check, "not run", owner questions
     → project-status + CLAUDE.md + docs draft→stable → next step
```

## Hard rules

- ❌ Never implement from a `draft` document. A fork discovered while coding stops the work: the
  question goes into the stage's *executor notes* and to the owner; the doc/ADR changes first.
  *Mine:* the agent invents the decision and the diff hides it.
- ❌ The executor never decides beyond the plan and never fixes things outside the stage's scope —
  it records them. Drive-by changes are how an agent's diff becomes unreviewable.
- ❌ No duplication between agent entry files: `AGENTS.md` (short, cross-agent), `CLAUDE.md`
  (Claude-specific + current phase), `PROJECT_CONTEXT.md` (the one context), skills (how to write
  code). A fact lives in one place; the others link to it. The copy drifts first.
- ❌ An ADR is never edited after acceptance — a new ADR supersedes or amends it (the old one says
  so in its header).
- ✅ Every doc starts with frontmatter: `status`, `owner`, `updated`, `related` (both directions).
- ✅ Decisions inside docs carry a maturity marker: **now**, **seam**, **later**.
  Expensive-to-retrofit things (identity, tenancy, ledgers, ids) are at least a seam from day one.
- ✅ A plan's decisions follow one rule: **reversible → decided at discretion and logged;
  irreversible (data loss, a published contract, money) → asked**. Forks the owner chose are
  marked (★) so a reviewer knows which ones are not the agent's.
- ✅ Every step plan names its **mine** — the defect that leaves the build green, the obvious test
  green, and the thing that matters missing — with its *locks*: named tests written first (stage
  x.1) against today's code and **each proven red** on a deliberately broken implementation.
  Method → [`the-mine-and-locks`](../the-mine-and-locks/SKILL.md).
- ✅ Every protection comes with two tests: "the stranger is refused" **and** "the rightful one is
  not". *Mine:* a protection that locks out its own users passes every refusal test.
- ✅ Every stage ends with the gate green (`./gradlew build`, or the constrained form written in the
  step README), its "Done when" satisfied, its status updated. **Commits** only when the owner asks
  — or when the owner pre-authorised per-stage commits, recorded in the run's log; Conventional
  Commits.
- ✅ In an orchestrated run, files shared by stages (baseline migrations, surface/vocabulary pins,
  error-code lists) belong to the orchestrator; sub-agents request changes in their notes. Parallel
  sub-agents only read, or write disjoint files. A reviewer never sees the executor's reasoning.
  → [`references/orchestrated-execution.md`](references/orchestrated-execution.md)
- ✅ A step closes with a **close stage**: the end-to-end scenario driven on a local server,
  acceptance as commands with expected numbers, plan corrections marked in the plan, a **"not run"**
  list copied into `CLAUDE.md` and `project-status.md`, owner questions consolidated. Never write
  "done" for what was not run. → [`references/owner-questions-and-close.md`](references/owner-questions-and-close.md)
- ✅ A step that adds a protection, an erasure or an audit trail gets a **hostile re-check** in a
  fresh context before it closes, or a recorded waiver → [`hostile-recheck`](../hostile-recheck/SKILL.md).
- ✅ At close, `project-status.md`, `CLAUDE.md`, `AGENTS.md` and the docs the step touched are
  re-read; docs whose code shipped move `draft → stable`. A stale claim poisons every later step.
- ✅ Bug → a failing test first, then the fix. Tests are never weakened to make a build green.
- ✅ Canonical domain names come from the glossary; code uses exactly those words.

## The docs tree

```
docs/
├── 00-meta/          documentation guide (statuses, markers, style)
├── 10-product/       vision, scope & roadmap, requirements, personas
├── 20-domain/        glossary (canonical), domain model, lifecycles, per-area domain docs
├── 30-architecture/  overview, modules, data model, tenancy, API design, auth, jobs, sync, AI…
├── 40-frontend/      architecture, design system, tokens, UX principles, shells
├── 50-delivery/      build & CI, testing strategy, release, deployment, runbook
├── 60-decisions/     ADRs NNNN-title.md + README index
├── 70-development/   getting started, coding conventions (hard rules), DoD, workflow,
│                     project-status.md, plans/, reviews/
└── PROJECT_CONTEXT.md
```

## References (load on demand)

- [`references/documents.md`](references/documents.md) — frontmatter, statuses, markers, "what/why" vs "how", open vs resolved questions, promotion at close.
- [`references/adr.md`](references/adr.md) — when to write one, format, supersession/amendment, index.
- [`references/agent-entry-files.md`](references/agent-entry-files.md) — AGENTS.md / CLAUDE.md / PROJECT_CONTEXT.md / skills: what goes where, re-read at close.
- [`references/step-plans.md`](references/step-plans.md) — the step overview, decisions and the decision rule, the mine (summary), stages, executor protocol.
- [`references/orchestrated-execution.md`](references/orchestrated-execution.md) — orchestrator + sub-agents, file ownership, briefs, model tiers, resume, the autonomous-decisions log, pre-authorised commits.
- [`references/owner-questions-and-close.md`](references/owner-questions-and-close.md) — owner-question ids, the close stage, acceptance as numbers, plan corrections, the "not run" list.
- [`references/status-reviews-dod.md`](references/status-reviews-dod.md) — project-status entries, dated reviews/audits, Definition of Done.
- [`references/context-economy.md`](references/context-economy.md) — keeping what agents read small, current and non-contradictory; size budgets.

Related skills: [`the-mine-and-locks`](../the-mine-and-locks/SKILL.md) (profile, locks, red-proof),
[`hostile-recheck`](../hostile-recheck/SKILL.md) (adversarial prompts, fresh context),
[`pre-deploy-schema-policy`](../pre-deploy-schema-policy/SKILL.md) (editing baseline migrations
before real data), [`single-host-stand-deploy`](../single-host-stand-deploy/SKILL.md) (the
rehearsal a close stage refers to), [`architecture-fitness-tests`](../architecture-fitness-tests/SKILL.md)
(rules as tests), [`kotlin-project-layout`](../kotlin-project-layout/references/constrained-gate.md)
(the gate in a memory-poor sandbox).

## Templates

`templates/`: `doc.md`, `adr.md`, `step-plan.md`, `step-README.md`, `stage.md`, `close-stage.md`,
`autonomous-decisions-log.md`, `project-status-entry.md`, `review.md`, `AGENTS.md`, `CLAUDE.md`,
`PROJECT_CONTEXT.md`, `definition-of-done.md`. Write them in the project's working language; keep
code identifiers in English.

## Apply this skill

Step-by-step → [`USAGE.md`](USAGE.md).

## Verify

Walk through [`checklists/workflow-checklist.md`](checklists/workflow-checklist.md).

## Evidence

family-planner steps 76–81: mine sections with five locks proven red (`81.1-mine-locks.md`),
orchestrated runs with `AUTONOMOUS-DECISIONS-step-NN.md`, two hostile re-checks in step 81 that
found twelve defects after a green build, close stages with acceptance greps and "not run" lists
(`81.9-close.md`).
