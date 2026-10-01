#!/usr/bin/env bash
# Download the latest Photon search index from the public GCS bucket and
# stage it for local use. Writes a `photon_data/.ready` sentinel so a
# subsequent `photon-start.sh` (in a container or locally) skips re-download.
#
# Usage: ./download-latest-photon-data.sh [suffix] [tag]
#   suffix: e.g. '-se' for country-specific data (default: none)
#   tag:    a build tag, 'prod' for what prd serves (needs kubectl access to prd, Norway
#           only), or omitted for the newest build in the bucket

set -euo pipefail

SUFFIX=${1:-}
TAG_INPUT=${2:-}

BUCKET=ent-geocoder-prd
PREFIX="photon-data${SUFFIX}"
FILENAME="photon_data.tar.gz"

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

TARBALL=$(mktemp)
trap 'rm -f "$TARBALL"' EXIT
curl -fL --retry 3 --retry-delay 10 -A "entur-geocoder" -o "$TARBALL" "$URL"

EXPECTED=$(curl -fsSL --retry 3 --retry-delay 5 -A "entur-geocoder" "${URL}.sha256" | tr -d '[:space:]')
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
