# Documents

## Frontmatter

```markdown
---
status: draft        # todo | draft | stable   (ADR: proposed | accepted | superseded | deprecated)
owner: alex
updated: 2026-09-29
related: [../30-architecture/data-model.md, ../60-decisions/0015-tenant-context.md]
---

# Title

> One-paragraph summary: what the document is about and who needs it.
```

- `todo` — a line in the index, maybe no file; `draft` — being discussed; `stable` — code may be
  generated from it. New open questions can return `stable → draft`.
- **Promotion cuts both ways.** "Never implement from a draft" has a mirror: a doc that describes
  working code is not left `draft`. The close stage promotes every doc the step touched whose code
  shipped (a release-process doc in `draft` while the pipeline releases every day tells the next
  agent the pipeline is still a proposal).
- While a step is in flight, docs edited at x.0 say "decided by step NN; built by stages …"; the
  close stage replaces that with "done in step NN".
- `related:` is maintained in **both** directions.
- `docs/README.md` indexes every document with its status badge.

## "What/why" vs "how"

Docs answer *what we build and why*. *How to write the code* lives in skills
(`.claude/skills/`). A doc references a skill; it never restates it. A skill never contains
project-specific decisions — those are docs/ADRs.

## Maturity markers

| Marker    | Meaning                                                                                  |
|-----------|------------------------------------------------------------------------------------------|
| **now**   | in the model and code in this phase                                                      |
| **seam**  | a cheap extension point now (a field, a port, a flag, an `Unconfigured*` adapter), filled later |
| **later** | can be added without a painful migration                                                 |

A decision without a marker is **now**. Expensive-to-retrofit decisions (identity, tenancy, ledgers,
id strategy, time zones, i18n) are at least **seam** from the first slice; cheap-to-add ones
(extra board groupings, analytics, external sync) are honestly **later**.

## Hard rules in the conventions doc

Each hard rule states **how it is caught** — `[now]` (compilation, a test, RLS) or `[seam]` (review
only, with the planned guard). A rule marked review-only is a candidate for a fitness test.

## Open and resolved questions

Every doc may end with `## Open questions`. A doc cannot be `stable` while an open question blocks
implementation. Resolved ones move to `## Resolved questions` with a short answer and a marker —
the history of choices stays next to the model.

## Style

- One document — one topic; split when it grows two.
- Every sentence carries information; don't invent numbers — mark unknowns as open questions.
- Diagrams in Mermaid (or ASCII when clearer).
- Domain terms exactly as in the glossary; code identifiers in `monospace`.
- Keep "why this is so" short in the doc body; the long story of how a decision came about belongs
  in the ADR or the step plan, not repeated in every doc that mentions it.
