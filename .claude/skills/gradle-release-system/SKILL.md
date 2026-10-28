---
name: gradle-release-system
description:
  Use when setting up or auditing the release pipeline of a Gradle (or asset-only) project — Conventional Commits, semantic-release, GitHub Actions CI, main → stable / next → -rc.N, CHANGELOG, app.version handoff, the first-release-is-1.0.0 trap, and per-profile publishing:
    backend-library (GitHub Packages + cleanup), frontend-webapp (release assets), docker-app (single workflow build → image → release in the release job (image first, so a failed image leaves no tag and a re-run retries) because GITHUB_TOKEN releases never trigger `on:
      release`, release-due detection from the dry-run, image context from release artifacts only, digest-pinned bases with a rebuild workflow; or two-stage docker-publish via Dockerfile.ci), asset-release (Docker CLI tools, verifyReleaseCmd vs publishCmd), full-stack-app. Verify the artifact, not the sources (verifyReleaseJar, thin/fat jar names). CI runs exactly ./gradlew build; hardening: permissions {}, SHA-pinned actions, lockfile semantic-release with --ignore-scripts, concurrency, timeouts, setup-gradle, self-hosted daemon cleanup, baking Android SDK/Chrome into a self-hosted runner's own image, finding what is slow (per-task Gradle timings in the log, step timeouts + a diagnostics step, a timeout is "cancelled"), a slow self-hosted network (persistent BuildKit, no gha cache, QEMU flags, LAN mirror, DNS), one commit = one run (push + PR).
metadata:
  version: "3.4.0"
  requires: "kotlin-project-layout"
  project-types: "backend-library, frontend-webapp, docker-app, asset-release, full-stack-app"
---

# Gradle Release System

## When to use

- Setting up release automation for a new repository.
- Adding Conventional-Commits-driven semantic versioning to an existing project.
- Adding a Docker publish step that doesn't rebuild everything from source.
- An image that should appear with every release but doesn't (the `on: release` trap).
- Wiring binary-asset releases (databases, archives) attached to GitHub Releases.

## Profiles

Pick one per repo. Each has its own `templates/<profile>/` folder.

| Profile           | What it builds                                                                        | Output destination                                                                                                   |
|-------------------|---------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------|
| `backend-library` | JVM/KMP library published to a package registry                                       | Maven package (e.g. GitHub Packages)                                                                                 |
| `frontend-webapp` | Web application with release-asset upload                                             | Archive attached to GitHub Release                                                                                   |
| `docker-app`      | Containerised application with Docker image (single-workflow preferred; two-stage)    | OCI image pushed to GHCR                                                                                             |
| `asset-release`   | Project that builds binary assets via Docker CLI tools (databases, bundles, archives) | Binary files attached to GitHub Release                                                                              |
| `full-stack-app`  | KMP server + web client, not yet containerised (once it is → `docker-app`)            | Fat JAR + web dist `.tar.gz` + `app.version` attached to GitHub Release (frontend-webapp templates + the server JAR) |

## What gets configured

- Branch-based releases: `main` → stable, `next` → `-rc.N`.
- `CHANGELOG.md` generation.
- Version handoff through the `app.version` file (single source of truth).
- Test-result reporting via `mikepenz/action-junit-report`.
- *(backend-library)* manual package-cleanup workflow → [
  `references/package-cleanup.md`](references/package-cleanup.md).
- *(docker-app)* image published by the release job itself, from the artifacts it just released
  → [`references/single-workflow-release-image.md`](references/single-workflow-release-image.md);
  or the two-stage form → [`references/docker-publish-two-stage.md`](references/docker-publish-two-stage.md).
- *(docker-app, full-stack-app)* artifact verification before anything ships
  → [`references/release-artifact-verification.md`](references/release-artifact-verification.md).
- *(asset-release)* pre-committed artifact verification + critical hook ordering
  → [`references/asset-release-gotchas.md`](references/asset-release-gotchas.md).

## Required files in a target project

All profiles share:

| File                       | Purpose                                                                                                    |
|----------------------------|------------------------------------------------------------------------------------------------------------|
| `.github/workflows/ci.yml` | Build, test, semantic-release                                                                              |
| `.releaserc.yaml`          | semantic-release configuration                                                                             |
| `prepare.sh`               | Called by `verifyReleaseCmd` — writes `app.version`                                                        |
| `publish.sh`               | Called by `publishCmd` — publishes artifacts                                                               |
| `app.version`              | Single source of truth for project version                                                                 |
| `build.gradle.kts`         | Version wired to `app.version` (see shared snippet)                                                        |
| `.github/dependabot.yml`   | Recommended: keeps SHA-pinned actions current — [`references/ci-hardening.md`](references/ci-hardening.md) |

Per-profile extras:

