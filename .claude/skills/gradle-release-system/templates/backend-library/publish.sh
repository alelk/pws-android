#!/usr/bin/env bash
set -euo pipefail

NEXT_RELEASE_VERSION="$(cat .nextRelease.txt)"

if [[ -z "${NEXT_RELEASE_VERSION}" ]]; then
  echo "ERROR: .nextRelease.txt is empty"
  exit 1
fi

if [[ "${NEXT_RELEASE_VERSION}" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Publishing ${NEXT_RELEASE_VERSION} as release"
  PUBLISH_VERSION="${NEXT_RELEASE_VERSION}"
elif [[ "${NEXT_RELEASE_VERSION}" =~ ^[0-9]+\.[0-9]+\.[0-9]+-rc.+$ ]]; then
  echo "Publishing ${NEXT_RELEASE_VERSION} as pre-release"
  PUBLISH_VERSION="${NEXT_RELEASE_VERSION}"
else
  echo "Publishing ${NEXT_RELEASE_VERSION} as snapshot"
  PUBLISH_VERSION="${NEXT_RELEASE_VERSION%-*}-SNAPSHOT"
fi

echo "Publishing backend artifacts with version: ${PUBLISH_VERSION}"
./gradlew \
  -Pversion="${PUBLISH_VERSION}" \
  publishAllPublicationsToGitHubPackagesRepository
