#!/usr/bin/env sh

set -eu

VERSION="v0.3.4"

SCRIPTDIR=$(cd "$(dirname "$0")"; pwd)
PHOTONDIR=$(cd "$SCRIPTDIR/.."; pwd)
BINARY="$SCRIPTDIR/build/nominatim-converter-$VERSION"
BASE_URL="https://github.com/entur/nominatim-converter/releases/download/$VERSION"

usage() {
    echo "Usage: $0 <config-file> [-z]"
    echo ""
    echo "Arguments:"
    echo "  config-file    Path to config file (e.g., import/config/sources-prod.conf)"
    echo "  -z             Compress output with gzip"
    echo ""
    echo "Available configs:"
    for f in "$SCRIPTDIR"/config/*.conf; do
        [ -f "$f" ] && echo "  import/config/$(basename "$f")"
    done
    exit 1
}

fail() {
    echo "Error: $*"
    exit 1
}

# Download converter binary if not present
if [ ! -f "$BINARY" ]; then
    mkdir -p "$SCRIPTDIR/build"
    OS=$(uname -s)
    case "$OS" in
        Linux)  ARTIFACT="nominatim-converter-linux-x86_64" ;;
        Darwin) ARTIFACT="nominatim-converter-macos-aarch64" ;;
        *) fail "Unsupported OS: $OS" ;;
    esac
    echo "Downloading nominatim-converter $VERSION..."
    curl -sfL --retry 2 "$BASE_URL/$ARTIFACT" -o "$BINARY"
    chmod +x "$BINARY"
fi

convert() {
    "$BINARY" "$@" -c "$SCRIPTDIR/config/nominatim-converter.json"
}

# Parse arguments
CONFIG_FILE=""
COMPRESS=false

for arg in "$@"; do
    case "$arg" in
        -z) COMPRESS=true ;;
        -h|--help) usage ;;
        *)
            if [ -z "$CONFIG_FILE" ]; then
                CONFIG_FILE="$arg"
            else
                fail "Unknown argument: $arg"
            fi
            ;;
    esac
done

[ -n "$CONFIG_FILE" ] || usage

# Resolve config path relative to script dir if not absolute
case "$CONFIG_FILE" in
    /*) ;;
    *) CONFIG_FILE="$PHOTONDIR/$CONFIG_FILE" ;;
esac

[ -f "$CONFIG_FILE" ] || fail "Config file not found: $CONFIG_FILE"

# Source config - sets URL variables
# shellcheck source=/dev/null
. "$CONFIG_FILE"

if [ -z "${GEONORGE_AREA:-}" ] && [ -z "${POI_URL:-}" ] && [ -z "${POI2_URL:-}" ] && [ -z "${STOPPLACE_URL:-}" ] && [ -z "${OSM_URL:-}" ]; then
    fail "No data sources configured. Set at least one of: GEONORGE_AREA, POI_URL, POI2_URL, STOPPLACE_URL, OSM_URL"
fi

echo "Using config: $CONFIG_FILE"

START_TIME=$(date +%s)

rm -f nominatim.ndjson

if [ -n "${GEONORGE_AREA:-}" ]; then
    convert matrikkel -r "$GEONORGE_AREA" -o nominatim.ndjson -a
    convert stedsnavn -r "$GEONORGE_AREA" -o nominatim.ndjson -a
fi

if [ -n "${POI_URL:-}" ]; then
    convert poi -i "$POI_URL" -o nominatim.ndjson -a
fi

if [ -n "${POI2_URL:-}" ]; then
    convert poi -i "$POI2_URL" -o nominatim.ndjson -a
fi

if [ -n "${STOPPLACE_URL:-}" ]; then
    convert stopplace -i "$STOPPLACE_URL" -o nominatim.ndjson -a
fi

if [ -n "${OSM_URL:-}" ]; then
    convert osm -i "$OSM_URL" -o nominatim.ndjson -a
fi

if [ -n "${BELAGENHET_AREA:-}" ]; then
    convert belagenhet -m "${BELAGENHET_AREA}" -o nominatim.ndjson -a
fi

END_TIME=$(date +%s)
echo "Created nominatim.ndjson in $((END_TIME - START_TIME)) seconds."

if $COMPRESS; then
    echo "Creating compressed nominatim.ndjson.gz..."
    START_TIME=$(date +%s)
    gzip -k -1 nominatim.ndjson
    END_TIME=$(date +%s)
    echo "Done in $((END_TIME - START_TIME)) seconds."
fi
