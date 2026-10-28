# Two-Stage Docker Publish (`docker-app`)

Building Docker images directly in CI is expensive (10–20 min for KMP projects).

**Pattern:** CI builds artifacts once and attaches them to the GitHub Release. The
separate `docker-publish` workflow downloads those artifacts and builds runtime-only
images via `Dockerfile.ci` — seconds instead of minutes.

> **Trigger trap.** Do not "automate" this workflow with `on: release` / `on: workflow_run`: a
> release created with the repository's `GITHUB_TOKEN` triggers no other workflow, so the image
> silently never appears. Either keep it manual (it then serves as the *rebuild* workflow), trigger
> it with a PAT/GitHub App token, or publish the image in the release job itself — the preferred
> variant, [`single-workflow-release-image.md`](single-workflow-release-image.md).

## Configuration

- Use `@semantic-release/github` `assets:` list to attach build artifacts (JAR, JS archive)
  to the GitHub Release.
- `docker-publish` workflow:
  - `workflow_dispatch` input: `release_tag`.
  - Uses `gh release download` to fetch the attached artifacts.
  - Builds with `Dockerfile.ci`.
  - Pushes to GHCR using `cache-from: type=gha`.

## Compose Multiplatform web distribution

Use `wasmJsBrowserDistribution` (NOT `wasmJsBrowserProductionWebpack`). The former produces a
complete dist at `build/dist/wasmJs/productionExecutable/` including `index.html`, resources,
and Skiko WASM.

```bash
./gradlew :webapp:wasmJsBrowserDistribution
tar -czf webapp.tar.gz -C webapp/build/dist/wasmJs/productionExecutable/ .
```

Attach `webapp.tar.gz` to the GitHub Release, then `Dockerfile.ci` accepts it via
`--build-arg DIST_ARCHIVE=webapp.tar.gz`.

## Layer cache for a runtime-only image

`cache-to: type=gha,mode=max` exports every layer (tens of seconds each) and buys little when the
image only copies a jar and a dist onto a pinned base. Use `mode=min`, or drop `cache-to`, until the
image has a genuinely expensive layer. Also keep the release job from rebuilding the artifacts the
build job produced — see [`ci-hardening.md`](ci-hardening.md#build-once-release-the-same-bytes).
