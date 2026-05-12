#!/usr/bin/env sh

set -eu

NOMINATIM_FILE="${1:-nominatim.ndjson}"
PHOTON_JAR=photon.jar

fail() {
    echo "Error: $*"
    exit 1
}

command -v gzip >/dev/null || fail "gzip not found. Please install it to proceed."
command -v java >/dev/null || fail "java not found. Please install it to proceed."

if [ ! -f "$PHOTON_JAR" ]; then
  fail "PHOTON_JAR must be a valid file path"
fi

[ -f "$NOMINATIM_FILE" ] || fail "Nominatim file '$NOMINATIM_FILE' not found. Please run import/create-nominatim-data.sh first."

IMPORT_FILE="$NOMINATIM_FILE"
if echo "$NOMINATIM_FILE" | grep -q '\.gz$'; then
  echo "Decompressing $NOMINATIM_FILE..."
  gzip -d -f -k "$NOMINATIM_FILE"
  IMPORT_FILE="${NOMINATIM_FILE%.gz}"
fi

# Photon importer is memory-hungry per worker (~1-2GB each). 5 is tuned for the
# CI/build runners (8 vCPU / 16GB+); reduce to 3 on smaller hosts or laptops.
IMPORT_THREADS="${PHOTON_IMPORT_THREADS:-5}"

START_TIME=$(date +%s)
java -jar "$PHOTON_JAR" \
        import \
        -j "$IMPORT_THREADS" \
        -import-file "$IMPORT_FILE" \
        -languages no,en \
        -stem-english-possessives \
        -extra-tags ALL
END_TIME=$(date +%s)
echo "Created photon_data in $((END_TIME - START_TIME)) seconds."
