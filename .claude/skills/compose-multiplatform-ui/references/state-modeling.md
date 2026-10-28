# State Modeling for Compose Screens

## Entry as Route — Content — Leaf split

The nav **entry** composable acts as the Route layer: acquires the ViewModel, collects
state, and handles one-off effects. Extract a pure composable for everything else.

```kotlin
@Composable
fun TaskListEntry(onOpenTask: (TaskId) -> Unit) {
    val viewModel = koinViewModel<TaskListViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is TaskListEffect.OpenTask    -> onOpenTask(effect.id)
                is TaskListEffect.ShowMessage -> snackbarHostState.showSnackbar(getString(effect.key))
            }
        }
    }

    TaskListContent(state = state, onEvent = viewModel::onEvent, onOpenTask = onOpenTask)
}

// Pure render — stateless, easy to preview and test independently
@Composable
fun TaskListContent(state: TaskListViewModel.State, onEvent: (TaskListEvent) -> Unit) {
    when (val s = state) {
        TaskListViewModel.State.Loading   -> LoadingPlaceholder()
        is TaskListViewModel.State.Error  -> ErrorBanner(s.message, onRetry = { onEvent(TaskListEvent.RetryClicked) })
        is TaskListViewModel.State.Success -> TaskList(
            items       = s.items,
            onItemClick = { onEvent(TaskListEvent.ItemClicked(it.id)) },
        )
    }
}
```

Rules:
- Entry composable — ViewModel acquisition, state collection, effect handling only. It receives
  navigation as explicit lambdas from `app/`; it never touches the back stack itself.
- `XxxContent()` — pure composable; receives state + a single `onEvent` + explicit navigation
  `on*` lambdas. No `ViewModel` parameter, no `*Callbacks` bag. Previewable.
- Leaf composables receive only the slice they actually render — no ViewModel, no `onEvent` of a
  whole screen, no lambda bag; at most the one or two callbacks the leaf itself triggers.

Naming: docs often call the first layer "Route" (Route → Content → Leaf); in code it is the
`*Entry` composable registered in the `entryProvider`. Pick one name per project and let
`ScreenSurfaceTest` match it.

**How this is checked:** a `ScreenSurfaceTest` in `features/src/jvmTest/.../conventions/` fails
the build if an `Entry` has no matching `Content`, and a `UiConventionsTest` fails it if a
composable takes a `ViewModel` parameter — a test, not a reviewer's eye
([`architecture-fitness-tests`](../../architecture-fitness-tests/SKILL.md)).

---

## Effect delivery — never consumable booleans in state

```kotlin
// BAD — event replays on re-subscription; needs "consume" logic to reset the flag
data class State(val showSavedBanner: Boolean = false)

// GOOD — Channel delivers exactly once
class TaskEditViewModel(...) : ViewModel() {

    private val _effects = Channel<TaskEditEffect>(Channel.BUFFERED)
    val effects: Flow<TaskEditEffect> = _effects.receiveAsFlow()

    fun onEvent(event: TaskEditEvent) {
        when (event) {
            TaskEditEvent.SaveClicked -> save()
            TaskEditEvent.BackClicked -> viewModelScope.launch { _effects.trySend(TaskEditEffect.NavigateBack) }
        }
    }

    private fun save() {
        viewModelScope.launch {
            // ... persist ...
            _effects.trySend(TaskEditEffect.ShowMessage(UiMessage.Saved))
        }
    }
}

sealed interface TaskEditEffect {
    data object NavigateBack                          : TaskEditEffect
    data class  ShowMessage(val key: UiMessage) : TaskEditEffect
}
```

**How this is checked:** the same `ScreenSurfaceTest` matches `val (done|sent|saved|...): Boolean`
in state and fails the build — a one-shot flag has to become a sealed-state variant
(`Form`/`Sent`) or a `Channel` effect before it can ship.

---

## State modeling for forms — 4 buckets

| Bucket                 | What goes here                                  | Example                           |
|------------------------|-------------------------------------------------|-----------------------------------|
| **Editable input**     | Raw text/choice values as the user types        | `title: String`, `amount: String` |
| **Derived/computed**   | Parsed, validated, calculated values            | `val canSave: Boolean get() = …`  |
| **Persisted snapshot** | Saved entity for dirty-check / reset            | `original: Task?`                 |
| **Transient UI-only**  | Visual state that is never business-significant | local `LazyListState`, focus      |

```kotlin
data class TaskEditState(
    val title:    String = "",
    val amount:   String = "",
    val errors:   Map<String, UiMessage> = emptyMap(),   // typed keys, resolved in the composable
    override val loading: Boolean = false,
    override val busy: Boolean = false,                  // one name: busy, never isSaving/submitting
    override val error: ApiError? = null,
) : Async {
    val canSave:  Boolean get() = title.isNotBlank() && amount.isNotBlank()
    val hasErrors:Boolean get() = errors.isNotEmpty()
}
```

**Avoid duplicated state:** don't store `total`, `formattedTotal`, and `totalText`
separately. Keep the canonical value; derive display variants at the UI boundary.

---

## Guard no-op state emissions

```kotlin
fun onTitleChanged(raw: String) {
    val current = (_state.value as? State.Form) ?: return
    if (current.title == raw) return          // no-op guard
    _state.value = current.copy(title = raw)
}
```

---

## MVI event naming — user perspective

Events describe **what the user did**, not what should happen next:

| Good                | Avoid            |
|---------------------|------------------|
| `OnSaveClicked`     | `SaveTask`       |
| `OnTitleChanged(v)` | `UpdateTitle(v)` |
| `OnRetryClicked`    | `RetryRequest`   |
| `OnBackClicked`     | `NavigateBack`   |

---

## When lighter patterns are fine

- Purely visual local state (scroll, expansion, tooltip visibility):
  `remember { mutableStateOf(false) }` — no ViewModel needed.
- Trivial screens with no async, no validation, no effects:
  `rememberSaveable` for a small form is acceptable.
- Do not introduce a ViewModel for every small reusable composable.
