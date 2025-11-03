#!/usr/bin/env sh

set -eu

COMPRESS=false
NOMINATIM_FILE=nominatim.ndjson
if [ "${1:-}" = "-z" ];then
    COMPRESS=true
    NOMINATIM_FILE=nominatim.ndjson.xz
    shift
fi

PHOTON_JAR_URL=${1:-}
TARGET_DIR=.

fail() {
    echo "Error: $*"
    exit 1
}

[ -n "$PHOTON_JAR_URL" ] || fail "Photon JAR URL not provided. Please provide it as an argument."
[ -f "$NOMINATIM_FILE" ] || fail "$NOMINATIM_FILE not found. Please run import-all.sh first."
which tar >/dev/null || fail "bsdtar not found. Please install it to proceed."
which curl >/dev/null || fail "curl not found. Please install it to proceed."
which xz >/dev/null || fail "xz not found. Please install it to proceed."
which java >/dev/null || fail "java not found. Please install it to proceed."

echo "Downloading Photon JAR..."

curl -sfL --retry 2 -o photon.jar "$PHOTON_JAR_URL"

if $COMPRESS; then
  echo "Decompressing $NOMINATIM_FILE..."
  xz -d $NOMINATIM_FILE
fi
java -jar photon.jar \
        -nominatim-import \
        -import-file nominatim.ndjson \
        -languages no,en \
        -extra-tags ALL

if $COMPRESS; then
  tar cJvf $TARGET_DIR/photon_data.tar.xz photon_data
  echo "$TARGET_DIR/photon_data.tar.xz created."
else
  echo "$TARGET_DIR/photon_data created."
fi
