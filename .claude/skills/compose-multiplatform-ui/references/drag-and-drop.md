# Drag & drop inside a scrollable list (kanban board pattern)

Dragging cards between scrolling columns is the hardest common gesture in a Compose app,
and it is where a touch screen differs most from a mouse. This is a working pattern, not a
sketch — it was validated on a phone and a laptop.

## The rule you cannot design around

**In one area, a swipe is either a drag or a scroll — nothing distinguishes them but
time.** A threshold of "finger drags a card immediately *and* the column still scrolls" is
unreachable; any spec that asks for it must be rewritten. Native apps (Trello, Jira,
Things) all resolve it the same way: long press to drag on touch.

Splitting **by area** ("card drags, empty space scrolls") fails on a phone: a column is
almost entirely cards, so every swipe lands on one. Split **by pointer type** instead.

## The gesture

```kotlin
suspend fun PointerInputScope.detectCardDrag(
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) = awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false)
    var overSlop = Offset.Zero

    val started = if (down.type == PointerType.Touch || down.type == PointerType.Stylus) {
        awaitLongPress(down)                      // touch: drag only after a long press
    } else {
        awaitTouchSlopOrCancellation(down.id) { change, slop ->
            change.consume(); overSlop = slop     // mouse: drag right after touch slop
        } != null
    }
    if (!started) return@awaitEachGesture

    onDragStart()
    if (overSlop != Offset.Zero) onDrag(overSlop)
    val completed = drag(down.id) { change -> onDrag(change.positionChange()); change.consume() }
    if (completed) onDragEnd() else onDragCancel()
}

/** true = finger stayed put for longPressTimeoutMillis; false = released or moved first. */
private suspend fun AwaitPointerEventScope.awaitLongPress(down: PointerInputChange): Boolean {
    val slop = viewConfiguration.touchSlop
    val timedOut = withTimeoutOrNull<Boolean>(viewConfiguration.longPressTimeoutMillis) {
        while (true) {
            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                ?: return@withTimeoutOrNull false
            if (change.changedToUpIgnoreConsumed()) return@withTimeoutOrNull false
            if ((change.position - down.position).getDistance() > slop) return@withTimeoutOrNull false
        }
        @Suppress("UNREACHABLE_CODE") false
    }
    return timedOut == null
}
```

**Why not `detectDragGestures`:** it claims the gesture on the first movement, so on a
touch screen an attempt to scroll the column starts dragging a card instead. The critical
detail above is that **nothing is consumed before the long press fires** — a finger that
moves earlier leaves the events to `LazyColumn`, and the column scrolls normally.

## Rendering the dragged card

Draw the "flying" copy as an overlay at the **root** of the composition, positioned in
window coordinates; dim the original slot. Inside the list it would be clipped by the
column and would fight the scroll:

```kotlin
Box(Modifier.fillMaxSize()) {
    Scaffold(...) { /* board */ }
    drag.draggingCard?.let { card ->
        Box(Modifier.offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }) {
            CardBody(card, elevated = true)
        }
    }
}
```

Each card reports its own window bounds via `onGloballyPositioned` (`boundsInWindow()`), so
the overlay starts exactly where the original sat.

## Computing the drop index — outside composition

Showing a gap where the card will land needs `LazyListState.layoutInfo` and column bounds.
Both are written **during layout**. Reading them in composition invalidates the composition
that produced them, and Compose loops recompose→layout inside one frame without returning
to the event loop: on a phone this looks like the app freezing mid-gesture.

```kotlin
var gapIndex by remember { mutableStateOf<Int?>(null) }
LaunchedEffect(columnId, cards, drag) {
    snapshotFlow {
        if (drag.hoveredColumnId != columnId) null
        else dropIndexFor(listState, cards, drag.floatingCenter.y - listTop.value)
    }.collect { index ->
        gapIndex = index
        if (index != null) drag.reportDropIndex(columnId, index)
    }
}
```

`snapshotFlow` observes the same state in its own observer and emits only the deduplicated
result. **Rule: layout-written state (`layoutInfo`, `onGloballyPositioned` values) is read
in `snapshotFlow` inside `LaunchedEffect`, never in composition.**

Give the gap item a distinct `key` and exclude it from the index calculation — otherwise its
appearance shifts the very measurement that produced it.

## Layout trap that hides the bug

```kotlin
Column {
    Text(header)
    LazyColumn(Modifier.fillMaxSize())   // BAD: takes the parent's full height,
                                         // ignoring the header — the tail hangs below
                                         // the clip and cannot be scrolled to
    LazyColumn(Modifier.weight(1f))      // GOOD
}
```

With `fillMaxSize` the list believes it has nothing to scroll, so "drag broke scrolling"
and "the last cards are unreachable" look like one gesture bug when they are two.

## Alternatives worth keeping on the table

Long press is the industry norm and costs nothing, but if it tests badly: a drag handle on
the card (drag only by the grip), or no drag on touch at all — a "move to…" menu, which is
often the better phone interaction anyway. Whatever you pick, write it down in the UX
principles: an undocumented gesture constraint comes back as a bug report.
