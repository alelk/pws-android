# CI hardening

The templates are deliberately readable; before a repository becomes important, harden them.

## Supply chain

- **Pin third-party actions to a full commit SHA**, keep the tag as a comment:
  `uses: actions/checkout@<40-char-sha> # v7`. A moved tag can execute anything with your
  `GITHUB_TOKEN`. Let Dependabot/Renovate bump the SHAs (`package-ecosystem: github-actions`).
- **Reproducible semantic-release**: instead of `npm install --global semantic-release@24 …`,
  commit a tiny `package.json` (exact versions) + lockfile under `.github/release/` and run
  `npm ci --prefix .github/release --ignore-scripts --no-audit --no-fund`, then call
  `.github/release/node_modules/.bin/semantic-release`. Floating majors pull new transitive code on
  every release; `--ignore-scripts` keeps install scripts of that code from running in the job that
  holds a write token.
- **The lockfile must point at the public registry.** `resolved` URLs written while a sandbox or
  corporate proxy was configured (`http://host.docker.internal:…/repository/npm-proxy/…`) fail on the
  CI runner with a 503 on the first `npm ci`. Check `grep -c 'host.docker\|localhost' .github/release/package-lock.json`
  is 0; regenerate with `npm install --registry=https://registry.npmjs.org/`. Integrity hashes stay valid.
- Mind `engines`: a fresh transitive dependency can require a newer Node than the workflow's
  `node-version` (`EBADENGINE` is only a warning, until it is not).
- **Commit the JS lockfile** of Kotlin/JS/Wasm builds (`kotlin-js-store/yarn.lock`).
- Gradle: enable dependency verification (`gradle/verification-metadata.xml`) once the dependency
  set is stable; `gradle/actions/setup-gradle` also validates the wrapper checksum.
- **Dependabot for the actions themselves**: [`templates/shared/dependabot.yml`](../templates/shared/dependabot.yml)
  (`package-ecosystem: github-actions`) — once actions are SHA-pinned, something has to move the
  SHA forward, or "pinned" quietly becomes "pinned to whatever was current the day CI was written."

## Least privilege

```yaml
permissions: { }         # workflow default: nothing; each job names what it needs

jobs:
  ci:
    permissions: # widen only where needed
      contents: read
      checks: write     # test report
  release:
    needs: ci
    if: github.event_name == 'push' && (github.ref_name == 'main' || github.ref_name == 'next')
    permissions:
      contents: write   # tag, release, CHANGELOG commit
      issues: write
      pull-requests: write
```

Splitting "build/test" from "release" also means PR runs never hold write tokens, and the job that
writes runs no tests and no third-party report action. In the build job, `actions/checkout` with
`persist-credentials: false` — nothing later in that job needs the token in `.git/config`.

## Reliability

