# Pagination Envelope (cursor)

## Wire

```kotlin
@Serializable
data class PageDto<T>(
    val items: List<T>,
    val nextCursor: String? = null,   // opaque; null = last page
)
```

No `total`, no page numbers: lists are keyset-paginated (stable under concurrent inserts, O(1)
per page). See [kotlin-domain-modeling / flow-and-pagination.md](../../kotlin-domain-modeling/references/flow-and-pagination.md).

## Resource

```kotlin
@Resource("/v1/tasks")
class Tasks(val status: List<String> = emptyList(), val cursor: String? = null, val limit: Int = DEFAULT_LIMIT)
```

`limit` is bounded server-side (domain `require(limit in 1..MAX)` after mapping validated it).
A malformed cursor is `422 VALIDATION_ERROR`, not `500`.

## Client

```kotlin
data class CursorPage<T>(val items: List<T>, val nextCursor: String?) {
    val hasMore: Boolean get() = nextCursor != null
}
internal fun <T> PageDto<T>.toCursorPage() = CursorPage(items, nextCursor)
```

The wire envelope stops at `api:client`: screens see `CursorPage`, so the envelope can gain
fields without every list screen learning about them.

## Rules

- Every list endpoint answers a `PageDto` — never a bare `List<T>` (a fitness test checks the
  client's return types).
- Small, bounded collections (a tenant's members) still use the envelope; it costs nothing and
  keeps the rule absolute.
- The closed exceptions are **streams**, not lists: a binary download (`application/zip` of an
  export, streamed without a temporary file) and the live channel (SSE). Name them in the API design;
  a JSON array answer is never one of them.
