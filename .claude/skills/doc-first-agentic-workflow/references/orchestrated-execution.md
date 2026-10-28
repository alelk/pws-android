# Orchestrated execution — an orchestrator with sub-agents

A large step (ten stages, several layers) is often run unattended: the main agent (**orchestrator**)
executes the plan stage by stage and launches **sub-agents** for parts of the work. This adds three
failure modes the single-executor protocol does not have: two writers in one file, a sub-agent that
re-decides what the plan closed, and an unattended run whose decisions nobody can find afterwards.

## The rule

**The orchestrator decides and holds the shared state; a sub-agent either counts (reads and returns
a table) or carries mechanics (writes the files it owns, fixes by compiler errors).** Everything
else spends tokens re-telling context.

## Rules (copied into every plan's "Sub-agents" section)

1. **Parallel only for reading**, or for writing **disjoint files** that the orchestrator assigned
   by ownership (stage A owns `transport/audit/*`, stage B owns `identity/consent/*`). One Gradle
   daemon, one Testcontainers database: two sub-agents that both build fight over the daemon and
   the port. The merged build is the orchestrator's.
   *Mine:* two writers in one file — the later one silently drops the earlier one's change.
2. **Shared files belong to the orchestrator** — baseline migrations, the route-surface pin, the
   wire-vocabulary pin, the error-code list, shared DTO files. A sub-agent writes a *request* in its
   notes ("add `kind_x` to the domain `audit_kind`"); the orchestrator applies requests in one place.
3. **The brief is a list of files and the answer form**, not "read the plan". Exact paths or grep
   patterns, the stage file, the output format, and a prohibition on everything else. The whole
   plan costs a sub-agent 15–20 k tokens and invites it to re-decide.
4. **The answer is a fixed-form table**, not prose and not code: `file:line`, class, one-line quote.
   Limit ~150 rows; more means the brief is too wide — split it.
5. **Build output never enters the context**: `./gradlew … > build/<stage>.log 2>&1; tail -40 build/<stage>.log`.
   Inside a stage run targeted tasks; the full gate once at the end.
6. **No sub-agent** when the change is under ~8 files, when it is SQL of the migrations, when it is a
   choice between options, or when the next step depends on understanding rather than on a result.
7. **Model tier by the work** (see below).
8. **A reviewer never sees the executor's reasoning.** It gets the plan, the code and the table —
   never the notes explaining why everything is fine ([`hostile-recheck`](../../hostile-recheck/SKILL.md)).
9. **Interrupted executors are resumed, not restarted.** A sub-agent killed by a usage limit or a
   sandbox crash is resumed with its own transcript (or briefed with the stage notes so far); the
   log records "interrupted by X and resumed". A restart re-does half-written files differently.

## Model tiers

| Work | Tier |
|------|------|
| inventory, grep classification without judgement, docs edits, client screens by an existing pattern | junior (fast, cheap) |
| mechanics by compiler errors, classification with light judgement | middle |
| domain, schema, erasure, audit, security boundaries, the mine's locks | senior |
| hostile re-check | senior, **fresh context**, same tier as the orchestrator |

Record the tier actually used per stage in the log (a junior model's stage is a reviewer's first
suspect).

## The plan's sub-agents table

```markdown
| Stage | Sub-agents                                         | Mode                         |
|-------|----------------------------------------------------|------------------------------|
| x.0   | 4 × explore: columns; routes; readers of X; jobs   | parallel, read               |
| x.1   | 2 × general: locks of A; locks of B (other files)  | parallel, write, own files   |
| x.2   | 1 × server; then 1 × client                        | sequential                   |
| x.4   | none — main agent (the boundary and its function)  | main agent                   |
| x.N   | 2 × general: two hostile re-checks                 | fresh context, parallel      |
```

Name the stages that may run in parallel and the files each owns; name the shared files.

## The autonomous-decisions log

An unattended run keeps **one log at the repository root**, `AUTONOMOUS-DECISIONS-step-NN.md`,
written by the orchestrator and **not committed** (`.git/info/exclude`, so it is never lost in a
`git clean` nor pushed by accident). Template: [`templates/autonomous-decisions-log.md`](../templates/autonomous-decisions-log.md).

1. **Owner's answers** — dated, verbatim in substance: scope ("the whole plan end to end"),
   sub-agents allowed with model choice, **commit authority** ("commits between stages, Conventional
   Commits, in the owner's name, no push"), decisions accepted as in the plan.
2. **Progress** — `| Stage | Status | Commits | Model |`.
3. **Decisions taken without you (check them)** — per stage, numbered `O-NN.k-n`, each one line
   with the reason; sub-lists *accepted* and *left to the owner*. Executor decisions the
   orchestrator accepted are listed by id, not repeated.

The log is the owner's entry point after the run; the stage notes stay the detail.

## Commits in an orchestrated run

The default is "commit only when the owner asks". The owner may **pre-authorise** per-stage commits
for a run; the authorisation is the first line of the log, and the commits follow it exactly
(author, trailer or none, push or none). Without it, the orchestrator leaves the tree uncommitted
and says so.

## Evidence

family-planner steps 76–81: "Sub-agents: whom to launch and whom not" (step 76, rules 1–7),
per-stage tables in steps 78–81, `AUTONOMOUS-DECISIONS-step-78..81.md` (stages interrupted by a
usage limit or a sandbox network failure and resumed; ten stages committed under a pre-authorisation),
`STEP-67-NOTES.md` as the earlier form (DEVIATION / PROBLEM / OWNER QUESTION / out of stage).
