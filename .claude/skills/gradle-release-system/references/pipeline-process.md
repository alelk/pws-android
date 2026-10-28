# Release Pipeline — what runs when

semantic-release executes hooks in this order:

```
1. verifyReleaseCmd     ← writes .nextRelease.txt, runs prepare.sh
                          (also where asset-release profile builds artifacts)
2. @semantic-release/git ← commits CHANGELOG.md, app.version back to repo
3. (git tag created automatically here)
4. publishCmd           ← runs publish.sh (Gradle publish task, etc.)
5. @semantic-release/github ← creates GitHub Release + uploads `assets` list
```

## CI workflow steps

1. **Determine version (dry-run)** — on release branches only.
   `semantic-release --dry-run` fires `verifyReleaseCmd` → `prepare.sh` →
   correct version is written to `app.version`. Subsequent Gradle build uses it.

2. **Build & Test** — every push and PR. On release branches, the version from step 1
   is baked into JARs and other artifacts.

3. **Publish Test Results** — JUnit XML reports via `mikepenz/action-junit-report`.

4. **Semantic Release** — only on push to release branches:
   a. `verifyReleaseCmd` runs again.
   b. `publishCmd` runs.
   c. `@semantic-release/github` creates the Release and attaches `assets`.
   d. `@semantic-release/git` commits `CHANGELOG.md` and `app.version` back.

5. **(docker-app)** **Docker Build & Publish**:
   - *single-workflow (preferred)* — in the release job, **before** semantic-release, only if a
     release is **due** (the dry-run moved `app.version`); image context from the artifacts just
     built; a failed image then leaves no tag and the re-run retries
     ([`single-workflow-release-image.md`](single-workflow-release-image.md));
   - *two-stage* — a separate `workflow_dispatch` workflow downloads the release assets and builds
     via `Dockerfile.ci` ([`docker-publish-two-stage.md`](docker-publish-two-stage.md)). It cannot
     be chained with `on: release`: events made with `GITHUB_TOKEN` trigger no other workflow.

## semantic-release dry-run notes

- `--dry-run` runs `verifyReleaseCmd` but NOT `publishCmd`, `@semantic-release/git`, or
  `@semantic-release/github`.
- Use `|| true` after the dry-run — non-zero exit when no release is pending.
- The full run later re-executes `verifyReleaseCmd` and `prepare.sh`, then continues.
- After the dry-run the working copy's `app.version` already names the **next** version; to know
  whether the full run released, compare with `git show HEAD:app.version`, not with the file.

## The first release and 1.0

- semantic-release's **first** release is `1.0.0` whatever the commits say — there is no earlier
  tag to bump from. To start below 1.0, push a tag `v0.1.0` on the first commit before the first
  release run; from then on `feat:` gives `0.x` minors.
- Before 1.0: no `!` / `BREAKING CHANGE` (it would jump to `1.0.0`); write that into the agent guide.
- Crossing 1.0 is the owner's decision, recorded (ADR or release doc): from then on a breaking
  commit raises the major, so agents must know which contract counts as public (the HTTP API, a
  published library, the stored data format) and mark breaking changes only for that.
