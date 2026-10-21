# Usage — Compose Multiplatform UI

Prerequisite: [`kmp-architecture`](../kmp-architecture/SKILL.md) applied; `api:client` exists
([`ktor-api-contract`](../ktor-api-contract/SKILL.md)).

## 1. The `features` module

```bash
cp <skill>/templates/features/build.gradle.kts.snippet features/build.gradle.kts
mkdir -p features/src/commonMain/kotlin/<pkg>/features/{app,designsystem,components,common,session,platform,di}
mkdir -p features/src/commonMain/composeResources/{values,values-ru}
mkdir -p features/src/jvmTest/kotlin/<pkg>/features/conventions
```

Layout → [`references/feature-module-structure.md`](references/feature-module-structure.md).
Design system (AppTheme tokens, wrappers) → [`compose-design-system`](../compose-design-system/SKILL.md).

## 2. Shared state base

```bash
cp <skill>/templates/features/Async.kt            features/src/commonMain/kotlin/<pkg>/features/common/
cp <skill>/templates/features/FeatureViewModel.kt features/src/commonMain/kotlin/<pkg>/features/common/
```

## 3. A screen

```kotlin
// goal/GoalsViewModel.kt — no Compose imports
data class GoalsUiState(
    val goals: ImmutableList<GoalRow> = persistentListOf(),
    override val loading: Boolean = true,
    override val busy: Boolean = false,
    override val error: ApiError? = null,
) : Async

class GoalsViewModel(private val api: GoalsApi) : FeatureViewModel<GoalsUiState>() {
    override val mutableState = MutableStateFlow(GoalsUiState())
    val state: StateFlow<GoalsUiState> = mutableState.asStateFlow()

    private val mutableEffects = Channel<GoalsEffect>(Channel.BUFFERED)
    val effects: Flow<GoalsEffect> = mutableEffects.receiveAsFlow()

    override fun GoalsUiState.startedRequest(busy: Boolean) = if (busy) copy(busy = true) else copy(loading = true)
    override fun GoalsUiState.finishedRequest(busy: Boolean, error: ApiError?) =
        if (busy) copy(busy = false, error = error) else copy(loading = false, error = error)

    init { reload() }

    fun onEvent(event: GoalsEvent) = when (event) {
        GoalsEvent.RetryClicked -> reload()
        is GoalsEvent.CreateClicked -> create(event.name)
    }

    private fun reload() = launchRequest(call = { api.list() }) { s, page -> s.copy(goals = page.items.map { it.toRow() }.toImmutableList()) }

    private fun create(name: String) {
        if (name.isBlank() || state.value.busy) return                // no-op guard
        launchRequest(busy = true, call = { api.create(CreateGoalRequest(newClientId(), name.trim())) }) { s, _ ->
            mutableEffects.trySend(GoalsEffect.Created)
            s
        }
    }
}
```

```kotlin
// goal/GoalsScreen.kt
@Composable
fun GoalsEntry(onOpenGoal: (GoalId) -> Unit, onBack: (() -> Unit)?) {
    val viewModel = koinViewModel<GoalsViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    GoalsContent(state, viewModel::onEvent, onOpenGoal, onBack)
}

@Composable
internal fun GoalsContent(state: GoalsUiState, onEvent: (GoalsEvent) -> Unit, onOpenGoal: (GoalId) -> Unit, onBack: (() -> Unit)?) {
    ScreenScaffold(title = stringResource(Res.string.goals_title), onBack = onBack) {
        when {
            state.loading && state.goals.isEmpty() -> GroupsPlaceholder()
            state.error != null && state.goals.isEmpty() -> ErrorBanner(state.error, onRetry = { onEvent(GoalsEvent.RetryClicked) })
            else -> GoalList(state.goals, onOpenGoal)
        }
    }
}
```

Patterns → [`references/state-viewmodel.md`](references/state-viewmodel.md),
[`references/state-modeling.md`](references/state-modeling.md).

## 4. Koin

```kotlin
val planningModule = module {
    viewModelOf(::GoalsViewModel)                                  // screen-scoped via the nav entry
    viewModel { (id: GoalId) -> GoalDetailViewModel(get(), id) }
    scope(SESSION) {                                               // tenant data: dies with the session
        scoped { GoalsStore(get(), get()) } withOptions { onClose { it?.close() } }
    }
}
```

Session scope → [`references/session-scope.md`](references/session-scope.md).

## 5. Routes and navigation

Declared in `app/` → [`compose-navigation3`](../compose-navigation3/SKILL.md).

## 5a. Shell chrome

Put `chromeOf`, `ownsBottom`, `chromeInset` and `canonicalStack` in `app/ShellChrome.kt` and
`isFullScreen` in `app/FullScreenRoutes.kt` — pure functions, tested over every route × width ×
role. `AppShell` reads `chromeOf` only →
[`references/shell-chrome.md`](references/shell-chrome.md).

## 6. A shell

```kotlin
// webapp/src/wasmJsMain/.../Main.kt
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startKoin { modules(webappModules()) }         // features modules + api:client:di + platform impls
    document.getElementById("app-loader")?.remove()
    ComposeViewport(document.body!!) { AppRoot() }  // NOT CanvasBasedWindow
}
```

Platform services → `commonMain` interfaces, shell implementations
([`references/preferences-storage.md`](references/preferences-storage.md)). Web shell details →
[`compose-web-shell`](../compose-web-shell/SKILL.md).

## 7. Strings

Every key in every `values*/strings.xml` — a parity test fails the build otherwise
([`references/localization.md`](references/localization.md)).

## 8. Guard the rules

Copy the UI fitness tests from
[`architecture-fitness-tests/templates/features`](../architecture-fitness-tests/templates/features/)
into `features/src/jvmTest/.../conventions/` and pin the screen list.

## 9. Run and verify

```bash
./gradlew :webapp:wasmJsBrowserDevelopmentRun      # dev server, see compose-web-shell
./gradlew :features:jvmTest                         # ViewModel + convention tests
./gradlew build
```

Then walk through [`checklists/ui-checklist.md`](checklists/ui-checklist.md).
