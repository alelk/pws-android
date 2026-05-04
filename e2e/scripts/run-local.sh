#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
E2E_DIR="$ROOT_DIR/e2e"
CONFIG_FILE="$E2E_DIR/config/local-apk.env"
REPORTS_DIR="$E2E_DIR/reports"
ARTIFACTS_DIR="$E2E_DIR/artifacts"
FLOW_FILE="$E2E_DIR/flows/smoke/suite.yaml"

usage() {
  cat <<'EOF'
Run local black-box smoke tests for a ready APK.

Usage:
  ./e2e/scripts/run-local.sh [--apk /abs/path/app.apk] [--flavor ru] [--dry-run]

Options:
  --apk      Override APK path from e2e/config/local-apk.env
  --flavor   Logical flavor label for report naming (default: ru)
  --dry-run  Print resolved configuration and exit
  -h, --help Show this help
EOF
}

require_cmd() {
  local cmd="$1"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "[ERROR] Required command is missing: $cmd" >&2
    exit 1
  fi
}

now_ts() {
  date +"%Y%m%d-%H%M%S"
}

APK_OVERRIDE=""
FLAVOR="ru"
DRY_RUN="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --apk)
      APK_OVERRIDE="${2:-}"
      shift 2
      ;;
    --flavor)
      FLAVOR="${2:-}"
      shift 2
      ;;
    --dry-run)
      DRY_RUN="true"
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "[ERROR] Unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "[ERROR] Missing config file: $CONFIG_FILE" >&2
  exit 1
fi

# shellcheck source=/dev/null
source "$CONFIG_FILE"

APK_PATH="${APK_OVERRIDE:-${APK_PATH:-}}"
APP_ID="${APP_ID:-}"
HOME_READY_TEXT="${HOME_READY_TEXT:-}"
HOME_ERROR_TEXT="${HOME_ERROR_TEXT:-}"
SEARCH_TAB_TEXT="${SEARCH_TAB_TEXT:-}"
SEARCH_PLACEHOLDER_TEXT="${SEARCH_PLACEHOLDER_TEXT:-}"
SEARCH_QUERY="${SEARCH_QUERY:-}"
SEARCH_EMPTY_TEXT="${SEARCH_EMPTY_TEXT:-}"
SEARCH_ERROR_TEXT="${SEARCH_ERROR_TEXT:-}"

if [[ -z "$APK_PATH" || -z "$APP_ID" ]]; then
  echo "[ERROR] APK_PATH and APP_ID must be set in $CONFIG_FILE" >&2
  exit 1
fi

if [[ ! -f "$APK_PATH" ]]; then
  echo "[ERROR] APK not found: $APK_PATH" >&2
  exit 1
fi

if [[ "$DRY_RUN" == "true" ]]; then
  echo "[DRY-RUN] ROOT_DIR=$ROOT_DIR"
  echo "[DRY-RUN] FLAVOR=$FLAVOR"
  echo "[DRY-RUN] APK_PATH=$APK_PATH"
  echo "[DRY-RUN] APP_ID=$APP_ID"
  echo "[DRY-RUN] FLOW_FILE=$FLOW_FILE"
  exit 0
fi

require_cmd adb
require_cmd maestro

if ! adb get-state >/dev/null 2>&1; then
  echo "[ERROR] No adb device/emulator is ready. Start Android emulator first." >&2
  exit 1
fi

mkdir -p "$REPORTS_DIR" "$ARTIFACTS_DIR"

RUN_ID="$(now_ts)-$FLAVOR"
RUN_DIR="$ARTIFACTS_DIR/$RUN_ID"
JUNIT_FILE="$REPORTS_DIR/smoke-$RUN_ID.xml"
mkdir -p "$RUN_DIR"

echo "[INFO] Installing APK: $APK_PATH"
adb install -r "$APK_PATH" >/dev/null

echo "[INFO] Running smoke flow: $FLOW_FILE"
maestro test "$FLOW_FILE" \
  --format junit \
  --output "$JUNIT_FILE" \
  --debug-output "$RUN_DIR" \
  --env APP_ID="$APP_ID" \
  --env HOME_READY_TEXT="$HOME_READY_TEXT" \
  --env HOME_ERROR_TEXT="$HOME_ERROR_TEXT" \
  --env SEARCH_TAB_TEXT="$SEARCH_TAB_TEXT" \
  --env SEARCH_PLACEHOLDER_TEXT="$SEARCH_PLACEHOLDER_TEXT" \
  --env SEARCH_QUERY="$SEARCH_QUERY" \
  --env SEARCH_EMPTY_TEXT="$SEARCH_EMPTY_TEXT" \
  --env SEARCH_ERROR_TEXT="$SEARCH_ERROR_TEXT"

echo "[OK] Smoke tests finished"
echo "[OK] JUnit report: $JUNIT_FILE"
echo "[OK] Artifacts: $RUN_DIR"

