# Shell chrome — what the app draws around a screen

"Chrome" is everything the composition root draws that is not the screen: bottom tabs, a sidebar /
rail and its toggle, banners at the top (unverified address, "a new version is out"). On one window
width and one role it is a few `if`s in `AppShell`. With two widths, two roles, a wizard and screens
that have their own bottom bar, those `if`s become the place where a fix for the phone silently
breaks the desktop.

## The rule: one pure function decides, the shell only reads it

```kotlin
data class ShellChrome(
    val tabs: Boolean,      // bottom tabs (narrow) — or the member's own floating capsule
    val sidebar: Boolean,   // sidebar + its toggle (wide, roles that have one)
    val banner: Boolean,    // account banners at the top (never touch the bottom)
    val update: Boolean,    // "new version" row — chrome, not a screen's business
)

fun chromeOf(stack: List<NavKey>, windowWide: Boolean, hasSidebar: Boolean): ShellChrome {
    if (isFullScreen(stack)) return ShellChrome(tabs = false, sidebar = false, banner = false, update = false)
    val own = (stack.lastOrNull() as? AppRoute)?.let(::ownsBottom) == true
    val wideWithSidebar = windowWide && hasSidebar
    return ShellChrome(tabs = !wideWithSidebar && !own, sidebar = wideWithSidebar, banner = true, update = true)
}
```

`AppShell` reads `chromeOf(...)` and nothing else. No screen sets a "hide tabs" flag
(`LocalHideTabs`, `showDetailOnNarrow`): a flag has no address, so F5 and browser-back do not know it
exists, and it becomes a second source of truth about what is open.

## Two questions, two functions — never one

| Function             | Question                                                   | Typical answer `true`                         |
|----------------------|------------------------------------------------------------|-----------------------------------------------|
| `isFullScreen(stack)`| "Draw **no** chrome at any width?"                         | a setup wizard only                           |
| `ownsBottom(route)`  | "Does this screen have its own bar at the bottom?"         | detail with an action bar, a thread's reply field, a decision strip |

```kotlin
@Suppress("CyclomaticComplexMethod") // one arm per route is the point
fun ownsBottom(route: AppRoute): Boolean = when (route) {
    is AppRoute.ItemDetail, is AppRoute.Thread, is AppRoute.Decision -> true
    AppRoute.Login, AppRoute.Home, AppRoute.Settings, /* … every other route by name … */ -> false
}   // no `else`: a new route does not compile until it answers
```

**The mine.** The shortest fix for "this screen must not have tabs on the phone" is to add it to
`isFullScreen`. The build is green and a gallery that renders screens in a phone frame (not through
`AppShell`) is green too — while on a wide window the screen silently loses the sidebar, its toggle
and the account banner, and a floating undo capsule lands on top of the screen's own bar.

Evidence: family-planner step 65, ADR 0065; locks `ShellChromeTest` (matrix of `chromeOf` over width
× role), `FullScreenRouteTest` (the own-bottom routes are explicitly `false` for `isFullScreen`),
`ShellChromeBoundaryTest` (source scan: `isFullScreen(` is called only from the chrome files;
`AppShell.kt` never names `ownsBottom`).

## One breakpoint, by window width

One breakpoint (e.g. `900.dp`, a token in `AppTheme.sizing`), measured on the **window**, not the
device: the browser does not know the device type, and a desktop window dragged narrow must behave
like a phone. Wider → sidebar/rail; narrower → tabs. The width is read once in `AppShell` and passed
down as a boolean; screens that need a second layout get it as a parameter, never by measuring again.
Evidence: ADR 0022.

## Bottom inset for floating things

A floating capsule (undo bar, snackbar) must sit above whatever occupies the bottom of the window —
the tabs **or** the screen's own bar. The shell computes it purely,
`chromeInset(chrome, tabsHeight, barHeight) = if (chrome.tabs) tabsHeight else barHeight`, and
provides it to the **whole** content; a screen's bar reports its height upwards. The design-system
side (`LocalBottomBarReport`, `Modifier.reportsBottomBar()`) →
[compose-design-system / sheets-and-insets.md](../../compose-design-system/references/sheets-and-insets.md).

## Width-dependent routes and dead ends

- A screen that owns its bottom (no tabs) is **never the root of a stack**: its only exits are the
  back chevron and browser-back, so after F5 the address must decode to `[parent, screen]`.
- A route that exists only on narrow windows (a phone's full-screen decision that is a second pane
  on wide) is translated by a pure `canonicalStack(stack, wide)` applied on width change and on
  restore.

Both are navigation properties with their own tests →
[compose-navigation3 / routes-and-backstack.md § No dead end](../../compose-navigation3/references/routes-and-backstack.md#no-dead-end).

## Zones

`AppRoot` picks a **zone** by session state with an exhaustive `when` (anonymous, onboarding, app,
and — when a shared device exists — a device zone, see [`shared-device-zone.md`](shared-device-zone.md)).
Each zone has its own back stack and its own chrome rules; the device zone has none.

## Revisit when

A screen needs **both** its own bottom bar and tabs in one frame (an input panel above tabs): then
`ownsBottom` stops being a boolean and `chromeOf` gets a second axis — a decision, not a patch.

A fuller adaptive-shell skill (zones, rail/tabs, wizard, own-bottom, canonical stacks together) is
planned as a separate skill (`compose-adaptive-shell`, later wave); until then this file and
`compose-navigation3` carry it.
