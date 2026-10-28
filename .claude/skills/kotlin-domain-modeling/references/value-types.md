# Value Classes & KMP-Compatible Types

## Ids

```kotlin
@JvmInline
value class TaskId(val value: String) {
    init { require(value.isNotBlank()) { "TaskId must not be blank" } }
}
```

- **String-backed UUIDs** keep ids opaque, serialisable everywhere (wasm included) and cheap to
  log. The adapter maps them to `uuid` columns (`Uuid.parse(value)`).
- **Client-generated** for anything a person creates: the client sends a UUIDv7 in the create
  request, which makes retries idempotent (see `idempotent-create.md`).
- **Server-generated** rows (events, ledger entries, jobs) use one helper:

  ```kotlin
  @OptIn(ExperimentalUuidApi::class)
  internal fun newId(): String = Uuid.generateV7().toString()   // time-ordered → index-friendly
  ```

- UUIDv7 over v4: time-ordered keys keep B-tree inserts local and give a free creation order.
- One file per id type in `domain/common/` (or next to its entity when used by one context only).

## Validated primitives

```kotlin
@JvmInline value class Email(val value: String) {
    init { require('@' in value) { "not an email" } }
}
```

`require` in `init` guards **programmer errors**. User input is rejected earlier, in mapping,
with a `Left` — constructing the value class is the last step, not the validation.

## KMP substitutions

| ❌ JVM-only                   | ✅ KMP-compatible                                          |
|------------------------------|-----------------------------------------------------------|
| `java.util.UUID`             | `kotlin.uuid.Uuid` (`generateV7()`)                        |
| `java.time.Instant`          | `kotlin.time.Instant`                                      |
| `java.time.Clock`            | `kotlin.time.Clock` (inject it)                            |
| `java.time.Duration`         | `kotlin.time.Duration`                                     |
| `java.time.LocalDate/Time`   | `kotlinx.datetime.LocalDate` / `LocalTime` / `TimeZone`    |
| `java.io.File`               | `@JvmInline value class FilePath(val value: String)`       |
| `java.net.URI` / `URL`       | `@JvmInline value class Url(val value: String)`            |
| `java.util.Base64`           | `kotlin.io.encoding.Base64`                                |
| `System.currentTimeMillis()` | `clock.now().toEpochMilliseconds()`                        |

Calendar logic (a tenant's local day, "is it 18:00 in their timezone") needs
`kotlinx-datetime`; compute it in the domain with the tenant's `TimeZone`, never with the
server's default zone.

## Rules

- Never raw `String`/`Long`/`Uuid` as an id in a signature.
- No `java.*` in `commonMain` — `compileKotlinWasmJs` proves it.
- A value that changes meaning with a unit (money, points, bytes) is a value class or carries
  its unit in the name (`limitBytes`).
