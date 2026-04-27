#!/usr/bin/env bash
# Download the latest Photon search index from the public GCS bucket and
# stage it for local use. Writes a `photon_data/.ready` sentinel so a
# subsequent `photon-start.sh` (in a container or locally) skips re-download.
#
# Usage: ./download-latest-photon-data.sh [suffix] [tag]
#   suffix: e.g. '-se' for country-specific data (default: none)
#   tag:    'latest-prod' (default), 'latest', or a specific timestamped tag

set -euo pipefail

SUFFIX=${1:-}
TAG_INPUT=${2:-latest-prod}

BUCKET=ent-geocoder-prd
PREFIX="photon-data${SUFFIX}"
FILENAME="photon_data.tar.gz"

case "$TAG_INPUT" in
  latest|latest-prod)
    POINTER_URL="https://storage.googleapis.com/${BUCKET}/${PREFIX}/${TAG_INPUT}.txt"
    echo "Resolving $TAG_INPUT pointer: $POINTER_URL"
    TAG=$(curl -fsSL "$POINTER_URL" | tr -d '[:space:]')
    ;;
  *)
    TAG="$TAG_INPUT"
    ;;
esac

URL="https://storage.googleapis.com/${BUCKET}/${PREFIX}/${TAG}/${FILENAME}"
echo "Downloading $URL"

TARBALL=$(mktemp)
trap 'rm -f "$TARBALL"' EXIT
curl -fL --retry 3 --retry-delay 10 -o "$TARBALL" "$URL"

EXPECTED=$(curl -fsSL --retry 3 --retry-delay 5 "${URL}.sha256" | tr -d '[:space:]')
ACTUAL=$(sha256sum "$TARBALL" | awk '{print $1}')
if [ "$EXPECTED" != "$ACTUAL" ]; then
  echo "checksum mismatch: expected $EXPECTED got $ACTUAL" >&2
  exit 1
fi

rm -rf photon_data photon_data.staging
mkdir -p photon_data.staging
tar -xzf "$TARBALL" -C photon_data.staging
mv photon_data.staging photon_data
touch photon_data/.ready
echo "The latest Photon data is now in ./photon_data"
