# Usage — Kotlin Domain Modeling

Prerequisite: [`kotlin-clean-architecture`](../kotlin-clean-architecture/SKILL.md) applied.

## 1. Shared kernel (`domain/common/`)

| File                    | Contents                                                                  |
|-------------------------|---------------------------------------------------------------------------|
| `<Entity>Id.kt`         | one value class per id (see `value-types.md`)                             |
| `IdGeneration.kt`       | `internal fun newId() = Uuid.generateV7().toString()` for server-made rows |
| `DomainError.kt`        | the sealed error hierarchy                                                |
| `Actor.kt`              | `sealed interface Actor { Member(id); System }`                           |
| `OptionalField.kt`      | tri-state PATCH field + `resolve()` helpers                               |
| `tx/TransactionRunner.kt`, `tx/TenantContext.kt` | transaction boundary (see `transaction-runner.md`) |
| `event/DomainEvent.kt`, `event/EventOutbox.kt`, `event/Outcome.kt` | events + outbox port    |
| `event/IdempotentCreate.kt` | `insertOrExisting` helper (see `idempotent-create.md`)                |

## 2. A context part

```
domain/src/commonMain/kotlin/<pkg>/domain/planning/task/
├── Task.kt                # entity: data class + transition functions returning Either
├── TaskStatus.kt          # enum / sealed
├── TaskStatusMachine.kt   # pure transition table
├── TaskRepository.kt      # port
├── CreateTask.kt          # CreateTaskCommand + CreateTask
├── StartTask.kt
└── UpdateTask.kt          # UpdateTaskCommand uses OptionalField for nullable fields
```

## 3. Write the use-case

Shape → [`references/use-cases-and-commands.md`](references/use-cases-and-commands.md).
Create with a client id → [`references/idempotent-create.md`](references/idempotent-create.md).

## 4. Port shape

```kotlin
interface TaskRepository {
    suspend fun insert(task: Task): Either<DomainError, Task>                            // PK dup → AlreadyExists
    suspend fun update(task: Task, expectedVersion: Long): Either<DomainError, Task>     // stale → Conflict
    suspend fun findById(tenantId: TenantId, id: TaskId): Either<DomainError, Task>      // wrong tenant → NotFound
    suspend fun list(tenantId: TenantId, query: TaskQuery): Either<DomainError, TaskRowsPage> // keyset page
}
```

## 5. Tests in `commonTest`

```kotlin
class StartTaskTest : FunSpec({
    test("the assignee starts a pending task and TaskStarted is stamped with the clock") {
        val tasks = FakeTaskRepository().apply { seed(aTask(status = TaskStatus.Pending)) }
        val outbox = FakeEventOutbox()
        val clock = TestClock()
        val start = StartTask(tasks, outbox, NoopTransactionRunner(), clock)

        val outcome = start(StartTaskCommand(FAMILY, TASK, ASSIGNEE, expectedVersion = 1)).shouldBeRight()

        outcome.value.status shouldBe TaskStatus.InProgress
        outbox.events.single().occurredAt shouldBe clock.now()
    }

    test("someone else is Forbidden and nothing is written") {
        // …shouldBeLeft().shouldBeInstanceOf<DomainError.Forbidden>(); outbox.events shouldBe empty
        // The fake runner has no transactions: "nothing is written" holds here only because the
        // refusal comes first. After a write, only a real-DB test proves the rollback.
    }
})
```

Fakes, object mothers, `TestClock` → [`references/test-fixtures.md`](references/test-fixtures.md).

## 6. Verify

```bash
./gradlew :domain:jvmTest
./gradlew :domain:compileTestKotlinWasmJs   # commonTest is portable off the JVM too
./gradlew build
```

Then walk through [`checklists/domain-checklist.md`](checklists/domain-checklist.md).

## Notes

- `Fake*` doubles live in `commonTest` (MockK is JVM-only and would break portability).
- A read-only use-case uses `inRoTransaction` — the database rejects writes inside it.
- A refused `Left` from `inRwTransaction` rolls everything back — the events in the `Outcome`
  you never returned are gone with it. A write that must survive the refusal goes through
  `inRwTransactionKeepingWrites` and gets a line in `KNOWN_COMMITS_ON_LEFT`
  ([`references/transaction-runner.md`](references/transaction-runner.md)); copy
  [`templates/domain/CommitsOnLeftTest.kt`](templates/domain/CommitsOnLeftTest.kt) into `domain/src/jvmTest`.
- A real `ExposedTransactionRunner` lives in `server:infra` — see
  [`exposed-postgres`](../exposed-postgres/SKILL.md) and
  [`postgres-multitenancy-rls`](../postgres-multitenancy-rls/SKILL.md).