| Profile           | Extra files                                                                                                                                                                                  |
|-------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `backend-library` | `.github/workflows/cleanup-packages.yml`                                                                                                                                                     |
| `docker-app`      | single-workflow: `ci.yml` (with release + image), `image.yml` (rebuild), `deploy/bin/image-context.sh`, runtime `Dockerfile`; two-stage: `Dockerfile`, `Dockerfile.ci`, `docker-publish.yml` |
| `frontend-webapp` | `.github/workflows/deploy.yml`                                                                                                                                                               |
| `asset-release`   | `build-<assets>.sh`, `stage-release-assets.sh`, `tool.version`, `.env.example`                                                                                                               |

## Inputs

| Parameter             | Default                    | Notes                                                                  |
|-----------------------|----------------------------|------------------------------------------------------------------------|
| `PROJECT_TYPE`        | —                          | `backend-library` / `frontend-webapp` / `docker-app` / `asset-release` |
| `RELEASE_BRANCH`      | `main`                     |                                                                        |
| `PRERELEASE_BRANCH`   | `next`                     |                                                                        |
| `VERSION_FILE`        | `app.version`              |                                                                        |
| `JAVA_VERSION`        | `21`                       |                                                                        |
| `NODE_VERSION`        | `20`                       | For semantic-release                                                   |
| `ARTIFACT_PATH`       | —                          | Frontend: archive path                                                 |
| `GRADLE_PUBLISH_TASK` | —                          | Backend: publish task name                                             |
| `DOCKER_REGISTRY`     | `ghcr.io`                  | docker-app only                                                        |
| `DOCKER_IMAGE_NAME`   | `${{ github.repository }}` | docker-app only                                                        |

## Hard rules

- ❌ **Never** publish from PR jobs — release steps run only on push to release branches.
- ❌ **Never** hardcode registry credentials — always `secrets.*`.
- ❌ *(asset-release)* **Never** build binary assets in `publishCmd` — they belong in
  `verifyReleaseCmd`. Read [`references/asset-release-gotchas.md`](references/asset-release-gotchas.md)
  before touching that profile.
