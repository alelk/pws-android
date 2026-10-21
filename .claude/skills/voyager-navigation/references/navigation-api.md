# Navigator & TabNavigator API

## Stack navigation — `Navigator`

```kotlin
@Composable
fun HomeTab.Content() {
    Navigator(HomeScreen) { navigator -> CurrentScreen() }
}
```

Inside any `Screen.Content()`:

```kotlin
val navigator = LocalNavigator.currentOrThrow

navigator.push(DetailScreen(itemId = 42L))
navigator.pop()
navigator.replace(EditScreen(itemId = 42L))
navigator.popUntilRoot()
navigator.pushAndForget(HomeScreen)
```

## Tab navigation — `TabNavigator`

```kotlin
@Composable
fun AppNavigation() {
    TabNavigator(HomeTab) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    TabNavigationItem(HomeTab)
                    TabNavigationItem(SettingsTab)
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding)) { CurrentTab() }
        }
    }
}

@Composable
private fun RowScope.TabNavigationItem(tab: Tab) {
    val tabNavigator = LocalTabNavigator.current
    NavigationBarItem(
        selected        = tabNavigator.current.key == tab.key,
        onClick         = { tabNavigator.current = tab },
        icon            = { Icon(painter = tab.options.icon!!, contentDescription = tab.options.title) },
        label           = { Text(tab.options.title) },
        alwaysShowLabel = false,
    )
}
```

Switching tabs programmatically:

```kotlin
val tabNavigator = LocalTabNavigator.current
tabNavigator.current = SettingsTab
```

## Nested back stacks

Each `Tab` wraps its content in its own `Navigator` so back-stack state is independent:

```
TabNavigator
├── HomeTab    ──► Navigator: [HomeScreen → DetailScreen → CommentScreen]
├── SearchTab  ──► Navigator: [SearchScreen → SearchResultsScreen]
└── SettingsTab ──► Navigator: [SettingsScreen]
```

## Shared state across screens in a Navigator

```kotlin
class SharedSelectionModel : ScreenModel {
    var selectedId by mutableStateOf<Long?>(null)
}

// Parent screen registers it once
val sharedModel = navigator.rememberNavigatorScreenModel { SharedSelectionModel() }

// Child screen retrieves the same instance
val sharedModel = navigator.rememberNavigatorScreenModel<SharedSelectionModel>()
```

## Transitions

```kotlin
Navigator(HomeScreen) { navigator ->
    SlideTransition(navigator)          // horizontal slide
    // FadeTransition(navigator)
    // ScaleTransition(navigator)
    // CrossFadeTransition(navigator)
}
```
