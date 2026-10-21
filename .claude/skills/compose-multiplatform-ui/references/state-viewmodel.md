# `ViewModel` + `StateFlow` + Koin

Every screen state holder is a multiplatform `androidx.lifecycle.ViewModel`, scoped to its
back-stack entry by `rememberViewModelStoreNavEntryDecorator()`
([`compose-navigation3`](../../compose-navigation3/SKILL.md)); removal from the stack cancels
`viewModelScope`.

## Three fields every screen has — and only those in common

```kotlin
interface Async {
    val loading: Boolean              // loading what the screen was opened for → a loader
    val busy: Boolean get() = false   // an action the PERSON started is in flight → a disabled button
    val error: ApiError?
}
```

No generic `UiState<T>` wrapper: screen states differ; what they share is these three fields.
One name per concept across the module — `busy`, never `submitting`/`saving`/`isSaving` (a
fitness test enforces it).

When fields genuinely differ per stage (a wizard, a form that becomes a confirmation), use a
sealed type instead: `sealed interface ResetUiState { Form(...); Sent }`.

## The one request block

The hand-written "raise flag → call → fold → lower flag" block is where the classic bug lives:
the failure branch forgets to lower the flag and the screen spins forever. Write it once:

```kotlin
abstract class FeatureViewModel<S : Async> : ViewModel() {
    protected abstract val mutableState: MutableStateFlow<S>
    protected abstract fun S.startedRequest(busy: Boolean): S
    protected abstract fun S.finishedRequest(busy: Boolean, error: ApiError?): S

    protected fun <T> launchRequest(
        busy: Boolean = false,
        call: suspend () -> Either<ApiError, T>,
        onFailure: (S, ApiError) -> S = { s, e -> s.finishedRequest(busy, e) },
        onSuccess: (S, T) -> S,
    ): Job = viewModelScope.launch {
        mutableState.update { it.startedRequest(busy) }
        call().fold(
            { e -> mutableState.update { onFailure(it, e) } },
            { v -> mutableState.update { onSuccess(it.finishedRequest(busy, null), v) } },
        )
    }
}
```

- Returns the `Job` so a search-as-you-type can cancel the previous request.
- `onFailure` override is for the rare screen where a failure is not "an error banner"
  (a `401` on sign-in means "wrong credentials", a field state).
- No `runCatching`, no `try/catch` — the client already returns `Either`.

## Effects

```kotlin
private val mutableEffects = Channel<GoalsEffect>(Channel.BUFFERED)
val effects: Flow<GoalsEffect> = mutableEffects.receiveAsFlow()
```

Navigate-once, show-snackbar-once, "moment of success" — collected in the Entry with
`LaunchedEffect(viewModel)`. Effects carry typed keys (`UiMessage.Saved`), never localised text.

## Koin

```kotlin
viewModelOf(::GoalsViewModel)
viewModel { (id: GoalId) -> GoalDetailViewModel(get(), id) }
```

```kotlin
val vm = koinViewModel<GoalDetailViewModel> { parametersOf(goalId) }
```

A ViewModel that reads tenant data depends on a **session-scoped store** (`koinInject()` resolves
it from the current session scope) rather than holding its own copy
([`session-scope.md`](session-scope.md)).

## Collecting

```kotlin
val state by viewModel.state.collectAsStateWithLifecycle()
```

`collectAsStateWithLifecycle` (lifecycle-runtime-compose) stops collecting when the host is not at
least STARTED; on web it behaves like `collectAsState`, on Android it saves work in background.

## Rules

- Always `koinViewModel<T>()` — never `remember { SomeViewModel() }`.
- ViewModels have no Compose imports and no platform objects.
- Guard no-op updates (`if (current.title == raw) return`) and double submissions
  (`if (state.value.busy) return`).
- Keep existing content visible while refreshing — `loading` over content is an inline indicator,
  not a full-screen spinner that wipes the list.
