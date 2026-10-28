# Sealed Hierarchies & State Machines

Closed sets of variants are sealed types — adding a variant becomes a compile error at every
`when` that must decide about it.

## Variants with payload

```kotlin
sealed interface TaskOrigin {
    data object OneOff : TaskOrigin
    data class Occurrence(val seriesId: TaskSeriesId, val slotDate: LocalDate) : TaskOrigin
    data class Pulled(val backlogItemId: BacklogItemId) : TaskOrigin
}
```

- `sealed interface` by default; `sealed class` when variants share state/constructor logic.
- `data object` (not `object`) for stateless variants — readable `toString`.
- Exhaustive `when` **without `else`**. An `else` on a sealed type is a future bug.
- Prefer one sealed type with a variant over a boolean + nullable fields that must agree.

## State machines as pure tables

```kotlin
object TaskStatusMachine {
    fun sources(t: TaskTransition.StatusChange): Set<TaskStatus> = when (t) {
        TaskTransition.Started -> setOf(Pending, Returned)
        TaskTransition.Submitted -> setOf(Pending, InProgress, Returned)
        // … every transition
    }
    fun target(t: TaskTransition.StatusChange): TaskStatus = when (t) { /* … */ }
}

fun Task.moveTo(t: TaskTransition.StatusChange, at: Instant, actor: Actor): Either<DomainError, Task> =
    if (status in TaskStatusMachine.sources(t)) copy(status = TaskStatusMachine.target(t), /* stamps */).right()
    else DomainError.ValidationError("cannot ${t.name} from $status").left()
```

- The **whole** table is written at once, even for transitions whose use-cases come later — the
  machine is then finished and testable (a test iterates every transition × status).
- The machine decides *legality*; the use-case decides *authority* (who may).
- Projections of state (board columns, "is it overdue") are pure functions over the state, not
  stored columns.

## What cannot fit in one transaction is a state

A long or external step (speech recognition, an LLM call, erasing files, deleting a tenant after a
grace period) cannot run inside one database transaction. Store **where the process is** on the
aggregate and let an idempotent runner move it:

```kotlin
sealed interface Hearing {                 // a state of the recording, not a job in memory
    data object NotAsked : Hearing
    data class Queued(val attempts: Int, val nextAttemptAt: Instant) : Hearing
    data class Heard(val transcript: String, val at: Instant) : Hearing
    data class Failed(val reason: String, val at: Instant) : Hearing
}
```

- No default value; every read is a `when` without `else` (a reader that treats `Queued` as
  "no text" is exactly the defect).
- The simplest form is one nullable moment: `deleting_at` (`NULL` = live, a date = pending,
  row gone = done). A wall at the door refuses work on a tenant in that state.
- The runner re-reads the state, does the step, settles in its own transaction, and is safe to
  re-run after a crash (files first, then rows; the second pass finds nothing to do).
- Readers that *wait* for the state (a queue that must not take a report whose words are still
  `Queued`) state that as a predicate, tested against the SQL twin on one set of rows.

Runner shape (claim → call outside a transaction → settle, lease, terminal degrade) →
[`outbox-and-background-jobs / process-as-state.md`](../../outbox-and-background-jobs/references/process-as-state.md).
Evidence: family-planner ADR 0076 (hearing is a state of the recording), ADR 0093 (deletion is a state).

## Derive, don't store — one reader per rule

When a rule changes over time (a schedule, a target, a rotation), store the **rule with a
`since`**, never its consequences:

- An edit acts from a stated moment (usually tomorrow); a day is judged by **the terms of that
  day**, so an edit never rewrites the past.
- Exactly **one function** reads the rule (`Days.judge(terms, date)`, `Slots.expand(rule, range)`);
  every caller — screen, job, counter, digest — goes through it. A second reader is the defect:
  the two disagree on an edge (a seat, a skipped date, a paused interval).
- An absence is an **outcome**, not a row: a missed day, an untouched occurrence are computed by
  that function; only touched days/occurrences are stored.
- Counting over a period (a promise, a streak) counts **from the rule**, never from whatever rows
  happen to exist — an untouched occurrence is work not done, not "nothing to count".

Evidence: family-planner ADR 0069/0070 (one slot expander), ADR 0079 (habit terms by date), ADR
0071 (a challenge counts from the rule). Full patterns → `virtual-recurrence-slots`,
`rule-as-of-date-counting` (planned skills).

## Rules

- `@SerialName` belongs on DTO mirrors in `api:contract`, never on domain types.
- `kotlin.Result<T>` is not a domain type: its error is `Throwable`, not a closed set. Use it only
  at stdlib/coroutine boundaries.
