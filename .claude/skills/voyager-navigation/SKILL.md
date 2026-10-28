---
name: voyager-navigation
description: Use when adding or modifying navigation in a Compose Multiplatform app with the Voyager library — Screen, Tab, Navigator, TabNavigator, StateScreenModel, koinScreenModel injection, nested back stacks per tab, screen parameters via parametersOf, rememberNavigatorScreenModel for shared state, and transitions (SlideTransition etc.). Apply after compose-multiplatform-ui when wiring navigation.
version: 1.1.0
requires:
  - compose-multiplatform-ui
projectTypes:
  - full-stack-kmp-app
  - compose-desktop
  - compose-mobile
---

# Voyager Navigation

## When to use

- Adding a new screen, tab, or navigation graph.
- Hitting back-stack issues (duplicate screens, lost state, broken pop).
- Switching from a Composable-state navigation to a managed back-stack.
- Adding shared state across multiple screens inside one tab.

## Core concepts at a glance

| Concept              | Purpose                                              |
|----------------------|------------------------------------------------------|
| `Screen`             | Composable destination. Serializable, pushed/popped. |
| `Tab`                | A `Screen` shown in `TabNavigator` (bottom nav).     |
| `ScreenModel`        | State holder tied to screen lifecycle.               |
| `StateScreenModel<S>`| `ScreenModel` with built-in `MutableStateFlow<S>`.   |
| `Navigator`          | Stack-based navigator: push, pop, replace.           |
| `TabNavigator`       | Tab-based navigator: switches between `Tab` instances. |

## Dependencies

`libs.versions.toml` and `features/build.gradle.kts` snippets are already provided by
[`compose-multiplatform-ui / templates/features/build.gradle.kts.snippet`](../compose-multiplatform-ui/templates/features/build.gradle.kts.snippet).
If you applied that skill, Voyager is already wired.

## Hard rules

- ❌ Never use `remember { ScreenModel() }` — lifecycle is not managed.
- ❌ Never pass lambdas or non-serializable objects as `Screen` constructor parameters.
- ❌ Never hold `Context`, `Activity`, or Android-specific objects in `ScreenModel`.
- ❌ Never put localised UI text in ScreenModel state/effects — see the i18n boundary in
  [`references/koin-screen-model.md`](references/koin-screen-model.md).
- ✅ `data class` for screens with parameters; `data object` for parameter-less screens.
- ✅ Always `koinScreenModel<T>()` for injection — supports `parametersOf(...)`.
- ✅ Each `Tab` wraps its content in its own `Navigator` for independent back stacks.
- ✅ `TabNavigator` for bottom nav — never manual `when (tab) {...}`.

## References (load on demand)

- [`references/screens-and-tabs.md`](references/screens-and-tabs.md) — `Screen` and `Tab` definitions, parameter patterns.
- [`references/navigation-api.md`](references/navigation-api.md) — `Navigator` / `TabNavigator` usage, nested back stacks, shared state, transitions.
- [`references/koin-screen-model.md`](references/koin-screen-model.md) — `StateScreenModel`, `koinScreenModel`, `parametersOf`, i18n boundary.

## Apply this skill

Step-by-step → [`USAGE.md`](USAGE.md).

## Verify

Walk through [`checklists/voyager-checklist.md`](checklists/voyager-checklist.md).
