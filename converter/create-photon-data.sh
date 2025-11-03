#!/usr/bin/env sh

set -eu

COMPRESS=false
NOMINATIM_FILE=nominatim.ndjson
if [ "${1:-}" = "-z" ];then
    COMPRESS=true
    NOMINATIM_FILE=nominatim.ndjson.xz
    shift
fi

PHOTON_JAR_SOURCE=${1:-photon.jar}

fail() {
    echo "Error: $*"
    exit 1
}

[ -f "$NOMINATIM_FILE" ] || fail "$NOMINATIM_FILE not found. Please run import-all.sh first."
which tar >/dev/null || fail "bsdtar not found. Please install it to proceed."
which curl >/dev/null || fail "curl not found. Please install it to proceed."
which xz >/dev/null || fail "xz not found. Please install it to proceed."
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

if $COMPRESS; then
  echo "Decompressing $NOMINATIM_FILE..."
  xz -d $NOMINATIM_FILE
fi
java -jar $PHOTON_JAR \
        -nominatim-import \
        -import-file nominatim.ndjson \
        -languages no,en \
        -extra-tags ALL

if $COMPRESS; then
  tar cJvf photon_data.tar.xz photon_data
  echo "photon_data.tar.xz created."
else
  echo "photon_data created."
fi
