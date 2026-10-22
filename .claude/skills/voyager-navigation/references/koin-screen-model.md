# StateScreenModel + Koin Integration

## `StateScreenModel`

```kotlin
class HomeScreenModel(
    private val getItems: GetItemsUseCase,
) : StateScreenModel<HomeScreenModel.State>(State.Loading) {

    sealed class State {
        data object Loading                              : State()
        data class  Success(val items: List<ItemSummary>) : State()
        data class  Error(val message: String)            : State()
    }

    init { loadData() }

    fun loadData() {
        coroutineScope.launch {
            mutableState.value = State.Loading
            runCatching { getItems() }
                .onSuccess { mutableState.value = State.Success(it) }
                .onFailure { mutableState.value = State.Error(it.message ?: "Unknown error") }
        }
    }

    fun retry() = loadData()
}
```

## Koin registration

```kotlin
val featuresModule = module {
    // No-parameter ScreenModels
    factoryOf(::HomeScreenModel)
    factoryOf(::SearchScreenModel)
    factoryOf(::SettingsScreenModel)

    // With runtime parameters
    factory { (itemId: Long)  -> DetailScreenModel(get(), itemId) }
    factory { (query: String) -> SearchResultsScreenModel(get(), query) }
}
```

## Injection in a Screen

```kotlin
val screenModel = koinScreenModel<HomeScreenModel>()

// With parameters
val screenModel = koinScreenModel<DetailScreenModel> { parametersOf(itemId) }
```

## i18n boundary — never put UI text in `ScreenModel`

`ScreenModel` emits typed messages; `Screen` resolves them via resources.

```kotlin
sealed interface UiMessage {
    data object Saved                              : UiMessage
    data class  Error(val details: String?)        : UiMessage
}

data class ShowSnackbar(val message: UiMessage)
```

```kotlin
val text = when (val m = effect.message) {
    UiMessage.Saved    -> getString(Res.string.saved)
    is UiMessage.Error -> getString(Res.string.error_prefix, m.details ?: "")
}
snackbarHostState.showSnackbar(text)
```

## Rules

- Always `koinScreenModel<T>()` — **never** `remember { ScreenModel() }`.
- `coroutineScope` from `ScreenModel` is tied to screen lifecycle; cancelled on pop.
- Never hold `Context`, `Activity`, or any Android-specific object in a ScreenModel.
- Never put localised text in ScreenModel state or effects — see the i18n boundary above.
