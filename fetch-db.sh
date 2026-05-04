#!/usr/bin/env bash
# fetch-db.sh — downloads pre-built DB assets from the alelk/pws-docs release.
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
variant_zip() {
  case "$1" in
    ruRelease|rustoreRelease|fullRelease) echo "pws.${DB_VERSION}-ru-prepared" ;;
    ruDebug|rustoreDebug|fullDebug)       echo "pws.${DB_VERSION}-ru-debug-prepared" ;;
    ukRelease)                            echo "pws.${DB_VERSION}-uk-prepared" ;;
    ukDebug)                              echo "pws.${DB_VERSION}-uk-debug-prepared" ;;
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
    *) return 1 ;;
  esac
}

# ── variants to process ───────────────────────────────────────────────────────
ALL_VARIANTS=(ruRelease ruDebug ukRelease ukDebug rustoreRelease rustoreDebug fullRelease fullDebug)
SELECTED_VARIANTS=("${@:-${ALL_VARIANTS[@]}}")

# ── temp workspace ────────────────────────────────────────────────────────────
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "${TMP_DIR}"' EXIT

# ── helpers ───────────────────────────────────────────────────────────────────
download_and_extract() {
  local zip_name="$1"
  local zip_file="${TMP_DIR}/${zip_name}.zip"

  if [[ ! -f "${zip_file}" ]]; then
    echo "  Downloading ${zip_name}.zip ..."
    gh release download "v${DB_VERSION}" \
      --repo "${GH_REPO}" \
      --pattern "${zip_name}.zip" \
      --dir "${TMP_DIR}" \
      --clobber
  else
    echo "  Already downloaded: ${zip_name}.zip"
  fi

  local extract_dir="${TMP_DIR}/${zip_name}"
  if [[ ! -d "${extract_dir}" ]]; then
    echo "  Extracting ..."
    mkdir -p "${extract_dir}"
    unzip -q "${zip_file}" -d "${extract_dir}"
    rm -rf "${extract_dir}/__MACOSX"
    find "${extract_dir}" -name '._*' -delete
  fi
}

copy_files() {
  local extract_dir="$1" target_dir="$2"
  local abs_target="${SCRIPT_DIR}/${target_dir}"

  # Flatten one wrapper dir if present
  local src="${extract_dir}"
  local children=("${extract_dir}"/*)
  [[ ${#children[@]} -eq 1 && -d "${children[0]}" ]] && src="${children[0]}"

  rm -rf "${abs_target}"
  mkdir -p "${abs_target}"
  cp -r "${src}/." "${abs_target}/"
  echo "  Done → ${target_dir}"
}

# ── main ──────────────────────────────────────────────────────────────────────
for variant in "${SELECTED_VARIANTS[@]}"; do
  if ! zip_name="$(variant_zip "${variant}" 2>/dev/null)"; then
    echo "WARNING: Unknown variant '${variant}', skipping." >&2; continue
  fi
  echo ""
  echo "=== ${variant} (${zip_name}) ==="
  download_and_extract "${zip_name}"
  copy_files "${TMP_DIR}/${zip_name}" "$(variant_dir "${variant}")"
done

echo ""
echo "All done."

