# `OptionalField<T>` — Partial Updates (PATCH)

`null` in a PATCH is ambiguous: "not sent" or "set to null"? `OptionalField` carries three
intents explicitly.

## Domain definition (commonMain)

```kotlin
sealed interface OptionalField<out T> {
    data object Unchanged : OptionalField<Nothing>
    data class Set<out T>(val value: T) : OptionalField<T>
    data object Clear : OptionalField<Nothing>          // only meaningful for nullable targets
}

fun <T> OptionalField<T?>.resolve(current: T?): T? = when (this) {
    OptionalField.Unchanged -> current
    is OptionalField.Set -> value
    OptionalField.Clear -> null
}

/** For targets whose "empty" is a value, not null (e.g. a policy layer's EMPTY). */
fun <T> OptionalField<T>.resolve(current: T, clearsTo: T): T = when (this) {
    OptionalField.Unchanged -> current
    is OptionalField.Set -> value
    OptionalField.Clear -> clearsTo
}
```

## Command shape

```kotlin
data class UpdateTaskCommand(
    val tenantId: TenantId,
    val taskId: TaskId,
    val expectedVersion: Long,
    val title: String? = null,                                         // required field: null = unchanged
    val dueAt: OptionalField<Instant?> = OptionalField.Unchanged,      // nullable: can be cleared
    val assigneeId: OptionalField<MemberId?> = OptionalField.Unchanged,
)
```

## Wire mirror

`api:contract` cannot see `domain`, so it has its own `OptionalFieldDto` with a custom serializer:
field omitted → `Unchanged` (property default), JSON `null` → `Clear`, value → `Set`. The mapping
`OptionalFieldDto → OptionalField` lives in `api:mapping`. See
[ktor-api-contract / sealed-dtos.md](../../ktor-api-contract/references/sealed-dtos.md).

## Rules

- Required (non-nullable) entity fields: plain `T? = null` means unchanged.
- Nullable fields: `OptionalField<T?>`.
- The DTO property of type `OptionalFieldDto` **must default to `Unchanged`**, and the response
  JSON policy (`explicitNulls = false`) does not affect it — PATCH semantics live on requests only.
