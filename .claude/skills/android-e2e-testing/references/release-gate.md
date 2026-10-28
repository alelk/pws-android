# The release gate: where E2E actually runs

An E2E suite has value only if it runs on the artifact before it is published. Decide **where** and
write it down; a suite that "exists" but runs nowhere is the common failure.

## Options

| Option                                   | Fits                                   | Cost / catch                                                              |
|------------------------------------------|----------------------------------------|---------------------------------------------------------------------------|
| **Local run before publishing**          | Solo / small team, a few flavors       | Free, real emulator/device. Depends on a person; must be a recorded checklist step |
| CI emulator on a hosted Linux runner     | When `/dev/kvm` is usable              | The [android-emulator-runner](https://github.com/ReactiveCircus/android-emulator-runner) README recommends `ubuntu-latest` over macOS (2–3× faster, cheaper) with a udev step that opens KVM — but it speaks of *larger* Linux runners. For standard or private-repo runners **probe, don't assume** (recipe step 1) |
| CI emulator on a macOS runner            | When the probe says no KVM             | Slower, pricier minutes, emulator boot flakiness; pin the system image   |
| Self-hosted runner with KVM / a device   | Frequent releases                      | You maintain it                                                           |
| Device farm (Firebase Test Lab, …)       | Matrix of real devices                 | Cost; the APK leaves your infrastructure — check what the data policy allows |

For an offline-first app with a handful of flavors, **local-before-publish with a record** is a
respectable gate. CI becomes worth it when releases are frequent, several people publish, or the
suite has proven stable for weeks.

## Make the chosen gate honest

- ❌ A workflow step named "E2E — flavor" that only echoes a notice, next to docs that call E2E a
  "quality gate". It reads as protection and gives none.
- ✅ If E2E is manual: the release workflow/document states *"E2E is run locally before the
  publish; results are recorded in the release PR/checklist"*, and the checklist line has the
  flavors, the version, the commit and APK SHA-256 (`reports/<run>/run-info.txt`) and the date.
  The workflow step is deleted or renamed to what it is.
- ✅ If E2E is in CI: it blocks the release job, the emulator image/API level is pinned, JUnit
  reports and failing-flow artifacts are uploaded, retries are limited to a few and the flaky flow
  list is reviewed.
- ✅ The release summary lists **what was not run** ("uk: not run — no device image", "upgrade
  over 3.7.x: not run") rather than saying "tested".

## Minimal CI recipe (when a runner with virtualization is available)

1. **Enable and probe KVM** on a Linux runner — decide by the probe, not by hearsay:
   ```yaml
   - name: Enable KVM
     run: |
       echo 'KERNEL=="kvm", GROUP="kvm", MODE="0666", OPTIONS+="static_node=kvm"' | sudo tee /etc/udev/rules.d/99-kvm4all.rules
       sudo udevadm control --reload-rules
       sudo udevadm trigger --name-match=kvm
       ls -l /dev/kvm   # no device -> the step fails -> use a macOS or self-hosted runner
   ```
   (The rule is the one from the android-emulator-runner README, read 2026-10-27; `emulator
   -accel-check` after the SDK is installed gives the emulator's own verdict.)
2. Build the release (or the debuggable twin when `run-as` is needed) for the flavor.
3. Start an emulator: API level pinned (a stable one, e.g. 34), `google_apis` x86_64 image, a
   fixed profile, `hw.keyboard=yes` **off** if you want to reproduce the keyboard-occlusion class of
   defect, snapshots disabled for a reproducible boot.
4. Install Maestro (pinned version), run `run-e2e.sh --flavor <f> --full --clean --retries 1`.
5. Always upload `reports/` (it holds `run-info.txt`) and `artifacts/`; fail the job on a non-zero exit.
6. Order is the runner's, from the tags: smoke first (fail fast), then mutation, then
   `device-state` flows; a red canary aborts the job before any flow runs.

## What to put in the Definition of Done

- A release candidate: smoke on each shipped flavor, full on the main one, upgrade smoke over the
  last published version when storage/R8 changed.
- A defect found by E2E becomes a regression flow only if it is artifact-level; otherwise it
  becomes a failing unit/ViewModel test first ([`kotlin-testing-strategy`](../../kotlin-testing-strategy/SKILL.md): bug → failing test first).
