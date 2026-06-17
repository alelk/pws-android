# Local APK E2E

Black-box smoke tests for a ready local APK using Maestro.

## Scope

Phase 1 covers only local execution for `ru` APK:

- app installs successfully
- app launches and shows home content
- basic search returns non-empty result set

Configured APK path:
`/Users/alexelkin/Projects/software-development/pws-android/output/pws-app-release-2.3.1-ru.apk`

## Files

- `e2e/config/local-apk.env` - local APK path, app id, and smoke strings
- `e2e/flows/smoke/install-and-songs.yaml` - launch + songs availability smoke
- `e2e/flows/smoke/search-basic.yaml` - basic search smoke
- `e2e/flows/smoke/suite.yaml` - suite entrypoint
- `e2e/scripts/run-local.sh` - local runner script

## Prerequisites

- Android emulator/device available via `adb`
- Maestro CLI installed and available as `maestro`

## Run

```bash
cd /Users/alexelkin/Projects/software-development/pws-android
chmod +x ./e2e/scripts/run-local.sh
./e2e/scripts/run-local.sh --flavor ru
```

## Override APK path

```bash
cd /Users/alexelkin/Projects/software-development/pws-android
./e2e/scripts/run-local.sh --flavor ru --apk /absolute/path/to/app.apk
```

## Outputs

- JUnit reports: `e2e/reports/`
- Debug artifacts/screenshots: `e2e/artifacts/`

## Troubleshooting

- `No adb device/emulator is ready` - start emulator and wait until it is fully booted.
- `Required command is missing: maestro` - install Maestro CLI and ensure it is in `PATH`.
- Search smoke fails with empty results - adjust `SEARCH_QUERY` in `e2e/config/local-apk.env` to a guaranteed dataset query.

---

## Compose app E2E (app-compose, locale-independent)

Full E2E suite for `app-compose` APK. Tests are **locale-independent**: selectors use
stable `contentDescription` values added to `pws-core/features`, not localized text.

### How locale-independence works

| Element                  | Selector                                                           | Where set                                                                                     |
|--------------------------|--------------------------------------------------------------------|-----------------------------------------------------------------------------------------------|
| Nav tabs                 | `contentDescription = destination.route` (`"home"`, `"search"`, …) | `NavigationBar.kt`                                                                            |
| Favorite FAB             | `contentDescription = "action:toggle-favorite"`                    | `SongDetailScreen.kt`                                                                         |
| More actions menu        | `contentDescription = "action:more-actions"`                       | `SongDetailScreen.kt`                                                                         |
| Edit song action         | `contentDescription = "action:edit-song"`                          | `SongDetailScreen.kt`                                                                         |
| Edit tags action         | `contentDescription = "action:edit-tags"`                          | `SongDetailScreen.kt`                                                                         |
| Save button              | `contentDescription = "action:save"`                               | `SongEditScreen.kt`                                                                           |
| Add tag FAB              | `contentDescription = "action:add-tag"`                            | `TagsScreen.kt`                                                                               |
| Search field             | `contentDescription = "field:search"`                              | `SearchField.kt`                                                                              |
| Song list rows           | `contentDescription = "Song number N"`                             | `SongListItem.kt` (pre-existing)                                                              |
| **Settings icon**        | `.testTag("action:open-settings")`                                 | `HomeScreen.kt`, `HistoryScreen.kt`, `FavoritesScreen.kt`, `SearchScreen.kt`, `TagsScreen.kt` |
| **Number search chip**   | `.testTag("action:number-search")`                                 | `HomeScreen.kt` (`QuickActionsRow`)                                                           |
| **Text search chip**     | `.testTag("action:text-search")`                                   | `HomeScreen.kt` (`QuickActionsRow`)                                                           |
| **Home search field**    | `.testTag("field:home-search")`                                    | `SearchBarWithSuggestions.kt`                                                                 |
| **Home suggestion N**    | `.testTag("home-suggestion-N")`                                    | `SearchBarWithSuggestions.kt`                                                                 |
| **Number input field**   | `.testTag("field:number-input")`                                   | `NumberInputModal.kt`                                                                         |
| **Number suggestion N**  | `.testTag("number-suggestion-N")`                                  | `NumberInputModal.kt`                                                                         |
| **Clear history button** | `.testTag("action:clear-history")`                                 | `HistoryScreen.kt`                                                                            |
| **Confirm clear dialog** | `.testTag("action:confirm-clear-history")`                         | `HistoryScreen.kt` (`ClearHistoryDialog`)                                                     |
| Tag list rows            | `.testTag("tag-row-{name}")`                                       | `TagsScreen.kt` (`TagListItem`)                                                               |

Search query `"1"` is used because it is numeric and guaranteed to return results in every flavour.

### Flow catalogue