- ✅ CI runs exactly the local gate (`./gradlew build`) — no hand-picked task subset.
- ✅ Use `fetch-depth: 0` so semantic-release can read full git history.
- ✅ Pre-1.0: no `!` / `BREAKING CHANGE` in commits — semantic-release would jump to `1.0.0`.
  Write the rule into the contributor/agent guide so an agent never adds one "for correctness".
  The **first** release is `1.0.0` anyway unless a `v0.x` tag is seeded; crossing 1.0 is a recorded
  owner decision → [`references/pipeline-process.md`](references/pipeline-process.md#the-first-release-and-10).
- ❌ *(docker-app)* **Never** chain the image to the release with `on: release` / `workflow_run` —
  events made with `GITHUB_TOKEN` trigger no workflow; the image silently never appears. Publish in
  the release job, gated on "released in **this** run" (version moved *and* tag exists), never on
  tag existence alone (that republishes the previous version over itself).
- ✅ *(docker-app)* The image context is the release artifacts only, assembled by a script that
  refuses a thin jar, a jar with local config, a web archive without `index.html` and foreign files;
  base images pinned by index digest, with a rebuild workflow for digest refreshes.
- ✅ Verify the **artifact**, not the sources: `verifyReleaseJar` opens the built jar and fails on
  `application-local*`; the thin jar has its own base name so `app-*.jar` is unambiguous
  → [`references/release-artifact-verification.md`](references/release-artifact-verification.md).
- ✅ A release builds, versions and publishes — it deploys nothing. Deployment →
  [`single-host-stand-deploy`](../single-host-stand-deploy/SKILL.md); schema changes inside a release →
  [`pre-deploy-schema-policy`](../pre-deploy-schema-policy/SKILL.md).
- ✅ Hardening → [`references/ci-hardening.md`](references/ci-hardening.md): `permissions: {}` at
  the top and grants per job, actions pinned to a commit SHA, `concurrency`, `timeout-minutes`,
  `gradle/actions/setup-gradle`, semantic-release from a committed lockfile with `--ignore-scripts`,
  daemons stopped at the end of every Gradle job on a self-hosted runner.
- ❌ **Never** combine `actions/setup-java`'s `cache: gradle` with `gradle/actions/setup-gradle`
  in the same job — they cache the same paths and conflict. Use `setup-gradle` alone.
- ✅ Set `org.gradle.jvmargs` explicitly in `gradle.properties` (heap **and** `-XX:MaxMetaspaceSize`)
  — see the template's comment. Gradle's auto-sizing has under-provisioned Metaspace on a GitHub
  Actions runner badly enough to kill the daemon mid-build.
- ✅ Build once, release the same bytes: the version enters artifacts as a resource, never as generated
  source, so the release job reuses the build job's compiled output; the lockfile of the release tools
  has no proxy URLs — [`references/ci-hardening.md`](references/ci-hardening.md).
- ❌ *(backend-library)* **Never** assume a consumer repo's `GITHUB_TOKEN` can read this library's
  GitHub Packages once the library repo is **private** (or a package's Actions access is not granted):
  the registry answers 404, so Gradle reports "Could not find …:<version>" for a version that does
  exist. Give the consumer's Gradle steps a **classic PAT** (`read:packages` + `repo`; fine-grained
  PATs do not work for Maven; authorise SSO if the org uses it) in a secret such as `GPR_TOKEN`,
  exported as `GITHUB_TOKEN` (+ `GITHUB_USER` if the owner is not the default) only on Gradle steps.
  Keep the built-in token for semantic-release / `gh`. Alternative: per-package *Manage Actions
  access* → Add repository (Read) — per package, and new modules need it again. Making a repo
  private is a CI-breaking change: grep the consumers' workflows first.
- ✅ Keep workflow branch filters and `.releaserc.yaml` `branches` aligned.
- ✅ Run containers as a non-root user.
- ✅ Use Docker layer caching: `cache-from: type=gha` on a hosted runner; on a persistent self-hosted runner a
  kept BuildKit builder instead (`keep-state`), the gha cache only adds slow transfers —
  [`references/ci-hardening.md`](references/ci-hardening.md#a-slow-network-on-a-self-hosted-runner).
- ❌ A push to a release branch with an open PR from it must not build twice, and the two runs must not
  share a cancelling concurrency group (the push run feeds `release`) — same file, "One commit, two runs".

## Templates

| Path                                                                                                               | Purpose                                                                     |
|--------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------|
| [`templates/shared/.releaserc.yaml`](templates/shared/.releaserc.yaml)                                             | semantic-release config                                                     |
| [`templates/shared/build.gradle.kts.snippet`](templates/shared/build.gradle.kts.snippet)                           | Version wiring from `app.version`                                           |
| [`templates/shared/app.version.example`](templates/shared/app.version.example)                                     | Starter version file                                                        |
| [`templates/shared/dependabot.yml`](templates/shared/dependabot.yml)                                               | Weekly bump of SHA-pinned GitHub Actions                                    |
| [`templates/shared/verify-release-jar.gradle.kts.snippet`](templates/shared/verify-release-jar.gradle.kts.snippet) | `verifyReleaseJar`, thin/fat jar names, fat jar out of `assemble`           |
| [`templates/shared/ci-task-timings.init.gradle.kts`](templates/shared/ci-task-timings.init.gradle.kts)             | `[timing]` per slow task + the 25 slowest, in the log and the job summary   |
| `templates/backend-library/*`                                                                                      | CI, cleanup workflow, prepare/publish scripts                               |
| `templates/frontend-webapp/*`                                                                                      | CI, deploy stub, prepare/publish scripts                                    |
| `templates/docker-app/ci-single-workflow.yml`, `rebuild-image.yml`, `image-context.sh`                             | Single-workflow release + image, rebuild, artifact-only context             |
| `templates/docker-app/*` (rest)                                                                                    | Two-stage: CI, docker-publish workflow, Dockerfile, prepare/publish scripts |

## References (load on demand)

- [`references/pipeline-process.md`](references/pipeline-process.md) — semantic-release hook order + CI step sequence +
  dry-run notes.
- [`references/asset-release-gotchas.md`](references/asset-release-gotchas.md) — critical rules for the `asset-release`
  profile.
- [`references/package-cleanup.md`](references/package-cleanup.md) — `backend-library` cleanup workflow inputs,
  behaviour, token requirements.
- [`references/single-workflow-release-image.md`](references/single-workflow-release-image.md) — release and
  image in one job (image before the release), release-due detection, artifact-only image context, digest refresh.
- [`references/docker-publish-two-stage.md`](references/docker-publish-two-stage.md) — CI builds artifacts →
  `docker-publish` uses them (and the trigger trap).
- [`references/release-artifact-verification.md`](references/release-artifact-verification.md) — verify the
  built jar/image, not the sources.
- [`references/ci-hardening.md`](references/ci-hardening.md) — supply-chain and reliability settings for the workflows.

## Apply this skill

Step-by-step → [`USAGE.md`](USAGE.md).

## Evidence

family-planner `.github/workflows/{ci,image}.yml` + `gradle/ci-task-timings.init.gradle.kts` (2026-10-28: a
30-minute release job cut at the image step on a slow line, duplicate push + PR builds; step 66 K10: `on: release` never
fired; step 78
P2: `permissions: {}`, lockfile under `.github/release/`), `server/app/build.gradle.kts`
(`verifyReleaseJar`, thin jar renamed after finding H5), first release `1.0.0` on 2026-09-21 with no
seed tag.
