#!/usr/bin/env bash
# Run Maestro E2E tests for app-compose against a local APK.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
E2E_DIR="$ROOT_DIR/e2e"
CONFIG_FILE="$E2E_DIR/config/compose.env"
ARTIFACTS_DIR="$E2E_DIR/artifacts"
REPORTS_DIR="$E2E_DIR/reports"

usage() {
  cat <<'EOF'
Run Maestro compose E2E flows against a local APK.

Usage:
  ./e2e/scripts/run-compose.sh [options] [FLOW...]

Positional FLOW arguments select specific flows by number or filename:
  2                # → flows/compose/02-search-basic.yaml
  4 5 6            # → flows 04, 05, 06 (in order)
  02-search-basic  # → flows/compose/02-search-basic.yaml (basename without .yaml)

When no positional args are given, runs the smoke suite (or full suite with --full).

Options:
  --apk PATH       Override APK path from compose.env
  --flow PATH      [legacy] run a single flow (path relative to e2e/)
  --full           Run full suite including mutating tests
  --clean          adb pm clear $APP_ID before the first flow (recommended for CI / full suite)
  --retries N      Retry each failed flow up to N times (default: 1)
  --dry-run        Print resolved config and exit
  -h, --help       Show this help

Examples:
  ./e2e/scripts/run-compose.sh                       # smoke suite, install -r, no pm clear
  ./e2e/scripts/run-compose.sh --full --clean        # full suite from a clean app state (CI)
  ./e2e/scripts/run-compose.sh 2                     # single flow by number
  ./e2e/scripts/run-compose.sh 4 5 6 --clean         # several flows by number, with pm clear
  ./e2e/scripts/run-compose.sh 14 --retries 2        # one flow, retry on flake
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
CLEAN_STATE="false"
RETRIES="1"
SELECTED_FLOWS=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    --apk)     APK_OVERRIDE="${2:-}"; shift 2 ;;
    --flow)    FLOW_OVERRIDE="${2:-}"; shift 2 ;;
    --full)    USE_FULL="true"; shift ;;
    --clean)   CLEAN_STATE="true"; shift ;;
    --retries) RETRIES="${2:-1}"; shift 2 ;;
    --dry-run) DRY_RUN="true"; shift ;;
    -h|--help) usage; exit 0 ;;
    --*) echo "[ERROR] Unknown option: $1" >&2; usage; exit 1 ;;
    *)         SELECTED_FLOWS+=("$1"); shift ;;
  esac
done

