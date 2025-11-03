#!/usr/bin/env sh

set -eu

PHOTON_JAR_URL=${1:-}
NOMINATIM_FILE=${2:-nominatim.ndjson.xz}
TARGET_DIR=${3:-.}

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

curl -sfL --retry 2 -o photon.jar "$PHOTON_JAR_URL"

xz -d $NOMINATIM_FILE
java -jar photon.jar \
        -nominatim-import \
        -import-file nominatim.ndjson \
        -languages no,en \
        -extra-tags ALL

tar cJvf $TARGET_DIR/photon_data.tar.xz photon_data
echo "$TARGET_DIR/photon_data.tar.xz created."
