# Use-Cases & Command Models

A use-case is **one class, one business operation, one entry point**. The convention is
mechanical on purpose: every use-case reads the same, so an agent (or a reviewer) sees a
deviation instantly.

## Shape

```kotlin
data class DeclineTaskCommand(
    val tenantId: TenantId,          // tenant — from the JWT, set by transport
    val taskId: TaskId,
    val actorId: MemberId,           // who is acting
    val actorRole: Role,             // authorisation input; the decision is made HERE
    val expectedVersion: Long,       // optimistic lock
)

class DeclineTask(
    private val tasks: TaskRepository,
    private val outbox: EventOutbox,
    private val txRunner: TransactionRunner,
    private val clock: Clock,
) {
    suspend operator fun invoke(command: DeclineTaskCommand): Either<DomainError, Outcome<Task>> =
        txRunner.inRwTransaction(TenantContext.Tenant(command.tenantId)) {
            either {
                val task = tasks.findById(command.tenantId, command.taskId).bind()
                ensure(task.obligation == Obligation.Optional) { DomainError.ValidationError("only optional tasks can be declined") }
                ensure(task.assigneeId == command.actorId) { DomainError.Forbidden("only the assignee may decline") }
                val now = clock.now()
                val saved = tasks.update(task.moveTo(TaskTransition.Declined, now).bind(), command.expectedVersion).bind()
                val events = listOf(TaskDeclined(saved.tenantId, saved.id, now, Actor.Member(command.actorId)))
                outbox.append(events).bind()
                Outcome(saved, events)
            }
        }
}
```

## Rules

- Name: `<Verb><Noun>` (`CreateTask`, `StartTask`, `SpendPoints`). The command is
  `<Verb><Noun>Command` **in the same file**.
- Exactly one public member: `suspend operator fun invoke(command)`. Call sites read
  `startTask(command)`.
- Constructor takes ports only (+ `Clock`, + pure policies). Never a DTO, never a framework type.
- Authorisation ("who may") is decided in the use-case from `actorId`/`actorRole` in the command
  — transport only extracts them from the token.
- Returns `Either<DomainError, Outcome<T>>` for mutations (value + raised events), plain
  `Either<DomainError, T>` for queries.
- Read `clock.now()` **once** and stamp every event of the command with that instant.
- Shared steps (resolve an assignee, apply side effects of a transition) are extracted into
  small collaborators injected into the use-case (`TransitionEffects`), not into base classes.

## Commands

- `data class`, only domain types (value-class ids, enums, `LocalDate`, `Instant`) — parsing from
  wire strings happened in `api:mapping`.
- The **new entity's id** is a command field for creates (see `idempotent-create.md`).
- PATCH commands use `OptionalField<T?>` for nullable fields and `T? = null` for required ones
  (see `optional-field.md`).
- Entity construction from a command goes through a builder/factory that enforces invariants
  (`PendingTaskDraft(...).build()`), not through a `toEntity()` that fakes a DB id.

## Anti-patterns

| Anti-pattern                                  | Fix                                                    |
|-----------------------------------------------|--------------------------------------------------------|
| `TaskService` with ten methods                | ten use-cases                                          |
| `Clock.System.now()` inside the use-case       | inject `Clock`; tests use a `TestClock`                |
| `SongId(0) // DB assigns the id`               | id in the command; `insert` fails on a duplicate PK    |
| Role check in the route                        | role in the command; `ensure` in the use-case          |
| Events published after the transaction         | `outbox.append(events)` inside it                      |
