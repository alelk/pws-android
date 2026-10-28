---
status: stable
owner: <owner>
updated: YYYY-MM-DD
related: [vibe-coding-workflow.md, coding-conventions.md, ../50-delivery/testing-strategy.md]
---

# Definition of Done

## Code

1. Doc precondition: the relevant docs were `stable` before coding.
2. Tests green per the testing strategy (domain on fakes, repositories on Testcontainers, routes);
   concurrency tests for anything touching statuses/ledgers/timers.
3. `./gradlew build` green (static analysis, all tests, all targets, fitness tests).
4. Each touched skill's checklist walked.
5. Tenancy: new tenant tables have RLS + isolation coverage; tenant from the token.
6. Domain hard rules (purity, value-class ids, `Either`).
7. Security: secrets from ENV, no PII in logs; adversarial tests for AI features.
8. Doc post-condition: docs still match the code; new forks → ADRs; new open questions recorded.

## Step close (in addition to 1–8)

9. The mine's locks exist and each was **proven red** on a deliberately broken implementation (one
   line per lock in the stage notes).
10. Paired tests: every protection "stranger refused" + "own not refused"; every erasure "nothing
    left" + "neighbour intact". `KNOWN_*` lists only shrank. An extended stored format has a new
    golden sample; old samples untouched.
11. The end-to-end scenario was driven on a local server; acceptance commands give the numbers the
    plan expects (or the plan is corrected with a mark); the **"not run"** list is written in the
    close file and copied into `CLAUDE.md` and `project-status.md`.
12. Hostile re-check in a fresh context done — findings closed by red-proven tests or recorded as
    owner questions — or a waiver recorded (required when the step adds a protection, an erasure or
    an audit trail).
13. Entry files and this DoD re-read; docs the step touched promoted `draft → stable` where their
    code shipped.

PR review by a human is <required | not required while a single owner reviews diffs — state which>.

## Docs only

Glossary terms; `related:` both ways, live links; tables aligned; open questions triaged; status +
`updated:` + index badge on promotion to `stable`.

## Principle

DoD protects the invariant "docs are the source of truth, the domain is portable and tested". If an
item can't be met cheaply, that is a signal to change the design or the doc — not to skip the item.
