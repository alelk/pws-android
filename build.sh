#!/bin/bash

APP_VERSION=$(cat app.version)
DEST_DIR="output"
DEST_PACKAGE_RU="$DEST_DIR/pws-app-release-$APP_VERSION-ru"
DEST_PACKAGE_UK="$DEST_DIR/pws-app-release-$APP_VERSION-uk"

./gradlew clean
rm -rf ./.gradle/buildOutputCleanup
rm -rf ./.gradle/configuration-cache

./gradlew bundleRuRelease bundleUkRelease assembleRuRelease assembleUkRelease

mkdir $DEST_DIR
cp app/build/outputs/bundle/ruRelease/app-ru-release.aab "$DEST_PACKAGE_RU.aab"
cp app/build/outputs/apk/ru/release/app-ru-release.apk "$DEST_PACKAGE_RU.apk"
cp app/build/outputs/bundle/ukRelease/app-uk-release.aab "$DEST_PACKAGE_UK.aab"
cp app/build/outputs/apk/uk/release/app-uk-release.apk "$DEST_PACKAGE_UK.apk"