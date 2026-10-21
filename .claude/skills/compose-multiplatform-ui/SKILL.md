---
name: compose-multiplatform-ui
description: Use when designing or extending a Compose Multiplatform UI layer shared across platforms (web/wasm, Telegram, desktop, Android) — the features module + thin-shell pattern, layers (designsystem ← components ← screens ← app), Entry/Content/Leaf screens with one onEvent, androidx ViewModel + StateFlow with a shared Async (loading/busy/error) base and launchRequest, Channel effects, session-scoped Koin stores that die on logout/switch, shell chrome decided by one pure function (chromeOf: tabs/sidebar/banners, isFullScreen vs ownsBottom, one width breakpoint), a shared-device (kiosk) zone that is not a member, undo instead of confirm, a live-channel loss row, a closed markup subset for member/AI text, platform services as commonMain interfaces, Compose resources i18n with locale parity and a language order, canvas-on-web behaviour (no error boundary, text input, gestures, secure context), drag & drop, ViewModel testing. Apply after kmp-architecture; pair with compose-navigation3 and compose-design-system.
metadata:
  version: "3.1.0"
  requires: "kotlin-project-layout, kmp-architecture, kotlin-domain-modeling"
  project-types: "full-stack-kmp-app, compose-desktop, compose-mobile, compose-web"
---

# Compose Multiplatform UI

## When to use

- Standing up the UI layer of a full-stack KMP app, or adding a screen / feature package.
- Adding or replacing a platform shell.
- Deciding where state lives (screen ViewModel vs a session store vs local `remember`).

## Architecture in one picture

```
shells (webapp, tgminiapp, androidApp)   main(), Koin start, platform implementations
        ↓
features/
  app/            composition root: zones, shell (rail/tabs), routes, navigation
  <screen>/       XxxEntry + XxxContent + XxxViewModel (+ XxxScreenModel)
  components/     subject-matter widgets (know domain types)
  designsystem/   leaf: tokens + primitives (knows only Compose) → compose-design-system
  session/ di/    session scope, Koin modules
```

Arrows point down only: a screen never imports another screen (it asks `app/` to navigate via a
`NavKey`), `components` never import screens, `designsystem` imports nothing from the app.
Details → [`references/feature-module-structure.md`](references/feature-module-structure.md),
[`references/shell-pattern.md`](references/shell-pattern.md).

## Screen shape

```kotlin
@Composable
fun GoalsEntry(onOpenGoal: (GoalId) -> Unit, onBack: (() -> Unit)?) {   // navigation as explicit lambdas
    val viewModel = koinViewModel<GoalsViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.effects.collect { /* one-shot: snackbar, navigate */ } }
    GoalsContent(state = state, onEvent = viewModel::onEvent, onOpenGoal = onOpenGoal, onBack = onBack)
}

@Composable
internal fun GoalsContent(state: GoalsUiState, onEvent: (GoalsEvent) -> Unit, onOpenGoal: (GoalId) -> Unit, onBack: (() -> Unit)?)
```

## Hard rules

- ❌ No platform code in `features` (`window.*`, `android.*`, `java.awt.*`) — interfaces in
  `commonMain`, implementations in shells.
- ❌ No `server:*` imports; no `contract.*` DTO inside a composable — screens draw screen models /
  domain types.
- ❌ No composable takes a `ViewModel` parameter; `Content` takes state + one `onEvent` + explicit
  navigation lambdas (no `*Callbacks` bags).
- ❌ Never `remember { SomeViewModel() }` — `koinViewModel<T>()` in the Entry.
- ❌ No one-shot flags in state (`val saved: Boolean`) — a `Channel` effect or a sealed stage.
- ❌ No `runCatching` in ViewModels (swallows `CancellationException`); API calls return
  `Either<ApiError, T>` — fold them.
- ❌ No string literals or hardcoded `contentDescription` in composables; no translated strings as
  route/tab keys; no localised text in ViewModel state or effects.
- ❌ No comparison with wire strings in UI (`status == "Pending"`) — map to domain enums first.
- ❌ Never read layout-written state (`LazyListState.layoutInfo`, `onGloballyPositioned` values) in
  composition — `snapshotFlow` in `LaunchedEffect`.
- ❌ Screens never use `MaterialTheme.*` or raw M3 components — `AppTheme.*` and design-system
  wrappers ([`compose-design-system`](../compose-design-system/SKILL.md)).
- ✅ Screen state implements `Async` (`loading` / `busy` / `error`) or is a sealed stage type when
  fields genuinely differ per stage; the name for "an action the person started is in flight" is
  `busy` everywhere.
- ✅ Tenant data lives in **session-scoped** stores (Koin `scope(SESSION)`), closed on logout /
  tenant switch — never in root singletons.
