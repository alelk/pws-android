# Usage — Android E2E Testing

Prerequisites: [`android-release-build`](../android-release-build/SKILL.md) (flavors with known
`applicationId`s, a release artifact you can produce locally) and
[`kotlin-testing-strategy`](../kotlin-testing-strategy/SKILL.md) (the layers below E2E exist).

## 1. Decide that E2E is worth it, and write the scope down

Fill this table in the project's testing doc (AGENTS.md / ADR) before any flow:

| Artifact-level risk (see SKILL.md "What E2E is for") | Flow that proves it | Lower-layer test that covers the logic |
|------------------------------------------------------|---------------------|----------------------------------------|
| R8 keeps what is read by name                        | smoke 01–03         | `ProguardRulesTest`                    |
| Flavor X opens with its id / seed                    | smoke, `--flavor X` | `FlavorContractTest`                   |
| First run offline                                    | 19-onboarding-…     | onboarding ViewModel test              |
| Backup → wipe → restore                              | 17-backup-restore   | backup use-case tests                  |

A row without a risk that only the artifact shows is not an E2E flow.

## 2. Make the app addressable

1. Tag the shell root with `testTagsAsResourceId` → [`templates/ShellRootSemantics.kt.snippet`](templates/ShellRootSemantics.kt.snippet).
2. Give every control a flow touches a locale-independent id (`testTag("action:save")`,
   `contentDescription = "home"`). Add ids where the flow needs them, not everywhere.
3. Copy [`templates/compose.env.example`](templates/compose.env.example) to `e2e/config/compose.env`
   and list the ids and timeouts.
4. Add [`templates/E2eSelectorParityTest.kt`](templates/E2eSelectorParityTest.kt) to the app module's
   unit tests; set `SOURCE_DIRS` (include included/composite builds that own the tags), `ID_KEY`
   prefixes and the floors. Run it, then rename one tag to see it red, revert.

## 3. Scaffold `e2e/`

```
e2e/config/compose.env
e2e/flows/compose/_helpers/{cold-start,dismiss-keyboard}.yaml     ← templates/
e2e/flows/compose/01-app-launch.yaml …                            ← from flow-template.yaml
e2e/flows/compose/{suite,suite-full}.yaml
e2e/scripts/run-e2e.sh                                            ← template, edit default_app_id + lists
```

Add `e2e/artifacts/`, `e2e/reports/`, `e2e/screenshots/` to `.gitignore`.
Maestro: install the CLI, pin its version in the e2e README (trap list is version-specific).

## 4. Write the smoke core first

launch (every root destination visible) → open an item through search → detail screen actions
visible. Run it on each flavor: `./e2e/scripts/run-e2e.sh --flavor <f>`. Make it green on a **release**
APK, then add the mutation flows one boundary at a time (clean state, write, read back, positive
assertion), then the regression flows for escaped artifact-level defects.

## 5. Host-side scenarios

Backup/restore, planted state, offline → [`references/state-and-flavors.md`](references/state-and-flavors.md).
Rule: Maestro does UI, the runner does adb, and the runner restores the device on `EXIT`.

## 6. Pick the gate and make it honest

[`references/release-gate.md`](references/release-gate.md). Then:

- `AGENTS.md`: the command, the flavors it covers, "E2E is a pre-publish gate — local" (or CI).
- Release workflow/doc says the same thing the workflow does. No notice-only "E2E" step.
- Release checklist line: flavors run, app version, date, what was **not** run.

## 7. Keep it small

At each release: delete flows that duplicate a lower layer, fix or delete flows that needed
retries, and re-check the Maestro traps ([`references/maestro-flows.md`](references/maestro-flows.md))
after a Maestro or Compose upgrade.
