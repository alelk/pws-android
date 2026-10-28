#!/usr/bin/env bash
# Run Maestro E2E flows against a built APK on the connected device/emulator.
#   ./e2e/scripts/run-e2e.sh [--flavor NAME] [--full] [--clean] [--reinstall] [--retries N] [--apk PATH] [FLOW_NUMBER...]
#
# Which flows run, and in what order, comes from the `tags:` in each flow header - there is no list here:
#   default = every `smoke` flow
#   --full  = smoke, then `mutation`, then everything also tagged `device-state` (airplane mode, locale...) LAST
#   `diagnostic` flows are in no run; pick them by number:  ./run-e2e.sh 5
# Flows live in $FLOWS_DIR and in $FLOWS_DIR/<flavor>/ (flavor-only flows); numbers are unique across both.
#
# The harness proves it can fail before it is trusted: a canary flow must be RED (skipped when flows are
# picked by number), and a green flow only counts if the app still owns the screen afterwards.
# Per run it writes reports/<run>/run-info.txt - what exactly was tested (commit, APK SHA-256, device).
# Exit code != 0 when any flow failed. One JUnit XML per flow, debug output per flow.
set -euo pipefail
shopt -s nullglob

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
E2E_DIR="$ROOT_DIR/e2e"
CONFIG_FILE="$E2E_DIR/config/compose.env"
FLOWS_DIR="$E2E_DIR/flows/compose"
CANARY="$FLOWS_DIR/_harness/canary-must-fail.yaml"

# flavor -> applicationId. Must match productFlavors in the app's build.gradle.kts (keep in sync).
default_app_id() {
  case "$1" in
    free)   echo "com.example.app" ;;
    storeb) echo "com.example.app.b" ;;
    *)      echo "[ERROR] Unknown flavor: $1" >&2; return 1 ;;
  esac
}

FLAVOR="free"; FULL=false; CLEAN=false; REINSTALL=false; RETRIES=0; APK_OVERRIDE=""; SELECTED=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    --flavor)    FLAVOR="$2"; shift 2 ;;
    --full)      FULL=true; shift ;;
    --clean)     CLEAN=true; shift ;;
    --reinstall) REINSTALL=true; shift ;;
    --retries)   RETRIES="$2"; shift 2 ;;
    --apk)       APK_OVERRIDE="$2"; shift 2 ;;
    --*)         echo "[ERROR] Unknown option: $1" >&2; exit 1 ;;
    *)           SELECTED+=("$1"); shift ;;
  esac
done

command -v adb >/dev/null || { echo "[ERROR] adb not found" >&2; exit 1; }
command -v maestro >/dev/null || { echo "[ERROR] maestro not found" >&2; exit 1; }

# --- helpers (portable: GNU/BSD stat, sha256sum/shasum, awk without gawk extensions) -------------------
file_mtime() { stat -c %Y "$1" 2>/dev/null || stat -f %m "$1"; }
sha256_of()  { if command -v sha256sum >/dev/null; then sha256sum "$1"; else shasum -a 256 "$1"; fi | cut -d' ' -f1; }

