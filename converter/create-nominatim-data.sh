#!/usr/bin/env sh

set -eu

SCRIPTDIR=$(cd "$(dirname "$0")"; pwd)
CONVERT="$SCRIPTDIR/convert.sh"

usage() {
    echo "Usage: $0 <config-file> [-z]"
    echo ""
    echo "Arguments:"
    echo "  config-file    Path to config file (e.g., config/prod.conf)"
    echo "  -z             Compress output with gzip"
    echo ""
    echo "Available configs:"
    for f in "$SCRIPTDIR"/config/*.conf; do
        [ -f "$f" ] && echo "  config/$(basename "$f")"
    done
    exit 1
}

fail() {
    echo "Error: $*"
    exit 1
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
    *) CONFIG_FILE="$SCRIPTDIR/$CONFIG_FILE" ;;
esac

[ -f "$CONFIG_FILE" ] || fail "Config file not found: $CONFIG_FILE"

# Source config - sets URL variables
# shellcheck source=/dev/null
. "$CONFIG_FILE"

# Verify at least one source is configured
if [ -z "${ADRESSE_URL:-}" ] && [ -z "${POI_URL:-}" ] && [ -z "${POI2_URL:-}" ] && [ -z "${STOPPLACE_URL:-}" ] && [ -z "${OSM_URL:-}" ]; then
    fail "No data sources configured. Set at least one of: ADRESSE_URL, POI_URL, POI2_URL, STOPPLACE_URL, OSM_URL"
fi

[ -f "$CONVERT" ] || fail "$CONVERT not found."

echo "Using config: $CONFIG_FILE"

START_TIME=$(date +%s)

# Remove existing output file to start fresh
rm -f nominatim.ndjson

# Matrikkel addresses + Stedsnavn (Norwegian cadastre data)
if [ -n "${ADRESSE_URL:-}" ] && [ -n "${STEDSNAVN_URL:-}" ]; then
    $CONVERT matrikkel -i "$ADRESSE_URL" -g "$STEDSNAVN_URL" -o nominatim.ndjson -a
fi

# POI data
if [ -n "${POI_URL:-}" ]; then
    $CONVERT poi -i "$POI_URL" -o nominatim.ndjson -a
fi

if [ -n "${POI2_URL:-}" ]; then
    $CONVERT poi -i "$POI2_URL" -o nominatim.ndjson -a
fi

# Stop places
if [ -n "${STOPPLACE_URL:-}" ]; then
    $CONVERT stopplace -i "$STOPPLACE_URL" -o nominatim.ndjson -a
fi

# OSM data
if [ -n "${OSM_URL:-}" ]; then
    $CONVERT osm -i "$OSM_URL" -o nominatim.ndjson -a
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
