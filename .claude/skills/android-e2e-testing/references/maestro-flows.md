# Writing Maestro flows that can fail

Versions: Maestro CLI 2.x (the traps below were observed on 2.5.1 — re-verify when upgrading).

## Selectors

- Production code owns the id; the env file mirrors it; flows use `${VAR}`.
  ```kotlin
  Modifier.testTag("action:save")                       // buttons, fields, rows
  Modifier.semantics { contentDescription = "home" }    // navigation items
  ```
- Naming: `action:<verb-noun>`, `field:<name>`, `nav` ids = route names, list rows
  `<kind>-row-<id>`, dynamic parts after a stable prefix (`song-row-1`, `tag-row-<name>`).
- Match a family with a regex id: `id: "song-row-.*"`.
- Visible text only for system UI (picker buttons) and always as a documented exception.
- `testTagsAsResourceId` on the shell root, or none of the above is visible
  ([template](../templates/ShellRootSemantics.kt.snippet)). A dialog or bottom sheet is a separate
  window in some Compose versions — if tags inside a sheet are not found, set the semantics on the
  sheet content too.
- Parity between the env file and production source is a fitness test, not a convention
  ([template](../templates/E2eSelectorParityTest.kt)).

## Structure

```
e2e/
  config/compose.env          ids, timeouts, test data (one file, KEY="value")
  flows/<app>/NN-name.yaml    numbered: stable order, selectable by number
  flows/<app>/_helpers/       subflows (cold-start, dismiss-keyboard, open-item…)
  flows/<app>/<flavor>/       flavor-only flows (own suite.yaml)
  scripts/run-<app>.sh        runner
  artifacts/ reports/ screenshots/   git-ignored
```

- Flow header: `appId: ${APP_ID}` + `name:`. The first comment says the **kind** (smoke / mutation
  / diagnostic) and the **precondition** (clean state, debuggable build, a planted state file).
- A flow reads like the scenario; mechanics are subflows. Over ~25 steps → split or lower a layer.

## Timeouts

- Constants in the env file: `TIMEOUT_SHORT / MEDIUM / LONG / COLD_START / BOOK_INSTALL`.
- `extendedWaitUntil` with a timeout for anything that loads; never a fixed `sleep`-style wait.
  The cold start after `clearState` creates the database and can take many seconds on a loaded
  machine — give it its own constant.
- Wait for *the thing you assert next*, not for a spinner to go away, unless the spinner is the
  thing under test (flow 19 anchors on the loading indicator on purpose).

## Traps

### The soft keyboard hides the bottom bar from the hierarchy

Maestro reads the accessibility hierarchy; nodes occluded by the IME are **absent**, not merely
untappable. Symptom: `Element not found: Id matching regex: home|library` on a bottom-nav tap; no
timeout helps. A screen that auto-focuses a text field (a Search tab) re-opens the keyboard when
you navigate back to it. Reproduces only with a full-size keyboard — on an emulator with
`hw.keyboard=yes` Gboard often shows a floating toolbar and the same flow passes, so it looks flaky.

Fix: a `dismiss-keyboard` subflow before the nav tap ([template](../templates/dismiss-keyboard.yaml)):
press Back **only when a key of the keyboard is visible** (the space bar has a stable id on
Gboard). A Back press while the IME is up is consumed by the IME and closes it without popping the
back stack.

Do not key the guard off "the nav bar is missing": the bar is also missing while the app is still
rendering (minutes on a loaded emulator); Back there exits the app — the bug the helper exists to prevent.

### `hideKeyboard` (2.5.1)

Sent the app to the launcher. Every later command failed with the same misleading "not found" —
and a flow whose remaining steps were all `assertNotVisible` went green against the launcher. Not
used anywhere.

### Negative-only endings

End every flow on `assertVisible` of something only the expected screen has. If a check must be
negative (`assertNotVisible` of a gated screen), pair it with a positive anchor first.

### Scrolling lists

`scrollUntilVisible` with `element: id` and a timeout; for a long Settings screen pass
`visibilityPercentage` so a half-hidden item is not tapped. Prefer a stable id with a regex prefix
(`.*action:export-backup`) when the node carries a parent's prefix.

### Time-based app behaviour

A feature that records only after a dwell (history written after N seconds on a song) needs a
helper that dwells longer than N — keep the number in the helper's comment and tie it to the
production constant. This is the main source of a "diagnostic" flow that never stabilises: fix the
timing in the app (inject the delay) or lower the test to a use-case test.

### System UI

The file picker, permission dialogs and share sheet are not yours: select by visible text there
and accept that those lines are locale/OEM-dependent; run those flows on the pinned emulator image
only. Prefer asserting "no system sheet opened" (gating flows) to driving it.

### Network

Online-dependent first run (catalog download) makes smoke flaky for reasons that are not the app.
Either pick the flows' content from what is seeded/bundled in the artifact, or make the online
step an explicit, tolerant subflow (`optional: true` taps, a generous wait) — and cover the
offline path with its own flow ([`state-and-flavors.md`](state-and-flavors.md)).

## Debugging a red flow

1. Run it alone by number, with `--clean` if it is a mutation flow.
2. Open `artifacts/<run>/<flow>/` — screenshot and view-hierarchy dump at the failing command.
   "Not found" with the right screenshot = occlusion or missing `testTagsAsResourceId`.
3. Only then change the flow. Never raise a timeout to hide a wrong selector.
