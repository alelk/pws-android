#!/bin/bash

NEXT_RELEASE_VERSION=$(cat .nextRelease.txt)
export NEXT_RELEASE_VERSION
if [[ $NEXT_RELEASE_VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Publishing $NEXT_RELEASE_VERSION as release"
  echo "$NEXT_RELEASE_VERSION" > app.version
  ./gradlew :database:publishGprPublicationToGitHubPackages
elif [[ $NEXT_RELEASE_VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+-.+$ ]]; then
  echo "Publishing $NEXT_RELEASE_VERSION as snapshot"
  echo "${NEXT_RELEASE_VERSION%-*}-SNAPSHOT" > app.version
  ./gradlew :database:publishGprPublicationToGitHubPackages
else
  echo "No release published"
fi