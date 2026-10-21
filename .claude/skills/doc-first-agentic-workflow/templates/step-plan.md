---
status: draft               # draft → stable → done
owner: <owner>
updated: YYYY-MM-DD
related: [ step-NN/README.md, ../project-status.md ]
---

# Step NN — <title>

## Goal

<The outcome, and what is wrong today — with evidence.>

**Result of the step:** <one sentence that can be checked>.

## End-to-end scenario

1. <Action> → <observable result: status code, row, message>.

## How to use this plan

Protocol: step <K>'s README (`plans/step-K/README.md`); below only this step's deltas.

1. Stages strictly in order, NN.0 → NN.N; NN.1 (locks) is written against today's behaviour.
2. No decisions beyond the plan; reversible ones are logged as `O-NN.k-n`, irreversible ones asked.
3. No drive-by fixes; risks are fixed only in the stage that names them.
4. Gate green at the end of each stage (constrained form: step README, "Gate in this environment").
5. Paired tests: protection — refused / not refused; erasure — nothing left / neighbour intact.
6. An extended stored format gets a new golden sample in the same commit; old samples untouched.
7. Commits: <on the owner's request | pre-authorised per stage — see the run's log>.

## Why this step now

## Forks

### Fork 1: <question>
Options … **Chosen:** … because …

## Decisions

Taken by the rule "at discretion and simple, but irreversible — asked". Forks the owner chose: ★.

| №  | Question | Decision | In plain words |
|----|----------|----------|----------------|
| L1 |          |          |                |

## Doc preconditions

- <doc> must be `stable`; ADR NNNN accepted.

## The mine to defuse first

**Profile.** <Build green, the obvious test green, and what is missing. Why nothing here catches it
by default.>

**Ways to break it.**
1. …

**Locks** (written in NN.1, `PENDING(NN.k)` until the stage that turns them on; each proven red):

| # | Test | Type (route / fitness / source scan / real DB) | Catches way |
|---|------|------------------------------------------------|-------------|
| 1 |      |                                                |             |

Method: the `the-mine-and-locks` skill.

## Risks

### Risk two: <title>
<What, where it is handled (stage), its test.>

## Scope

### In
### Not in

## What changes in the schema

<Tables, columns, domains, grants — the close stage compares the dump with this list.>

## What changes on the wire

<Routes added (count), vocabulary additions, error codes.>

## Stages

### NN.0 — docs and inventory (no product code)
### NN.1 — the mine's locks (tests only)
### NN.2 — …
### NN.N — close

## Sub-agents

| Stage | Sub-agents | Mode |
|-------|------------|------|
| NN.0  |            |      |

Shared files (orchestrator only): <migrations, surface pin, vocabulary pin, error codes>.

**Hostile re-check A (<lens>), verbatim.** <Prompt — see the `hostile-recheck` skill.>

## Tests

### Pure
### On fakes
### Testcontainers / real disk
### Through the route (concurrency, counters, refusal before read …)
### Fitness
### Proven red (one line per test in the stage notes)
### Regression (`git diff --stat <base> -- <dir>` …)

## Acceptance

1. Gate green.
2. `<command>` → `<expected number>`.

## Resolved questions

## Open questions

- O1. …
