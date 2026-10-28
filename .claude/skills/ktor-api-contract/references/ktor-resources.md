# Ktor Resources — type-safe routing on both banks

`@Resource` classes live in `api:contract/route/` and are used by the server routing **and** the
client, so a path or parameter rename is a compile error on both sides.

## Nested definitions

```kotlin
@Resource("/v1/tasks")
class Tasks(
    val status: List<String> = emptyList(),   // repeated param: ?status=A&status=B
    val cursor: String? = null,
    val limit: Int = DEFAULT_LIMIT,
) {
    @Resource("{id}")
    class Id(val parent: Tasks = Tasks(), val id: String) {
        @Resource("transitions") class Transitions(val parent: Id)
        @Resource("time-entries") class TimeEntries(val parent: Id) {
            @Resource("{entryId}") class EntryId(val parent: TimeEntries, val entryId: String)
        }
    }
    companion object { const val DEFAULT_LIMIT = 50 }
}
```

- A sub-resource is declared **inside** its parent — never a second class that spells
  `/v1/tasks/{taskId}/…` in its own string (two places for one segment, and they will disagree).
- Only the first level gets a default `parent = Tasks()`; deeper levels take the parent explicitly.
- Query parameters are typed constructor params with defaults; the set of parameters is part of
  the contract and is reviewed like a DTO.

## Server

```kotlin
install(Resources)
routing {
    authenticate("jwt") {
        tenantGet<Tasks> { resource, ctx -> … resource.cursor … }
        tenantPost<Tasks.Id.Transitions> { resource, ctx -> val taskId = TaskId(resource.parent.id) … }
    }
}
```

## Client

```kotlin
install(Resources)
http.get(Tasks(status = listOf("Pending"), cursor = next))
http.post(Tasks.Id.Transitions(Tasks.Id(id = taskId))) { setBody(TaskTransitionRequest.Start(version)) }
```

## Rules

- `ktor-resources` is an `api:contract` dependency (KMP); server and client add their own
  `*-resources` plugin artifacts.
- No string paths anywhere except health routes and static files.
