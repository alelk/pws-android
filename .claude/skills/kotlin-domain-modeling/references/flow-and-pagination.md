# Repositories, Flow and Pagination

## `suspend fun` vs `Flow`

- Server ports (Exposed/JDBC): `suspend fun` only — there is no reactive query API, and the
  transaction boundary is the use-case.
- Client-side stores that the UI observes: expose `StateFlow` (see
  [compose-multiplatform-ui / state-viewmodel.md](../../compose-multiplatform-ui/references/state-viewmodel.md)).
- Local databases with reactive queries (Room, SQLDelight): `Flow<T>` ports are free.

## Pagination: keyset (cursor), not offset

Lists that grow are paginated by a **keyset cursor**: stable under inserts, O(1) per page,
no `COUNT(*)`.

```kotlin
data class TaskQuery(
    val statuses: Set<TaskStatus> = emptySet(),
    val assigneeId: MemberId? = null,
    val cursor: String? = null,         // opaque token from the previous page
    val limit: Int = DEFAULT_LIMIT,
) {
    init { require(limit in 1..MAX_LIMIT) }
}

data class TaskRowsPage(val items: List<Task>, val nextCursor: String?)   // null = last page
```

- The cursor encodes `(sort key, id)` of the last row. The **adapter** owns the encoding
  (URL-safe base64 of `"<epochMs>:<id>"`), so the domain and the client treat it as opaque; a
  malformed token decodes to `ValidationError`, not an infrastructure error.
- The query is `WHERE (sort_key, id) > (?, ?) ORDER BY sort_key, id LIMIT n + 1` — the extra row
  says whether there is a next page without a second query.
- No `total`: a keyset scan does not count the tail. If a screen truly needs a count, it is a
  separate, cheaper query (or a badge aggregate).
- Offset pagination is acceptable only for small, admin-style lists that never change during
  paging.

## Working sets without pagination

Some reads are bounded by *domain* windows instead (the board: all non-terminal tasks + done
within N days + dated up to a horizon). Express the window as a pure domain predicate **and**
as SQL, and test that both agree on the same seeded data
([kotlin-testing-strategy](../../kotlin-testing-strategy/SKILL.md), "SQL predicate == domain predicate").

## Coroutine rules

- Never swallow `CancellationException`: `catch (e: CancellationException) { throw e }` before any
  broad `catch`, and never `runCatching` around suspending domain calls.
- Inject dispatchers into adapters that need them; domain code never picks a dispatcher.
- Flows: `.catch { }` before `.collect { }`; share with `stateIn(scope, WhileSubscribed(5_000), initial)`.