- `timeout-minutes` on every job (a hung Testcontainers pull shouldn't burn 6 hours).
  A job that hits it ends **cancelled**, not failed, with `Error: The operation was canceled.` and the
  annotation *"The job has exceeded the maximum execution time of Nm0s"* — the same words as a runner
  killed by the OOM killer, so read the annotation first. Give the steps that can hang or crawl their
  own, shorter `timeout-minutes` (assemble, image): they then fail under their own name. End the job
  with a step `if: cancelled() || failure()` + `continue-on-error: true` that prints `free -m`,
  `docker ps -a`, `docker stats --no-stream`, `docker inspect <runner container> --format
  '{{.State.OOMKilled}}'` and the tail of the BuildKit container's log: the next "canceled" then names
  its cause. Size the job as the sum of its steps' ceilings plus checkout and setup — a 30-minute
  release job with an 18-minute image step and 8 minutes of everything else was cut off the day the
  base-image pulls were slow.
- `concurrency`: see "Superseded runs and docs-only changes" below.
- `gradle/actions/setup-gradle` for caching (build + configuration cache) **instead of**
  `setup-java`'s `cache: gradle`, not alongside it — remove `cache: gradle` when you add
  `setup-gradle`. Both cache `~/.gradle/caches`; running both has caused cache corruption in
  practice, not just wasted disk.
- **CI memory is a shared *total* budget, not a per-JVM setting** — this is one lesson, learned
  in two parts on the same project:

    1. **The Gradle daemon's own JVM needs explicit sizing** (`org.gradle.jvmargs` in
       `gradle.properties` — see [
       `kotlin-project-layout`'s template](../../kotlin-project-layout/templates/shared/gradle.properties)).
       Gradle's auto-detected default has been observed to pick **512m heap / 384m Metaspace** on
       a GitHub Actions runner for a multi-module Kotlin project — Detekt, ktlint and the
       Compose/KMP plugins all load into the one daemon process and exhaust Metaspace long before
       heap. Don't just raise `-Xmx`; raise `-XX:MaxMetaspaceSize` specifically.
    2. **Fixing (1) by raising `-Xmx` generously caused a second, different crash** on the same
       project a few days later — same symptom (`Error: The operation was canceled.`, no Gradle
       error to grep), but this time mid-`test`, not mid-compile. The Gradle daemon's new, bigger
       heap ceiling + the Kotlin daemon (`kotlin.daemon.jvmargs`, routinely 4g for a wasmJs build)
        + an **unbounded forked test-worker JVM** (`maxHeapSize` never set on the `Test` task, so it
          got its own auto-detected default too) + Testcontainers' Postgres container together
          exceeded the box's *actual* RAM, and Linux's OOM killer did the rest. **A GitHub-hosted
          runner on a private repo is only 2 vCPU / ~7 GB total** (public repos get 4 vCPU / 16 GB —
          check which one you're actually running on before sizing anything). Fix: bound the test
          JVM too (`tasks.withType<Test> { maxHeapSize = "1g"; maxParallelForks = 1 }` — see the
          convention plugin templates), and keep the Gradle daemon's `-Xmx` modest even after fixing
          the Metaspace ceiling. **Raising one JVM's ceiling to fix a crash, without checking what
          else runs alongside it, just moves the OOM to a later task.**
    3. **Moving to a bigger self-hosted runner (~10 GB) did not fix it either** — the job still died
       in `:features:jvmTest` with `Process 'Gradle Test Executor 1' finished with non-zero exit
     value 137` (SIGKILL = the OOM killer). Measured, not guessed, it was three things, none of
       them a heap setting:
        - **native memory outside `-Xmx`**: a Compose gallery test rendered ~570 screens through
          `ImageComposeScene`, each `render()` returning a Skia `Image` that was never closed. The
          pixels are native; GC rarely runs on a heap of small objects, so the worker grew to
          2.5 GB RSS at `-Xmx1g`. Closing every frame: 885 MB
          ([
          `compose-design-system`](../../compose-design-system/references/wrappers-and-review.md#previews-and-gallery));
        - **a second, idle Kotlin daemon** — `kotlin-dsl` compiles `convention-plugins` with Gradle's
          embedded Kotlin, a different version, so a separate daemon starts and holds ~1.2 GB to the
          end of the build (log: `Detected multiple Kotlin daemon sessions`). Fix:
          `kotlin.compiler.execution.strategy=in-process` in `convention-plugins/gradle.properties`;
        - **the Kotlin daemon on ParallelGC** keeps its whole `-Xmx4g` committed after the wasm link,
          through every test task; G1 with `-XX:G1PeriodicGCInterval` gives it back. And with
          `org.gradle.parallel`, *test tasks of different modules* all start together on a many-core
          box — `maxParallelForks` bounds forks inside one task only; a shared `BuildService` with
          `maxParallelUsages` bounds tasks (see `kotlin-project-layout`'s `TestSlots.kt`).

       How to measure instead of guessing: run the suspected task alone and sample
       `ps -eo rss,args | grep "Gradle Test Executor"` every second; RSS well above `maxHeapSize`
       means native memory, not heap.
- **Self-hosted runners outlive the job.** A Gradle daemon and a Kotlin daemon left idle stay in
  the next job's memory for hours. End each Gradle job with an `if: always()` step:
  `./gradlew --stop || true; pkill -f "[K]otlinCompileDaemon" || true` (the bracket keeps `pkill -f`
  from matching the shell that runs it). In a release job, stop them **before** the image build
  too — buildx under QEMU needs the memory. Keep the hosted `runs-on` as a comment beside the
  self-hosted one ("to revert: swap these two lines"), and write in a comment where the runner is
  defined and how its network reaches GitHub. With a persistent Gradle User Home on the runner,
  `setup-gradle` can run with `cache-disabled: true` (the local disk is the cache).
- **A bare self-hosted runner image has none of what `ubuntu-latest` bundles for free** — Android
  SDK, Chrome/Firefox, fonts, a dozen preinstalled toolchains. Switching `runs-on` surfaces these
  one gate at a time, each failure further into the build than the last (the next `./gradlew build`
  attempt just reaches a later task before dying). Bake what the build actually needs into the
  runner's **own** Docker image — not installed per-job — so it's there once and cached across
  jobs, same as the Gradle/toolcache volumes. Two concrete gotchas hit doing this for a KMP project
  with an `android` target and a `jsTest`/`wasmJsTest` that actually runs in a browser (not just
  disabled per [
  `kmp-architecture`'s wasm-test-task note](../../kmp-architecture/SKILL.md#the-wasm-test-task-under-gradle-9)):
    - `yes | sdkmanager --licenses` reliably dies with SIGPIPE (exit 141) the moment `sdkmanager` has
      read enough prompts and closes its end of the pipe — under the shell's default `set -o
    pipefail` that fails the whole `RUN`/step even though the licenses *were* accepted. Wrap it:
      `(yes || true) | sdkmanager --licenses`. A genuine licensing failure still surfaces loudly on
      the next `sdkmanager install` call, which needs those licenses to succeed.
    - A karma/Kotlin-JS browser test (`ChromeHeadless` by default) fails with "No binary for
      ChromeHeadless browser on your platform." without a real Chrome installed — install
      `google-chrome-stable` from Google's own apt repo (not Ubuntu's snap-only `chromium`, which
      doesn't run in a container without snapd) and point `CHROME_BIN` at it. If the runner's job
      steps run as root (common for these runner images — see the Docker-outside-of-Docker note
      above), Chrome then refuses to start at all ("Running as root without --no-sandbox is not
      supported."); `karma-chrome-launcher` has no env var for extra flags, only `CHROME_BIN` for
      the binary path, so point it at a wrapper script instead that always adds `--no-sandbox
    --disable-dev-shm-usage`.
- Publish JUnit reports on `success() || failure()` so a red run shows *which* test.
- Testcontainers on a current Docker Engine: docker-java falls back to a stale built-in API
  version the engine rejects. Pass the daemon's own API version to the test JVM, detected once in the
  convention plugin (`DockerApiVersion` in [`kotlin-project-layout`](../../kotlin-project-layout/SKILL.md)),
  not a hand-picked constant in CI — a pinned `1.40` goes stale with the next engine upgrade.

## Where the time goes

Find the slow *step* first, then the slow thing inside it.

- **Steps.** The log's gear menu has *Show timestamps*, the raw log has them on every line, and the
  step list shows each step's duration. Compare a slow run with a normal one step by step.
- **Inside Gradle.** `--profile` writes an HTML report (configuration, dependency resolution, time per
  task) — upload `build/reports/profile` as an artifact, in the release job too, not only in `build`.
  A report in an artifact is rarely opened, so also put the numbers in the log:
  [`templates/shared/ci-task-timings.init.gradle.kts`](../templates/shared/ci-task-timings.init.gradle.kts),
  used as `./gradlew --init-script gradle/ci-task-timings.init.gradle.kts build`. It prints
  `[timing] :path 42.3s EXECUTED` the moment a task over 10 s finishes (a job cut by its timeout still
  leaves them in the log), then the 25 slowest tasks, and appends the same table to the job summary.
  The outcome column answers "did the cache work": `FROM-CACHE` / `UP-TO-DATE` / `EXECUTED`.
  Limits: tasks overlap under `org.gradle.parallel`, so their sum exceeds the wall time; configuration
  and dependency downloads are not tasks (`--profile` shows them). Do not reach for a Gradle build
  scan on a restricted network — it is one more slow call to an external service.
- **Inside a Docker build.** BuildKit's plain progress prints `#N DONE 12.3s` per instruction and the
  time of every layer pull; read those before changing anything.

## A slow network on a self-hosted runner

A self-hosted runner shares the host's Docker daemon, so images, binfmt registrations and BuildKit
state outlive the job — which the hosted-runner recipes (fresh machine every time) do not use.

- **Keep the builder.** `docker/setup-buildx-action` creates a fresh builder per job and removes it:
  every release pulled both platforms' base layers again at the internet's speed. Give it a fixed
  `name:` and `keep-state: true`.
- **Then drop `cache-from/to: type=gha`.** On a persistent builder it only adds a download at the start
  and an upload at the end through the same slow line (28 MB took 3 minutes). Keep it on hosted runners.
- **`docker/setup-qemu-action`**: `cache-image: false` (the daemon already holds the binfmt image; the
  action's tarball restore from the GitHub cache is slower than the daemon's own pull) and
  `platforms: arm64` (the default `all` registers every emulator).
- **A LAN pull-through cache for base images** (Nexus, `registry:2`): `buildkitd-config-inline` with
  `[registry."docker.io"] mirrors = ["localhost:8082"]` + `[registry."localhost:8082"] http = true`, and
  `driver-opts: network=host` so the BuildKit container reaches the published port. BuildKit falls
  back to the origin when the mirror is down. A layer the mirror has is a LAN transfer; a miss is as
  slow as the origin, so the first build after a base-image refresh is still slow.
- **A resolver that costs seconds.** Docker's embedded DNS tries the `dns:` upstreams in order, at
  most three. An upstream that drops UDP while TCP works (seen with 1.1.1.1 and 8.8.8.8 on one home
  line) costs ~4 s on **every** lookup of **every** container, hundreds of times per build, and a
  single big-file speed test will not show it. Measure the lookup, not the bandwidth:
  `time getent hosts registry-1.docker.io` in a container; ask each upstream over both transports
  (`dig +tries=1 +time=5 @server name`, then `+tcp`); put the ones that answer UDP first, from two or
  three independent operators (router, Quad9, a regional one), and re-run the check before changing the list.

## Build once, release the same bytes

The `release` job must not redo what `build` already did.

- **The version is a resource, never a source.** `semantic-release --dry-run` rewrites `app.version`
  *before* the release job assembles. If the version feeds a generated `.kt`/`const val`, every
  release recompiles and relinks the whole module (for Kotlin/Wasm: compile + link + wasm-opt, the
  most expensive tasks of the build) and the build cache cannot help. Put the version in
  `processResources` output instead (`build-info.properties`, an `index.html` meta tag) and read it
  at runtime. Check: change `app.version`, run the compile tasks, they stay `UP-TO-DATE`.
- `actions/checkout` cleans ignored files, so `build/` is gone in the release job: reuse comes from
  the Gradle build cache (a persistent Gradle User Home on a self-hosted runner) or from artifacts
  passed between jobs (`upload-artifact`/`download-artifact`) — the latter does not depend on a cache
  and ships exactly what the tests ran against.
- Verify, don't assume: in the release job's log the heavy tasks (`compile*`, `*Optimize`, `shadowJar`)
  should be `FROM-CACHE`/`UP-TO-DATE`. To see where time goes, run with `--profile` and upload
  `build/reports/profile` as an artifact.

## Secrets

- Never echo secrets; never pass them as Docker build args (use BuildKit secrets).
- Prefer OIDC (`id-token: write`) to long-lived cloud keys when deploying.
- A secret scanner (gitleaks / GitHub secret scanning with push protection) on every PR.

## Superseded runs and docs-only changes

Without `concurrency`, every push starts a full build and, on one (self-hosted) runner, they queue:
the stale commits run first, though only the latest result matters.

- **Libraries and webapps (nothing is written by the build/release in a way that a cancel breaks):**
  workflow-level `concurrency: { group: ci-${{ github.ref }}, cancel-in-progress: <PRs only> }`.
- **Single-workflow release (build → release → image, `single-workflow-release-image.md`):** a
  workflow-level group with cancel would kill a running release between image, tag and GitHub
  release. Put `concurrency` on the **jobs** instead: `build` in
  `ci-build-${{ github.event.pull_request.number || github.ref }}` with `cancel-in-progress: true`
  (also for pushes to release branches); `release` in `ci-release-${{ github.ref }}` with
  `cancel-in-progress: false` (the newest pending run replaces older pending ones, which is enough:
  semantic-release works from the branch head). A cancelled `build` skips its `release` (`needs`).
  Keep cleanup steps `if: always()`: they run on cancel too.
- **`paths-ignore` for docs-only changes** (`docs/**`, root `*.md`) saves a full build per
  documentation commit. Before adding it:
    1. Grep the tests for files they read from those paths (fitness tests pinning docs, API surface,
       vocabulary). Anything a test reads is not ignorable.
    2. `*` does not cross `/`: `*.md` is the repository root only. Markdown under `src/**/resources`
       (prompts, policy texts) is code; do not ignore it.
    3. A required status check on a skipped workflow stays "pending" and blocks the merge. If the
       branch protection requires the build, either skip `paths-ignore` or add a no-op workflow with
       the same job name for those paths.
    4. Write the list out per trigger; do not rely on YAML anchors in workflows.
- **One commit, two runs.** With both `push` (release branches) and `pull_request` as triggers, a
  push to `next` while a PR `next → main` is open starts two full builds of the same commit. Their
  concurrency groups differ (`ref` vs the PR number), so on one runner they simply queue back to back.
  Fix it on the PR side, in the job's `if`:
  `github.event_name != 'pull_request' || github.event.pull_request.head.repo.full_name != github.repository || (github.head_ref != 'main' && github.head_ref != 'next')`
  A job skipped by `if` counts as passed for a required check (a *workflow* skipped by `paths-ignore`
  does not — see 3 above); PRs from forks and from other branches still build.
  ❌ Do not "fix" it with one shared concurrency group and `cancel-in-progress`: the PR run can cancel
  the push run, `release` needs that run's `build` and is skipped — no release, no failure to see.
