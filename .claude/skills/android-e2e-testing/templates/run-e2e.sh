#!/usr/bin/env bash
# Run Maestro E2E flows against a built APK on the connected device/emulator.
#   ./e2e/scripts/run-e2e.sh [--flavor NAME] [--full] [--clean] [--retries N] [--apk PATH] [FLOW_NUMBER...]
# Exit code != 0 when any flow failed. One JUnit XML per flow, debug output per flow.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
E2E_DIR="$ROOT_DIR/e2e"
CONFIG_FILE="$E2E_DIR/config/compose.env"
FLOWS_DIR="$E2E_DIR/flows/compose"

# flavor -> applicationId. Must match productFlavors in the app's build.gradle.kts (keep in sync).
default_app_id() {
  case "$1" in
    free)   echo "com.example.app" ;;
    storeb) echo "com.example.app.b" ;;
    *)      echo "[ERROR] Unknown flavor: $1" >&2; return 1 ;;
  esac
}

FLAVOR="free"; FULL=false; CLEAN=false; RETRIES=0; APK_OVERRIDE=""; SELECTED=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    --flavor)  FLAVOR="$2"; shift 2 ;;
    --full)    FULL=true; shift ;;
    --clean)   CLEAN=true; shift ;;
    --retries) RETRIES="$2"; shift 2 ;;
    --apk)     APK_OVERRIDE="$2"; shift 2 ;;
    --*)       echo "[ERROR] Unknown option: $1" >&2; exit 1 ;;
    *)         SELECTED+=("$1"); shift ;;
  esac
done

command -v adb >/dev/null || { echo "[ERROR] adb not found" >&2; exit 1; }
command -v maestro >/dev/null || { echo "[ERROR] maestro not found" >&2; exit 1; }

APP_ID="$(default_app_id "$FLAVOR")"
APP_VERSION="$(cat "$ROOT_DIR/app.version")"
APK_PATH="${APK_OVERRIDE:-$ROOT_DIR/output/app-release-${APP_VERSION}-${FLAVOR}.apk}"
[[ -f "$APK_PATH" ]] || { echo "[ERROR] APK not found: $APK_PATH" >&2; exit 1; }

DEVICE_ID="$(adb devices | awk 'NR>1 && $2=="device" {print $1; exit}')"
[[ -n "$DEVICE_ID" ]] || { echo "[ERROR] No online adb device" >&2; exit 1; }

# A flow that toggles airplane mode and fails midway must not leave the device offline.
restore_network() {
  adb -s "$DEVICE_ID" shell cmd connectivity airplane-mode disable >/dev/null 2>&1 \
    || adb -s "$DEVICE_ID" shell "settings put global airplane_mode_on 0; am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false" >/dev/null 2>&1 \
    || true
}
trap restore_network EXIT

RUN_ID="$(date +%Y%m%d-%H%M%S)-$FLAVOR"
RUN_DIR="$E2E_DIR/artifacts/$RUN_ID"; REPORT_DIR="$E2E_DIR/reports/$RUN_ID"; SHOTS="$E2E_DIR/screenshots"
mkdir -p "$RUN_DIR" "$REPORT_DIR" "$SHOTS"

adb -s "$DEVICE_ID" install -r "$APK_PATH" || { adb -s "$DEVICE_ID" uninstall "$APP_ID" >/dev/null 2>&1 || true; adb -s "$DEVICE_ID" install "$APK_PATH"; }
$CLEAN && adb -s "$DEVICE_ID" shell pm clear "$APP_ID" >/dev/null

# Forward every KEY= of the env file, plus the values the script resolves itself.
MAESTRO_ENV=()
while IFS= read -r line; do
  [[ "$line" =~ ^([A-Z_][A-Z0-9_]*)= ]] || continue
  key="${BASH_REMATCH[1]}"
  MAESTRO_ENV+=(--env "$key=$(bash -c "source '$CONFIG_FILE'; printf %s \"\${$key:-}\"")")
done < "$CONFIG_FILE"
MAESTRO_ENV+=(--env "APP_ID=$APP_ID" --env "SCREENSHOTS_DIR=$SHOTS")

# Order matters: smoke (read-only) first, mutation next, device-state togglers LAST.
SMOKE=(01-app-launch.yaml 02-search-basic.yaml 08-navigation-tabs.yaml)
MUTATION=(04-favorites-add-verify.yaml)
TOGGLERS=(19-onboarding-offline-skip.yaml)

FLOWS=()
if (( ${#SELECTED[@]} )); then
  for n in "${SELECTED[@]}"; do
    f=("$FLOWS_DIR/$(printf '%02d' "$n")"-*.yaml); [[ -f "${f[0]}" ]] || { echo "[ERROR] no flow $n" >&2; exit 1; }
    FLOWS+=("${f[0]}")
  done
else
  for f in "${SMOKE[@]}"; do FLOWS+=("$FLOWS_DIR/$f"); done
  if $FULL; then for f in "${MUTATION[@]}" "${TOGGLERS[@]}"; do FLOWS+=("$FLOWS_DIR/$f"); done; fi
fi

FAILED=()
for i in "${!FLOWS[@]}"; do
  flow="${FLOWS[$i]}"; name="$(basename "$flow" .yaml)"; ok=false
  for attempt in $(seq 0 "$RETRIES"); do
    echo "[INFO] [$((i+1))/${#FLOWS[@]}] $name (attempt $((attempt+1)))"
    if maestro test "$flow" --udid "$DEVICE_ID" --debug-output "$RUN_DIR/$name" \
         --format junit --output "$REPORT_DIR/junit-$name.xml" "${MAESTRO_ENV[@]}"; then ok=true; break; fi
  done
  $ok && echo "[PASS] $name" || { echo "[FAIL] $name"; FAILED+=("$name"); }
done

echo "[INFO] reports: $REPORT_DIR  artifacts: $RUN_DIR"
(( ${#FAILED[@]} == 0 )) && { echo "[OK] ${#FLOWS[@]} flow(s) passed"; exit 0; }
echo "[FAIL] ${#FAILED[@]} failed:"; printf '  - %s\n' "${FAILED[@]}"; exit 1
