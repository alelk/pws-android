# Screens & Tabs

## Screen — composable destination

```kotlin
// Parameter-less
data object HomeScreen : Screen {
    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<HomeScreenModel>()
        // ...
    }
}

// With parameters (data class for correct equals/hashCode → back-stack)
data class DetailScreen(val itemId: Long) : Screen {
    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<DetailScreenModel> { parametersOf(itemId) }
        // ...
    }
}

// With complex parameters (mark @Serializable for deep links)
@Serializable
data class SearchScreen(val query: String = "") : Screen {
    @Composable
    override fun Content() { /* ... */ }
}
```

## Tab — bottom-navigation entry

```kotlin
object HomeTab : Tab {
    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 0u,
            title = stringResource(Res.string.tab_home),
            icon  = rememberVectorPainter(Icons.Default.Home),
        )

    @Composable
    override fun Content() {
        Navigator(HomeScreen) { navigator -> CurrentScreen() }
    }
}
```

## Rules

- `data class` for screens with parameters; `data object` for parameter-less screens.
- Keep `Content()` thin — no business logic, only render + delegate to `ScreenModel`.
- Never pass lambdas or non-serializable objects in `Screen` constructor params (back-stack restoration breaks).
- `TabOptions.title` may be a localised `stringResource(...)` for display, but route
  keys (`tab.key`) must be stable identifiers — see
  [compose-multiplatform-ui / localization.md](../../compose-multiplatform-ui/references/localization.md).
