# Domain Test Doubles & Fixtures

## Layout (domain/src/commonTest)

```
commonTest/kotlin/<pkg>/domain/
├── fakes/                 # in-memory port implementations + TestClock + freshId()
│   ├── FakeRepositories.kt
│   ├── FakeEventOutbox.kt
│   └── TestClock.kt
├── fixtures/Fixtures.kt   # object mothers: aTask(), aMember(), aTenant() …
└── <context>/<part>/*Test.kt
```

## Fakes, not mocks

```kotlin
class FakeTaskRepository(val store: MutableMap<TaskId, Task> = mutableMapOf()) : TaskRepository {
    fun seed(task: Task) = task.also { store[it.id] = it }

    override suspend fun findById(tenantId: TenantId, id: TaskId): Either<DomainError, Task> =
        store[id]?.takeIf { it.tenantId == tenantId }?.right()          // wrong tenant → NotFound
            ?: DomainError.NotFound("Task", id.value).left()

    override suspend fun update(task: Task, expectedVersion: Long): Either<DomainError, Task> {
        val current = store[task.id] ?: return DomainError.NotFound("Task", task.id.value).left()
        if (current.version != expectedVersion) return DomainError.Conflict("Task", task.id.value).left()
        return task.copy(version = expectedVersion + 1).also { store[it.id] = it }.right()
    }
}
```

- Fakes **implement the port's contract** (tenant scoping, CAS, duplicate PK) — a fake that
  ignores `expectedVersion` makes every concurrency test meaningless.
- One fake per port, shared by all tests; MockK stays out of `commonTest` (JVM-only).
- `FakeEventOutbox` records appended events; assert on them.

## Object mothers

```kotlin
fun aTask(
    id: TaskId = TaskId("task-1"),
    tenantId: TenantId = TenantId("tenant-1"),
    status: TaskStatus = TaskStatus.Pending,
    assigneeId: MemberId? = MemberId("mem-1"),
    version: Long = 1,
): Task = Task(id = id, tenantId = tenantId, status = status, /* sensible defaults */)
```

- Every parameter defaulted; a test names only what it is about (`aTask(status = Returned)`).
- Deterministic defaults (fixed instants, fixed ids); `freshId("task")` when uniqueness matters.
- One `Fixtures.kt` (or one per context) — never re-declare builders per test file.
- Detekt relaxes `LongParameterList` for `fixtures/` and `fakes/` — that is what makes call
  sites readable.

## TestClock

```kotlin
class TestClock(private var instant: Instant = Instant.parse("2026-01-01T09:00:00Z")) : Clock {
    override fun now() = instant
    fun advance(by: Duration) { instant += by }
}
```

## Property-based tests (optional)

Kotest `Arb` generators pay off for pure functions with large input spaces (a recurrence rule,
a status machine, a money calculation). Keep them next to the object mothers
(`fixtures/Arbs.kt`); don't make them the default for use-case tests.

## Sharing fixtures with other modules

`server:infra` repository tests need the same builders. Either a `:domain:test-fixtures` module
or Gradle's `java-test-fixtures` for JVM modules; don't copy the mothers.
