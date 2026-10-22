# Validation Boundaries

**Rule:** each layer validates what only it can know — never the same check twice.

```
HTTP body ──(kotlinx.serialization: shape, types)──▶ DTO
DTO ──(api:mapping toCommand(): parse ids/enums/dates, required fields)──▶ Either<DomainError, Command>
Command ──(use-case: invariants, existence, permissions, cross-entity rules)──▶ Either<DomainError, Outcome>
DB ──(CHECK / FK / UNIQUE constraints: the last line, not the validation)
```

## Structural — `api:mapping`

Turns wire strings into domain types. It never throws: an unknown enum value is a `Left`, not an
`IllegalArgumentException`.

```kotlin
fun CreateTaskRequest.toCommand(tenantId: TenantId, actor: MemberId, role: Role): Either<DomainError, CreateTaskCommand> =
    either {
        val priority = Priority.entries.firstOrNull { it.name == priority }
            ?: raise(DomainError.ValidationError("unknown priority '$priority'"))
        CreateTaskCommand(tenantId, TaskId(id), actor, role, TaskSpec(title.trim(), priority))
    }
```

## Business — the use-case

Existence, uniqueness, who may do what, state-machine legality, limits that depend on stored
data. Expressed with `ensure`/`raise` inside `either { }`.

## Database — constraints mirror, they don't validate

A `CHECK (length(title) <= 120)` mirrors the domain constant and says so in a comment. The domain
rejects first with a 422; the constraint catches a bug or a manual edit.

## Rules

- Mapping returns `Either<DomainError, Command>` (or collects several field errors when the UI
  needs them all at once).
- `require()` in a value-class `init` is for **programmer errors** (blank id) — mapping must have
  rejected bad input before constructing the value.
- Never repeat a business rule in mapping "for convenience"; the second copy drifts.
