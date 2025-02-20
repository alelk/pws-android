#!/bin/bash

NEXT_RELEASE_VERSION=$(cat .nextRelease.txt)
export NEXT_RELEASE_VERSION
if [[ $NEXT_RELEASE_VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Publishing $NEXT_RELEASE_VERSION as release"
  echo "$NEXT_RELEASE_VERSION" > app.version
  ./gradlew :database:publishAllPublicationsToGitHubPackagesRepository :domain:publishAllPublicationsToGitHubPackagesRepository :domain:domain-test-fixtures:publishAllPublicationsToGitHubPackagesRepository
elif [[ $NEXT_RELEASE_VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+-rc.+$ ]]; then
  echo "Publishing $NEXT_RELEASE_VERSION as pre-release"
  echo "$NEXT_RELEASE_VERSION" > app.version
  ./gradlew :database:publishAllPublicationsToGitHubPackagesRepository :domain:publishAllPublicationsToGitHubPackagesRepository :domain:domain-test-fixtures:publishAllPublicationsToGitHubPackagesRepository
elif [[ $NEXT_RELEASE_VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+-.+$ ]]; then
  echo "Publishing $NEXT_RELEASE_VERSION as snapshot"
  echo "${NEXT_RELEASE_VERSION%-*}-SNAPSHOT" > app.version
  ./gradlew :database:publishAllPublicationsToGitHubPackagesRepository :domain:publishAllPublicationsToGitHubPackagesRepository :domain:domain-test-fixtures:publishAllPublicationsToGitHubPackagesRepository
else
  echo "No release published"
fi