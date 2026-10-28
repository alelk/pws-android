# Localisation & runtime i18n

Compose Multiplatform resources are the **only** source of user-facing text.

## Layout

```
features/src/commonMain/composeResources/
├── values/strings.xml       # default (English) — also the silent fallback for missing keys
├── values-ru/strings.xml
└── font/                    # bundled fonts covering every shipped script (see compose-design-system)
```

## The fallback trap — test key parity

A key added to `values/` but not to `values-ru/` produces no error and no warning: the Russian UI
silently renders English. Guard it with a symmetric test:

```kotlin
class ResourceKeysTest : FunSpec({
    val root = File("src/commonMain/composeResources")
    fun keysOf(locale: String): Set<String> {
        val text = File(root, "$locale/strings.xml").takeIf { it.isFile }?.readText()
        text shouldNotBe null                           // wrong working dir must not pass vacuously
        return Regex("""<(?:string|plurals)\s+name="([^"]+)"""").findAll(text!!).map { it.groupValues[1] }.toSet()
    }
    test("every locale declares the same keys") {
        val en = keysOf("values"); val ru = keysOf("values-ru")
        (en - ru).sorted().shouldBeEmpty()
        (ru - en).sorted().shouldBeEmpty()              // stale translations are the rarer half
    }
})
```

Pair it with a glyph test: no `So`-category symbols used as icons in UI source, and no maths
symbols the bundled font subset cannot draw (they render as tofu on a canvas).

## Resolving

```kotlin
Text(stringResource(Res.string.goals_title))
Icon(painter, contentDescription = stringResource(Res.string.common_back))
```

Outside composition: typed messages from the ViewModel, resolved in the Entry:

```kotlin
sealed interface UiMessage { data object Saved : UiMessage; data class Failed(val error: ApiError) : UiMessage }
// Entry:
val text = when (m) { UiMessage.Saved -> getString(Res.string.saved); is UiMessage.Failed -> getString(m.error.messageRes()) }
```

`ApiError → StringResource` is one function (`error/ApiErrorText.kt`) keyed by the error **code**,
never by the server's `message`.

## Which language

Decide explicitly and write it down. A resolution order proven in production:

1. the person's choice **on this device** (`PreferencesStorage`), if made — a field "Language:
   As the tenant / English / Русский" on an appearance screen (available to every role);
2. the tenant's locale, once the tenant is known (after sign-in);
3. the system/browser locale (before sign-in, and as the fallback).

Apply it once at the root (`AppRoot`) as a thin wrapper over the Compose resource environment, so
every screen follows one choice; the resolved locale also drives date, time and number formatting
(the time zone stays the tenant's). On web the wrapper sets `window.__customLocale`, which the
shell's `navigator.language(s)` override answers with
([compose-web-shell](../../compose-web-shell/SKILL.md)). The server-side language for
letters/notifications and AI content follows the tenant setting alone (a device choice never
reaches the server). Before relying on the override, time-box a check that it works on wasm without
private API; if it does not, the device choice is a seam and the UI follows the system locale.
Evidence: family-planner ADR 0057.

## Rules

- No string literals in composables; no hardcoded `contentDescription`.
- Route/tab keys are stable identifiers; only visible labels are translated.
- Plurals via `<plurals>` + `pluralStringResource`; numbers and dates formatted in the composable
  from canonical values (never pre-formatted in state).
- One `values-XX` folder per supported locale, all at key parity (tested).
