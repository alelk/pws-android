# Usage — Ktor API Contract

Prerequisites: [`kmp-architecture`](../kmp-architecture/SKILL.md),
[`kotlin-clean-architecture`](../kotlin-clean-architecture/SKILL.md).

## 1. Modules

```bash
cp <skill>/templates/api-contract/build.gradle.kts.snippet api/contract/build.gradle.kts
cp <skill>/templates/api-mapping/build.gradle.kts.snippet  api/mapping/build.gradle.kts
cp <skill>/templates/api-contract/*.kt   api/contract/src/commonMain/kotlin/<pkg>/contract/common/
mv api/contract/src/commonMain/kotlin/<pkg>/contract/common/ApiError{Dto,Codes}.kt \
   api/contract/src/commonMain/kotlin/<pkg>/contract/error/      # package …contract.error
cp <skill>/templates/api-mapping/Mapping.kt api/mapping/src/commonMain/kotlin/<pkg>/mapping/common/
cp <skill>/templates/api-client/*.kt     api/client/src/commonMain/kotlin/<pkg>/client/
```

`contract/` packages: `common/`, `error/`, `route/` (all `@Resource`s), one package per API area.

## 2. DTOs for a new area

```kotlin
/** `POST /v1/tasks` — client-generated [id]; a replay with the same id returns the same task. */
@Serializable
data class CreateTaskRequest(
    val id: String,
    val title: String,
    val priority: String = "Normal",              // enum by stable name; new fields default
)

/** `PATCH /v1/tasks/{id}` — last-seen [version]; absent = unchanged, null = clear. */
@Serializable
data class UpdateTaskRequest(
    val version: Long,
    val title: String? = null,
    val dueAt: OptionalFieldDto<String> = OptionalFieldDto.Unchanged,
)

@Serializable
data class TaskDto(val id: String, val title: String, val status: String, val dueAt: String? = null, val version: Long)
```

Polymorphic bodies → [`references/sealed-dtos.md`](references/sealed-dtos.md).

## 3. Routes

```kotlin
@Resource("/v1/tasks")
class Tasks(val status: List<String> = emptyList(), val cursor: String? = null, val limit: Int = DEFAULT_LIMIT) {
    @Resource("{id}")
    class Id(val parent: Tasks = Tasks(), val id: String) {
        @Resource("transitions") class Transitions(val parent: Id)
    }
    companion object { const val DEFAULT_LIMIT = 50 }
}
```

URL rules → [`references/url-design.md`](references/url-design.md).

## 4. Mapping (`api:mapping`)

```kotlin
fun Task.toDto(): TaskDto = TaskDto(id.value, spec.title, status.name, dueAt?.toString(), version)

fun UpdateTaskRequest.toCommand(tenant: TenantId, taskId: TaskId, actor: MemberId): Either<DomainError, UpdateTaskCommand> =
    either {
        UpdateTaskCommand(
            tenantId = tenant, taskId = taskId, actorId = actor, expectedVersion = version,
            title = title?.trim(),
            dueAt = dueAt.toDomain { parseInstant(it).bind() },
        )
    }
```

Rules → [`references/domain-dto-mapping.md`](references/domain-dto-mapping.md).

## 5. Transport (server)

```kotlin
authenticate("jwt") {
    tenantPatch<Tasks.Id> { resource, ctx ->
        call.receive<UpdateTaskRequest>()
            .toCommand(ctx.tenantId, TaskId(resource.id), ctx.memberId)
            .flatMap { deps.update(it) }
            .map { it.value.toDto() }
            .fold({ call.respondError(it) }, { call.respond(HttpStatusCode.OK, it) })
    }
}
```

A new refusal a screen must recognise: add a constant to `ValidationReasons` (`api:contract`), a
constant to the domain's `ValidationReason` enum, and its line in the transport's exhaustive
`wireName()`; the screen compares `info.reason` with the constant
→ [`references/error-format.md`](references/error-format.md). A new code: a constant in
`ApiErrorCodes`, never a literal.

## 6. Client (`api:client`)

```kotlin
interface TaskApi {                                               // one interface per file, no bodies
    suspend fun list(filter: TaskListFilter = TaskListFilter()): Either<ApiError, CursorPage<TaskDto>>
    suspend fun patch(id: String, request: UpdateTaskRequest): Either<ApiError, TaskDto>
}

internal class KtorTaskApi(private val http: HttpClient) : TaskApi {
    override suspend fun list(filter: TaskListFilter) =
        apiCall<PageDto<TaskDto>> { http.get(Tasks(filter.statuses, filter.cursor, filter.limit)) }.map { it.toCursorPage() }
    override suspend fun patch(id: String, request: UpdateTaskRequest) =
        apiCall<TaskDto> { http.patch(Tasks.Id(id = id)) { setBody(request) } }
}
```

→ [`references/typed-client.md`](references/typed-client.md).

## 7. Tests

- `commonTest` in `api:contract`: JSON round-trips through `ApiJson` (sealed variants, absent vs
  null vs value for `OptionalFieldDto`, unknown fields ignored).
- `commonTest` in `api:mapping`: both directions; unknown enum string → `Left`.
- `jvmTest` in `api:contract`: `ContractConventionsTest` (incl. "every creation body carries `id`").
- server transport: `ErrorCodeLiteralTest`, `WireVocabularyTest`; UI module: `ErrorReasonTest`
  (all in `architecture-fitness-tests`).
- route tests: a create replayed with the same `id` answers the same; with another body `409`.
- `api:client` tests with `ktor-client-mock`: happy path + every error status → the right `ApiError`.

```bash
./gradlew :api:contract:compileKotlinWasmJs :api:mapping:compileKotlinWasmJs
./gradlew build
```

Then walk through [`checklists/api-checklist.md`](checklists/api-checklist.md).