# Resolve a positional selector (number or basename) to a flow path.
# Number: "2" → "02-*.yaml" (zero-padded prefix, glob match).
# Name:   "02-search-basic" → "02-search-basic.yaml".
resolve_flow() {
  local sel="$1"
  local flows_dir="$E2E_DIR/flows/compose"

  if [[ "$sel" =~ ^[0-9]+$ ]]; then
    local padded; padded="$(printf '%02d' "$sel")"
    local matches=("$flows_dir"/"$padded"-*.yaml)
    if [[ ! -f "${matches[0]}" ]]; then
      echo "[ERROR] No flow matches number $sel (looking for $padded-*.yaml)" >&2
      return 1
    fi
    if (( ${#matches[@]} > 1 )); then
      echo "[ERROR] Ambiguous selector $sel — multiple matches:" >&2
      printf '  - %s\n' "${matches[@]}" >&2
      return 1
    fi
    echo "${matches[0]}"
    return 0
  fi

  local path="$flows_dir/${sel%.yaml}.yaml"
  if [[ ! -f "$path" ]]; then
    echo "[ERROR] No such flow: $path" >&2
    return 1
  fi
  echo "$path"
}

[[ -f "$CONFIG_FILE" ]] || { echo "[ERROR] Missing config: $CONFIG_FILE" >&2; exit 1; }
# shellcheck source=/dev/null
source "$CONFIG_FILE"

APP_VERSION="$(cat "$ROOT_DIR/app.version" 2>/dev/null || true)"
APK_PATH="${APK_OVERRIDE:-${APK_PATH:-$ROOT_DIR/output/compose/pws-app-release-${APP_VERSION}-ru.apk}}"
APP_ID="${APP_ID:-}"

[[ -z "$APK_PATH" ]] && { echo "[ERROR] APK_PATH not set" >&2; exit 1; }
[[ -z "$APP_ID" ]]   && { echo "[ERROR] APP_ID not set" >&2; exit 1; }

if [[ "$DRY_RUN" == "true" ]]; then
  echo "[DRY-RUN] APK_PATH=$APK_PATH"
  echo "[DRY-RUN] APP_ID=$APP_ID"
  echo "[DRY-RUN] USE_FULL=$USE_FULL"
  echo "[DRY-RUN] CLEAN_STATE=$CLEAN_STATE"
  echo "[DRY-RUN] RETRIES=$RETRIES"
  if (( ${#SELECTED_FLOWS[@]} > 0 )); then
    echo "[DRY-RUN] SELECTED:"
    for sel in "${SELECTED_FLOWS[@]}"; do
      if path="$(resolve_flow "$sel" 2>&1)"; then
        echo "  - $sel → $path"
      else
        echo "  - $sel → ERROR: $path"
      fi
    done
  elif [[ -n "$FLOW_OVERRIDE" ]]; then
    echo "[DRY-RUN] FLOW=$FLOW_OVERRIDE"
  elif [[ "$USE_FULL" == "true" ]]; then
    echo "[DRY-RUN] FLOW=<full suite>"
  else
    echo "[DRY-RUN] FLOW=<smoke suite>"
  fi
  exit 0
fi

[[ -f "$APK_PATH" ]] || { echo "[ERROR] APK not found: $APK_PATH" >&2; exit 1; }

require_cmd adb
require_cmd maestro

DEVICE_ID="$(adb devices | awk 'NR>1 && $2=="device" {print $1; exit}')"
[[ -z "$DEVICE_ID" ]] && { echo "[ERROR] No online adb device. Start emulator first." >&2; exit 1; }
echo "[INFO] Device: $DEVICE_ID"

mkdir -p "$ARTIFACTS_DIR" "$REPORTS_DIR"
RUN_ID="$(now_ts)-compose"
RUN_DIR="$ARTIFACTS_DIR/$RUN_ID"
REPORT_DIR="$REPORTS_DIR/$RUN_ID"
mkdir -p "$RUN_DIR" "$REPORT_DIR"

echo "[INFO] Installing APK: $APK_PATH"
adb install -r "$APK_PATH" || {
  echo "[WARN] Install failed, uninstalling first..."
  adb uninstall "$APP_ID" >/dev/null 2>&1 || true
  adb install "$APK_PATH"
}

if [[ "$CLEAN_STATE" == "true" ]]; then
  echo "[INFO] Clearing app data: $APP_ID"
  adb shell pm clear "$APP_ID" >/dev/null
fi

# ---------------------------------------------------------------------------
# Env vars forwarded to every flow — built from compose.env keys.
# Only keys matching ^[A-Z_][A-Z0-9_]*= are forwarded; everything else (blank
# lines, comments, malformed entries) is ignored.
# ---------------------------------------------------------------------------
MAESTRO_ENV=()
while IFS= read -r line; do
  [[ "$line" =~ ^[[:space:]]*# ]] && continue
  [[ "$line" =~ ^[[:space:]]*$ ]] && continue
  [[ "$line" =~ ^([A-Z_][A-Z0-9_]*)= ]] || continue
  key="${BASH_REMATCH[1]}"
  val="${!key:-}"
  MAESTRO_ENV+=(--env "$key=$val")
done < "$CONFIG_FILE"

# ---------------------------------------------------------------------------
# Flow lists
# ---------------------------------------------------------------------------
SMOKE_FLOWS=(
  01-app-launch.yaml
  02-search-basic.yaml
  03-song-detail-actions.yaml
  08-navigation-tabs.yaml
  09-books-to-song.yaml
  10-settings-open.yaml
  11-home-number-search.yaml
  15-search-empty-results.yaml
  16-home-recently-viewed.yaml
  18-home-search-suggestions.yaml
)
FULL_FLOWS=(
  "${SMOKE_FLOWS[@]}"
  # Mutation flows — all use clearState:true for isolation.
  04-favorites-add-verify.yaml
  05-history-after-open.yaml
  06-tags-create-assign.yaml
  07-song-edit-title.yaml
  12-favorites-remove.yaml
  13-tag-to-songs.yaml
  14-history-clear-all.yaml
)

FLOWS_TO_RUN=()
if (( ${#SELECTED_FLOWS[@]} > 0 )); then
  for sel in "${SELECTED_FLOWS[@]}"; do
    resolved="$(resolve_flow "$sel")" || exit 1
    FLOWS_TO_RUN+=("$resolved")
  done
elif [[ -n "$FLOW_OVERRIDE" ]]; then
  FLOWS_TO_RUN=("$E2E_DIR/$FLOW_OVERRIDE")
elif [[ "$USE_FULL" == "true" ]]; then
  FLOWS_TO_RUN=("${FULL_FLOWS[@]/#/$E2E_DIR/flows/compose/}")
else
  FLOWS_TO_RUN=("${SMOKE_FLOWS[@]/#/$E2E_DIR/flows/compose/}")
fi

# ---------------------------------------------------------------------------
# Run a single flow with up to $RETRIES retries.
# Emits one JUnit XML per flow into $REPORT_DIR.
# ---------------------------------------------------------------------------
run_flow() {
  local idx="$1"
  local total="$2"
  local flow="$3"
  local name; name="$(basename "$flow" .yaml)"
  local debug="$RUN_DIR/$name"
  local junit="$REPORT_DIR/junit-$name.xml"
  mkdir -p "$debug"

  local attempt=1
  local max_attempts=$((RETRIES + 1))
  while (( attempt <= max_attempts )); do
    if (( attempt == 1 )); then
      echo "[INFO] [$idx/$total] Running: $name"
    else
      echo "[INFO] [$idx/$total] Retry $((attempt - 1))/$RETRIES: $name"
    fi

    if (cd "$debug" && maestro test "$flow" \
        --udid "$DEVICE_ID" \
        --debug-output "$debug" \
        --format junit \
        --output "$junit" \
        "${MAESTRO_ENV[@]}"); then
      echo "[PASS] [$idx/$total] $name"
      return 0
    fi
    attempt=$((attempt + 1))
  done

  echo "[FAIL] [$idx/$total] $name (after $RETRIES retries)"
  return 1
}

# ---------------------------------------------------------------------------
# Main loop
# ---------------------------------------------------------------------------
TOTAL=${#FLOWS_TO_RUN[@]}
FAILED=()
PASSED=0
for i in "${!FLOWS_TO_RUN[@]}"; do
  flow="${FLOWS_TO_RUN[$i]}"
  idx=$((i + 1))
  if run_flow "$idx" "$TOTAL" "$flow"; then
    PASSED=$((PASSED + 1))
  else
    FAILED+=("$(basename "$flow")")
  fi
done

echo ""
echo "[INFO] JUnit reports: $REPORT_DIR"
echo "[INFO] Artifacts:     $RUN_DIR"

if [[ ${#FAILED[@]} -eq 0 ]]; then
  echo "[OK] All $TOTAL flow(s) passed"
else
  echo "[FAIL] $PASSED/$TOTAL passed, ${#FAILED[@]} failed:"
  printf '  - %s\n' "${FAILED[@]}"
  exit 1
fi
