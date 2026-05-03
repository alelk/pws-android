#!/usr/bin/env bash
# Run Maestro E2E tests for app-compose against a local APK.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
E2E_DIR="$ROOT_DIR/e2e"
CONFIG_FILE="$E2E_DIR/config/compose.env"
ARTIFACTS_DIR="$E2E_DIR/artifacts"

usage() {
  cat <<'EOF'
Run Maestro compose E2E flows against a local APK.

Usage:
  ./e2e/scripts/run-compose.sh [--apk /abs/path.apk] [--flow flows/compose/FILE.yaml] [--full] [--dry-run] [-h]

Options:
  --apk      Override APK path from compose.env
  --flow     Run a single flow (path relative to e2e/)
  --full     Run full suite including mutating tests
  --dry-run  Print resolved config and exit
  -h         Show this help
EOF
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 && return
  echo "[ERROR] Required command not found: $1" >&2
  exit 1
}

now_ts() { date +"%Y%m%d-%H%M%S"; }

APK_OVERRIDE=""
FLOW_OVERRIDE=""
USE_FULL="false"
DRY_RUN="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --apk)     APK_OVERRIDE="${2:-}"; shift 2 ;;
    --flow)    FLOW_OVERRIDE="${2:-}"; shift 2 ;;
    --full)    USE_FULL="true"; shift ;;
    --dry-run) DRY_RUN="true"; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "[ERROR] Unknown argument: $1" >&2; usage; exit 1 ;;
  esac
done

[[ -f "$CONFIG_FILE" ]] || { echo "[ERROR] Missing config: $CONFIG_FILE" >&2; exit 1; }
# shellcheck source=/dev/null
source "$CONFIG_FILE"

APP_VERSION="$(cat "$ROOT_DIR/app.version" 2>/dev/null || true)"
APK_PATH="${APK_OVERRIDE:-${APK_PATH:-$ROOT_DIR/output/compose/pws-app-release-${APP_VERSION}-ru.apk}}"
APP_ID="${APP_ID:-}"

[[ -z "$APK_PATH" ]] && { echo "[ERROR] APK_PATH not set" >&2; exit 1; }
[[ -z "$APP_ID" ]]   && { echo "[ERROR] APP_ID not set" >&2; exit 1; }
[[ -f "$APK_PATH" ]] || { echo "[ERROR] APK not found: $APK_PATH" >&2; exit 1; }

if [[ "$DRY_RUN" == "true" ]]; then
  echo "[DRY-RUN] APK_PATH=$APK_PATH"
  echo "[DRY-RUN] APP_ID=$APP_ID"
  echo "[DRY-RUN] FLOW=${FLOW_OVERRIDE:-<smoke suite>}"
  echo "[DRY-RUN] USE_FULL=$USE_FULL"
  exit 0
fi

require_cmd adb
require_cmd maestro

DEVICE_ID="$(adb devices | awk 'NR>1 && $2=="device" {print $1; exit}')"
[[ -z "$DEVICE_ID" ]] && { echo "[ERROR] No online adb device. Start emulator first." >&2; exit 1; }
echo "[INFO] Device: $DEVICE_ID"

mkdir -p "$ARTIFACTS_DIR"
RUN_ID="$(now_ts)-compose"
RUN_DIR="$ARTIFACTS_DIR/$RUN_ID"
mkdir -p "$RUN_DIR"

echo "[INFO] Installing APK: $APK_PATH"
adb install -r "$APK_PATH" || {
  echo "[WARN] Install failed, uninstalling first..."
  adb uninstall "$APP_ID" >/dev/null 2>&1 || true
  adb install "$APK_PATH"
}

