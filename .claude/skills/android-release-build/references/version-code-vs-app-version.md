# versionCode vs app.version

Two numbers, two jobs:

| Number        | Type               | Who reads it                     | Rules                                                     |
|---------------|--------------------|----------------------------------|-----------------------------------------------------------|
| `versionName` | free string        | humans, crash reports            | from `app.version` (semantic, rewritten by the release pipeline) |
| `versionCode` | int, 1..2 100 000 000 | the OS and every store         | strictly greater than every code ever uploaded to that listing; **never** goes back; one code = one upload |

`app.version` is the single product-version source ([`kotlin-project-layout`](../../kotlin-project-layout/SKILL.md));
semantic-release rewrites only that file ([`gradle-release-system`](../../gradle-release-system/SKILL.md)).
Nothing in that pipeline knows `versionCode` exists — so the Android build has to decide.

*Mine:* a release that builds green and is rejected at upload ("version code already used"), or —
worse — is accepted with a code lower than what users of another listing have installed, so it never
reaches them.

## pws-android today

- Root `build.gradle.kts`: `val versionCode by extra(48)` — a hand-bumped integer, independent of
  `app.version` (`3.7.1`).
- `versionName` = `app.version` + `-` + build date (`MMM-d-yyyy`, UTC) + flavor suffix,
  e.g. `3.7.1-oct-21-2026-uk`; debug adds `-debug`.
- `prepare.sh` writes `app.version` from semantic-release and strips the prerelease suffix
  (`3.8.0-rc.2` → `3.8.0`), so rc builds and the final release share a `versionName` stem.
- The `rustore` flavor enforces a floor: `versionCode > 38` (the last code of the fork it replaces)
  in `VerifyRustoreInvariantsTask`. The store-side rule is written down: "versionCode in a store
  cannot be rolled back; a hotfix ships only with a new versionCode".

Works, with one gap: nothing fails at build time when a release forgets the bump — the store finds it.

## Schemes

**A. Derived from `app.version`** (template default,
[`../templates/version-code.gradle.kts.snippet`](../templates/version-code.gradle.kts.snippet)):

```
MAJOR * 1_000_000 + MINOR * 1_000 + PATCH * 10 + slot      slot: rc.N → N (≤ 8), final → 9
3.7.1        → 3 007 019
3.8.0-rc.2   → 3 008 002   <  3.8.0 → 3 008 009
```

- Monotonic as long as semantic versions are; no manual step; reproducible from a tag.
- rc builds sort **below** their release, so a tester on the rc track gets the final as an update.
- Requires `app.version` to carry the `-rc.N` suffix for prerelease builds — if `prepare.sh` strips
  it (as pws-android's does), every rc and the final share one code and only the first upload wins.
- Leave headroom: `MINOR < 1000`, `PATCH < 100`; assert it.

**B. Explicit counter** (pws-android): one integer, bumped in the commit of every upload.

- Simple and obvious; needed if codes were already assigned irregularly.
- Add a **build-time floor** and bump discipline (a checklist line, or CI comparing with the last
  released tag's value).

**C. CI-derived** (run number / commit count): monotonic for free, but not reproducible from a tag
and tied to one CI system. Use only with an offset above every code already published.

Switching A ← B is a one-way jump upward (fine); never switch to a scheme that yields a lower code.

## Rules

- ✅ One scheme for all flavors; per-listing floors where a flavor replaces something already
  published (`VERSION_CODE_FLOOR`, or a per-flavor invariant like pws-android's `> 38`).
- ✅ The floor is checked at **configuration/pre-build time**, not discovered at upload.
- ✅ A re-upload of the same `x.y.z` (rejected build, hotfix of the packaging only) needs a new code —
  provide an explicit override (`-PversionCodeOverride=`) rather than editing `app.version`.
- ✅ Crash-reporter mapping files are named with **both** `versionName` and `versionCode`
  (`mapping-<variant>-<versionName>-<versionCode>.txt`): two uploads of one version name are
  different binaries.
- ❌ Never per-flavor `versionCode` offsets that can overlap after a few releases.
- ❌ Never put `versionCode` in `app.version` or let semantic-release compute it from commit
  messages — the store's ordering rule is not semver's.
