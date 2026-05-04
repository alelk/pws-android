#!/usr/bin/env bash
# fetch-db.sh
#
# Downloads and extracts pre-built database assets from the pws-docs release.
# The database version is read from db.version that sits next to this script.
#
# Usage:
#   ./fetch-db.sh                  # fetch all variants
#   ./fetch-db.sh ruRelease        # fetch only the specified variant(s)
#   ./fetch-db.sh ruRelease ukRelease
#
# Supported variants:
#   ruRelease, ruDebug, ukRelease, ukDebug,
#   rustoreRelease, rustoreDebug, fullRelease, fullDebug

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# ── version ──────────────────────────────────────────────────────────────────
DB_VERSION_FILE="${SCRIPT_DIR}/db.version"
if [[ ! -f "${DB_VERSION_FILE}" ]]; then
  echo "ERROR: ${DB_VERSION_FILE} not found." >&2
  exit 1
fi
DB_VERSION="$(tr -d '[:space:]' < "${DB_VERSION_FILE}")"
echo "DB version: ${DB_VERSION}"

# ── auth / download method ───────────────────────────────────────────────────
# gh release download uses GraphQL which has cross-repo limitations with
# repo-scoped tokens. We prefer the REST API (curl) whenever a token is
# available, and fall back to gh CLI only for local interactive use.
#
# Priority:
#   1. GH_TOKEN or GITHUB_TOKEN present → GitHub REST API via curl
#   2. gh CLI with stored credentials   → gh CLI  (local dev, no explicit token)
GH_REPO="alelk/pws-docs"
AUTH_TOKEN="${GH_TOKEN:-${GITHUB_TOKEN:-}}"

if [[ -n "${AUTH_TOKEN}" ]]; then
  DOWNLOAD_METHOD="curl_api"
  echo "Using: GitHub REST API (token)"
elif command -v gh &>/dev/null && gh auth status &>/dev/null 2>&1; then
  DOWNLOAD_METHOD="gh"
  echo "Using: gh CLI"
else
  echo "ERROR: No GitHub auth found." >&2
  echo "  Option 1: install & authenticate gh CLI  →  brew install gh && gh auth login" >&2
  echo "  Option 2: export GH_TOKEN=<pat-with-pws-docs-read-access>" >&2
  exit 1
fi

# ── release base URL ─────────────────────────────────────────────────────────
BASE_URL="https://github.com/${GH_REPO}/releases/download/v${DB_VERSION}"

# ── variant lookup (bash 3.2 compatible, no associative arrays) ───────────────
variant_zip() {
  local variant="$1"
  case "${variant}" in
    ruRelease)      echo "pws.${DB_VERSION}-ru-prepared" ;;
    ruDebug)        echo "pws.${DB_VERSION}-ru-debug-prepared" ;;
    ukRelease)      echo "pws.${DB_VERSION}-uk-prepared" ;;
    ukDebug)        echo "pws.${DB_VERSION}-uk-debug-prepared" ;;
    rustoreRelease) echo "pws.${DB_VERSION}-ru-prepared" ;;       # same as ru
    rustoreDebug)   echo "pws.${DB_VERSION}-ru-debug-prepared" ;; # same as ru
    fullRelease)    echo "pws.${DB_VERSION}-ru-prepared" ;;       # same as ru
    fullDebug)      echo "pws.${DB_VERSION}-ru-debug-prepared" ;; # same as ru
    *) return 1 ;;
  esac
}

