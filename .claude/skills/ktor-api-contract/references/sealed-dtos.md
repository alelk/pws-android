# Sealed DTOs & PATCH fields

## Discriminated unions

```kotlin
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed interface TaskTransitionRequest {
    val version: Long

    @Serializable @SerialName("start")
    data class Start(override val version: Long) : TaskTransitionRequest

    @Serializable @SerialName("mark_done")
    data class MarkDone(override val version: Long, val note: String? = null) : TaskTransitionRequest
}
```

- `@JsonClassDiscriminator("type")` on **every** sealed DTO — explicit, not inherited from a
  global setting.
- `@SerialName` on every variant, **snake_case**, never the Kotlin class name — the discriminator
  is wire data and outlives refactors.
- A shared field (`version`) is declared on the interface so the handler can read it before
  dispatching.
- Domain types never carry `@SerialName`; the DTO mirrors them.

## `OptionalFieldDto` — absent / null / value

```kotlin
@Serializable(with = OptionalFieldDtoSerializer::class)
sealed interface OptionalFieldDto<out T> {
    data object Unchanged : OptionalFieldDto<Nothing>
    data class Set<out T>(val value: T) : OptionalFieldDto<T>
    data object Clear : OptionalFieldDto<Nothing>
}
```

The serializer (template: [`../templates/api-contract/OptionalFieldDto.kt`](../templates/api-contract/OptionalFieldDto.kt))
runs only on a **present** field: JSON `null` → `Clear`, a value → `Set`. Absence is the property
default:

```kotlin
data class UpdateTaskRequest(
    val version: Long,
    val dueAt: OptionalFieldDto<String> = OptionalFieldDto.Unchanged,   // MUST default to Unchanged
)
```

`Unchanged` is never serialised (the client omits the field); encoding it is an error by design.

## Rules

- Round-trip tests for every sealed DTO and every `OptionalFieldDto` state through `ApiJson`.
- A fitness test checks: every sealed type in the contract has a discriminator; every
  `@SerialName` is snake_case.
