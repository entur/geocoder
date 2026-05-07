#!/usr/bin/env bash
# Download the latest Nominatim NDJSON data from the public GCS bucket.
# Useful for local debugging - downloads the file into the current directory.
# Usage: ./download-latest-nominatim-data.sh [suffix] [tag]
#   suffix: e.g. '-se' for country-specific data (default: none)
#   tag:    'latest-prod' (default), 'latest', or a specific timestamped tag

set -euo pipefail

SUFFIX=${1:-}
TAG_INPUT=${2:-latest-prod}

BUCKET=ent-geocoder-prd
PREFIX="nominatim-data${SUFFIX}"
FILENAME="nominatim.ndjson.gz"

case "$TAG_INPUT" in
  latest|latest-prod)
    POINTER_URL="https://storage.googleapis.com/${BUCKET}/${PREFIX}/${TAG_INPUT}.txt"
    echo "Resolving $TAG_INPUT pointer: $POINTER_URL"
    TAG=$(curl -fsSL -A "entur-geocoder" "$POINTER_URL" | tr -d '[:space:]')
    ;;
  *)
    TAG="$TAG_INPUT"
    ;;
esac

URL="https://storage.googleapis.com/${BUCKET}/${PREFIX}/${TAG}/${FILENAME}"
echo "Downloading $URL"
curl -fL --retry 3 --retry-delay 10 -A "entur-geocoder" -o "$FILENAME" "$URL"
ls -lh "$FILENAME"
