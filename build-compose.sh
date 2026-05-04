#!/bin/bash
set -euo pipefail

APP_VERSION=$(cat app.version)
DEST_DIR="output/compose"

DEST_RU="$DEST_DIR/pws-app-release-$APP_VERSION-ru"
DEST_UK="$DEST_DIR/pws-app-release-$APP_VERSION-uk"
DEST_FULL="$DEST_DIR/pws-app-release-$APP_VERSION-full"
DEST_RUSTORE="$DEST_DIR/pws-app-release-$APP_VERSION-rustore"

./gradlew clean
rm -rf ./.gradle/buildOutputCleanup
rm -rf ./.gradle/configuration-cache

./gradlew :data:db-android:testRuDebugUnitTest :app-compose:check \
  :app-compose:bundleRuRelease \
  :app-compose:bundleUkRelease \
  :app-compose:bundleFullRelease \
  :app-compose:bundleRustoreRelease \
  :app-compose:assembleRuRelease \
  :app-compose:assembleUkRelease \
  :app-compose:assembleFullRelease \
  :app-compose:assembleRustoreRelease

mkdir -p "$DEST_DIR"

# ru
cp app-compose/build/outputs/bundle/ruRelease/app-compose-ru-release.aab   "$DEST_RU.aab"
cp app-compose/build/outputs/apk/ru/release/app-compose-ru-release.apk     "$DEST_RU.apk"

# uk
cp app-compose/build/outputs/bundle/ukRelease/app-compose-uk-release.aab   "$DEST_UK.aab"
cp app-compose/build/outputs/apk/uk/release/app-compose-uk-release.apk     "$DEST_UK.apk"

# full
#cp app-compose/build/outputs/bundle/fullRelease/app-compose-full-release.aab   "$DEST_FULL.aab"
#cp app-compose/build/outputs/apk/full/release/app-compose-full-release.apk     "$DEST_FULL.apk"

# rustore
#cp app-compose/build/outputs/bundle/rustoreRelease/app-compose-rustore-release.aab   "$DEST_RUSTORE.aab"
#cp app-compose/build/outputs/apk/rustore/release/app-compose-rustore-release.apk     "$DEST_RUSTORE.apk"

echo ""
echo "Build complete: $APP_VERSION"
ls -lh "$DEST_DIR"/pws-app-release-"$APP_VERSION"-*.apk