- ✅ `collectAsStateWithLifecycle()` for ViewModel state.
- ✅ Web: error boundary + global error reporting in the shell; guard every back-stack pop.
- ✅ What the shell draws around a screen (tabs, sidebar, banners, "new version" row) is decided by
  **one pure function** `chromeOf(stack, windowWide, role)`; "no chrome at all" (`isFullScreen`) and
  "the screen has its own bottom bar" (`ownsBottom`, exhaustive `when` without `else`) are two
  functions, never one. Mine: widening `isFullScreen` to hide phone tabs silently removed the
  desktop sidebar and banner. Locks: `ShellChromeTest`, `ShellChromeBoundaryTest`
  ([`references/shell-chrome.md`](references/shell-chrome.md)).
- ✅ One layout breakpoint, measured on the **window** width (a token), never on the device type.
- ❌ No screen-level "hide the tabs" flag — a flag has no address, so F5 and back don't know it.
- ✅ Reversible actions act at once and offer an undo capsule; no "Are you sure?" dialog
  ([`references/interaction-patterns.md`](references/interaction-patterns.md)).
- ❌ Member or AI text is never handed to a general Markdown renderer — parse into your own closed
  tree; unknown constructs render as their source; link policy by author
  ([`references/closed-markup.md`](references/closed-markup.md)).
- ✅ A shared device (wall tablet) is a separate session state and zone with its own Koin scope —
  never a member with a role; what it shows is filtered by the server
  ([`references/shared-device-zone.md`](references/shared-device-zone.md)).
- ✅ Most of these rules are tests in `features/jvmTest` (`UiConventionsTest`, `ScreenSurfaceTest`,
  `ResourceKeysTest`) — [`architecture-fitness-tests`](../architecture-fitness-tests/SKILL.md).

## References (load on demand)

- [`references/shell-pattern.md`](references/shell-pattern.md) — what goes in shells vs `features`.
- [`references/feature-module-structure.md`](references/feature-module-structure.md) — package layout and layer rules.
- [`references/state-viewmodel.md`](references/state-viewmodel.md) — `Async`, `FeatureViewModel.launchRequest`, effects, Koin.
- [`references/state-modeling.md`](references/state-modeling.md) — Entry/Content/Leaf, effects, forms, event naming.
- [`references/session-scope.md`](references/session-scope.md) — session-scoped stores, logout/switch, SSE lifetime.
- [`references/performance.md`](references/performance.md) — stability, immutable collections, `derivedStateOf`, keys.
- [`references/canvas-web.md`](references/canvas-web.md) — the web target is a canvas: errors, input, gestures, secure context, bundle.
- [`references/drag-and-drop.md`](references/drag-and-drop.md) — dragging cards in scrollable columns.
- [`references/anti-patterns.md`](references/anti-patterns.md) — BAD/GOOD quick reference.
- [`references/testing.md`](references/testing.md) — ViewModel tests with Kotest + coroutines-test, fakes of client APIs.
- [`references/preferences-storage.md`](references/preferences-storage.md) — KMP interface + per-shell implementations.
- [`references/localization.md`](references/localization.md) — composeResources, locale parity test, language resolution order.
- [`references/shell-chrome.md`](references/shell-chrome.md) — `chromeOf`, `isFullScreen` vs `ownsBottom`, breakpoint, bottom inset, zones.
- [`references/interaction-patterns.md`](references/interaction-patterns.md) — undo instead of confirm, live-channel loss row, offline as a seam, update row.
- [`references/shared-device-zone.md`](references/shared-device-zone.md) — kiosk / wall-tablet client: fourth session state, own scope, no history.
- [`references/closed-markup.md`](references/closed-markup.md) — rendering member/AI text: closed CommonMark subset, own tree, gated formulas.

## Templates

- [`templates/features/Async.kt`](templates/features/Async.kt), [`FeatureViewModel.kt`](templates/features/FeatureViewModel.kt) — shared state base.
- [`templates/features/ShellChrome.kt`](templates/features/ShellChrome.kt) — `chromeOf` / `ownsBottom` / `chromeInset` / `canonicalStack`.
- [`templates/features/PreferencesStorage.kt`](templates/features/PreferencesStorage.kt), [`build.gradle.kts.snippet`](templates/features/build.gradle.kts.snippet).

## Related skills

- Navigation 3 → [`compose-navigation3`](../compose-navigation3/SKILL.md).
- Tokens, skins, wrappers, fonts → [`compose-design-system`](../compose-design-system/SKILL.md).
- Web shell (index.html, dev server, bundle) → [`compose-web-shell`](../compose-web-shell/SKILL.md).
- Telegram shell → [`telegram-miniapp`](../telegram-miniapp/SKILL.md).
- Device principal and refresh routes (server half of the shared device) → [`ktor-auth-sessions`](../ktor-auth-sessions/SKILL.md).
- Planned (later wave), pointers only: `compose-adaptive-shell` (zones + chrome + canonical stacks as
  one skill), [`ui-wording-discipline`](../ui-wording-discipline/SKILL.md), `ux-rules-as-tests`, `designer-pass-process`.

## Apply this skill

Step-by-step → [`USAGE.md`](USAGE.md).

## Verify

Walk through [`checklists/ui-checklist.md`](checklists/ui-checklist.md).
