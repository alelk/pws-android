#!/usr/bin/env bash
# Puts a RuStore-fork entitlement file (files/datastore/pws-app-preferences.preferences_pb) into the
# installed rustore app, exactly where the fork 2.1.1–2.3.1 kept it — for e2e flows
# (flows/compose/rustore/) and the manual upgrade test (tools/rustore-upgrade-test.md).
#
# Usage:
#   e2e/scripts/rustore-entitlement.sh <kind> [--app-id ID]
#     kind: none | lifetime | lifetime_false_sub | sub_future | sub_past | sub_and_full | bad_date |
#           corrupted | show
#   Dates are relative to today (future = +30 days, past = -30 days).
#   `show` only prints the current file's SHA-256 (compare before/after an upgrade: it must not change).
#
# Needs `run-as`, i.e. a debuggable build: rustoreDebug (signed with the RuStore key when the
# android.release.*Rustore properties are set, so it installs over the published fork) or the
# fork's own debug build.
set -euo pipefail

KIND="${1:?kind required: none|lifetime|lifetime_false_sub|sub_future|sub_past|sub_and_full|bad_date|corrupted|show}"
shift
APP_ID="io.github.alelk.pws.app"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --app-id) APP_ID="$2"; shift 2 ;;
    *) echo "unknown option: $1" >&2; exit 2 ;;
  esac
done

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
REMOTE="files/datastore/pws-app-preferences.preferences_pb"

show() {
  adb exec-out run-as "$APP_ID" sh -c "cat $REMOTE 2>/dev/null" | sha256sum | cut -d' ' -f1
}

if [[ "$KIND" == "show" ]]; then
  show
  exit 0
fi

TMP="$(mktemp)"
trap 'rm -f "$TMP"' EXIT
python3 "$ROOT/tools/make-legacy-entitlement-fixtures.py" --single "$KIND" --out "$TMP"

# The app must not be running: DataStore caches the file in memory.
adb shell am force-stop "$APP_ID"
adb exec-in run-as "$APP_ID" sh -c "mkdir -p files/datastore && cat > $REMOTE" < "$TMP"
echo "entitlement '$KIND' installed for $APP_ID; sha256=$(show)"
