# Domain ↔ DTO Mapping (`api:mapping`)

`api:mapping` is KMP, depends on `domain` + `api:contract`, holds **no business logic** — only
translation and structural validation. It runs on the server (DTO → command) and in the UI
(DTO → domain view types the screens use).

## Domain → DTO (total, never fails)

```kotlin
fun Task.toDto(): TaskDto = TaskDto(
    id = id.value, title = spec.title, status = status.name,
    dueAt = dueAt?.toString(), version = version,
)
```

## DTO → Command (validates, returns `Either`)

```kotlin
fun CreateTaskRequest.toCommand(tenant: TenantId, actor: MemberId, role: Role): Either<DomainError, CreateTaskCommand> =
    either {
        val priority = enumOrLeft<Priority>(priority, "priority").bind()
        CreateTaskCommand(tenant, TaskId(id), actor, role, TaskSpec(title.trim(), priority))
    }

inline fun <reified E : Enum<E>> enumOrLeft(raw: String, field: String): Either<DomainError, E> =
    enumValues<E>().firstOrNull { it.name == raw }?.right()
        ?: DomainError.ValidationError("unknown $field '$raw'").left()
```

## DTO → domain on the client

Screens decide by domain types, never by wire strings (`status == "Pending"` in a composable is a
fitness-test failure). An **unknown** value from a newer server maps to `null`/an `Unknown`
variant and the UI degrades (hides the action) — it never throws.

## Rules

- KMP only; no JVM types.
- Tenant and actor come from the call context, passed in as parameters — never from the DTO.
- Never `valueOf()` on a wire string (throws); never `!!`.
- Tests in `commonTest`: both directions, unknown enum → `Left`, round-trip through `ApiJson`.
