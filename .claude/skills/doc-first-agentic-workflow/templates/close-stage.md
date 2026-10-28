---
status: stable
owner: <owner>
updated: YYYY-MM-DD
related: [ README.md, ../step-NN-<title>.md ]
---

# Stage NN.N — Close

> The plan's "NN.N — close" section and its acceptance; the executor does not revisit them.

## Scope

Schema dump "after" vs "before"; delivery scripts the step grows; the end-to-end scenario on a local
server (`curl` where there is no client); acceptance; plan corrections; hostile re-checks (run by
the orchestrator); `project-status.md`, `CLAUDE.md`, `AGENTS.md`, touched docs `draft → stable`;
owner questions consolidated.

## Executor notes

<Executor (model), date. What was and was not edited.>

### 1. Schema: "after" against "before"

Dump method: <same as NN.0>. Lines <a → b>, tables <a → b>, FKs <a → b>.

| Plan ("What changes in the schema") | In the dump |
|-------------------------------------|-------------|
|                                     |             |

### 2. Delivery scripts and runbook

### 3. End-to-end scenario (<server command, config>)

| # | Command | Expected | Observed |
|---|---------|----------|----------|
| 1 |         |          |          |

### 4. Scenario findings (`D-1` …)

### 5. Acceptance

| № | Check | Result |
|---|-------|--------|
| 1 | gate (`build/gate-NN.N.log`) | |
| 2 | `<command>` → `<expected>` | |

### 6. Plan corrections ("(correction NN.N)" in the main document)

- …

### 7. Hostile re-checks

<Per finding: what, decision, test proven red (how broken, failure message), fix.>

## Owner questions (consolidated)

### A. Plan questions (O1–On) with what building showed
### B. From stages (still open)
### C. New since NN.N

## Not run

- <What the plan asked for and nobody ran> — <why: no stand / no client / sandbox limit / no key>.

## Result

Gate: <command> — <result>. Status of the step: <built | walked | accepted>. Files changed by this
stage: <list>.
