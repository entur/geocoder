#!/usr/bin/env sh
# Photon container entrypoint.
#
# On a fresh pod the search index is downloaded from $PHOTON_DATA_URL (a public
# GCS object) and verified against its .sha256 sidecar. A `photon_data/.ready`
# sentinel marks successful extraction so in-place container restarts (same
# pod) skip the download. Pod replacement (rolling deploy, eviction, node
# drain) starts fresh and re-downloads.
#
# CI injects PHOTON_DATA_URL via _deploy-and-test.yml. For local debugging,
# run download-latest-photon-data.sh first - it populates photon_data/ and
# the sentinel so this script skips the download.

set -eu
# shellcheck disable=SC3040  # ash supports pipefail; needed so curl|tar surfaces curl failures
set -o pipefail

DATA_DIR=photon_data
SENTINEL="$DATA_DIR/.ready"

if [ ! -f "$SENTINEL" ]; then
  if [ -z "${PHOTON_DATA_URL:-}" ]; then
    echo "PHOTON_DATA_URL is not set" >&2
    exit 1
  fi

  echo "Downloading $PHOTON_DATA_URL"
  TARBALL=$(mktemp)
  trap 'rm -f "$TARBALL"' EXIT
  curl -fL --retry 3 --retry-delay 10 --connect-timeout 30 -A "entur-geocoder" -o "$TARBALL" "$PHOTON_DATA_URL"

  # CI always uploads .sha256 alongside the tarball; treat its absence as an error.
  EXPECTED=$(curl -fsSL --retry 3 --retry-delay 5 -A "entur-geocoder" "${PHOTON_DATA_URL}.sha256" | tr -d '[:space:]')
  ACTUAL=$(sha256sum "$TARBALL" | awk '{print $1}')
  if [ "$EXPECTED" != "$ACTUAL" ]; then
    echo "checksum mismatch: expected $EXPECTED got $ACTUAL" >&2
    exit 1
  fi
  echo "checksum verified ($ACTUAL)"

  # Stage then rename so a crash mid-extract can't leave a half-tree that
  # would then be confused with a clean install on the next container start.
  rm -rf "$DATA_DIR" "$DATA_DIR.staging"
  mkdir -p "$DATA_DIR.staging"
  tar -xzf "$TARBALL" -C "$DATA_DIR.staging"
  mv "$DATA_DIR.staging" "$DATA_DIR"
  touch "$SENTINEL"
  echo "Extracted $(find "$DATA_DIR" -type f | wc -l) files"
fi

# Photon/Lucene leaves stale lock files after ungraceful shutdowns; clear
# them so this restart can open the index.
find "$DATA_DIR" -name '*.lock' -delete

# Heap of 1536m fits within the 2560Mi container request with headroom for
# native, metaspace, and mmap'd index pages. Helm request must stay at least
# 50% above heap; bump both together.
PHOTON="java -Xms1536m -Xmx1536m -XX:+UseG1GC -XX:+UseStringDeduplication \
             -XX:MaxGCPauseMillis=75 -XX:+ExitOnOutOfMemoryError \
             -jar photon.jar"

$PHOTON serve -default-language no -listen-ip 0.0.0.0 \
              -listen-port "${SERVER_PORT:-2322}" -metrics-enable prometheus
