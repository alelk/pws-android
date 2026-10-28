# Typed Result Sealed Interfaces (no-Arrow alternative)

The catalogue's default is `Either<DomainError, T>` with the Raise DSL
([`either-error-handling.md`](either-error-handling.md)). Use typed results **instead** only in a
project that deliberately avoids Arrow — decide it once (ADR), never mix the two.

## Shape

```kotlin
sealed interface CreateResult<out T> {
    data class Created<T>(val value: T) : CreateResult<T>
    data class AlreadyExists(val field: String) : CreateResult<Nothing>
    data class Invalid(val message: String) : CreateResult<Nothing>
}

sealed interface UpdateResult<out T> {
    data class Updated<T>(val value: T) : UpdateResult<T>
    data object NotFound : UpdateResult<Nothing>
    data object Conflict : UpdateResult<Nothing>          // optimistic-lock failure
    data class Invalid(val message: String) : UpdateResult<Nothing>
}
```

## Trade-offs

| Either + Raise                                  | Typed results                                         |
|-------------------------------------------------|-------------------------------------------------------|
| One error vocabulary across the system          | Each operation lists only its possible outcomes       |
| Composes (`bind`, `ensure`) across ports        | Composition is manual `when` nesting                  |
| Needs Arrow (KMP, wasm-ready)                   | No dependency                                         |
| Transport maps one sealed type → HTTP           | Transport maps each result type                       |

Choose typed results for small CRUD services where operations don't chain; choose Either as
soon as use-cases call several ports and must short-circuit.

## Rules (if chosen)

- Never add an `UnknownError(cause: Throwable)` variant that swallows exceptions — adapters
  still map specific exceptions and let bugs propagate.
- Exhaustive `when` in transport, no `else`.
