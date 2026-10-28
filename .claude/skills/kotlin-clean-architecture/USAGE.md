# Usage — Kotlin Clean Architecture

Prerequisite: [`kotlin-project-layout`](../kotlin-project-layout/SKILL.md) applied (modules
declared, convention plugins in place). Code shapes below follow
[`kotlin-domain-modeling`](../kotlin-domain-modeling/SKILL.md).

## 1. Domain module skeleton

```kotlin
// domain/build.gradle.kts
plugins { id("app.kmp") }
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.arrow.core)
        }
        commonTest.dependencies {
            implementation(libs.kotest.framework.engine)
            implementation(libs.kotest.assertions)
        }
        jvmTest.dependencies { implementation(libs.kotest.runner.junit5) }
    }
}
```

```
domain/src/commonMain/kotlin/<pkg>/domain/
├── common/            DomainError, ids, Actor, OptionalField
│   ├── tx/            TransactionRunner, TenantContext
│   └── event/         DomainEvent, EventOutbox, Outcome
└── <context>/<part>/  entity + port + use-cases (+ commands in the same file)
```

## 2. A context part

```
domain/.../planning/task/
├── Task.kt              # entity (+ invariants, transitions)
├── TaskRepository.kt    # port
├── CreateTask.kt        # CreateTaskCommand + CreateTask use-case
└── StartTask.kt
```

```kotlin
class StartTask(
    private val tasks: TaskRepository,
    private val outbox: EventOutbox,
    private val txRunner: TransactionRunner,
    private val clock: Clock,
) {
    suspend operator fun invoke(command: StartTaskCommand): Either<DomainError, Outcome<Task>> =
        txRunner.inRwTransaction(TenantContext.Tenant(command.tenantId)) {
            either {
                val task = tasks.findById(command.tenantId, command.taskId).bind()
                ensure(task.assigneeId == command.actorId) { DomainError.Forbidden("only the assignee may start") }
                val now = clock.now()
                val saved = tasks.update(task.start(now).bind(), command.expectedVersion).bind()
                val events = listOf(TaskStarted(saved.tenantId, saved.id, now, Actor.Member(command.actorId)))
                outbox.append(events).bind()
                Outcome(saved, events)
            }
        }
}
```

## 3. Adapter in `server:infra`

```kotlin
// server/infra/build.gradle.kts
plugins { id("app.jvm.serialization") }
dependencies {
    implementation(projects.domain)         // ONLY domain — never api:*
    implementation(libs.exposed.core); implementation(libs.exposed.jdbc)
}
```

```kotlin
internal class ExposedTaskRepository : TaskRepository {
    override suspend fun findById(tenantId: TenantId, id: TaskId): Either<DomainError, Task> =
        catchingDb {
            TaskTable.selectAll()
                .where { (TaskTable.id eq id.uuid()) and (TaskTable.tenantId eq tenantId.uuid()) }
                .firstOrNull()?.toTask()
        }.flatMap { it?.right() ?: DomainError.NotFound("Task", id.value).left() }
}
```

## 4. Route in `server:transport` (no business logic)

```kotlin
tenantPost<Tasks.Id.Start> { resource, context ->          // tenant comes from the JWT
    val request = call.receive<StartTaskRequest>()
    deps.start(StartTaskCommand(context.tenantId, TaskId(resource.parent.id), context.memberId, request.version))
        .map { it.value.toDto() }
        .fold({ call.respondError(it) }, { call.respond(HttpStatusCode.OK, it) })
}
```

## 5. DI — one Koin module per context

```kotlin
val planningModule = module {
    single<TaskRepository> { ExposedTaskRepository() }
    factoryOf(::StartTask)
}
val infrastructureModule = module {
    single<TransactionRunner> { ExposedTransactionRunner() }
    single<ReviewerPort> { if (get<LlmConfig>().apiKey.isBlank()) UnconfiguredLlm() else BudgetedLlm(OpenRouterLlm(get()), get()) }
}
```

## 6. Guard the boundaries mechanically

- Module graph: Gradle — what isn't a dependency can't be imported.
- Domain purity: `./gradlew :domain:compileKotlinWasmJs` (fails on `java.*`).
- Context order + cycles: `ContextDependencyTest` in `domain/src/jvmTest` (template
  [`architecture-fitness-tests/templates/domain/ContextDependencyTest.kt`](../architecture-fitness-tests/templates/domain/ContextDependencyTest.kt)).
- A downstream reaction needed synchronously → a reverse port, not a cycle
  ([`references/cross-context-reactions.md`](references/cross-context-reactions.md)); the order of
  an effect funnel → a test in `server:di` over the production Koin module.

```bash
./gradlew :domain:dependencies --configuration commonMainRuntimeClasspath | grep -E "ktor|exposed|flyway|koin"
# expected: nothing
./gradlew build
```

Then walk through [`checklists/architecture-checklist.md`](checklists/architecture-checklist.md).

## Notes

- One use-case class = one business operation, named by the verb (`StartTask`, not `TaskService`).
- A use-case that truly needs server-only infrastructure still lives in `domain` behind a port;
  only the *adapter* is server-specific.
- `if/when` on domain state in transport means logic leaked — move it into the use-case.
