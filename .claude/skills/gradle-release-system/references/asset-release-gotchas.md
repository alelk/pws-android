# `asset-release` Profile — Critical Gotchas

This profile builds binary assets (databases, archives) via Docker CLI tools and attaches
them to a GitHub Release. No Gradle required.

## 1. Build assets in `verifyReleaseCmd`, NEVER in `publishCmd`

semantic-release tags the release **before** `publishCmd`. If asset building fails in
`publishCmd`, the tag is created but the Release has no attachments. On re-run,
semantic-release sees the tag and skips everything — broken release with no recovery.

```yaml
# .releaserc.yaml
- - "@semantic-release/exec"
  - verifyReleaseCmd: |
      echo "${nextRelease.version}" > .nextRelease.txt
      bash prepare.sh
      bash build-assets.sh             # ← failure here aborts before any tag
      bash stage-release-assets.sh
    publishCmd: "echo 'Assets already staged in verifyReleaseCmd'"
```

## 2. Verify pre-committed artifacts on every branch

Expensive-to-build assets that get committed to the repo must be regenerated and diffed
in CI on **every** branch (not only release branches) — catches forgotten rebuilds early.

```yaml
# .github/workflows/ci.yml
- name: Rebuild artifacts
  run: bash build-expensive-assets.sh

- name: Check nothing changed
  run: |
    if ! git diff --quiet -- path/to/artifacts/; then
      echo "::error::Artifacts are out of date! Rebuild locally and commit."
      git diff --name-only -- path/to/artifacts/
      exit 1
    fi
```

## 3. Use a tool-version file, not a hardcoded image tag

Store the Docker image reference for CLI build tools in a dedicated file
(`tool.version`, `pws-mgr.version`). Local scripts and CI read the same file.

```bash
TOOL_IMAGE="$(cat tool.version | tr -d '[:space:]')"
```

One-line upgrades; no drift between local and CI environments.

## 4. Multiple encryption keys — use `.env` locally

When different artifact flavours need different keys (production vs debug), never
hardcode them.

- **Locally:** `.env` (git-ignored), loaded via `set -a; source .env; set +a`.
- **CI:** repository secrets passed as env vars.
- Commit `.env.example` documenting every required variable.

## 5. Glob patterns in `.releaserc.yaml` must not overlap

When the same release has encrypted and non-encrypted variants:

- Use `name-[0-9]*.ext` to match production (avoids catching `name-debug-*`).
- One glob per asset family — never trust a wildcard to pick the right files.

## Asset naming convention

`<name>-<flavour>-<version>.<ext>` — e.g.:

- `pws-ru-1.2.3.dbz.enc`           (production, encrypted)
- `pws-ru-debug-1.2.3.dbz.enc`     (debug, encrypted)
- `pws-ru-test-1.2.3.dbz`          (test, compressed only)
