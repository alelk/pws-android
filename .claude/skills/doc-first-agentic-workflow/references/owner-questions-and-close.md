# Owner questions and the close stage

## Owner questions

Agents meet questions the plan did not answer. Each gets an id so it can be cited from code
comments, tests, ADRs and the next plan:

| Id | Where born | Example |
|----|------------|---------|
| `O1…On` | the plan's "Open questions" | `O3. Retention of the journal: a year?` |
| `O-NN.k-n` | stage `NN.k`'s notes (executor or orchestrator) | `O-81.2-1. A refusal for an unknown address writes no row…` |

Each stage-born question is classed when written: **decided** (reversible, taken at discretion, the
reason in one line) or **open** (needs the owner). A decided one still appears in the log so the
owner can overturn it.

**The decision rule**, stated once in the plan: *at discretion and simple, but anything
irreversible asks* — data loss, a published contract, money, a password-guarded action. Forks the
owner chose personally are marked ★ in the decisions table.

## The close stage (`NN.N-close.md`)

Template: [`templates/close-stage.md`](../templates/close-stage.md). In order:

1. **Schema "after" vs "before".** A schema dump taken the same way as at x.0 (sorted, comments and
   history table stripped); a table "plan says → dump shows", with every deviation named and given an
   id. Counts (tables, foreign keys, lines) both times.
2. **Delivery scripts** the step must grow (smoke steps, runbook sections) — run locally where a stand
   is missing.
3. **End-to-end scenario**, point by point, on a local server — by `curl` where there is no client.
   Each point: command, expected, observed. Findings get ids (`D-1`…).
4. **Acceptance** — a table `| № | Check | Result |` with the plan's commands and **expected
   numbers**: `wc -l api-surface.txt → 166`, `grep -c "audit=-" → <n, written>`, `grep -rn Port
   domain/ → only the port file`. A check whose command is wrong by construction is not forced
   green: the plan text is corrected (see 5) and the honest check is stated.
5. **Plan corrections** — every place where building showed the plan wrong is fixed **in the plan**,
   marked `(correction NN.N, O-…)`, and listed in the close file. History is kept by the mark, not by
   leaving the plan wrong.
6. **Hostile re-check** — results and the fix of each finding, each fix by a test proven red
   ([`hostile-recheck`](../../hostile-recheck/SKILL.md)). An empty answer is a valid result; a
   skipped re-check is a recorded waiver.
7. **Owner questions, consolidated**: A — plan questions `O1…On` with what building showed;
   B — stage questions still open; C — new since the close. Decided ones stay in the log, not here.
8. **Not run** — an explicit list: what the plan asked for and nobody ran (stand rehearsal, hand
   walk in the client, bundle-size measure, a real provider key), and why. **Copied verbatim in
   substance into `CLAUDE.md`'s current-phase block and `project-status.md`.**
9. **Docs and entry files** — `project-status.md` entry; `CLAUDE.md` latest step; `AGENTS.md`
   re-read for stale "never"/commands; docs touched by the step promoted `draft → stable`; ADR index
   and `amended_by`/`superseded_by` headers.
10. **Result** — two lines in the step README: what was built, what was not run, where the open
    questions are.

## Acceptance as numbers

A sentence ("audit covers the credential routes") cannot fail; a number can. Write acceptance as
commands whose output is a count, a file list, or a status code, with the expected value in the
plan. Rules:

- The expected number is written **before** building (in the plan), the observed one at close.
- Prefer counting at a pin (route surface, vocabulary file, `KNOWN_*` list) over counting source.
- A "zero" expectation states what it scans; a grep that also counts named arguments or KDoc
  cannot reach zero — fix the command, not the code.
- A regression check names its base commit: `git diff --stat <base> -- <dir>` empty except for
  the listed files.

## "Not run" — the honesty rule

*Mine:* a status that says "done" for a step whose stand rehearsal, hand walk or measure never
happened. The next session reads "done" and builds on it.

- Status words are exact: **built** (code and tests green), **walked** (scenario run by hand or
  `curl`), **accepted** (on the stand by the owner). A step is "built; stand acceptance pending"
  until someone runs it.
- The "not run" list is part of the close file, the `CLAUDE.md` block and the project-status
  entry; it is never dropped when the next step starts — it is carried until run or explicitly
  waived by the owner.
- Sandbox limits are named as such ("wasm-opt killed by memory in the sandbox: no bundle measure")
  — never silently replaced by a smaller check.

## Evidence

family-planner `plans/step-81/81.9-close.md` (schema diff table, acceptance with 12 numbered checks
of which two corrected by "correction 81.9", 12 + 1 defects closed after hostile re-checks,
questions consolidated in A/B/C, "not run": stand rehearsal, client walk, bundle measure);
`CLAUDE.md` "Project status" carrying the same "not run" statements for steps 72–81.
