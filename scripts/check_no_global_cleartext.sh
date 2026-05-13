#!/usr/bin/env bash
# SECURITY: Ensures network security config does not permit cleartext globally.
# Audit finding: cleartext was previously re-introduced via base-config.
# Run from repo root (e.g. in CI or pre-commit). Exits 1 if base-config permits cleartext.

set -e
CONFIG="mobile/src/main/res/xml/configure_localhost_media_file_http_server.xml"
if [ ! -f "$CONFIG" ]; then
  echo "Error: $CONFIG not found. Run from repo root."
  exit 1
fi

# Fail if base-config has cleartextTrafficPermitted="true"
if grep -A1 '<base-config' "$CONFIG" | grep -q 'cleartextTrafficPermitted="true"'; then
  echo "SECURITY: $CONFIG must NOT set base-config cleartextTrafficPermitted=\"true\" (audit)."
  echo "Only domain-config for localhost may permit cleartext. See file comments."
  exit 1
fi

echo "OK: No global cleartext in $CONFIG"
exit 0
