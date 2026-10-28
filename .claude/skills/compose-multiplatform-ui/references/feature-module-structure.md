# `features/` module structure

```
features/src/commonMain/kotlin/<pkg>/features/
├── app/                        composition root
│   ├── AppRoot.kt              session/zone/skin/theme/language choice (exhaustive `when` over session state)
│   ├── AppShell.kt             rail / tabs / banners; window width read once; reads only chromeOf
│   ├── ShellChrome.kt          chromeOf / ownsBottom / chromeInset / canonicalStack (pure, tested)
│   ├── FullScreenRoutes.kt     isFullScreen (pure; the wizard only)
│   ├── DeviceZone.kt           shared-device zone, if any (own Koin scope, no history)
│   ├── AppRoute.kt             @Serializable routes + navConfig
│   ├── AppAddress.kt           route ↔ URL fragment codec (web)
│   ├── NavigationPlan.kt       which destinations exist for which role (pure)
│   └── ZoneRender.kt           what the top of the stack draws (pure, tested)
├── designsystem/               LEAF: tokens + primitives; knows only Compose/kotlinx/resources
├── components/<domain>/        subject-matter widgets: know designsystem + domain types
├── <screen>/                   one package per screen family: board/ today/ task/ goal/ …
│   ├── XxxScreen.kt            XxxEntry (acquire, collect, effects) + XxxContent (pure)
│   ├── XxxViewModel.kt         StateFlow<XxxUiState>; no Compose imports
│   ├── XxxScreenModel.kt       DTO → screen model mapping when drawing needs translation
│   └── XxxStore.kt             session-scoped cache shared by several screens (if any)
├── common/                     Async, FeatureViewModel, client ids
├── session/                    ClientSession, session-level stores
├── platform/                   interfaces the shells implement (storage, history, media, clipboard)
├── error/                      ApiError → string resource
└── di/                         FeaturesModule.kt + one module per package group
```

## Rules

- **Three layers, no shortcut, no reverse arrow:** `designsystem` ← `components` ← screens ← `app`.
  `designsystem` imports nothing from `features`/client; a screen never imports another screen —
  moving between screens crosses `app/` and a `NavKey`.
- A widget that knows what a task or a member *is* belongs in `components/`, not `designsystem/`.
- Navigation lives in `app/`; there is no `navigation/` package scattered across screens.
- One type per file for anything public; screen files stay under ~600 lines (a fitness test
  lists the documented exceptions — a token file, a ViewModel too cohesive to split).
- No `if/when` on platform in `features` — that's a shell concern behind an interface.
- Split `features` into several Gradle modules only when build time demands it; the package
  layers already make the split mechanical.

Machine checks for these rules → [`architecture-fitness-tests`](../../architecture-fitness-tests/SKILL.md)
(`UiConventionsTest` A5 "design system is a leaf", `ScreenSurfaceTest`).
