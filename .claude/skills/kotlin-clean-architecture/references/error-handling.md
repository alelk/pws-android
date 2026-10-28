# Error Handling Across Layers

Domain returns `Either<DomainError, T>` for every expected failure (see
[kotlin-domain-modeling / either-error-handling.md](../../kotlin-domain-modeling/references/either-error-handling.md)).
Exceptions are for bugs and for infrastructure that must roll a transaction back.

## Designing `DomainError`

One sealed class in `domain/common`. **A new variant exists when the client's reaction differs,
not when the message differs.**

```kotlin
sealed class DomainError {
    data class NotFound(val entity: String, val id: String) : DomainError()          // 404
    data class AlreadyExists(val entity: String, val field: String, val value: String) : DomainError() // 409
    data class Conflict(val entity: String, val id: String) : DomainError()          // 409 stale version: re-read + replay
    data class ValidationError(val message: String) : DomainError()                  // 422
    data class Gone(val entity: String, val id: String) : DomainError()              // 410 address valid, thing gone
    data class Unauthorized(val reason: String) : DomainError()                      // 401
    data class Forbidden(val reason: String) : DomainError()                         // 403 this role may not
    data object NoActiveTenant : DomainError()                                       // 403 NO_ACTIVE_TENANT: pick a tenant first
    data class QuotaExceeded(val resetAt: Instant) : DomainError()                   // 429 + Retry-After
    data class Upstream(val service: String, val reason: String? = null) : DomainError() // 502 provider failed, not us
    data class InfrastructureError(val message: String, val cause: String? = null) : DomainError() // 500
}
```

Rules of thumb:

- `Conflict` ≠ `AlreadyExists`: "someone else moved it, re-read" vs "your id is taken".
- `Upstream` (502) ≠ `InfrastructureError` (500): the screen says "try again" vs "we broke".
- A refusal that carries a *time* (`QuotaExceeded.resetAt`, a lockout's `until`) is a variant —
  a limit without its number is advice, not a rule.
- A **foreign tenant's** resource is `NotFound`, never `Forbidden` — don't reveal it exists.
- Keep the variant coarse where detail would leak (auth failures never say whether an account exists).

## Adapters map exceptions narrowly

```kotlin
internal inline fun <T> catchingDb(block: () -> T): Either<DomainError, T> =
    try { block().right() }
    catch (e: CancellationException) { throw e }                // cooperative cancellation
    catch (e: SQLException) {
        TransactionManager.current().rollback()                   // clear the aborted PG transaction
        DomainError.InfrastructureError(e.message ?: "SQL error", e::class.simpleName).left()
    }
```

- Catch the **specific** technology exception (`SQLException`, `IOException`), never `Exception`
  inside an adapter — and always rethrow `CancellationException` first.
- Map known SQLSTATEs to domain meaning: `23505` on the PK → `AlreadyExists`; on a named "race"
  index → `Conflict` (retryable).

## Transport maps every variant, exhaustively

```kotlin
fun DomainError.toHttpStatus(): HttpStatusCode = when (this) {   // no `else` — a new variant must not compile
    is DomainError.NotFound -> HttpStatusCode.NotFound
    is DomainError.Conflict, is DomainError.AlreadyExists -> HttpStatusCode.Conflict
    // …
}
```

- Client-facing `message` never leaks infrastructure detail; `correlationId` links to logs.
- Headers derived from a variant (`Retry-After`) are set in the **one** `respondError` helper, so a
  new producer can't ship without them.
- Transport never `try/catch`es a use-case call; `StatusPages` handles true bugs (500 + correlationId).

## Transactions and `Left`

`inRwTransaction` rolls the transaction back when the block returns a `Left` (and on an
exception): a refused command leaves nothing behind, its outbox rows included. The few writes
that must outlive a refusal (a failed-attempt counter, the audit row of a call that happened) use
`inRwTransactionKeepingWrites`, each named with its reason in the `KNOWN_COMMITS_ON_LEFT` ratchet.
Details →
[kotlin-domain-modeling / transaction-runner.md](../../kotlin-domain-modeling/references/transaction-runner.md).
