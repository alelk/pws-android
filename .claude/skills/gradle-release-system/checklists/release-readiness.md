# Release Readiness Checklist

## Branches and permissions

- [ ] Release branches defined and protected (`main`, `next`).
- [ ] Workflow-level `permissions: {}`; each job grants only what it needs; only the release job
      writes (`contents`, `issues`, `pull-requests`, `packages`).
- [ ] `packages: write` enabled if backend package publication or Docker push is used.
- [ ] `id-token: write` enabled if OIDC-based authentication is used.

## CI and semantic-release

- [ ] Workflow uses full git history (`fetch-depth: 0`).
- [ ] semantic-release runs **only** on push to release branches (not PRs).
- [ ] A superseded build is cancelled; a running release never is (job-level `concurrency`, `ci-hardening.md`).
- [ ] `.releaserc.yaml` branch config matches workflow branch filters.
- [ ] Conventional Commits policy enforced in PR review.
- [ ] semantic-release and plugins installed with `npm ci --ignore-scripts` from a committed lockfile
      (`.github/release/`), exact versions.
- [ ] First release decided: `1.0.0` (default) or a seeded `v0.1.0` tag; crossing 1.0 recorded.
- [ ] `semantic-release --dry-run` passes on release branch context.

## Versioning and metadata

- [ ] `app.version` file exists and is tracked in git.
- [ ] `prepare.sh` correctly writes the expected version format.
- [ ] `publish.sh` reads `.nextRelease.txt` and handles release / prerelease / snapshot.
- [ ] `CHANGELOG.md` is generated and committed back by semantic-release.

## Artifact flow — backend-library

- [ ] Gradle publish task works with release and prerelease versions.
- [ ] `-Pversion=<value>` is passed correctly in `publish.sh`.
- [ ] Published package is accessible in the target registry.
- [ ] `cleanup-packages.yml` workflow is present.
- [ ] Cleanup dry-run passes (lists packages without errors).
- [ ] `PKG_PREFIX` in `cleanup-packages.yml` matches the project's Maven group prefix.
- [ ] If packages span multiple repositories, `PACKAGES_PAT` secret is configured.

## Artifact flow — frontend-webapp

- [ ] Frontend archive path exists before `@semantic-release/github` uploads it.
- [ ] Artifacts are reproducible in a clean CI environment.

## Artifact flow — docker-app

- [ ] `Dockerfile` builds successfully (`docker build .`).
- [ ] Multi-stage build produces a minimal runtime image.
- [ ] Container runs as a non-root user.
- [ ] Health check is configured (`HEALTHCHECK` instruction).
- [ ] JVM container flags are set (`-XX:+UseContainerSupport`, `-XX:MaxRAMPercentage`).
- [ ] The image is published by the release job (single-workflow) or by a manual/PAT-triggered
      workflow — never by `on: release` with `GITHUB_TOKEN`.
- [ ] The image is published **before** the release is created, gated on "a release is due"
      (the dry-run moved `app.version` vs `git show HEAD:app.version`); a failed image build
      leaves no tag, so a re-run retries. The release step fails if it released a version the
      image was not built for.
- [ ] Image context = release artifacts only (`image-context.sh`): refuses thin jar, local config,
      web archive without `index.html`, foreign files.
- [ ] Base images pinned by index digest; refresh recipe in the Dockerfile header; a rebuild
      workflow (`image.yml`) for an existing release.
- [ ] Image tagged with the version (and any extra tag decided once).
- [ ] Docker layer caching (`cache-from: type=gha`) is enabled.
- [ ] Image is accessible in GHCR after a successful publish.
- [ ] Private dependencies reach the build through BuildKit secrets, never build-args.

## Artifact verification

- [ ] `verifyReleaseJar` runs right after `shadowJar` in the release job and fails on
      `application-local*` inside the jar.
- [ ] Thin jar has its own base name; `app-*.jar` matches the shadow jar only.
- [ ] Release jobs call `shadowJar` explicitly; `assemble` does not build the fat jar.

