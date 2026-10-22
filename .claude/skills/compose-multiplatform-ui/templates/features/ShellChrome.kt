package io.github.myorg.myapp.features.app

import androidx.compose.ui.unit.Dp
import androidx.navigation3.runtime.NavKey

/**
 * What the shell draws around the top of the stack — the ONLY place that decides it; `AppShell`
 * reads nothing else. See compose-multiplatform-ui/references/shell-chrome.md.
 *
 * [isFullScreen] answers "no chrome at any width" (a wizard); [ownsBottom] answers "the screen's own
 * bar is at the bottom". Two questions, never one function: widening [isFullScreen] to hide phone
 * tabs silently takes the sidebar and the banners from wide windows.
 */
data class ShellChrome(
    val tabs: Boolean,
    val sidebar: Boolean,
    val banner: Boolean,
    val update: Boolean,
)

fun chromeOf(
    stack: List<NavKey>,
    windowWide: Boolean,
    hasSidebar: Boolean,
): ShellChrome {
    if (isFullScreen(stack)) return ShellChrome(tabs = false, sidebar = false, banner = false, update = false)
    val own = (stack.lastOrNull() as? AppRoute)?.let(::ownsBottom) == true
    val wideWithSidebar = windowWide && hasSidebar
    return ShellChrome(tabs = !wideWithSidebar && !own, sidebar = wideWithSidebar, banner = true, update = true)
}

/**
 * Only the setup wizard draws without chrome. In a project, keep it in `FullScreenRoutes.kt`; a boundary
 * test pins its callers to the chrome files (shown here in one file for brevity).
 */
fun isFullScreen(stack: List<NavKey>): Boolean =
    when (stack.lastOrNull()) {
        AppRoute.Setup -> true
        else -> false
    }

/**
 * Whether the screen has its own panel at the bottom (an action bar, a reply field, a decision strip).
 * Exhaustive on purpose — no `else`: a new route does not compile until it answers.
 */
@Suppress("CyclomaticComplexMethod")
fun ownsBottom(route: AppRoute): Boolean =
    when (route) {
        is AppRoute.ItemDetail, is AppRoute.Thread, is AppRoute.Decision -> true
        AppRoute.Login, AppRoute.Home, AppRoute.Items, AppRoute.Settings, AppRoute.Setup,
        is AppRoute.Queue,
        -> false
    }

/** How far above the window's bottom a floating capsule sits: the tabs OR the screen's own bar. */
fun chromeInset(
    chrome: ShellChrome,
    tabsHeight: Dp,
    barHeight: Dp,
): Dp = if (chrome.tabs) tabsHeight else barHeight

/**
 * A route that exists only on narrow windows: on wide the decision is the queue's second pane,
 * `Queue(selected = k)`; on narrow it is its own screen over the queue. Applied on width change and on
 * restore, so F5, back and a resize land on the same item. Never empty when the input is not.
 */
fun canonicalStack(
    stack: List<AppRoute>,
    wide: Boolean,
): List<AppRoute> {
    val top = stack.lastOrNull()
    return when {
        wide && top is AppRoute.Decision -> {
            val below = stack.dropLast(1)
            (if (below.lastOrNull() is AppRoute.Queue) below.dropLast(1) else below) + AppRoute.Queue(selected = top.key)
        }
        !wide && top is AppRoute.Queue && top.selected != null ->
            stack.dropLast(1) + AppRoute.Queue() + AppRoute.Decision(top.selected)
        else -> stack
    }
}