# Tags of a flow = the `- word` items under `tags:` in its header (above the first `---`).
# Block-list form, bare words; trailing and comment-only lines are ignored. `tags: [smoke]` is NOT read - on
# purpose: a flow without a readable kind makes the runner stop, never run unplaced.
flow_tags() {
  awk '/^---[ \t]*$/ { exit }
       { sub(/#.*/, "") }
       /^[ \t]*$/ { next }
       /^tags:[ \t]*$/ { in_tags = 1; next }
       in_tags && /^[ \t]*-/ { gsub(/^[ \t]*-|[ \t"]/, ""); print; next }
       { in_tags = 0 }' "$1"
}
# (a here-string, not `flow_tags | grep -q`: grep -q exits early and pipefail would report SIGPIPE as "no tag")
has_tag() { local tags; tags="$(flow_tags "$1")"; grep -qx "$2" <<<"$tags"; }

# --- what is being tested -----------------------------------------------------------------------------
APP_ID="$(default_app_id "$FLAVOR")"
APP_VERSION="$(cat "$ROOT_DIR/app.version")"
APK_PATH="${APK_OVERRIDE:-$ROOT_DIR/output/app-release-${APP_VERSION}-${FLAVOR}.apk}"
[[ -f "$APK_PATH" ]] || { echo "[ERROR] APK not found: $APK_PATH" >&2; exit 1; }

# --- sort the flows by tag (fail before touching the device) -------------------------------------------
SMOKE=(); MUTATION=(); TOGGLERS=()
for f in "$FLOWS_DIR"/[0-9][0-9]-*.yaml "$FLOWS_DIR/$FLAVOR"/[0-9][0-9]-*.yaml; do
  kinds=()
  for k in smoke mutation diagnostic; do if has_tag "$f" "$k"; then kinds+=("$k"); fi; done
  (( ${#kinds[@]} == 1 )) || { echo "[ERROR] ${f#"$ROOT_DIR"/} needs exactly one kind tag (smoke | mutation | diagnostic) in its header" >&2; exit 1; }
  [[ "${kinds[0]}" == diagnostic ]] && continue
  if has_tag "$f" device-state; then TOGGLERS+=("$f")
  elif [[ "${kinds[0]}" == smoke ]]; then SMOKE+=("$f")
  else MUTATION+=("$f"); fi
done

FLOWS=()
if (( ${#SELECTED[@]} )); then
  for n in "${SELECTED[@]}"; do
    nn="$(printf '%02d' "$((10#$n))")"          # 10#: "08" is not an octal number
    match=("$FLOWS_DIR/$nn"-*.yaml "$FLOWS_DIR/$FLAVOR/$nn"-*.yaml)
    (( ${#match[@]} )) || { echo "[ERROR] no flow $n" >&2; exit 1; }
    (( ${#match[@]} == 1 )) || { echo "[ERROR] flow number $n is ambiguous: ${match[*]}" >&2; exit 1; }
    FLOWS+=("${match[0]}")
  done
else
  # ${arr[@]+"${arr[@]}"}: an empty array must not trip `set -u` on bash < 4.4 (macOS ships 3.2)
  for f in ${SMOKE[@]+"${SMOKE[@]}"}; do FLOWS+=("$f"); done
  if $FULL; then
    for f in ${MUTATION[@]+"${MUTATION[@]}"} ${TOGGLERS[@]+"${TOGGLERS[@]}"}; do FLOWS+=("$f"); done
  fi
fi
(( ${#FLOWS[@]} )) || { echo "[ERROR] no flows to run (no flow tagged smoke in $FLOWS_DIR?)" >&2; exit 1; }

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

# The record of what was tested - paste it into the release checklist. A green run of an APK that does not
# contain the code under test proves nothing, so a stale APK is a warning (not a failure: --apk and docs-only
# commits are legitimate).
GIT_HEAD="$(git -C "$ROOT_DIR" rev-parse --short HEAD 2>/dev/null || echo unknown)"
if [[ -n "$(git -C "$ROOT_DIR" status --porcelain 2>/dev/null)" ]]; then GIT_HEAD="$GIT_HEAD+dirty"; fi
{
  echo "date:         $(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "flavor:       $FLAVOR ($APP_ID)"
  echo "app.version:  $APP_VERSION"
  echo "git:          $GIT_HEAD"
  echo "apk:          $APK_PATH"
  echo "apk sha256:   $(sha256_of "$APK_PATH")"
  echo "maestro:      $(maestro --version 2>/dev/null | head -n1 || true)"
  echo "device:       $DEVICE_ID, $(adb -s "$DEVICE_ID" shell getprop ro.product.model | tr -d '\r'), API $(adb -s "$DEVICE_ID" shell getprop ro.build.version.sdk | tr -d '\r')"
} > "$REPORT_DIR/run-info.txt"
sed 's/^/[INFO] /' "$REPORT_DIR/run-info.txt"
if [[ -z "$APK_OVERRIDE" ]]; then
  HEAD_TS="$(git -C "$ROOT_DIR" log -1 --format=%ct 2>/dev/null || echo 0)"
  APK_TS="$(file_mtime "$APK_PATH")"
  if (( APK_TS < HEAD_TS )); then
    echo "[WARN] the APK is older than the HEAD commit - it may not contain the code under test. Rebuild, or pass --apk to test a build on purpose." >&2
  fi
fi

# Never uninstall silently: that wipes the app's data, which is exactly what an upgrade smoke must keep.
if ! adb -s "$DEVICE_ID" install -r "$APK_PATH"; then
  $REINSTALL || { echo "[ERROR] install -r failed (signature mismatch? downgrade?). --reinstall uninstalls first and WIPES the app data - never for an upgrade smoke." >&2; exit 1; }
  echo "[WARN] --reinstall: uninstalling $APP_ID, its data is wiped" >&2
  adb -s "$DEVICE_ID" uninstall "$APP_ID" >/dev/null 2>&1 || true
  adb -s "$DEVICE_ID" install "$APK_PATH"
fi
if $CLEAN; then adb -s "$DEVICE_ID" shell pm clear "$APP_ID" >/dev/null; fi

# Forward every KEY= of the env file, plus the values the script resolves itself.
MAESTRO_ENV=()
while IFS= read -r line; do
  [[ "$line" =~ ^([A-Z_][A-Z0-9_]*)= ]] || continue
  key="${BASH_REMATCH[1]}"
  MAESTRO_ENV+=(--env "$key=$(bash -c "source '$CONFIG_FILE'; printf %s \"\${$key:-}\"")")
done < "$CONFIG_FILE"
MAESTRO_ENV+=(--env "APP_ID=$APP_ID" --env "SCREENSHOTS_DIR=$SHOTS")

# The canary asserts an id that exists nowhere and MUST be red. A green canary means the runner would report
# green for anything, so nothing below could be believed. (It cannot tell *why* it failed; a dead device fails
# the real flows too.) Skipped when flows are picked by number - that is debugging, not a verdict.
if (( ${#SELECTED[@]} == 0 )); then
  [[ -f "$CANARY" ]] || { echo "[ERROR] canary flow missing: $CANARY" >&2; exit 1; }
  echo "[INFO] canary (must fail)"
  if maestro test "$CANARY" --udid "$DEVICE_ID" "${MAESTRO_ENV[@]}" >/dev/null 2>&1; then
    echo "[FATAL] the canary flow PASSED - this harness cannot fail, so a green run proves nothing" >&2
    exit 2
  fi
  echo "[OK] canary failed as expected"
fi

# A flow can go green while the app is gone (Maestro 2.5.1 `hideKeyboard` sent it to the launcher and the
# remaining assertNotVisible steps passed). After a green flow the app must still own the screen.
# The match is "<appId>/": com.example.app must not match com.example.app.b.
focus_lines() { adb -s "$DEVICE_ID" shell dumpsys window 2>/dev/null | tr -d '\r' | grep -E 'mCurrentFocus|mFocusedApp' || true; }

FAILED=()
for i in "${!FLOWS[@]}"; do
  flow="${FLOWS[$i]}"; name="$(basename "$flow" .yaml)"; ok=false
  for attempt in $(seq 0 "$RETRIES"); do
    echo "[INFO] [$((i+1))/${#FLOWS[@]}] $name (attempt $((attempt+1)))"
    if maestro test "$flow" --udid "$DEVICE_ID" --debug-output "$RUN_DIR/$name" \
         --format junit --output "$REPORT_DIR/junit-$name.xml" "${MAESTRO_ENV[@]}"; then
      focus="$(focus_lines)"
      if [[ "$focus" == *"$APP_ID/"* ]]; then ok=true; break; fi
      echo "[WARN] $name is green but the app is not in the foreground afterwards - counted as a failure. Focus: ${focus:-<none>}" >&2
    fi
  done
  if $ok; then echo "[PASS] $name"; else echo "[FAIL] $name"; FAILED+=("$name"); fi
done

echo "[INFO] reports: $REPORT_DIR  artifacts: $RUN_DIR"
(( ${#FAILED[@]} == 0 )) && { echo "[OK] ${#FLOWS[@]} flow(s) passed"; exit 0; }
echo "[FAIL] ${#FAILED[@]} failed:"; printf '  - %s\n' "${FAILED[@]}"; exit 1
