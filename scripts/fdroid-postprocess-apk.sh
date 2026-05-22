#!/usr/bin/env bash
# Apply the same reproducible-apk-tools steps as fdroiddata metadata (postbuild).
# Run from repo root after :mobile:assembleFdroidRelease. Optional: copy artifact.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

RAT="${RAT_DIR:-$REPO_ROOT/reproducible-apk-tools}"
if [[ ! -d "$RAT" ]]; then
  echo "Error: reproducible-apk-tools not found at $RAT (set RAT_DIR or checkout beside repo)"
  exit 1
fi

OUT="$(find mobile/build/outputs/apk/fdroid/release -maxdepth 1 -type f -name 'mobile-fdroid-release-unsigned.apk' | head -n 1)"
if [[ -z "$OUT" ]]; then
  echo "Error: mobile-fdroid-release-unsigned.apk not found under mobile/build/outputs/apk/fdroid/release"
  exit 1
fi

python3 "$RAT/inplace-fix.py" --page-size 16 sort-baseline "$OUT" --apk
python3 "$RAT/inplace-fix.py" --page-size 16 fix-pg-map-id "$OUT" '381d455'
python3 "$RAT/inplace-fix.py" --page-size 16 fix-newlines "$OUT" --from-crlf 'META-INF/services/*'
mv "$OUT" unaligned.apk
python3 "$RAT/zipalign.py" --page-size 16 --pad-like-apksigner --replace unaligned.apk "$OUT"

if [[ "${1:-}" == "--copy-artifact" && -n "${2:-}" ]]; then
  cp "$OUT" "$2"
fi
