# Compose Multiplatform UI Checklist

## Architecture

- [ ] All screens and components are in `features/commonMain`; shells hold only glue
- [ ] No `server:*` imports in `features`; no `contract.*` DTO inside a composable
- [ ] Layers respected: `designsystem` ← `components` ← screens ← `app`; no screen imports another screen
- [ ] Navigation centralised in `app/`
- [ ] Platform services are `commonMain` interfaces with shell implementations bound in Koin

## Screens

- [ ] Every Entry has a pure `Content(state, onEvent, on*)`; no `ViewModel` parameter, no `*Callbacks` bag
- [ ] State implements `Async` (`loading`/`busy`/`error`) or is a sealed stage type; `busy` is the only name for in-flight actions
- [ ] Requests go through `launchRequest` (flag lowered on both branches); no `runCatching`
- [ ] One-shot events are `Channel` effects, never `Boolean` flags in state
- [ ] No UI decision by wire strings; screens draw domain types / screen models
- [ ] `collectAsStateWithLifecycle()`; existing content stays visible during refresh
- [ ] Screen files under the size limit (documented exceptions only)

## Shell chrome & zones

- [ ] `AppShell` reads only `chromeOf(stack, windowWide, role)`; no screen-level "hide tabs" flag
- [ ] `isFullScreen` (wizard only) and `ownsBottom` (exhaustive `when`, no `else`) are separate functions
- [ ] One breakpoint, by window width, from a token
- [ ] Floating capsules read the shell's bottom inset (tabs or the screen's own bar)
- [ ] `ShellChromeTest` (width × role matrix) and a boundary test on who may call the chrome functions
- [ ] `AppRoot` chooses the zone by an exhaustive `when` over session state
- [ ] A shared device (if any): own session state, own Koin scope, no history address, server-filtered narrow DTOs, idle sheets close themselves

## Interaction

- [ ] Reversible actions: act + undo capsule (window from `offeredAt` in the model), no confirm dialog
- [ ] A notification about an undoable fact waits for the window + relay margin and checks the fact's stamp
- [ ] "Reconnecting…" only after a grace period (`transformLatest` + `delay`), decided in a store
- [ ] "New version" row is shell chrome, shown only when the server version is known and differs
- [ ] Member/AI text rendered through the closed markup tree; parser has one call site (source guard)

## State & DI

- [ ] ViewModels via `viewModelOf` / `viewModel { }` and `koinViewModel<T>()`
- [ ] Tenant data lives in `scope(SESSION)` stores with `onClose`; the session closes on logout/switch
- [ ] The authenticated tree is keyed by the session id

## Resources

- [ ] All strings in `composeResources/values*/strings.xml`; key parity across locales is tested
- [ ] No hardcoded `contentDescription`; route keys are not translated labels
- [ ] ViewModel messages are typed keys resolved in the Entry; `ApiError` → string by code
- [ ] Language resolution order written down (device choice → tenant → system) and applied at the root; it also drives date/number formats

## Web (canvas)

- [ ] `ComposeViewport`, not `CanvasBasedWindow`
- [ ] Global error reporting + DOM reload overlay in the shell (`window.onerror`, `onunhandledrejection`)
- [ ] Every back-stack pop guarded; the stack is never empty
- [ ] `touch-action: none; overscroll-behavior: none` on body and canvas
- [ ] Browser APIs feature-detected with a readable message; every stand is HTTPS
- [ ] Multiline fields don't rely on the floating M3 label
- [ ] Bundle size measured compressed against a stated budget

## Interaction & performance

- [ ] Drag in scrollable lists splits by pointer type (mouse: slop, touch: long press)
- [ ] No layout-written state read in composition
- [ ] `LazyColumn` inside a `Column` uses `weight(1f)`; list items have stable keys
- [ ] Collection fields in state are immutable collections

## Build & guards

- [ ] `./gradlew :features:compileKotlinWasmJs` passes (inside `./gradlew build`)
- [ ] UI fitness tests (`UiConventionsTest`, `ScreenSurfaceTest`, `ResourceKeysTest`) are present and green
