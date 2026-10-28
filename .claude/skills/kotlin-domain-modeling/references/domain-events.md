# Domain Events + Transactional Outbox

Events decouple side effects (live sync, notifications, analytics, audit) from the use-case —
**without** losing them: they are written to an outbox table in the same transaction as the
state change, and a relay dispatches them afterwards.

## Event definition (commonMain)

```kotlin
interface DomainEvent {
    val tenantId: TenantId     // tenant partition of the outbox
    val occurredAt: Instant    // ALWAYS supplied by the caller — never defaulted to a wall clock
    val actor: Actor           // Member(id) or System — who caused it
}

data class TaskStarted(
    override val tenantId: TenantId,
    val taskId: TaskId,
    override val occurredAt: Instant,
    override val actor: Actor,
) : DomainEvent

/** Marker per area lets the outbox map a whole group of events with one branch. */
sealed interface CatalogDomainEvent : DomainEvent
```

- Events carry **ids, not payload copies**, unless a consumer needs the value at that moment
  (an amount, a verdict). Consumers re-read current state.
- Granularity follows the consumer: "one event per area, row id only" is fine when the client
  just invalidates a list.

## Outcome + outbox port

```kotlin
data class Outcome<out T>(val value: T, val events: List<DomainEvent>)

interface EventOutbox {
    /** Inside the caller's RW transaction; never opens its own. */
    suspend fun append(events: List<DomainEvent>): Either<DomainError, Unit>
}
```

The use-case appends **and** returns the events (`Outcome`): tests assert on them, and
in-process callers can react without re-reading the outbox.

## Rules

- `outbox.append(events)` runs in the same `inRwTransaction` as the write → "changed ⇔ recorded"
  is atomic. Never publish to an external system from inside the use-case.
- `occurredAt` = the single `clock.now()` the command read; all events of one command share it.
- Events are immutable `data class`es with `val`s only.
- An idempotent replay emits no events.
- Serialisation of events for the outbox lives in `server:infra` (`EventPayloadPm`), decoding back
  is exhaustive over the payload hierarchy — that is where a forgotten event would do damage.
- Relay, per-tenant sequence numbers, consumers, retention →
  [`outbox-and-background-jobs`](../../outbox-and-background-jobs/SKILL.md).
