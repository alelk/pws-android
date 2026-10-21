# Agent entry files — what goes where

| File                    | Audience                  | Contents                                                                        | Size      |
|-------------------------|---------------------------|---------------------------------------------------------------------------------|-----------|
| `AGENTS.md`             | every agent (Codex, Junie, Claude…) | what the project is, stack in one line, key dirs, commands, the gate, the "never" list, links | ≤ 1 page |
| `CLAUDE.md`             | Claude Code               | **current phase** (latest step, what's next), pointers to history and docs, domain names most often confused, doc-first workflow | short; links out |
| `docs/PROJECT_CONTEXT.md` | agents + humans         | the one working context: purpose, stack (from the catalogue), modules and layer rules, commands, invariants, external services, pitfalls, doc map | the canonical page |
| `.claude/skills/*`      | Claude (auto-loaded by description) | *how* to write code for each layer                                | per skill |
| `docs/70-development/coding-conventions.md` | all       | hard rules + where each is caught; table "topic → skill"                        | short     |

## Rules

- **One fact, one place.** Stack versions live in the version catalogue; PROJECT_CONTEXT quotes
  them; CLAUDE.md and AGENTS.md link to PROJECT_CONTEXT. When they drift, fix the canonical place
  and delete the copy.
- `CLAUDE.md`'s "Project status" block holds only the **latest** step and **what's next** — the full
  history is `project-status.md`, read before implementation work.
- Rules an agent must never break (versions only in the catalogue, no `java.*` in commonMain, no
  weakening tests, no implementation from drafts, tenant always from the token…) are in AGENTS.md's
  "never" list **and** enforced by the build wherever possible.
- Tell agents what to do when a fitness test fails ("fix the code; a `KNOWN_*` list only shrinks").
- Skills are installed from a shared registry (symlink or copy, git-ignored in the project) —
  project-specific decisions never go into a skill.
- Commands in these files are copy-pasteable and verified; a wrong command in AGENTS.md is worse
  than none.
- Keep these files current at the end of each step; a stale "current phase" misleads every session.
- **Re-read every entry file at each step close**, not only the one with the "current phase".
  The usual drift: `CLAUDE.md` is updated every step, `AGENTS.md` is not — and its "never" list
  still forbids something the project has since built ("never invent deploy commands: deployment is
  not implemented" after a deploy ADR and `deploy/` exist), or a workflow doc still draws a flow
  the project left behind. A close stage lists the entry files it re-read.
- The current-phase block states the latest step's **status word** (built / walked / accepted) and
  its **"not run"** list ([`owner-questions-and-close.md`](owner-questions-and-close.md)).
