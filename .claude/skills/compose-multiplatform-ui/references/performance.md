# Compose Performance & Recomposition

## Common Mistakes

| #  | Issue                                                                      | Fix                                                                       |
|----|----------------------------------------------------------------------------|---------------------------------------------------------------------------|
| 1  | Unstable state: `MutableList`, raw `List` rebuilt each emission, lambdas in data class | `@Immutable data class` + `ImmutableList` from kotlinx-collections-immutable |
| 2  | Parent reads whole state and passes everything to leaves                   | Slice state; pass only the props each leaf renders                        |
| 3  | Expensive calculations during composition (parse, sort, format, sum)       | Pre-compute in ViewModel before emitting state                            |
| 4  | Callback recreation in hot paths (lazy list rows)                          | `remember(key, callback) { { callback(key) } }`                           |
| 5  | No-op state emissions — state update on every keystroke even if unchanged  | Guard identical values before `_state.value = ...`                        |
| 6  | Ephemeral visual state in ViewModel (shimmer alpha, pulse phase)           | Local composable state                                                    |
| 7  | `derivedStateOf` wrapping cheap boolean / string expressions               | Use only when derived from fast-changing Compose state with coarse output |
| 8  | `rememberSaveable` for whole-screen state or domain objects                | Small UI-local values only (tab index, scroll position)                   |
| 9  | State reads too high in tree (`LazyListState`, animation offset, keyboard) | Read close to the use site                                                |
| 10 | Missing list keys or index-based keys                                      | Stable domain ID: `items(items, key = { it.id }) { ... }`                 |

## API Decision Table

| API                | Use for                                                         | Avoid for                                                   |
|--------------------|-----------------------------------------------------------------|-------------------------------------------------------------|
| `remember`         | Local objects / UI state across recompositions                  | Business state, repo results, derived domain data           |
| `rememberSaveable` | Small UI-local state surviving recreation                       | Whole-screen state, large graphs, domain objects            |
| `derivedStateOf`   | Reducing downstream updates from rapidly changing Compose state | Cheap expressions; any data that comes from ViewModel state |
| `LaunchedEffect`   | Collecting effects, one-shot startup work                       | Business logic inside leaf composables                      |
| `DisposableEffect` | Register/unregister listeners with cleanup                      | Long-running business coroutines                            |

## Stable state models

```kotlin
// BAD — MutableList and lambda in data class defeat Compose skipping
data class RowState(val id: String, val tags: MutableList<String>, val onClick: () -> Unit)

// GOOD — @Immutable data class, stable list key, stable callback
@Immutable
data class RowUi(val id: String, val title: String, val subtitle: String)

@Composable
fun ItemList(items: List<RowUi>, onOpen: (String) -> Unit) {
    LazyColumn {
        items(items = items, key = { it.id }) { item ->
            val onClick = remember(item.id, onOpen) { { onOpen(item.id) } }
            ListItem(
                headlineContent    = { Text(item.title) },
                supportingContent  = { Text(item.subtitle) },
                modifier           = Modifier.clickable(onClick = onClick),
            )
        }
    }
}
```

## derivedStateOf — correct vs wrong

```kotlin
// GOOD: coarse boolean from rapidly changing scroll state
val showScrollToTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 2 } }

// BAD: cheap expression — just write it inline
val text by remember { derivedStateOf { if (canSubmit) "Submit" else "Fix errors" } }
// Use instead: Text(if (canSubmit) "Submit" else "Fix errors")
```

## @Immutable / @Stable

- `@Immutable` — type with no observable mutation after construction. Enables full
  Compose skipping on parameters of that type. Honor the contract or you get
  incorrect skipping.
- `@Stable` — rare in app code. Use only when equality is stable but the compiler
  cannot prove it (e.g. a class with manual `equals`).
- ❌ Don't apply either annotation just to silence compiler warnings.

## Strong skipping (on by default)

Since Kotlin 2.0.20 the Compose compiler enables **strong skipping** by default: composables with
unstable parameters skip when arguments are instance-equal (`===`), and lambdas are remembered
automatically. Don't set the old `enableStrongSkippingMode` flag (deprecated).

What it does *not* fix: a new `List` instance every emission (`items.map { … }` in the state
producer) is a new argument every time. Produce state once in the ViewModel, and use
`kotlinx-collections-immutable` (`ImmutableList`/`PersistentList`) for collection fields so the
compiler can treat them as stable and `equals` stays cheap.
