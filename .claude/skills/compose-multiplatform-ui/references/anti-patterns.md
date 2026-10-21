# Compose Anti-Patterns

Quick-reference for common mistakes in Compose Multiplatform + Navigation 3 codebases.

## Cross-Cutting Anti-Patterns

| Anti-pattern                                                             | Why harmful                                             | Fix                                                                                                                                       |
|--------------------------------------------------------------------------|---------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| Business logic inside composables                                        | Reruns during composition; untestable                   | Move to ViewModel                                                                                                                         |
| One giant ViewModel per feature module                                   | Blast radius too large, hard to reason                  | One ViewModel per screen                                                                                                                  |
| Consumable boolean events in state (`showSnackbar = true`)               | Replays on re-subscription; needs "consume" reset logic | `Channel<Effect>` — see [`state-modeling.md`](state-modeling.md)                                                                          |
| `MutableList` or lambdas in state `data class`                           | Defeats Compose skipping, excess recompositions         | Immutable `data class` + immutable collections                                                                                            |
| Parent reads whole state, passes all fields to leaves                    | Cascade recompositions to unrelated children            | Slice: pass only what each leaf renders                                                                                                   |
| Full-screen spinner wipes existing content during refresh                | Layout jumps, destroys user trust                       | Keep old content visible + inline loading indicator                                                                                       |
| Pre-baked localised strings in state (formatted text baked in ViewModel) | Breaks on locale change; duplication                    | Keep canonical value; format in composable                                                                                                |
| Animation-only flags in ViewModel state (`shakeCount`, `alpha`)          | Pollutes business state, unnecessary complexity         | Local composable animation state                                                                                                          |
| Platform calls directly in ViewModel (share, clipboard, haptic)          | Platform coupling; untestable in commonTest             | Emit `Effect`; shell executes it                                                                                                          |
| `remember { SomeViewModel() }`                                           | Lifecycle not managed by entry ViewModelStore           | Always `koinViewModel<T>()`                                                                                                               |
| Non-serializable objects in route (`NavKey`) parameters                  | Crashes when the back stack is restored                 | Only stable, serializable values in route data classes                                                                                    |
| `server:*` imports in `features` module                                  | Breaks shell/features architectural boundary            | Use domain interfaces only                                                                                                                |
| `runCatching { api.call() }` in a ViewModel                              | Swallows `CancellationException` and bugs alike          | Client returns `Either<ApiError, T>`; fold it (`launchRequest`)                                                                          |
| `ViewModel` passed into a `Content` composable                           | Content no longer previewable/testable; hidden coupling  | `Content(state, onEvent, on*)`                                                                                                           |
| Tenant data in a root singleton store                                    | Survives logout / tenant switch; old SSE keeps running   | `scope(SESSION)` store with `onClose` — see [`session-scope.md`](session-scope.md)                                                      |
| `status == "Pending"` in a composable                                    | UI decides by wire strings; breaks on rename             | Map DTO → domain enum in `api:mapping`/screen model                                                                                       |
| `MaterialTheme.colorScheme.primary` in a screen                          | Skin/theme swap needs every screen touched               | `AppTheme.colors.*` — see [`compose-design-system`](../../compose-design-system/SKILL.md)                                              |
| Localised strings emitted from ViewModel state or effects                | i18n boundary violated; locale coupling                 | Typed `UiMessage` keys; resolve strings in composable — see [`viewmodel-koin.md`](../../compose-navigation3/references/viewmodel-koin.md) |
| `expect/actual` for services with lifecycle or async                     | Inflexible, difficult to fake in tests                  | `commonMain` interface + Koin platform module                                                                                             |
| Cross-feature ViewModel imports                                          | Circular risk; invisible coupling                       | Feature API contracts + Koin injection                                                                                                    |
| Reading `layoutInfo` / `onGloballyPositioned` values inside composition   | recompose→layout loop inside one frame — the app freezes | Compute in `snapshotFlow { }` inside `LaunchedEffect`; emit only the result                                                              |
| `LazyColumn(Modifier.fillMaxSize())` as the last child of a `Column`     | Takes the parent's full height ignoring siblings; tail is clipped and unreachable | `Modifier.weight(1f)`                                                                            |
| `detectDragGestures` on an item inside a scrollable list                 | Claims the gesture on first movement — touch scrolling dies | Pointer-type split (mouse: slop, touch: long press) — see [`drag-and-drop.md`](drag-and-drop.md)                                       |
| `CanvasBasedWindow` in a web shell                                       | Deprecated entrypoint                                   | `ComposeViewport(document.body!!)`                                                                                                        |
| Web app with no root error boundary                                      | One uncaught exception kills the whole canvas scene — dead page | Error-boundary wrapper + global error reporting — see [`canvas-web.md`](canvas-web.md)                                             |

## BAD / GOOD: Business logic in composable

```kotlin
// BAD — logic reruns every recomposition; a ViewModel parameter; impossible to test in isolation
@Composable
fun OrderScreen(viewModel: OrderViewModel) {
    val state by viewModel.state.collectAsState()
    val total = state.items.sumOf { it.price * it.qty } // ← business logic
    Text("Total: $total")
}

// GOOD — compute in ViewModel; composable only renders
data class Success(val items: List<LineItem>, val total: Double) : State()

@Composable
fun OrderContent(state: OrderViewModel.State) {
    (state as? OrderViewModel.State.Success)?.let { s ->
        Text("Total: ${s.total}")
    }
}
```

## BAD / GOOD: Consumable boolean in state

```kotlin
// BAD — needs a paired "dismiss" event; replays on re-subscription
data class State(val form: FormData = FormData(), val showSavedBanner: Boolean = false)

// GOOD — Channel<Effect> fires once and is consumed
sealed interface TaskEffect {
    data class ShowMessage(val key: UiMessage) : TaskEffect
}
// trySend from ViewModel; collect in Content() with LaunchedEffect
```

## BAD / GOOD: Layout-written state read in composition

```kotlin
// BAD — layoutInfo is rewritten on every layout pass; reading it here invalidates the
// composition that produced it. Compose loops recompose→layout inside one frame and never
// returns to the event loop: the app freezes mid-gesture, with no exception to point at.
@Composable
fun Column(listState: LazyListState) {
    val gapIndex = dropIndexFor(listState, cards, pointerY)   // ← read during composition
    ...
}

// GOOD — observe outside composition, publish only the deduplicated result
var gapIndex by remember { mutableStateOf<Int?>(null) }
LaunchedEffect(cards) {
    snapshotFlow { dropIndexFor(listState, cards, pointerY) }
        .collect { gapIndex = it }
}
```

## BAD / GOOD: Blocking refresh with full-screen spinner

```kotlin
// BAD — wipes content on every refresh
fun refresh() {
    _state.value = State.Loading   // ← user loses their scroll position
    viewModelScope.launch { ... }
}

// GOOD — keep old content, set loading flag
fun refresh() {
    _state.value = (_state.value as? State.Success)
        ?.copy(isRefreshing = true)
        ?: State.Loading
    viewModelScope.launch { ... }
}
```
