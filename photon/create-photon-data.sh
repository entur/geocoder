#!/usr/bin/env sh

set -eu

NOMINATIM_FILE=nominatim.ndjson
PHOTON_JAR=photon.jar

fail() {
    echo "Error: $*"
    exit 1
}

which tar >/dev/null || fail "bsdtar not found. Please install it to proceed."
which curl >/dev/null || fail "curl not found. Please install it to proceed."
which gzip >/dev/null || fail "gzip not found. Please install it to proceed."
which java >/dev/null || fail "java not found. Please install it to proceed."

if [ ! -f "$PHOTON_JAR" ]; then
  fail "PHOTON_JAR must be a valid file path"
fi

[ -f "$NOMINATIM_FILE.gz" ] && gzip -d $NOMINATIM_FILE.gz
[ -f "$NOMINATIM_FILE" ] || fail "$NOMINATIM_FILE not found. Please run create-nominatim-data.sh in converter first."

START_TIME=$(date +%s)
java -jar "$PHOTON_JAR" \
        import \
        -j 4 \
        -import-file nominatim.ndjson \
        -languages no,en \
        -extra-tags ALL
END_TIME=$(date +%s)
echo "Created photon_data in $((END_TIME - START_TIME)) seconds."