| File                           | Type         | What it tests                                                          |
|--------------------------------|--------------|------------------------------------------------------------------------|
| `01-app-launch.yaml`           | smoke        | launch, all nav tabs visible                                           |
| `02-search-basic.yaml`         | smoke        | search "1" → results → open song                                       |
| `03-song-detail-actions.yaml`  | smoke        | open song, verify actions sheet                                        |
| `04-favorites-add-verify.yaml` | mutation     | add favorite → verify in Favorites tab                                 |
| `05-history-after-open.yaml`   | diagnostic   | history recorded after opening song (run separately while stabilizing) |
| `06-tags-create-assign.yaml`   | mutation     | create tag → assign to song                                            |
| `07-song-edit-title.yaml`      | mutation     | edit song title → verify                                               |
| `08-navigation-tabs.yaml`      | smoke        | all 6 nav tabs open without error                                      |
| `09-books-to-song.yaml`        | smoke        | Books → book song list → open song                                     |
| `10-settings-open.yaml`        | smoke        | settings icon → open → back                                            |
| `11-home-number-search.yaml`   | smoke        | home number-search modal → suggestion → song                           |
| `12-favorites-remove.yaml`     | mutation     | add favorite → remove → verify gone                                    |
| `13-tag-to-songs.yaml`         | mutation     | create tag → assign → tap tag → tag-songs screen                       |
| `14-history-clear-all.yaml`    | mutation     | populate history → clear all → verify empty                            |
| `15-search-empty-results.yaml` | smoke        | query with no matches → empty state                                    |
| `16-home-recently-viewed.yaml` | smoke        | open song → home "recently viewed" → tap card                          |
| `18-home-search-suggestions.yaml` | smoke     | home inline search → suggestion → song                                 |
| `suite.yaml`                   | smoke bundle | flows 01 02 03 08 09 (read-only)                                       |
| `suite-full.yaml`              | full bundle  | all smoke + mutation flows                                             |

### Shared subflows (`_helpers/`)

Common sequences are factored out as Maestro `runFlow` subflows so that 11 main
flows no longer duplicate the same 7-step search/open-song boilerplate.

| Helper                              | What it does                                                                |
|-------------------------------------|-----------------------------------------------------------------------------|
| `cold-start.yaml`                   | `launchApp clearState:true` + wait Home (use as first step of mutation flow) |
| `open-song-1-via-search.yaml`       | Search tab → numeric query → tap song-row-1 → wait detail                   |
| `populate-history.yaml`             | open song + dwell past 5s HistoryRecorder threshold + back to root          |
| `create-tag.yaml`                   | Tags tab → add → type TEST_TAG_NAME → save → scroll to it                   |
| `assign-tag-to-song-1.yaml`         | open song → more → edit-tags → pick TEST_TAG_NAME → save                    |

Edit a selector or timeout once in `_helpers/` instead of 11 times across flows.

### Prerequisites

- Android emulator/device available via `adb`
- Maestro CLI installed: `curl -Ls https://get.maestro.mobile.dev | bash`
- APK at `output/compose/pws-app-release-<version>-ru.apk` (version from `app.version`)
- **pws-core rebuilt** after Phase 2 testTag changes and APK reinstalled

### Run (smoke suite)

```bash
cd /Users/alexelkin/Projects/software-development/pws-android
./e2e/scripts/run-compose.sh
```

### Run full suite (includes mutation tests)

```bash
./e2e/scripts/run-compose.sh --full
```

### Run one or several flows by number

The simplest way — pass the flow number(s) as positional arguments:

```bash
./e2e/scripts/run-compose.sh 2              # → 02-search-basic.yaml
./e2e/scripts/run-compose.sh 4 5 6 --clean  # → 04, 05, 06 with pm clear
./e2e/scripts/run-compose.sh 14 --retries 2 # → 14, retry once on flake
./e2e/scripts/run-compose.sh 02-search-basic # → also works by basename
```

(Legacy form `--flow flows/compose/02-search-basic.yaml` still works.)

### Override APK

```bash
./e2e/scripts/run-compose.sh --apk /path/to/other.apk
```

### Clean state + retries (recommended for CI)

```bash
./e2e/scripts/run-compose.sh --full --clean --retries 2
```

- `--clean` runs `adb shell pm clear $APP_ID` before the first flow so the
  whole run starts from a fresh app state (required for reproducible full-suite).
- `--retries N` retries each failed flow up to N times.

### Outputs

- **JUnit reports** (one per flow): `e2e/reports/<timestamp>-compose/junit-<flow>.xml`
- **Maestro debug output** (screenshots, hierarchy dumps): `e2e/artifacts/<timestamp>-compose/<flow>/`

### Troubleshooting

- `No adb device ready` — start the Android emulator and wait for boot to complete.
- `Unable to launch app` — restart adb: `adb kill-server && adb start-server`.
- Nav tap fails — rebuild `pws-core` after the `contentDescription` changes and reinstall the APK.
