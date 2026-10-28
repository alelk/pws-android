# Error Handling with `Either<DomainError, T>` (Arrow 2, Raise DSL)

## Why Either (and not exceptions)

Expected failures are **values**: the compiler makes every caller handle them, they compose, and
on a Compose web target an uncaught exception kills the whole canvas scene — a returned error is
far cheaper than a thrown one. The client mirrors the same idea (`Either<ApiError, T>`).

## Ports return Either

```kotlin
interface TaskRepository {
    suspend fun findById(tenantId: TenantId, id: TaskId): Either<DomainError, Task>
}
```

`findById` returns `NotFound` rather than `null` — "absent" is a domain answer. When a caller
wants "absent is fine", convert explicitly:

```kotlin
internal fun <T> Either<DomainError, T>.orAbsent(): Either<DomainError, T?> =
    when (this) {
        is Either.Right -> this
        is Either.Left -> if (value is DomainError.NotFound) Either.Right(null) else this
    }
```

## Compose with the Raise DSL

```kotlin
suspend operator fun invoke(cmd: AssignTaskCommand): Either<DomainError, Outcome<Task>> =
    txRunner.inRwTransaction(TenantContext.Tenant(cmd.tenantId)) {
        either {
            val task = tasks.findById(cmd.tenantId, cmd.taskId).bind()       // short-circuits on Left
            val member = members.findById(cmd.tenantId, cmd.assigneeId).bind()
            ensure(member.status == MemberStatus.Active) { DomainError.ValidationError("assignee is not active") }
            ensureNotNull(task.spec.title.takeIf { it.isNotBlank() }) { DomainError.ValidationError("untitled") }
            // …
        }
    }
```

- `bind()` unwraps a `Right` or short-circuits with the `Left`.
- `ensure(cond) { error }`, `ensureNotNull(x) { error }`, `raise(error)` for early exits.
- Helpers that participate in the DSL take `Raise<DomainError>` as a receiver
  (`suspend fun Raise<DomainError>.insertOrExisting(...)`) instead of returning `Either`.
- Use `flatMap`/`map` only for short, linear pipelines at the boundary (transport, mapping).

## Caller (transport) folds at the edge

```kotlin
deps.start(command)
    .map { it.value.toDto() }
    .fold({ call.respondError(it) }, { call.respond(HttpStatusCode.OK, it) })
```

## Rules

- Every use-case returns `Either<DomainError, T>`.
- Adapters catch the specific technology exception and map it (see
  [kotlin-clean-architecture / error-handling.md](../../kotlin-clean-architecture/references/error-handling.md)).
- Never `throw` a business error; never `runCatching` around domain code (it swallows
  `CancellationException` and every bug alike).
- Remember: inside `inRwTransaction`, a `Left` **rolls back** everything written before it;
  only `inRwTransactionKeepingWrites` commits it (see `transaction-runner.md`).
