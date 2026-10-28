# Checklist — Voyager Navigation

## Setup

- [ ] `voyager-navigator`, `voyager-tab-navigator`, `voyager-koin` added to `features` dependencies
- [ ] Voyager version is pinned in `libs.versions.toml`

## Screens

- [ ] All screens implement `Screen`
- [ ] Parameter-less screens use `data object`
- [ ] Screens with parameters use `data class`
- [ ] No business logic in `Screen.Content()` — delegated to ScreenModel
- [ ] No lambdas or non-serializable objects as Screen constructor parameters

## ScreenModels

- [ ] All ScreenModels extend `StateScreenModel<S>`
- [ ] All ScreenModels are registered in Koin (`factoryOf` or `factory { ... }`)
- [ ] All ScreenModels are injected via `koinScreenModel<T>()` — not `remember { ... }`
- [ ] No Android `ViewModel` in `commonMain`
- [ ] ScreenModel effects/state do not contain localized UI text literals
- [ ] UI messages are represented by typed keys/codes and localized in Screen layer

## Navigation

- [ ] `TabNavigator` used for bottom navigation (not manual `when(tab)`)
- [ ] Each `Tab` wraps its content in a `Navigator` for independent back stacks
- [ ] `LocalNavigator.currentOrThrow` used for push/pop operations
- [ ] `LocalTabNavigator.current` used for programmatic tab switching
- [ ] Route/navigation keys are stable ids and independent from current UI language

## Lifecycle

- [ ] Long-running operations launched in `coroutineScope` (ScreenModel scope)
- [ ] No manual coroutine scope creation in ScreenModels

