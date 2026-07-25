#!/usr/bin/env bash
#
# Downloads the raw MISeD dataset (train/validation/test JSONL) from the
# google-research-datasets/MISeD GitHub repo into data/raw/.
#
# Idempotent: skips any file that already exists unless --force is passed.
# Verifies each downloaded file's line count against the known-good counts
# (303 / 63 / 66) so a truncated or changed upstream file fails loudly instead
# of silently propagating into the ETL.
#
# Usage:
#   ./tools/download_mised.sh          # download missing files only
#   ./tools/download_mised.sh --force  # re-download everything

set -euo pipefail

BASE_URL="https://raw.githubusercontent.com/google-research-datasets/MISeD/main/mised"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
OUT_DIR="${REPO_ROOT}/data/raw"

FORCE=0
if [[ "${1:-}" == "--force" ]]; then
  FORCE=1
fi

mkdir -p "${OUT_DIR}"

# split:expected-line-count, verified against the actual dataset at plan time.
# (Plain "key:value" list rather than an associative array — macOS ships
# bash 3.2, which predates `declare -A`, and this script should run as-is
# on both macOS and Linux without requiring a newer bash.)
EXPECTED_LINES="train:303 validation:63 test:66"

expected_lines_for() {
  local split="$1"
  for pair in ${EXPECTED_LINES}; do
    if [[ "${pair%%:*}" == "${split}" ]]; then
      echo "${pair##*:}"
      return 0
    fi
  done
  return 1
}

echo "Downloading MISeD dataset into ${OUT_DIR}"

for split in train validation test; do
  dest="${OUT_DIR}/${split}.jsonl"

  if [[ -f "${dest}" && "${FORCE}" -eq 0 ]]; then
    echo "  [skip] ${split}.jsonl already present (use --force to re-download)"
  else
    echo "  [fetch] ${split}.jsonl"
    curl -fsSL "${BASE_URL}/${split}.jsonl" -o "${dest}.tmp"
    mv "${dest}.tmp" "${dest}"
  fi

  actual_lines=$(wc -l < "${dest}" | tr -d ' ')
  expected="$(expected_lines_for "${split}")"
  if [[ "${actual_lines}" != "${expected}" ]]; then
    echo "  [ERROR] ${split}.jsonl has ${actual_lines} lines, expected ${expected}." >&2
    echo "          The upstream file may have changed — verify before ingesting." >&2
    exit 1
  fi
  echo "  [ok] ${split}.jsonl: ${actual_lines} lines"
done

echo "Done. 432 dialogs total across the three files."
