# Context economy for agents

Every file an agent opens is paid for in context and attention. The workflow works best when what
an agent reads is **small, current, and non-contradictory**.

## Code comments

- Comment the **why** in one to three sentences at the declaration that needs it; cite the ADR or
  plan for the full story (`// ADR 0050: the command carries the new id`).
- Don't narrate history in KDoc ("until step 59 there were four…", "the step-44 mine…") — it is
  valuable, but it belongs in the ADR / plan / review, where it is read once, not in a file an
  agent reads on every edit. A 200-line KDoc on a sealed class is paid for by every future change
  to that class.
- Keep one language for code comments (English is the safest default for mixed tooling); docs may
  be in the team's language.
- A comment that restates the code is noise; a comment that states a non-obvious invariant, trap
  or ordering constraint is gold ("the order matters: replace the root first, then trim").

## Entry files

- `CLAUDE.md` / `AGENTS.md` short, linking out; history in `project-status.md`; decisions in ADRs.
- Remove duplicated tables between entry files and docs — the copy drifts first.
- Prefer "read X before touching Y" pointers over pasting X.
- **Size budget for the current-phase block**: the latest step in ≤ ~10 lines (what it is, its
  mine, what was not run, where the open questions are) plus "next". When a new step closes, the
  previous step's paragraph moves to `project-status.md` — it does not stay "for context". A
  current-phase block that lists the last ten steps is loaded into every session and is the first
  thing to go stale. *Mine:* the entry file that says "do not duplicate" while carrying sixty lines
  of history.
- **Grep, don't read**, for long history files: tell agents to `grep -n "Step NN\|<term>"
  project-status.md` and read the hit, not the whole file. Say so next to the link.

## Plans

- A stage file lists exactly what to read; long background stays in the step overview.
- Absolute machine paths (scratch dirs, home dirs) don't belong in committed plans — they break for
  the next session; copy the artefact into the repo or describe how to regenerate it.
- Keep plan-internal consistency: if the protocol says "no commits", the stage's "Done when" must
  not say "commit …".
- Inherit the protocol by reference ("the protocol of step K's README, with these deltas") instead
  of copying it into every plan; list only the deltas.
- Sub-agent briefs are file lists and an answer form, never "read the plan"
  ([`orchestrated-execution.md`](orchestrated-execution.md)).
- Build output goes to a file; only its tail enters the context.

## Skills

- `SKILL.md` ≤ ~200 lines: when to use, hard rules, pointers; details in `references/`, code in
  `templates/`; progressive disclosure.
- Descriptions are trigger-rich (technology names verbatim) — they decide whether the skill loads.
- No project-specific paths or decisions in a shared skill; cite "field-proven in a production-shaped
  project" instead of internal step numbers.
