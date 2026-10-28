# Package Cleanup Workflow (`backend-library`)

Triggered manually via **Actions → Cleanup Old Package Versions → Run workflow**.

## Inputs

| Input                  | Default | Description                                                       |
|------------------------|---------|-------------------------------------------------------------------|
| `min_versions_to_keep` | `5`     | Number of latest pre-release versions to keep per package         |
| `ignore_versions`      | —       | Regex to permanently protect specific versions (e.g. `^2\.24\.`)  |
| `dry_run`              | `true`  | List only — no deletions                                          |

## Behaviour

- Scans only packages whose `repository.name` matches the current repo (cross-repo safe).
- Filters by a configurable Maven group prefix (`PKG_PREFIX` constant in the workflow).
- **Stable release versions are never deleted** — only versions matching
  `-(rc|alpha|beta|snapshot).` are eligible.
- Among eligible pre-releases, keeps the `min_versions_to_keep` newest; deletes the rest.
- Always run `dry_run: true` first.

## Token requirements

| Scenario                                       | Token                                                                          |
|------------------------------------------------|--------------------------------------------------------------------------------|
| Packages published from *this* repository      | `GITHUB_TOKEN` (default)                                                       |
| Packages published from *another* repository   | PAT with `read:packages` + `delete:packages`, stored as `PACKAGES_PAT` secret  |

If `PACKAGES_PAT` is set, it takes priority over `GITHUB_TOKEN`.

`GITHUB_TOKEN` can only delete packages linked to the *current* repository. Cross-repo
deletes return `404 Not Found` — use the PAT path.

## Customisation

- Edit `PKG_PREFIX` constant to match your Maven group prefix.
- Adjust the `isPreRelease` regex if your project uses a different naming convention.
