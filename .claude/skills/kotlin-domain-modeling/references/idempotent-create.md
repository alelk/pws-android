# Idempotent Create — the command carries the new id

## Why

A create over a flaky network is retried: the request succeeded but the response was lost, or
an offline queue replays it. If the server assigns the id, the retry creates a **second** row.
If the client assigns it, the retry is recognisable: same id → same answer.

## Protocol

1. The client generates a UUIDv7 and sends it as `id` in the create request.
2. The use-case looks the id up **before any write**:
   - absent → normal create (events emitted);
   - present and the stored row matches the request (`sameRequest`) → return it, **no events**;
   - present but different → `Conflict` (someone reused an id — a client bug).
3. Two truly concurrent creates that both miss the lookup meet at the primary key; the loser gets
   `AlreadyExists` and its retry lands in case 2.
4. Transport answers `201` for a fresh create and the same body for the replay (a replay may
   answer `200`/`201` consistently — pick one and document it).

## Helper (commonMain)

```kotlin
internal suspend fun <T> Raise<DomainError>.insertOrExisting(
    entity: String,
    id: String,
    reread: suspend () -> Either<DomainError, T?>,
    sameRequest: (T) -> Boolean,
    insert: suspend () -> Outcome<T>,
): Outcome<T> {
    val existing = reread().bind() ?: return insert()
    ensure(sameRequest(existing)) { DomainError.Conflict(entity, id) }
    return Outcome(existing, emptyList())
}
```

Usage:

```kotlin
either {
    insertOrExisting(
        entity = "Task", id = command.id.value,
        reread = { tasks.findById(command.tenantId, command.id).orAbsent() },
        sameRequest = { it.createdBy == command.createdBy && it.spec.title == command.spec.title },
    ) {
        val saved = tasks.insert(buildTask(command)).bind()
        val events = listOf(TaskCreated(saved.tenantId, saved.id, now, actor))
        outbox.append(events).bind()
        Outcome(saved, events)
    }
}
```

## Why look up first instead of "insert, catch duplicate, re-read"

In PostgreSQL a failed `INSERT` aborts the transaction — the re-read cannot run in it, and the
tenant setting is gone with the rollback. And a create that *debits or reserves* something before
inserting must not do that twice. Lookup-first keeps the whole thing in one clean transaction.

## Rules

- `sameRequest` compares the fields a person actually sent (author + defining content), not
  server-computed ones (timestamps, version).
- A replay emits **no events** — nothing happened.
- The client keeps the generated id until it sees a definitive answer; a `409 Conflict` on replay
  means "your id collided with something else", not "already created".
- Test it: create twice with the same id → one row, one event; same id with a different title →
  `Conflict`.
