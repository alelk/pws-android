#!/bin/bash

NEXT_RELEASE_VERSION=$(cat .nextRelease.txt)
export NEXT_RELEASE_VERSION

if [[ $NEXT_RELEASE_VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+ ]]; then
  echo "${NEXT_RELEASE_VERSION%%-*}" > app.version
else
  echo "No release published"
fi