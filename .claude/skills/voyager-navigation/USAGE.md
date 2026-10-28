# Usage — Voyager Navigation

Prerequisite: [`compose-multiplatform-ui`](../compose-multiplatform-ui/SKILL.md) applied —
`features` module has the Voyager dependencies wired.

## 1. Create a ScreenModel

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
                .onFailure { mutableState.value = State.Error(it.message ?: "Unknown") }
        }
    }
    fun retry() = loadData()
}
```

## 2. Register in Koin

```kotlin
val featuresModule = module {
    factoryOf(::HomeScreenModel)
    factoryOf(::SettingsScreenModel)
    factory { (id: Long) -> DetailScreenModel(get(), id) }
}
```

## 3. Create a Screen

```kotlin
data object HomeScreen : Screen {
    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<HomeScreenModel>()
        val state by screenModel.state.collectAsState()

        when (val s = state) {
            is HomeScreenModel.State.Loading -> CircularProgressIndicator()
            is HomeScreenModel.State.Error   -> ErrorBanner(s.message) { screenModel.retry() }
            is HomeScreenModel.State.Success -> ItemList(
                items = s.items,
                onItemClick = {
                    val navigator = LocalNavigator.currentOrThrow
                    navigator.push(DetailScreen(it.id))
                },
            )
        }
    }
}
```

## 4. Create a Screen with parameters

```kotlin
data class DetailScreen(val itemId: Long) : Screen {
    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<DetailScreenModel> { parametersOf(itemId) }
        val state by screenModel.state.collectAsState()
        // render detail UI
    }
}
```

## 5. Create a Tab

```kotlin
object HomeTab : Tab {
    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 0u,
            title = stringResource(Res.string.nav_home),
            icon  = rememberVectorPainter(Icons.Default.Home),
        )

    @Composable
    override fun Content() {
        Navigator(HomeScreen) { CurrentScreen() }
    }
}
```

## 6. Assemble AppNavigation

```kotlin
@Composable
fun AppNavigation() {
    AppTheme {
        TabNavigator(HomeTab) {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        TabNavigationItem(HomeTab)
                        TabNavigationItem(SearchTab)
                        TabNavigationItem(SettingsTab)
                    }
                }
            ) { padding ->
                Box(Modifier.padding(padding)) { CurrentTab() }
            }
        }
    }
}
```

Full pattern (including `TabNavigationItem`) → [`references/navigation-api.md`](references/navigation-api.md).

## 7. Navigate

```kotlin
val navigator = LocalNavigator.currentOrThrow
navigator.push(DetailScreen(id = 42L))
navigator.pop()
navigator.replace(EditScreen(id = 42L))
navigator.popUntilRoot()
```

## 8. Switch tabs programmatically

```kotlin
val tabNavigator = LocalTabNavigator.current
tabNavigator.current = SettingsTab
```

## 9. Verify

Walk through [`checklists/voyager-checklist.md`](checklists/voyager-checklist.md).

## Notes

- `koinScreenModel<T>()` creates one instance per screen entry on the stack — `DetailScreen(42)` and `DetailScreen(43)` each get their own model.
- `coroutineScope` in `ScreenModel` is cancelled automatically when the screen is popped.
- `data object` for parameter-less screens avoids duplicate stack entries.
- `voyager-koin` is a runtime dependency — keep it in `commonMain`, never `*Test` source sets.
