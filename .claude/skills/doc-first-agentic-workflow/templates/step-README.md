---
status: stable
owner: <owner>
updated: YYYY-MM-DD
related: [ ../step-NN-<title>.md ]
---

# Step NN — stages

> Executable stage files. Goal, decisions and risks: [`../step-NN-<title>.md`](../step-NN-<title>.md)
> — **read it fully before your stage.**

| Stage | File                         | Status      |
|-------|------------------------------|-------------|
| NN.0  | [docs and inventory](NN.0-docs-and-inventory.md) | not started |
| NN.1  | [mine locks](NN.1-mine-locks.md)                 | not started |
| NN.N  | [close](NN.N-close.md)                           | not started |

Statuses: `not started` → `in progress` → `done`; `waiting` when blocked on an owner question.

## Stage file layout

Read before starting · Stage decisions · Work · Checks · Done when · Executor notes.

## Executor protocol

1. Stages strictly in order; one executor per stage, or the orchestrator with the sub-agents the
   plan's table names.
2. Read this README, the step overview, your stage file, `CLAUDE.md`, and the **executor notes of
   previous stages**.
3. Do not decide beyond the plan. Reversible → decide, write `O-NN.k-n` with the reason in the
   notes. Irreversible / unclear / contradicts the code → stop, write the question, mark
   `waiting`, continue with what doesn't depend on it.
4. Do not fix along the way — record out-of-scope findings in the notes.
5. `KNOWN_*` lists in fitness tests only shrink.
6. Paired tests for every protection / erasure / audit kind; every lock and every fix of a
   re-check finding proven red (one line in the notes).
7. Shared files (<list>) are the orchestrator's; request changes in the notes.
8. A stage ends with the gate below green, its checks passed, and its status set here. Build output
   to a file (`> build/gate-NN.k.log 2>&1`).
9. Commit only when the owner asks — or per stage if pre-authorised in the run's log; Conventional
   Commits, one line; before 1.0 no `!` / `BREAKING CHANGE`.

## Gate in this environment

```bash
./gradlew build > build/gate.log 2>&1; tail -40 build/gate.log
# Constrained sandbox (agreed exception; what it skips is listed as "not run" at close):
# ./gradlew build -x <optimise task> -x <heavy test task> --max-workers=2
# ./gradlew <heavy test task> --max-workers=2     # when the stage touched that module
```

Local schema reset: `<command>`. Schema dump "before": `build/schema-before.sql` (taken in NN.0).

## Result

<Written at close: built / walked / accepted; not run; where the open questions are.>