## CI performance and reliability

- [ ] `org.gradle.jvmargs` in `gradle.properties` sets **both** `-Xmx` and
      `-XX:MaxMetaspaceSize` explicitly — not left to Gradle's auto-detected default.
- [ ] `tasks.withType<Test> { maxHeapSize = ...; maxParallelForks = ... }` is set explicitly in
      the convention plugin — an unbounded forked test JVM is the same "smart default" risk as
      an unbounded Gradle daemon, just discovered later in the build.
- [ ] Checked whether the repo is private (2 vCPU / ~7 GB GitHub-hosted runner) or public
      (4 vCPU / 16 GB) before sizing any of the above — the Gradle daemon, the Kotlin daemon,
      every forked test JVM and Testcontainers all share that one total.
- [ ] Test tasks of different modules are limited build-wide (a `BuildService` with
      `maxParallelUsages`), not only forks within a task; no second idle Kotlin daemon for the
      convention build (in-process compile there); the Kotlin daemon runs G1 so it returns memory.
- [ ] The version (`app.version`) reaches the artifact only through resources, never through
      generated sources: after changing `app.version`, `compile*` tasks stay `UP-TO-DATE`.
- [ ] The release job's heavy Gradle tasks are `FROM-CACHE`/`UP-TO-DATE` in its log (or it consumes
      the build job's artifacts) — it does not rebuild from scratch.
- [ ] `.github/release/package-lock.json` has no `resolved` URL pointing at a proxy or localhost.
- [ ] On a self-hosted runner, every Gradle job ends with an `if: always()` step that stops the
      Gradle and Kotlin daemons (the release job also before the image build); the hosted
      `runs-on` is kept as a comment to revert to.
- [ ] An exit 137 of a test executor is diagnosed by measured RSS (native memory counts), not
      fixed by raising `-Xmx`.
- [ ] `gradle/actions/setup-gradle` is used, and `actions/setup-java`'s `cache: gradle` is
      **not** also set in the same job (the two cache the same paths and conflict).
- [ ] `.github/dependabot.yml` has a `github-actions` entry so SHA-pinned actions get bumped.

## Test reporting

- [ ] JUnit XML reports are published via `mikepenz/action-junit-report`.
- [ ] Test results appear in the GitHub Actions checks tab.

## Smoke checks

- [ ] `./gradlew check` passes.
- [ ] `npx semantic-release --dry-run` passes on release branch context.
- [ ] *(docker-app)* `docker build .` completes without errors.
- [ ] *(asset-release)* `bash build-<assets>.sh` completes locally.
- [ ] *(asset-release)* `bash stage-release-assets.sh` produces all expected files in `release-assets/`.

## Artifact flow — asset-release

- [ ] Binary assets are built inside `verifyReleaseCmd` (NOT `publishCmd`) — failure aborts before tag creation.
- [ ] Pre-committed artifacts (e.g. optimized libraries) are verified in CI via `git diff` check on every branch.
- [ ] `stage-release-assets.sh` copies files with versioned, human-readable names.
- [ ] Asset names include flavor and version: `<name>-<flavor>-<version>.<ext>`.
- [ ] Encrypted assets use `.dbz.enc` extension; compressed-only use `.dbz`.
- [ ] `.releaserc.yaml` glob patterns for production assets use `[0-9]*` to avoid matching `debug`/`test` variants.
- [ ] `release-assets/` and `.nextRelease.txt` are in `.gitignore`.
- [ ] Generated databases directory is in `.gitignore`.
- [ ] Docker tool image version is pinned in a dedicated version file (e.g. `tool.version`).
- [ ] `.env.example` is committed and documents all required secret variables.
- [ ] `.env` is git-ignored.
- [ ] CI secrets `PROD_KEY`, `DEBUG_KEY` (or equivalent) are configured in repository settings.
- [ ] `publish.sh` is a no-op (or minimal) — does NOT rebuild assets.
