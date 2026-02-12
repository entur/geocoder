#!/usr/bin/env sh

set -eu

SCRIPTDIR=$(cd "$(dirname "$0")"; pwd)
CONVERT="$SCRIPTDIR/convert.sh"
BUILDDIR="$SCRIPTDIR/build/create-nominatim-data"

usage() {
    echo "Usage: $0 <config-file> [-z] [-f] [-k]"
    echo ""
    echo "Arguments:"
    echo "  config-file    Path to config file (e.g., config/prod.conf)"
    echo "  -z             Compress output with gzip"
    echo "  -f             Force download even if files exist locally"
    echo "  -k             Keep downloaded files"
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

DOWNLOADED_FILES=""

download() {
    URL="$1"
    OUTPUT="$2"
    EXTRACT_PATTERN="${3:-}"

    # Check if URL is a local file path
    case "$URL" in
        http://*|https://*)
            # Remote URL - download it
            if [ -f "$OUTPUT" ] && ! $FORCE; then
                SIZE=$(du -h "$OUTPUT" | awk '{ print $1 }')
                echo "Using existing $OUTPUT (size: $SIZE)"
                return
            fi

            printf "Downloading: %s... " "$URL"

            if [ -n "$EXTRACT_PATTERN" ]; then
                curl -sfL --retry 2 "$URL" | bsdtar -xOf - "$EXTRACT_PATTERN" > "$OUTPUT"
            else
                curl -sfL --retry 2 "$URL" -o "$OUTPUT"
            fi

            if [ -f "$OUTPUT" ]; then
                SIZE=$(du -h "$OUTPUT" | awk '{ print $1 }')
                echo "Downloaded $OUTPUT, size: $SIZE"
                DOWNLOADED_FILES="$DOWNLOADED_FILES $OUTPUT"
            else
                fail "Failed to download $URL"
            fi
            ;;
        *)
            # Local file path - resolve relative to script dir
            case "$URL" in
                /*) LOCAL_PATH="$URL" ;;
                *) LOCAL_PATH="$SCRIPTDIR/$URL" ;;
            esac
            [ -f "$LOCAL_PATH" ] || fail "Local file not found: $LOCAL_PATH"
            SIZE=$(du -h "$LOCAL_PATH" | awk '{ print $1 }')
            echo "Using local file: $LOCAL_PATH (size: $SIZE)"
            cp "$LOCAL_PATH" "$OUTPUT"
            DOWNLOADED_FILES="$DOWNLOADED_FILES $OUTPUT"
            ;;
    esac
}

cleanup() {
  if [ "$KEEP" = "true" ]; then
    echo "Downloaded files kept in $BUILDDIR"
    return
  fi
  for f in $DOWNLOADED_FILES; do
      rm -f "$f"
  done
}

# Parse arguments
CONFIG_FILE=""
COMPRESS=false
FORCE=false
KEEP=false

for arg in "$@"; do
    case "$arg" in
        -z) COMPRESS=true ;;
        -f|--force) FORCE=true ;;
        -k|--keep) KEEP=true ;;
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
if [ -z "${ADRESSE_URL:-}" ] && [ -z "${POI_URL:-}" ] && [ -z "${POI2_URL:-}" ] && [ -z "${STOPPLACE_URL:-}" ] && [ -z "${OSM_URL:-}" ] && [ -z "${LANTMATERIET_DIR:-}" ]; then
    fail "No data sources configured. Set at least one of: ADRESSE_URL, POI_URL, POI2_URL, STOPPLACE_URL, OSM_URL, LANTMATERIET_DIR"
fi

# Check dependencies
which bsdtar >/dev/null 2>&1 || fail "bsdtar not found. Please install bsdtar to proceed."
which curl >/dev/null 2>&1 || fail "curl not found. Please install curl to proceed."
which gzip >/dev/null 2>&1 || fail "gzip not found. Please install gzip to proceed."
which java >/dev/null 2>&1 || fail "java not found. Please install java to proceed."
[ -f "$CONVERT" ] || fail "$CONVERT not found."

echo "Using config: $CONFIG_FILE"

mkdir -p "$BUILDDIR"

START_TIME=$(date +%s)

# Remove existing output file to start fresh
rm -f nominatim.ndjson

# Matrikkel addresses + Stedsnavn (Norwegian cadastre data)
if [ -n "${ADRESSE_URL:-}" ] && [ -n "${STEDSNAVN_URL:-}" ]; then
    download "$ADRESSE_URL" "$BUILDDIR/adresse.csv" '*.csv'
    download "$STEDSNAVN_URL" "$BUILDDIR/stedsnavn.gml" '*.gml'
    $CONVERT -a -m "$BUILDDIR/adresse.csv" -g "$BUILDDIR/stedsnavn.gml" -o nominatim.ndjson
fi

# POI data
if [ -n "${POI_URL:-}" ]; then
    download "$POI_URL" "$BUILDDIR/poi.xml"
    $CONVERT -a -x "$BUILDDIR/poi.xml" -o nominatim.ndjson
fi

if [ -n "${POI2_URL:-}" ]; then
    download "$POI2_URL" "$BUILDDIR/poi2.xml"
    $CONVERT -a -x "$BUILDDIR/poi2.xml" -o nominatim.ndjson
fi

# Stop places
if [ -n "${STOPPLACE_URL:-}" ]; then
    download "$STOPPLACE_URL" "$BUILDDIR/stopplace.xml" '*.xml'
    $CONVERT -a -s "$BUILDDIR/stopplace.xml" -o nominatim.ndjson
fi

# OSM data
if [ -n "${OSM_URL:-}" ]; then
    download "$OSM_URL" "$BUILDDIR/osm.pbf"
    $CONVERT -a -p "$BUILDDIR/osm.pbf" -o nominatim.ndjson
fi

# Lantmäteriet Swedish addresses (pre-downloaded directory of .gpkg files)
if [ -n "${LANTMATERIET_DIR:-}" ]; then
    case "$LANTMATERIET_DIR" in
        /*) LM_PATH="$LANTMATERIET_DIR" ;;
        *) LM_PATH="$SCRIPTDIR/$LANTMATERIET_DIR" ;;
    esac
    [ -d "$LM_PATH" ] || fail "Lantmäteriet directory not found: $LM_PATH"
    $CONVERT -a -w "$LM_PATH" -o nominatim.ndjson
fi

cleanup

END_TIME=$(date +%s)
echo "Created nominatim.ndjson in $((END_TIME - START_TIME)) seconds."

if $COMPRESS; then
    echo "Creating compressed nominatim.ndjson.gz..."
    START_TIME=$(date +%s)
    gzip -k -1 nominatim.ndjson
    END_TIME=$(date +%s)
    echo "Done in $((END_TIME - START_TIME)) seconds."
fi
