#!/usr/bin/env bash
# F-Droid release verification: ensure fdroidRelease APK contains no Firebase, GMS, or Dropbox.
# Run from repo root. Requires: gradle, unzip.
# Usage: ./scripts/verify-fdroid-release.sh [--no-build]
#   --no-build  Skip build; only inspect existing APK(s) in mobile/build/outputs/apk/fdroid/release/
#
# This is a verification helper, not absolute proof. It checks archive paths (unzip -l);
# passing does not guarantee there are no runtime references to these packages.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APK_DIR="${REPO_ROOT}/mobile/build/outputs/apk/fdroid/release"
# Package-based matching: Firebase, Google Play Services, Dropbox
FORBIDDEN_PATTERN='com/google/firebase|com/google/android/gms|com/dropbox'

cd "$REPO_ROOT"

if [[ "${1:-}" != "--no-build" ]]; then
  echo "Building fdroidRelease with -Pfdroid..."
  ./gradlew :mobile:assembleFdroidRelease -Pfdroid --no-daemon -q
fi

if [[ ! -d "$APK_DIR" ]]; then
  echo "Error: APK directory not found: $APK_DIR"
  exit 1
fi

shopt -s nullglob
apks=("$APK_DIR"/*.apk)
shopt -u nullglob

if [[ ${#apks[@]} -eq 0 ]]; then
  echo "Error: No APKs found in $APK_DIR"
  exit 1
fi

FAILED=0
for apk in "${apks[@]}"; do
  echo "Checking $(basename "$apk")..."
  MATCHES="$(unzip -l "$apk" 2>/dev/null | grep -E "$FORBIDDEN_PATTERN" || true)"
  if [[ -n "$MATCHES" ]]; then
    echo "FAIL: Forbidden classes/resources found in APK:"
    echo "$MATCHES"
    FAILED=1
  fi
done

if [[ $FAILED -eq 1 ]]; then
  echo
  echo "Verification failed. F-Droid APK must not contain Firebase, GMS, or Dropbox."
  exit 1
fi

echo "OK: No Firebase, GMS, or Dropbox found in fdroid release APK(s)."
echo "Recommendation: also sanity-test the build on a device (vault, connections, settings)."
