# Step plans and stages

## The step overview (`plans/step-NN-<title>.md`)

1. **Goal** — the outcome in two or three paragraphs, including what is wrong today (with evidence:
   counts, file names, audit findings).
2. **Why this step now.**
3. **End-to-end scenario** the step must make work — numbered points, each with the observable
   result (status code, row, message). The close stage drives it on a local server.
4. **How to use this plan** — the executor protocol it inherits (by reference to an earlier step's
   README) and only this step's deltas.
5. **Forks** — each open design question with options and the chosen answer; closed before code.
6. **Decisions table** — `| № | Question | Decision | In plain words |` (L1…Ln). Executors never
   revisit these. State the rule the decisions were taken by — **reversible → at discretion,
   irreversible → asked** — and mark with ★ the forks the owner chose himself.
7. **Doc preconditions** — which docs/ADRs must be `stable` first.
8. **The mine** — see below.
9. **Risks** — the second, third… defects, each with where it is handled ("fixed only in the stage
   that names it").
10. **Scope** — in / **not in** (explicitly).
11. **What changes in the schema / on the wire** — the expected diff, so the close stage can compare
    a schema dump "before" with "after" and the surface pins with their counts.
12. **Stages** — the order of work, x.0 … x.N.
13. **Sub-agents** (orchestrated runs) — table `stage → sub-agents → mode`
    ([`orchestrated-execution.md`](orchestrated-execution.md)), plus the verbatim hostile re-check
    prompts.
14. **Tests** — pure / on fakes / Testcontainers / through the route / fitness / gallery, which
    must be **proven red** (one line in the notes per test), and the regression checks.
15. **Acceptance** — commands with expected numbers ([`owner-questions-and-close.md`](owner-questions-and-close.md)).
16. **Resolved questions** / **Open questions** (O1…On).

## The mine (summary)

A section titled for what it is ("the mine to defuse first"):

- **Profile** — the build is green, the obvious test is green, and the thing that matters is
  missing; why that is structural in this project (where nothing would catch it by default).
- **Ways to break it** — concrete implementations an executor could plausibly write that ship the
  defect.
- **Locks** — named tests with their type (route / fitness / source scan / real database), one per
  way to break it, written in stage x.1 against today's behaviour and tagged `PENDING(x.k)` until
  the stage that turns them on.
- **Red proof** — every lock is run against a deliberately broken implementation; the stage notes
  carry one line per lock with the failure message.

Full method → [`the-mine-and-locks`](../../the-mine-and-locks/SKILL.md). The one-sentence form
("the defect + the test") is the minimum for a small step only.

## Stage layout

Typical sequence for a large step:

| Stage | Content |
|-------|---------|
| x.0 | docs and inventory: docs edited to the plan ("decided by step x; built by stages…"), greps that count what the step will touch, a schema dump "before" — no product code |
| x.1 | the mine's locks — tests only, red or `PENDING` where the defect lives today |
| x.2 … x.N-1 | one reviewable change each (one layer or one screen family) |
| x.N | close ([`templates/close-stage.md`](../templates/close-stage.md)) |

```markdown
# Stage NN.k — <title>
> One-line purpose. Next stage: NN.k+1.

## Read before starting      ← exactly what is needed, nothing more
## Stage decisions           ← not revisited by the executor
## Work                      ← what to do; order is the executor's unless stated
## Checks                    ← commands, greps, tests that must pass
## Done when                 ← objective completion criteria
## Executor notes            ← written by the executor: decisions (O-NN.k-n), deviations and why,
                                red proofs, findings, questions for the owner, requests for shared
                                files, what is left for the next stage
```

## Executor protocol (in the step README)

1. Stages strictly in order. One executor (agent session) per stage, or the orchestrator with the
   sub-agents the plan's table names.
2. Read: the README, the step overview, the stage file, `CLAUDE.md`, and the **executor notes of the
   previous stages** — that is how context is handed over.
3. Don't decide beyond the plan. If a step is unclear, contradicts the code, or needs a decision the
   plan lacks — a reversible one is taken and written as `O-NN.k-n` in the notes; an irreversible
   one stops: question in the notes, stage `waiting`, continue with what doesn't depend on it.
4. Don't fix along the way — record out-of-scope findings in the notes.
5. `KNOWN_*` lists only shrink (fitness tests).
6. Every protection: two tests ("stranger refused" / "own not refused"); every erasure: two
   ("nothing left" / "neighbour intact"); every audit kind: two ("success written" / "refusal
   written").
7. A stored format the stage extends gets a **new** golden sample in the same commit; existing
   samples are never edited.
8. Shared files are the orchestrator's (see [`orchestrated-execution.md`](orchestrated-execution.md)).
9. The stage ends with the gate green (or the constrained gate the README states), its checks
   passed, and its status set in the README. Build output goes to a file, not the context.
10. Commits only when the owner asks, or per stage when pre-authorised in the run's log;
    Conventional Commits, one line. Pre-1.0: no `!`/`BREAKING CHANGE`
    ([`gradle-release-system`](../../gradle-release-system/SKILL.md)).
11. While the schema is still a baseline, baseline files are edited in place and the local database
    is recreated; what that means for deploys → [`pre-deploy-schema-policy`](../../pre-deploy-schema-policy/SKILL.md).

## The step README

Stage table with statuses (`not started` → `in progress` → `done`; `waiting` on an owner question),
the protocol (or "the protocol of step K, with these deltas"), the **gate for this environment**
(e.g. a constrained sandbox form — [`kotlin-project-layout`](../../kotlin-project-layout/references/constrained-gate.md)),
the local schema-reset recipe and where the schema dump "before" lives, and a short **Result**
paragraph written at close.

## Sizing

A stage = one reviewable change (one PR), typically one layer or one screen family, finishing green.
If a stage's "Work" section needs sub-headings for independent areas, it is two stages.

## Why this works with agents

- Each session starts cold; the stage file + previous notes are a complete, small context.
- Closed decisions remove the space where an agent would improvise.
- Locks written first turn "the mine" from a paragraph into a red test the executor must turn green.
- "Executor notes" turn surprises into data for the owner instead of silent workarounds.
- Green-per-stage keeps `main` shippable and bisectable.
