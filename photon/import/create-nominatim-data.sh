#!/usr/bin/env sh

set -eu

VERSION="v0.4.0"

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
    curl -sfL --retry 2 -A "entur-geocoder" "$BASE_URL/$ARTIFACT" -o "$BINARY"
    chmod +x "$BINARY"
fi

# Cache downloaded source archives (OSM PBF, Geonorge matrikkel/stedsnavn, stop place ZIPs)
# so re-runs skip the multi-GB downloads. Default persists across reboots via XDG cache;
# override with NOMINATIM_CACHE_DIR=/path, or point it at ${TMPDIR}/... for ephemeral caching.
CACHE_DIR="${NOMINATIM_CACHE_DIR:-${XDG_CACHE_HOME:-$HOME/.cache}/nominatim-converter}"

# USAGE_FILE is set further down if the active config defines USAGE_URL.
USAGE_FILE=""

convert() {
    if [ -n "$USAGE_FILE" ]; then
        "$BINARY" "$@" --cache-dir "$CACHE_DIR" -c "$SCRIPTDIR/config/nominatim-converter.json" --usage "$USAGE_FILE"
    else
        "$BINARY" "$@" --cache-dir "$CACHE_DIR" -c "$SCRIPTDIR/config/nominatim-converter.json"
    fi
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

if [ -z "${GEONORGE_AREA:-}" ] && [ -z "${MATRIKKEL_URL:-}" ] && [ -z "${STEDSNAVN_URL:-}" ] && [ -z "${POI_URL:-}" ] && [ -z "${POI2_URL:-}" ] && [ -z "${STOPPLACE_URL:-}" ] && [ -z "${OSM_URL:-}" ]; then
    fail "No data sources configured. Set at least one of: GEONORGE_AREA, MATRIKKEL_URL, STEDSNAVN_URL, POI_URL, POI2_URL, STOPPLACE_URL, OSM_URL"
fi

echo "Using config: $CONFIG_FILE"

START_TIME=$(date +%s)

# Download popular-stops CSV once if the config requested usage-driven boosting.
# `--usage` only accepts local paths, so we resolve the URL up-front and reuse the
# same file across every per-source convert call.
if [ -n "${USAGE_URL:-}" ]; then
    USAGE_FILE="${TMPDIR:-/tmp}/nominatim-usage.csv"
    echo "Downloading usage CSV: $USAGE_URL"
    curl -sfL --retry 2 -A "entur-geocoder" "$USAGE_URL" -o "$USAGE_FILE"
    echo "  $(wc -l < "$USAGE_FILE") rows -> $USAGE_FILE"
fi

rm -f nominatim.ndjson

if [ -n "${MATRIKKEL_URL:-}" ]; then
    [ -n "${STEDSNAVN_URL:-}" ] || fail "MATRIKKEL_URL requires STEDSNAVN_URL (used as -g GML for matrikkel)"
    convert matrikkel -i "$MATRIKKEL_URL" -g "$STEDSNAVN_URL" -o nominatim.ndjson -a
elif [ -n "${GEONORGE_AREA:-}" ]; then
    convert matrikkel -r "$GEONORGE_AREA" -o nominatim.ndjson -a
fi

if [ -n "${STEDSNAVN_URL:-}" ]; then
    convert stedsnavn -i "$STEDSNAVN_URL" -o nominatim.ndjson -a
elif [ -n "${GEONORGE_AREA:-}" ]; then
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
