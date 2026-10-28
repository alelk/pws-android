# Doc-first Workflow Checklist

## Structure

- [ ] Numbered docs tree with an index listing every doc and its status
- [ ] Every doc has frontmatter (`status`, `owner`, `updated`, `related` both ways)
- [ ] Glossary is canonical; code uses its names
- [ ] Coding conventions list hard rules, each with how it is caught
- [ ] ADR folder with template, sequential numbers, index; accepted ADRs never edited

## Agent entry

- [ ] `AGENTS.md` short: project, stack line, dirs, commands, gate, "never" list, links
- [ ] `CLAUDE.md`: current phase (latest step ≤ ~10 lines, status word, "not run") + pointers; no
      duplicated stack/architecture tables, no history of older steps
- [ ] `AGENTS.md` "never" list and commands still true (re-read at the last step close)
- [ ] `PROJECT_CONTEXT.md` is the one working context and matches the build files
- [ ] Skills installed from the shared registry; no project decisions inside skills
- [ ] Agents are told what to do when a fitness test fails

## Steps

- [ ] Each step has an overview with closed decisions (★ for the owner's, the reversible/irreversible
      rule), scope in/out, expected schema/wire changes, acceptance as commands with numbers
- [ ] The mine is profiled with ways to break it and named locks; locks written first (x.1) and
      each proven red (a line in the notes)
- [ ] Paired tests for every protection / erasure / audit kind
- [ ] Each stage is one executable file with read-before, decisions, work, checks, done-when, executor notes
- [ ] The step README holds the stage table, the protocol (or its deltas) and the gate for this
      environment
- [ ] Stages end green; statuses updated; commits on request or pre-authorised in the log;
      Conventional Commits (no `!` pre-1.0)
- [ ] Orchestrated runs: sub-agents table, shared files owned by the orchestrator, parallel writes
      only to disjoint files, model tier recorded, `AUTONOMOUS-DECISIONS-step-NN.md` kept and
      git-excluded
- [ ] Owner questions carry ids (`O1…`, `O-NN.k-n`), classed decided/open
- [ ] Close stage: schema diff, scenario on a local server, acceptance numbers, plan corrections
      marked, hostile re-check (or waiver), questions consolidated, "not run" list

## History

- [ ] `project-status.md` updated when a step closes (shipped, mine, not run, not built), with an
      exact status word (built / walked / accepted)
- [ ] Docs whose code shipped are `stable`, not `draft`; ADR `amended_by`/`superseded_by` headers set
- [ ] Dated reviews with IDs, severity, resolution
- [ ] DoD (code, step close, docs-only) exists, is current, and is referenced by the workflow

## Hygiene

- [ ] No absolute machine paths in committed plans
- [ ] Code comments state short "why"; history lives in ADRs/plans
- [ ] Protocol and stage criteria don't contradict each other
