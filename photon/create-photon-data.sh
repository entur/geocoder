#!/usr/bin/env sh

set -eu

NOMINATIM_FILE=nominatim.ndjson.gz
PHOTON_JAR_SOURCE=${1:-photon.jar}

fail() {
    echo "Error: $*"
    exit 1
}

[ -f "$NOMINATIM_FILE" ] || fail "$NOMINATIM_FILE not found. Please run create-nominatim-data.sh in converter first."
which tar >/dev/null || fail "bsdtar not found. Please install it to proceed."
which curl >/dev/null || fail "curl not found. Please install it to proceed."
which gzip >/dev/null || fail "gzip not found. Please install it to proceed."
which java >/dev/null || fail "java not found. Please install it to proceed."

if [ -f "$PHOTON_JAR_SOURCE" ]; then
  echo "Using local Photon JAR: $PHOTON_JAR_SOURCE"
  PHOTON_JAR="$PHOTON_JAR_SOURCE"
elif echo "$PHOTON_JAR_SOURCE" | grep -qE '^https?://'; then
  echo "Downloading Photon JAR from URL: $PHOTON_JAR_SOURCE"
  curl -sfL --retry 2 -o photon.jar "$PHOTON_JAR_SOURCE"
  PHOTON_JAR="photon.jar"
else
  fail "PHOTON_JAR_SOURCE must be either a valid file path or a URL (http:// or https://)"
fi

echo "Decompressing $NOMINATIM_FILE..."
gzip -d $NOMINATIM_FILE

START_TIME=$(date +%s)
java -jar "$PHOTON_JAR" \
        -nominatim-import \
        -import-file nominatim.ndjson \
        -languages no,en \
        -extra-tags ALL
END_TIME=$(date +%s)
echo "Created photon_data in $((END_TIME - START_TIME)) seconds."