variant_dir() {
  local variant="$1"
  case "${variant}" in
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

# ── select variants to process ────────────────────────────────────────────────
ALL_VARIANTS=(ruRelease ruDebug ukRelease ukDebug rustoreRelease rustoreDebug fullRelease fullDebug)

if [[ $# -gt 0 ]]; then
  SELECTED_VARIANTS=("$@")
else
  SELECTED_VARIANTS=("${ALL_VARIANTS[@]}")
fi

# ── temp workspace ────────────────────────────────────────────────────────────
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "${TMP_DIR}"' EXIT
echo "Using temp dir: ${TMP_DIR}"

# ── helpers ───────────────────────────────────────────────────────────────────
download_and_extract() {
  local zip_name="$1"   # e.g. pws.3.0.0-ru-prepared
  local zip_file="${TMP_DIR}/${zip_name}.zip"
  local extract_dir="${TMP_DIR}/${zip_name}"

  # Download only once even if multiple variants share the same archive
  if [[ ! -f "${zip_file}" ]]; then
    if [[ "${DOWNLOAD_METHOD}" == "curl_api" ]]; then
      # Use GitHub REST API to locate the asset, then download it.
      # This avoids the GraphQL cross-repo resolution issues with scoped tokens.
      if ! command -v jq &>/dev/null; then
        echo "ERROR: 'jq' is required for REST API downloads. Install it: brew install jq" >&2
        exit 1
      fi
      echo "  Resolving asset ${zip_name}.zip via GitHub REST API ..."
      local asset_url
      asset_url=$(
        curl -fsSL \
          -H "Authorization: Bearer ${AUTH_TOKEN}" \
          -H "Accept: application/vnd.github+json" \
          "https://api.github.com/repos/${GH_REPO}/releases/tags/v${DB_VERSION}" \
        | jq -r ".assets[] | select(.name == \"${zip_name}.zip\") | .url"
      )
      if [[ -z "${asset_url}" || "${asset_url}" == "null" ]]; then
        echo "ERROR: Asset '${zip_name}.zip' not found in release v${DB_VERSION} of ${GH_REPO}" >&2
        exit 1
      fi
      echo "  Downloading ${zip_name}.zip via curl ..."
      curl -fsSL --retry 3 --retry-delay 2 \
        -H "Authorization: Bearer ${AUTH_TOKEN}" \
        -H "Accept: application/octet-stream" \
        -o "${zip_file}" "${asset_url}"
    else
      echo "  Downloading ${zip_name}.zip via gh CLI ..."
      gh release download "v${DB_VERSION}" \
        --repo "${GH_REPO}" \
        --pattern "${zip_name}.zip" \
        --dir "${TMP_DIR}" \
        --clobber
    fi
  else
    echo "  Already downloaded: ${zip_name}.zip"
  fi

  if [[ ! -d "${extract_dir}" ]]; then
    echo "  Extracting ${zip_name}.zip ..."
    mkdir -p "${extract_dir}"
    unzip -q "${zip_file}" -d "${extract_dir}"
    # Remove macOS resource-fork artifacts
    rm -rf "${extract_dir}/__MACOSX"
    find "${extract_dir}" -name '._*' -delete
  fi
}

copy_files() {
  local extract_dir="$1"  # e.g. ${TMP_DIR}/pws.3.0.0-ru-prepared
  local target_dir="$2"   # e.g. app-compose/src/ruRelease/assets/db

  local abs_target="${SCRIPT_DIR}/${target_dir}"
  echo "  Clearing ${target_dir} ..."
  rm -rf "${abs_target}"
  mkdir -p "${abs_target}"

  # Copy everything that was extracted (flatten one extra wrapper dir if present)
  local src="${extract_dir}"
  local children=("${extract_dir}"/*)
  if [[ ${#children[@]} -eq 1 && -d "${children[0]}" ]]; then
    src="${children[0]}"
  fi

  echo "  Copying files to ${target_dir} ..."
  cp -r "${src}"/. "${abs_target}/"
  echo "  Done → ${target_dir}"
}

# ── main loop ─────────────────────────────────────────────────────────────────
for variant in "${SELECTED_VARIANTS[@]}"; do
  if ! zip_name="$(variant_zip "${variant}" 2>/dev/null)"; then
    echo "WARNING: Unknown variant '${variant}', skipping." >&2
    continue
  fi
  target_dir="$(variant_dir "${variant}")"

  echo ""
  echo "=== ${variant} (${zip_name}) → ${target_dir} ==="
  download_and_extract "${zip_name}"
  copy_files "${TMP_DIR}/${zip_name}" "${target_dir}"
done

echo ""
echo "All done."

