# project-status, reviews, Definition of Done

## `docs/70-development/project-status.md`

One entry per step, newest first:

```markdown
## Step NN — <title> (`plans/step-NN-….md`, done YYYY-MM-DD)

**Shipped.** What now exists, by layer, in a few bullets (with the ADRs it produced).
**The mine.** What this step was most likely to break and the locks that prove it is not there.
**Not run.** What the plan asked for and nobody ran (stand rehearsal, hand walk, measure) — and why.
**Deliberately not built.** Seams left open, with where they are recorded.
**Open questions carried over.** Links (the close file's consolidated list).
```

Rules: updated in the same change that closes the step; claims are about the code *as it is*
(verify before writing); status words are exact — *built*, *walked*, *accepted*
([`owner-questions-and-close.md`](owner-questions-and-close.md)); never delete history — correct it
with a dated note. Read it before any implementation task; a doc edit or a point fix usually
doesn't need it. It grows long — agents should `grep` it for the step or term they need rather than
read it whole ([`context-economy.md`](context-economy.md)).

## Reviews and audits (`docs/70-development/reviews/YYYY-MM-DD-<topic>.md`)

```markdown
# Review YYYY-MM-DD — <scope>
Snapshot: commit <sha>.
| ID   | Severity | Where (file:line) | Finding | Resolution (step/stage or "won't fix: why") |
```

- An audit that finds N places where a written rule isn't followed produces a step whose result is
  the rule **as a test** (not N hand fixes alone).
- A review written by attacker personas (outsider, member with the lowest role, former member,
  operator) is a security review; the step that executes it cites its finding ids. A short,
  adversarial re-check *inside* a step is a different tool → [`hostile-recheck`](../../hostile-recheck/SKILL.md).
- Findings reference stable IDs (F-07) so plans and ADRs can cite them.

## Definition of Done (code)

1. Doc precondition: the relevant docs were `stable` before coding.
2. Tests green per the testing strategy (domain on fakes, repositories on Testcontainers, routes);
   concurrency tests for anything touching statuses/ledgers/timers.
3. `./gradlew build` green (static analysis, all tests, all targets, fitness tests).
4. Each touched skill's checklist walked.
5. Tenancy: new tenant tables have RLS + isolation coverage; tenant from the token.
6. Domain hard rules (purity, value-class ids, `Either`).
7. Security: secrets from ENV, no PII in logs; adversarial tests for AI features.
8. Doc post-condition: docs still match the code; new forks → ADRs; new open questions recorded.
9. The mine's locks exist, each **proven red** on a broken implementation (one line per lock in the
   stage notes) — [`the-mine-and-locks`](../../the-mine-and-locks/SKILL.md).
10. Paired tests: every protection "stranger refused" + "own not refused"; every erasure "nothing
    left" + "neighbour intact"; `KNOWN_*` lists only shrank; extended stored formats got new golden
    samples, old ones untouched.
11. Closing scenario driven on a local server; acceptance numbers recorded; the **"not run"** list
    written and copied into `CLAUDE.md` / `project-status.md`.
12. Hostile re-check done (findings closed by red-proven tests or recorded as open questions), or a
    recorded waiver — for steps that add a protection, an erasure or an audit trail.
13. Entry files and the DoD itself re-read; docs the step touched promoted `draft → stable` where
    their code shipped.

Items 9–13 apply to a step close; a point fix needs 1–8. The DoD says explicitly whether a human
PR review is required. Template: [`templates/definition-of-done.md`](../templates/definition-of-done.md).

## Definition of Done (docs only)

Glossary terms; `related:` both ways, live links; tables aligned; open questions triaged; status +
`updated:` + index badge on promotion to `stable`.

## Keep the project's own DoD current

The DoD is a document like any other and goes stale: a DoD last updated before the project adopted
mine locks or close stages silently tells agents those are optional. Re-read it at each step close
together with the entry files.
