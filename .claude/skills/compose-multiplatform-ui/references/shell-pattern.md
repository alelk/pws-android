# Shell Pattern — what goes where

```
                    UI Platforms
         ┌──────────┬──────────┬──────────┐
         │ tgminiapp│  webapp  │androidapp│  ← thin shells (platform glue only)
         └────┬─────┴────┬─────┴────┬─────┘
              │          │          │
              └──────────┼──────────┘
                         │
                  ┌──────▼──────┐
                  │  features   │  ← ALL screens, components, navigation
                  └──────┬──────┘
                         │
              ┌──────────┼──────────┐
              ▼          ▼          ▼
           domain    api:client  api:mapping
```

## Shell responsibilities (only these)

1. Application entry point (`main()` — `ComposeViewport` on web).
2. DI initialisation (Koin).
3. Platform-specific `PreferencesStorage` implementation.
4. Platform-specific API-client configuration (base URL, auth header).
5. Platform-specific interop (Telegram WebApp, Android lifecycle, browser history,
   microphone/`MediaRecorder`, …) — always as an **interface in `features/commonMain`
   with the implementation in the shell**, so `features` stays platform-agnostic.
6. Error boundary + crash reporting for the platform (mandatory on web: `window.onerror` /
   `onunhandledrejection` in the shell script render a DOM "reload" overlay, because Compose cannot
   catch a scene-killing exception in-tree — see [`canvas-web.md`](canvas-web.md)); the shell's
   `ErrorReporter` sends throttled reports with the build version
   ([compose-web-shell / error-reporting-and-update.md](../../compose-web-shell/references/error-reporting-and-update.md)).
7. The build's own version (`BuildInfo`) and a page reloader (`AppReloader`) — interfaces in
   `features`, so the "new version" row is decided in common code.

Not a shell concern, despite the name: the **app shell chrome** (tabs, sidebar, banners around a
screen) is `features/app` code decided by one pure function → [`shell-chrome.md`](shell-chrome.md).

## `features` responsibilities (everything else)

- Routes and entry composables (Navigation 3 `NavKey` / `entryProvider`).
- ViewModels (state holders).
- Navigation graphs.
- Shared UI components.
- Theming.

## Rule of thumb

If a new shell can be added in under an hour by writing only the items above —
the shell pattern is healthy. If it needs to fork screens, copy ViewModels, or
re-implement navigation — features are leaking.
