---
status: stable
owner: <owner>
updated: YYYY-MM-DD
related: [ README.md, ../step-NN-<title>.md ]
---

# Stage NN.k — <title>

> <One-line purpose.> Next stage: NN.k+1.

## Read before starting

- …

## Stage decisions

- … (not revisited by the executor)

## Work

1. …

## Checks

```bash
./gradlew build > build/gate-NN.k.log 2>&1; tail -40 build/gate-NN.k.log
```

## Done when

- …

## Executor notes

<Written by the executor (name the model), date.>

- **Decisions** — `O-NN.k-1` <decision> — <reason> (reversible; taken).
- **Questions for the owner** — `O-NN.k-2` <question> (open).
- **Proven red** — `<TestName>`: broken by <one-line change>, failed with `<message>`; fix restored.
- **Requests for shared files** — <file>: <change> (for the orchestrator).
- **Deviations** — <what differs from the stage and why>.
- **Out of stage, not fixed** — <finding, file:line>.
- **Left for the next stage** — …
