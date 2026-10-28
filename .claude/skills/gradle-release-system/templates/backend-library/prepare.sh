#!/usr/bin/env bash
set -euo pipefail

NEXT_RELEASE_VERSION="$(cat .nextRelease.txt)"

if [[ -z "${NEXT_RELEASE_VERSION}" ]]; then
  echo "ERROR: .nextRelease.txt is empty"
  exit 1
fi

# Write version to app.version (single source of truth for the build).
# For rc prereleases semantic-release emits X.Y.Z-rc.N — keep as-is.
echo "${NEXT_RELEASE_VERSION}" > app.version
echo "Prepared app.version: ${NEXT_RELEASE_VERSION}"
