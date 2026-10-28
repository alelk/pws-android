#!/usr/bin/env bash
set -euo pipefail

# TODO: adjust the Gradle task and paths to match your real project structure.

NEXT_RELEASE_VERSION="$(cat .nextRelease.txt)"

if [[ -z "${NEXT_RELEASE_VERSION}" ]]; then
  echo "ERROR: .nextRelease.txt is empty"
  exit 1
fi

echo "Publishing frontend artifact for version: ${NEXT_RELEASE_VERSION}"

# Build distribution
./gradlew :webapp:wasmJsBrowserDistribution

# Package for GitHub Release asset upload
DIST_DIR="webapp/build/dist/wasmJs/productionExecutable"
ARCHIVE="webapp-${NEXT_RELEASE_VERSION}.tar.gz"

tar -czf "${ARCHIVE}" -C "${DIST_DIR}" .

echo "Prepared ${ARCHIVE}"
