#!/usr/bin/env bash
# fetch-db.sh — downloads pre-built encrypted DB assets from the alelk/pws-docs release.
#
# Files on GitHub Releases are now single encrypted files (*.dbz.enc), not split chunks.
# Debug variants (*-debug*.dbz.enc) use the public debug key (embedded in code).
# Release variants use the prod key (stored in GitHub Secrets / local.properties).
#
# Usage:
#   ./fetch-db.sh                        # all variants
#   ./fetch-db.sh ruRelease              # specific variant(s)
#   ./fetch-db.sh ruRelease ukRelease
#
# Variants: ruRelease ruDebug ukRelease ukDebug rustoreRelease rustoreDebug fullRelease fullDebug
#
# Auth (in priority order):
#   1. GH_TOKEN env var  — PAT with 'Contents: Read' on alelk/pws-docs (required in CI)
#   2. gh CLI stored credentials — for local dev

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GH_REPO="alelk/pws-docs"

# ── version ───────────────────────────────────────────────────────────────────
DB_VERSION="$(tr -d '[:space:]' < "${SCRIPT_DIR}/db.version")"
echo "DB version: ${DB_VERSION}"

# ── auth check ────────────────────────────────────────────────────────────────
if ! command -v gh &>/dev/null; then
  echo "ERROR: gh CLI not found. Install: https://cli.github.com" >&2; exit 1
fi
if [[ -z "${GH_TOKEN:-}" ]] && ! gh auth status &>/dev/null 2>&1; then
  echo "ERROR: No GitHub auth. Run 'gh auth login' or set GH_TOKEN." >&2; exit 1
fi

# ── variant maps ──────────────────────────────────────────────────────────────
# Returns the release asset filename (.dbz.enc — single encrypted compressed file)
# rustoreRelease has its own dedicated file on GitHub Releases.
# rustoreDebug falls back to pws-ru-debug (no separate rustore-debug asset yet).
variant_asset() {
  case "$1" in
    ruRelease|fullRelease) echo "pws-ru-${DB_VERSION}.dbz.enc" ;;
    ruDebug|fullDebug)     echo "pws-ru-debug-${DB_VERSION}.dbz.enc" ;;
    ukRelease)             echo "pws-uk-${DB_VERSION}.dbz.enc" ;;
    ukDebug)               echo "pws-uk-debug-${DB_VERSION}.dbz.enc" ;;
    rustoreRelease)        echo "pws-rustore-${DB_VERSION}.dbz.enc" ;;
    rustoreDebug)          echo "pws-ru-debug-${DB_VERSION}.dbz.enc" ;;
    ruLocalSeed)           echo "pws-ru-test-${DB_VERSION}.dbz" ;;
    *) return 1 ;;
  esac
}

variant_dir() {
  case "$1" in
    ruRelease)      echo "app-compose/src/ruRelease/assets/db" ;;
    ruDebug)        echo "app-compose/src/ruDebug/assets/db" ;;
    ukRelease)      echo "app-compose/src/ukRelease/assets/db" ;;
    ukDebug)        echo "app-compose/src/ukDebug/assets/db" ;;
    rustoreRelease) echo "app-compose/src/rustoreRelease/assets/db" ;;
    rustoreDebug)   echo "app-compose/src/rustoreDebug/assets/db" ;;
    fullRelease)    echo "app-compose/src/fullRelease/assets/db" ;;
    fullDebug)      echo "app-compose/src/fullDebug/assets/db" ;;
    ruLocalSeed)    echo "app-compose/src/ruLocalSeed/assets/db" ;;
    *) return 1 ;;
  esac
}

# ── variants to process ───────────────────────────────────────────────────────
ALL_VARIANTS=(ruRelease ruDebug ukRelease ukDebug rustoreRelease rustoreDebug fullRelease fullDebug ruLocalSeed)
SELECTED_VARIANTS=("${@:-${ALL_VARIANTS[@]}}")

# ── temp workspace ────────────────────────────────────────────────────────────
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "${TMP_DIR}"' EXIT

# ── helpers ───────────────────────────────────────────────────────────────────
download_asset() {
  local asset_name="$1"
  local asset_file="${TMP_DIR}/${asset_name}"

  if [[ -f "${asset_file}" ]]; then
    echo "  Already downloaded: ${asset_name}"
    return 0
  fi

  echo "  Downloading ${asset_name} ..."
  local max_attempts=5
  local attempt=1
  local wait=5
  while (( attempt <= max_attempts )); do
    if gh release download "v${DB_VERSION}" \
        --repo "${GH_REPO}" \
        --pattern "${asset_name}" \
        --dir "${TMP_DIR}" \
        --clobber 2>&1; then
      return 0
    fi
    echo "  [attempt ${attempt}/${max_attempts}] Download failed, retrying in ${wait}s..."
    sleep "${wait}"
    (( attempt++ ))
    (( wait = wait * 2 ))  # exponential backoff: 5 10 20 40 80
  done
  echo "ERROR: Failed to download ${asset_name} after ${max_attempts} attempts." >&2
  exit 1
}

copy_asset() {
  local asset_name="$1"
  local target_dir="$2"
  local abs_target="${SCRIPT_DIR}/${target_dir}"

  rm -rf "${abs_target}"
  mkdir -p "${abs_target}"
  cp "${TMP_DIR}/${asset_name}" "${abs_target}/${asset_name}"
  echo "  Done → ${target_dir}/${asset_name}"
}

# ── main ──────────────────────────────────────────────────────────────────────
for variant in "${SELECTED_VARIANTS[@]}"; do
  if ! asset_name="$(variant_asset "${variant}" 2>/dev/null)"; then
    echo "WARNING: Unknown variant '${variant}', skipping." >&2; continue
  fi
  echo ""
  echo "=== ${variant} → ${asset_name} ==="
  download_asset "${asset_name}"
  copy_asset "${asset_name}" "$(variant_dir "${variant}")"
done

echo ""
echo "All done."