# ---------------------------------------------------------------------------
# Env vars forwarded to every flow
# ---------------------------------------------------------------------------
MAESTRO_ENV=(
  --env APP_ID="$APP_ID"
  --env NAV_HOME="${NAV_HOME}"
  --env NAV_SEARCH="${NAV_SEARCH}"
  --env NAV_BOOKS="${NAV_BOOKS}"
  --env NAV_TAGS="${NAV_TAGS}"
  --env NAV_FAVORITES="${NAV_FAVORITES}"
  --env NAV_HISTORY="${NAV_HISTORY}"
  --env ACTION_TOGGLE_FAVORITE="${ACTION_TOGGLE_FAVORITE}"
  --env ACTION_MORE="${ACTION_MORE}"
  --env ACTION_EDIT_SONG="${ACTION_EDIT_SONG}"
  --env ACTION_EDIT_TAGS="${ACTION_EDIT_TAGS}"
  --env ACTION_SAVE="${ACTION_SAVE}"
  --env ACTION_ADD_TAG="${ACTION_ADD_TAG}"
  --env ACTION_SAVE_TAG="${ACTION_SAVE_TAG}"
  --env ACTION_SAVE_SONG_TAGS="${ACTION_SAVE_SONG_TAGS}"
  --env FIELD_SEARCH="${FIELD_SEARCH}"
  --env FIELD_TAG_NAME="${FIELD_TAG_NAME}"
  --env FIELD_SONG_EDIT_TITLE="${FIELD_SONG_EDIT_TITLE}"
  --env SEARCH_QUERY_NUMERIC="${SEARCH_QUERY_NUMERIC}"
  --env SEARCH_QUERY_UNIQUE="${SEARCH_QUERY_UNIQUE}"
  --env SONG_TITLE_UNIQUE="${SONG_TITLE_UNIQUE}"
  --env SONG_ITEM_ONE="${SONG_ITEM_ONE}"
  --env SONG_LIST_ITEM_PREFIX="${SONG_LIST_ITEM_PREFIX}"
  --env RECENT_ITEM_PREFIX="${RECENT_ITEM_PREFIX}"
  --env TEST_TAG_NAME="${TEST_TAG_NAME}"
  --env TEST_SONG_EDIT_TITLE="${TEST_SONG_EDIT_TITLE}"
)

# ---------------------------------------------------------------------------
# Flow lists
# ---------------------------------------------------------------------------
SMOKE_FLOWS=(
  01-app-launch.yaml
  02-search-basic.yaml
  03-song-detail-actions.yaml
  08-navigation-tabs.yaml
  09-books-to-song.yaml
)
FULL_FLOWS=(
  01-app-launch.yaml
  02-search-basic.yaml
  03-song-detail-actions.yaml
  04-favorites-add-verify.yaml
  06-tags-create-assign.yaml
  07-song-edit-title.yaml
  08-navigation-tabs.yaml
  09-books-to-song.yaml
)

if [[ -n "$FLOW_OVERRIDE" ]]; then
  FLOWS_TO_RUN=("$E2E_DIR/$FLOW_OVERRIDE")
elif [[ "$USE_FULL" == "true" ]]; then
  FLOWS_TO_RUN=("${FULL_FLOWS[@]/#/$E2E_DIR/flows/compose/}")
else
  FLOWS_TO_RUN=("${SMOKE_FLOWS[@]/#/$E2E_DIR/flows/compose/}")
fi

# ---------------------------------------------------------------------------
# Run a single flow
# ---------------------------------------------------------------------------
run_flow() {
  local flow="$1"
  local name; name="$(basename "$flow" .yaml)"
  local debug="$RUN_DIR/$name"
  mkdir -p "$debug"

  echo "[INFO] Running: $name"
  if (cd "$debug" && maestro test "$flow" \
      --udid "$DEVICE_ID" \
      --debug-output "$debug" \
      "${MAESTRO_ENV[@]}"); then
    echo "[PASS] $name"
    return 0
  fi

  echo "[FAIL] $name"
  return 1
}
# ---------------------------------------------------------------------------
# Main loop
# ---------------------------------------------------------------------------
FAILED=()
for flow in "${FLOWS_TO_RUN[@]}"; do
  run_flow "$flow" || FAILED+=("$(basename "$flow")")
done
echo ""
if [[ ${#FAILED[@]} -eq 0 ]]; then
  echo "[OK] All flows passed"
  echo "[OK] Artifacts: $RUN_DIR"
else
  echo "[FAIL] ${#FAILED[@]} flow(s) failed:"
  printf '  - %s\n' "${FAILED[@]}"
  echo "[INFO] Artifacts: $RUN_DIR"
  exit 1
fi
