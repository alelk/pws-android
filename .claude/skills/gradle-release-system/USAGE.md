# Usage — Gradle Release System

## 1. Pick a profile

| Profile           | When to use                                                                   |
|-------------------|-------------------------------------------------------------------------------|
| `backend-library` | JVM/KMP libraries published to a package registry                             |
| `frontend-webapp` | Web apps with release-asset upload to GitHub Releases                         |
| `docker-app`      | Containerised apps with Docker image published to GHCR                        |
| `asset-release`   | Projects that produce binary assets via Docker CLI tools (no Gradle required) |

## 2. Copy shared files

```bash
cp <skill>/templates/shared/app.version.example   <target>/app.version
cp <skill>/templates/shared/.releaserc.yaml       <target>/.releaserc.yaml
cp <skill>/templates/shared/dependabot.yml        <target>/.github/dependabot.yml
# Merge templates/shared/build.gradle.kts.snippet into your root build.gradle.kts
```

## 3. Copy profile-specific files

### backend-library

```bash
cp <skill>/templates/backend-library/ci.yml              <target>/.github/workflows/ci.yml
cp <skill>/templates/backend-library/cleanup-packages.yml <target>/.github/workflows/cleanup-packages.yml
cp <skill>/templates/backend-library/prepare.sh          <target>/prepare.sh
cp <skill>/templates/backend-library/publish.sh          <target>/publish.sh
```

### frontend-webapp

```bash
cp <skill>/templates/frontend-webapp/ci.yml        <target>/.github/workflows/ci.yml
cp <skill>/templates/frontend-webapp/deploy.yml    <target>/.github/workflows/deploy.yml
cp <skill>/templates/frontend-webapp/prepare.sh    <target>/prepare.sh
cp <skill>/templates/frontend-webapp/publish.sh    <target>/publish.sh
```

### docker-app — single workflow (preferred)

```bash
cp <skill>/templates/docker-app/ci-single-workflow.yml <target>/.github/workflows/ci.yml
cp <skill>/templates/docker-app/rebuild-image.yml      <target>/.github/workflows/image.yml
mkdir -p <target>/deploy/bin <target>/.github/release
cp <skill>/templates/docker-app/image-context.sh       <target>/deploy/bin/image-context.sh
cp <skill>/templates/docker-app/prepare.sh             <target>/prepare.sh
cp <skill>/templates/docker-app/publish.sh             <target>/publish.sh
# Runtime Dockerfile from artifacts: gradle-docker-build templates/jvm/Dockerfile.ci → deploy/Dockerfile
#   (uncomment BUILD_META; add the web stage if the image serves the web client)
# Merge templates/shared/verify-release-jar.gradle.kts.snippet into the server app module
# .github/release/package.json with exact semantic-release versions, then:
#   npm install --prefix .github/release --registry=https://registry.npmjs.org/   # commit the lockfile
```

Read [`references/single-workflow-release-image.md`](references/single-workflow-release-image.md) first.

### docker-app — two-stage

```bash
cp <skill>/templates/docker-app/ci.yml             <target>/.github/workflows/ci.yml
cp <skill>/templates/docker-app/docker-publish.yml <target>/.github/workflows/docker-publish.yml
cp <skill>/templates/docker-app/prepare.sh         <target>/prepare.sh
cp <skill>/templates/docker-app/publish.sh         <target>/publish.sh
cp <skill>/templates/docker-app/Dockerfile         <target>/Dockerfile
```

### asset-release

This profile has no pre-built template directory — assemble per the recipe in
[`references/asset-release-gotchas.md`](references/asset-release-gotchas.md). At
minimum you'll need:

- `prepare.sh` (writes `.nextRelease.txt` version → `app.version`)
- `publish.sh` (no-op — assets already staged in `verifyReleaseCmd`)
- `build-<assets>.sh` (builds the binary assets via your Docker tool)
- `stage-release-assets.sh` (copies built files to `release-assets/` with versioned names)
- `tool.version` (Docker image reference for the CLI tool)
- `.env.example` (committed) + `.env` (git-ignored)
- `.github/workflows/ci.yml` (rebuilds pre-committed artifacts and runs `git diff` on every branch)

## 4. Customise

1. `.releaserc.yaml` — branch names if your release branch isn't `main`.
2. `ci.yml` — `java-version`, `node-version`, Gradle tasks.
3. `publish.sh` — your actual Gradle publish task or archive path.
4. *(docker-app)* `Dockerfile` — build stages, module paths, JDK version, labels.
5. *(docker-app)* `docker-publish.yml` / the release job's `Publish image` — image name, build-args,
   module paths in `Assemble release artifacts` and in `image-context.sh`.

## 5. Make scripts executable

```bash
chmod +x prepare.sh publish.sh
```

## 6. Validate before first publish

```bash
./gradlew check
npx semantic-release --dry-run     # requires Node.js
```

## 7. Enable publish

- Workflow permissions: `contents: write` and `packages: write`.
- Merge a Conventional Commit (`feat:`, `fix:`, etc.) to `main` (stable) or `next` (-rc).
- Confirm: GitHub Release is created; `CHANGELOG.md` + `app.version` committed back;
  artifacts published; *(docker-app)* the image tag equal to the version exists in GHCR, and a push
  **without** a releasable commit published no image.

## Notes

- Keep branch names synchronised across workflow and `.releaserc.yaml`.
- If your default branch is `master`, update both templates.
- For frontend assets, adjust `path:` in `.releaserc.yaml` to the real artifact path.
- `docker-publish.yml` is `workflow_dispatch`-triggered. Do **not** wire it with `on: release`:
  a release made with `GITHUB_TOKEN` triggers no other workflow. Publish from the release job
  instead (single-workflow variant).
- First release: semantic-release starts at `1.0.0`. To stay below 1.0, push `v0.1.0` on the first
  commit before the first release run.
- **asset-release**: build assets in `verifyReleaseCmd`, never in `publishCmd` —
  see [`references/asset-release-gotchas.md`](references/asset-release-gotchas.md).
- *(backend-library)* Cleanup workflow inputs, tokens, customisation →
  [`references/package-cleanup.md`](references/package-cleanup.md).
