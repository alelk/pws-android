#!/usr/bin/env bash
set -euo pipefail

NEXT_RELEASE_VERSION="$(cat .nextRelease.txt)"

if [[ -z "${NEXT_RELEASE_VERSION}" ]]; then
  echo "ERROR: .nextRelease.txt is empty"
  exit 1
fi

if [[ "${NEXT_RELEASE_VERSION}" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Publishing ${NEXT_RELEASE_VERSION} as release"
  echo "${NEXT_RELEASE_VERSION}" > app.version
elif [[ "${NEXT_RELEASE_VERSION}" =~ ^[0-9]+\.[0-9]+\.[0-9]+-rc.+$ ]]; then
  echo "Publishing ${NEXT_RELEASE_VERSION} as pre-release"
  echo "${NEXT_RELEASE_VERSION}" > app.version
elif [[ "${NEXT_RELEASE_VERSION}" =~ ^[0-9]+\.[0-9]+\.[0-9]+-.+$ ]]; then
  echo "Publishing ${NEXT_RELEASE_VERSION} as snapshot"
  echo "${NEXT_RELEASE_VERSION%-*}-SNAPSHOT" > app.version
else
  echo "No release published"
fi

