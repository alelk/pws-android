# Compose on the web: what changes when the UI is a canvas

On web, Compose Multiplatform renders into a single `<canvas>` via Skiko. The browser does
**not** participate in layout, text input, accessibility or error isolation. Everything
below was found on real devices (iPhone/Safari, desktop Chrome) — each item is a behaviour
difference, not a bug to file.

## Entrypoint

```kotlin
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startKoin { modules(platformModule, featuresModule) }
    document.getElementById("app-loader")?.remove()
    ComposeViewport(document.body!!) { AppNavigation() }
}
```

`ComposeViewport` — not the deprecated `CanvasBasedWindow`, and no `onWasmReady` wrapper.
It sizes the canvas to its container and handles DPR/resize.

## There is no error boundary — build one

**The single most important consequence of canvas rendering.** An uncaught exception in
composition takes down the entire Skiko scene: the page stops reacting to anything at all,
with no visible cause and no partial UI. To the user this is "the app died"; the only cure
is a reload. In a DOM app, one component would have broken.

Compose has no in-tree error boundary that can catch this. Therefore, in every web shell:

1. In the shell script loaded by `index.html` (a file, not inline — a strict CSP forbids inline
   script), `error` / `unhandledrejection` listeners reveal a **DOM** overlay
   ("Something went wrong — Reload") over the dead canvas and log the error; the overlay is plain
   HTML, so it works when Compose no longer does.
2. Report to observability with the app version behind an `ErrorReporter` interface
   (`commonMain`), implemented by the shell and throttled — canvas errors are otherwise invisible
   in support ([compose-web-shell / error-reporting-and-update.md](../../compose-web-shell/references/error-reporting-and-update.md)).
3. Keep exceptions out of composition: API errors are `Either` values, not thrown; `runCatching`
   is not a boundary (it swallows cancellation).
4. Never let a routine navigation edge case throw. The classic one: popping the **root**
   entry leaves the back stack empty and `NavDisplay` throws → dead page. Guard every pop
   with `if (backStack.size > 1)` (see [`compose-navigation3`](../../compose-navigation3/SKILL.md)).

Treat "an exception can reach the frame loop" as a release blocker on web, not a polish item.

## Text input

- **Non-Latin input works** — Cyrillic typed on an on-screen keyboard arrives without
  losses or reordering, cursor and selection behave, autocorrect doesn't corrupt the text.
  Make sure the bundled font actually carries the glyphs you need.
- **Multiline `OutlinedTextField` (`minLines > 1`): the floating label does not float.**
  It stays in place and typed text renders on top of it. Use a plain `Text` label above the
  field plus a `placeholder` inside — the placeholder disappears reliably on the first
  character. Re-check on Compose upgrades before removing the workaround.
- **iOS Safari: pasting from another app costs two taps** — the WebKit paste-confirmation
  prompt appears after the system "Paste" item. This is browser policy (DOM apps behave the
  same), not something the UI can remove. Design flows that don't depend on a single-tap
  paste.

## Gestures and pointers

Touch and mouse must be handled differently — details and a ready gesture in
[`drag-and-drop.md`](drag-and-drop.md). Two things belong to the shell, not to `features`:

```css
html, body { touch-action: none; overscroll-behavior: none; }
canvas { touch-action: none; user-select: none;
         -webkit-user-select: none; -webkit-touch-callout: none; }
```

Without these the browser hijacks a gesture mid-flight (text selection, iOS callout,
native pan) and Compose stops receiving pointer events — a card "sticks" under the finger.

## Browser APIs need a secure context

`navigator.mediaDevices` (microphone), clipboard APIs, service workers and notifications
exist only over HTTPS or on `localhost`. On `http://<lan-ip>:8081` — the usual way to open
a dev build from a phone — the object is simply absent and the raw failure is
`undefined is not an object`. Feature-detect and say why:

```kotlin
private fun requireMediaDevices() {
    if (hasMediaDevices()) return
    error(
        if (isSecureContext()) "browser does not support getUserMedia/MediaRecorder"
        else "recording needs HTTPS or localhost — this address is not a secure context",
    )
}
```

**Plan for HTTPS on every stand, including dev/QA on the LAN**, not just production.

Recording format differs by browser: Safari produces `audio/mp4`, Chrome `audio/webm`.
Whatever consumes the audio server-side must accept both.

## Bundle and startup cost

A real Compose canvas app (board + forms + navigation + Ktor client) measured:

| Artifact          | raw     | brotli  |
|-------------------|---------|---------|
| `skiko.wasm`      | 8.6 MB  | 2.60 MB |
| app `.wasm`       | 4.3 MB  | 0.97 MB |
| `app.js`          | 0.6 MB  | 0.09 MB |
| **total**         | 13.5 MB | **3.5 MB** |

Read it this way: the Skiko runtime dominates and is immutable-hashed, so it is cached
across releases — a warm start pulls roughly 1 MB. That makes the canvas approach cheap for
"opened rarely, used a lot" (a kitchen tablet) and expensive for "opened often on mobile
data". Decide which one you are before treating the number as fine.

Serve compressed (`.br`/`.gz` with `Content-Encoding`) — a plain static server hands over
the raw ~13 MB and makes cold start look several times worse than it is. Measuring →
[`compose-web-shell`](../../compose-web-shell/SKILL.md).

## Accepted trade-offs

Screen readers see one canvas, browser text selection and find-in-page do not work, there
is no SEO. Fine behind a login for a known device fleet; not fine for a public marketing
surface — put that in HTML.
