# Architecture Decision Records

## When

A significant, hard-to-reverse choice with real alternatives: a technology, a model shape, a
protocol, a rule that constrains future code ("the command carries the new id", "a penalty is not a
purchase", "one baseline migration until the first deploy"). Also: a decision that *replaces* an
earlier one. Not for choices a skill already standardises.

## Format

```markdown
---
status: proposed            # proposed | accepted | superseded | deprecated
owner: alex
updated: 2026-09-29
supersedes: [0012]          # optional
superseded_by: []           # filled on the OLD record when a new one replaces it
amends: []                  # optional: a narrower change to an earlier record
amended_by: []              # filled on the OLD record when a later one amends it
---

# NNNN — Short statement of the decision

## Context
Forces, constraints, what broke or what is about to.

## Decision
What we do, precisely enough to implement and to test.

## Alternatives
What else was considered and why it lost.

## Consequences
What gets easier/harder, what this commits us to, what must now be checked (and where).
```

## Rules

- Numbering is sequential, four digits, never reused. The index (`60-decisions/README.md`) lists
  number, title, status.
- **Immutable after acceptance.** A change of mind is a new ADR that supersedes the old; the old one
  gets `superseded_by` and one line in its header. Nothing is deleted.
- A narrower change that keeps the old decision standing **amends** it: the new ADR carries
  `amends: [NNNN]`, the old one gets `amended_by` in its header (the only edit allowed). The close
  stage checks both headers and the index.
- Title states the decision, not the topic ("Navigation 3 over Voyager", not "Navigation").
- Docs that the decision affects link to it; code comments may cite it (`// ADR 0050`) where the
  reason isn't obvious from the code.
- A decision that can be checked mechanically names the check in *Consequences* (a fitness test, a
  schema test, a lint rule).
