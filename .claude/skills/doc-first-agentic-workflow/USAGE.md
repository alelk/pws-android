# Usage — Doc-first, agent-executed development

## 1. Bootstrap the tree (new project)

```bash
mkdir -p docs/{00-meta,10-product,20-domain,30-architecture,40-frontend,50-delivery,60-decisions,70-development/{plans,reviews}}
cp <skill>/templates/AGENTS.md  AGENTS.md
cp <skill>/templates/CLAUDE.md  CLAUDE.md
cp <skill>/templates/PROJECT_CONTEXT.md docs/PROJECT_CONTEXT.md
cp <skill>/templates/definition-of-done.md docs/70-development/definition-of-done.md
cp <skill>/templates/adr.md docs/60-decisions/0000-template.md
```

Write first: product vision, glossary, domain model, module structure, coding conventions (hard
rules, each with *how it is caught*), testing strategy. Status `draft` until discussed.

## 2. Stabilise before coding

A doc becomes `stable` when no open question blocks implementation. Resolved questions move to
"Resolved questions" with a one-line answer (history of choices stays visible). Forks → ADRs.

## 3. Plan a step

```bash
cp <skill>/templates/step-plan.md   docs/70-development/plans/step-NN-<short-title>.md
mkdir docs/70-development/plans/step-NN
cp <skill>/templates/step-README.md docs/70-development/plans/step-NN/README.md
cp <skill>/templates/stage.md       docs/70-development/plans/step-NN/NN.0-docs-and-inventory.md
cp <skill>/templates/stage.md       docs/70-development/plans/step-NN/NN.1-mine-locks.md
cp <skill>/templates/close-stage.md docs/70-development/plans/step-NN/NN.N-close.md
```

The plan closes every decision (table L1…Ln with a plain-words column, ★ on the owner's own forks,
the rule "reversible at discretion, irreversible asked"), profiles the mine and names its locks
([`the-mine-and-locks`](../the-mine-and-locks/SKILL.md)), lists scope in/out, the expected schema and
wire changes, the stages, the tests that must be proven red, and acceptance as commands with
expected numbers. → [`references/step-plans.md`](references/step-plans.md)

## 4. Execute a stage with an agent

Prompt shape (the plan already holds the details — don't restate skills or rules):

```
Execute stage NN.k of step NN.
Read: docs/70-development/plans/step-NN/README.md (protocol, gate), the step plan, stage file NN.k,
executor notes of the previous stages.
Reversible gaps: decide and record as O-NN.k-n. Irreversible or contradicting the code: stop and ask.
Done = the stage's "Done when" + the README's gate green (output to build/gate-NN.k.log) + status
updated in the README. Do not commit.
```

### Orchestrated (unattended) run

```
Implement step NN end to end as orchestrator, stage by stage, per the plan's "Sub-agents" table.
Keep AUTONOMOUS-DECISIONS-step-NN.md at the root (add it to .git/info/exclude).
Owner's answers: <scope>, <model choice allowed>, <commits between stages: yes/no, author, push>.
Shared files are yours only. Briefs to sub-agents: file lists + answer form. Resume interrupted
sub-agents; log it.
```

→ [`references/orchestrated-execution.md`](references/orchestrated-execution.md),
log template [`templates/autonomous-decisions-log.md`](templates/autonomous-decisions-log.md).

## 5. Close the stage / step

Each stage: human (or pre-authorised orchestrator) commits, Conventional Commits. The last stage is
the **close** ([`references/owner-questions-and-close.md`](references/owner-questions-and-close.md)):
schema "after" vs "before", the scenario on a local server, acceptance numbers, plan corrections,
hostile re-checks ([`hostile-recheck`](../hostile-recheck/SKILL.md)), owner questions consolidated,
the **"not run"** list. Then `project-status.md` (template), the current-phase block in `CLAUDE.md`
(within its budget; the previous step moves to project-status), `AGENTS.md` re-read, touched docs
`draft → stable`, ADR index and amendment headers.

## 6. Periodically: review and audit

Dated review files in `docs/70-development/reviews/` with findings, severity, and resolution; an
audit that finds rules nobody checks produces a step whose output is fitness tests
([`architecture-fitness-tests`](../architecture-fitness-tests/SKILL.md)).

Then walk through [`checklists/workflow-checklist.md`](checklists/workflow-checklist.md).
