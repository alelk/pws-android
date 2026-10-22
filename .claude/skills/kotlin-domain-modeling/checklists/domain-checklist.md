# Domain Modeling Checklist

## Types

- [ ] Every entity id is a `@JvmInline value class`; no raw `String`/`Uuid`/`Long` ids in signatures
- [ ] Created entities get their id from the command (client UUIDv7); server rows use `newId()` (v7)
- [ ] Time types are `kotlin.time.Instant`/`Duration` and `kotlinx.datetime.*`; no `java.time`
- [ ] No `java.*` in `commonMain`; `./gradlew :domain:compileKotlinWasmJs` passes
- [ ] Closed sets are sealed; `when` over them has no `else`
- [ ] State machines are pure tables, complete for every transition
- [ ] A rule that changes over time carries `since`; exactly one function reads it; misses are computed, not stored

## Errors

- [ ] One `DomainError` sealed class; a variant per distinct client reaction
- [ ] Use-cases/ports return `Either<DomainError, T>`; composition via `either { bind/ensure }`
- [ ] No `throw` for business errors, no `runCatching` around domain calls

## Use-cases

- [ ] Verb-named class; `<Verb><Noun>Command` in the same file; one `suspend operator fun invoke`
- [ ] `TransactionRunner` with an explicit `TenantContext`; RO for reads, RW for writes
- [ ] `Clock` injected; `clock.now()` read once and used for every event of the command
- [ ] Authorisation decided in the use-case from actor fields of the command
- [ ] Updates pass `expectedVersion`; stale → `Conflict`
- [ ] Creates are idempotent (`insertOrExisting`): replay → same answer, no events; different body → `Conflict`
- [ ] Writes use `inRwTransaction` (a `Left` rolls back); `inRwTransactionKeepingWrites` only where the
      write is the record of the refusal, and that file is in `KNOWN_COMMITS_ON_LEFT` with a reason
- [ ] The use-case KDoc says what a refusal leaves behind (nothing, or the named record)
- [ ] A batch over many rows in one transaction is one row per transaction or a savepoint per row
- [ ] A step that cannot fit in one transaction is a stored state with a resumable runner

## Events

- [ ] Every mutation appends its events to `EventOutbox` in the same transaction and returns `Outcome`
- [ ] Events carry tenant id, `occurredAt` (supplied, not defaulted), `actor`

## Tests

- [ ] Domain tests live in `commonTest` and compile for wasm (`compileTestKotlinWasmJs`)
- [ ] Fakes honour the port contract (tenant scoping, CAS, duplicate PK); no MockK in `commonTest`
- [ ] Object mothers with defaulted parameters in `fixtures/`; `TestClock` for time
- [ ] Every use-case has a happy path, a refusal per `ensure`, and an assertion on emitted events
- [ ] `CommitsOnLeftTest` ratchet green; real-DB tests cover "rolls back on Left" and every keeping-writes case
