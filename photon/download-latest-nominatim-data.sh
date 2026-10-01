#!/usr/bin/env bash
# Download the latest Nominatim NDJSON data from the public GCS bucket.
# Useful for local debugging - downloads the file into the current directory.
# Usage: ./download-latest-nominatim-data.sh [suffix] [tag]
#   suffix: e.g. '-se' for country-specific data (default: none)
#   tag:    a build tag, 'prod' for what prd serves (needs kubectl access to prd, Norway
#           only), or omitted for the newest build in the bucket

set -euo pipefail

SUFFIX=${1:-}
TAG_INPUT=${2:-}

BUCKET=ent-geocoder-prd
PREFIX="nominatim-data${SUFFIX}"
FILENAME="nominatim.ndjson.gz"

case "$TAG_INPUT" in
  "")
    # Tags sort chronologically, so the last main.* prefix in the listing is the newest build.
    LIST_URL="https://storage.googleapis.com/storage/v1/b/${BUCKET}/o?prefix=${PREFIX}/main.&delimiter=/&fields=prefixes"
    TAG=$(curl -fsSL -A "entur-geocoder" "$LIST_URL" | grep -o "${PREFIX}/main\.[^/\"]*" | sort | tail -1 | sed "s|^${PREFIX}/||")
    [ -n "$TAG" ] || { echo "no builds under ${PREFIX}/" >&2; exit 1; }
    echo "Newest build: $TAG"
    ;;
  prod)
    DATA_URL=$(kubectl --context prd -n geocoder get deployment geocoder-photon \
      -o jsonpath='{.spec.template.spec.initContainers[*].env[?(@.name=="PHOTON_DATA_URL")].value}')
    TAG=$(basename "$(dirname "$DATA_URL")")
    echo "prd serves: $TAG"
    ;;
  *)
    TAG="$TAG_INPUT"
    ;;
esac

URL="https://storage.googleapis.com/${BUCKET}/${PREFIX}/${TAG}/${FILENAME}"
echo "Downloading $URL"
curl -fL --retry 3 --retry-delay 10 -A "entur-geocoder" -o "$FILENAME" "$URL"
ls -lh "$FILENAME"
