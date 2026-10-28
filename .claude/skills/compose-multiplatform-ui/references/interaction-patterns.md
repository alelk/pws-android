# Interaction patterns that cross screens

Patterns that every screen of a product should get the same way. Each is small; each has a defect
it prevents.

## Undo instead of confirm

For a reversible action (approve, archive, delete-to-trash, move), act at once and offer a floating
**undo capsule** for a fixed window (e.g. 6 s), instead of an "Are you sure?" dialog. A confirm
dialog is pressed through without reading; an undo is read only by the person who needs it.

- The capsule is a design-system wrapper (`UndoBar`) that owns only its timer; the **caller** owns
  the restore command.
- Its remaining time is computed from an `offeredAt: Instant` stored **in the screen model**, not
  from composition: a screen replaced under the capsule (the next item of a queue) must not restart
  the window, and a capsule drawn in the frame it appeared in must not vanish when the entry is
  recreated.
- It floats above whatever occupies the bottom of the window (tabs or the screen's own bar) — the
  inset comes from the shell
  ([`shell-chrome.md`](shell-chrome.md#bottom-inset-for-floating-things)).
- Irreversible actions (account deletion, erasure) are not undoable — those take a typed
  confirmation or re-authentication, never a capsule.

**The cross-system trap.** If the action also notifies someone (a push "your request was approved"),
the notification must wait for the undo window **plus** a relay margin (e.g. 6 s + 4 s = 10 s
held server-side), and at send time it is suppressed if the fact it announces is no longer the same
fact. Compare the **fact's stamp** (the `decidedAt` the notification was created from), not the
status: after approve → undo → approve the status is "approved" again, but the first notification's
stamp is stale and must not go out. Evidence: family-planner ADR 0066, `UndoBar.kt`,
`UndoWordsTest` (one wording for the capsule everywhere).

## Live channel loss: tell, but not too early

An SSE/WebSocket channel drops for a moment whenever a phone changes network. A "Reconnecting…" row
that flashes for that moment teaches people to stop reading it.

```kotlin
val CHANNEL_LOSS_GRACE: Duration = 5.seconds

fun Flow<ChannelStatus>.lostFor(grace: Duration = CHANNEL_LOSS_GRACE): Flow<Boolean> =
    transformLatest { status ->
        if (status == ChannelStatus.Live) emit(false) else { delay(grace); emit(true) }
    }.distinctUntilChanged()   // a new status cancels the old wait: Reconnecting, Live, Reconnecting restarts the clock
```

The decision lives in a flow in a session-scoped store, not in a composable that forgets it on every
recomposition. Server push semantics (snapshot + deltas + catch-up by sequence) →
[outbox-and-background-jobs](../../outbox-and-background-jobs/SKILL.md).

## Offline: a seam, not a feature

The contract pillars that make an offline queue possible later — client-generated ids,
`Idempotency-Key`, a `version` for CAS, snapshot + sequence catch-up — are cheap and belong in the
contract from day one ([ktor-api-contract](../../ktor-api-contract/SKILL.md)). A persistent command
queue with optimistic projection is **not** covered by this skill: do not build one from a proposed
design as if it were proven. Until it exists, the client shows the last snapshot and the loss row
above; it does not pretend writes succeeded.

## A new version under an open tab

Client and server are one release; a tab that outlived a deploy talks to an API that may have
changed. Show a "new version — reload" row as **shell chrome** (not a screen's banner), only when
the server's version is *known* and differs from the client's ("can't tell" is not "new"); the
reload happens only on a person's press. Web mechanics (version meta, response header) →
[compose-web-shell / error-reporting-and-update.md](../../compose-web-shell/references/error-reporting-and-update.md).

## Wording and UX rules as tests (pointer)

UI wording discipline (one concept = one wording function, voice and formatting rules scanned in
resources) and the UX principles checklist (one filled primary action per screen, verb + result on
buttons, state in words not only colour, empty state = one sentence + one button, no hidden
gestures) are planned as separate skills ([`ui-wording-discipline`](../../ui-wording-discipline/SKILL.md), `ux-rules-as-tests`, later wave).
Until then, their fitness tests (`OneWordPerConceptTest`, `VoiceRulesTest`, `FormattingRulesTest`,
`UndoWordsTest`, `EmptyFieldRowsTest`) follow the source-scanning shape of
[architecture-fitness-tests](../../architecture-fitness-tests/SKILL.md).
