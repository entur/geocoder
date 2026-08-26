#!/usr/bin/env sh

set -eu

VERSION="v0.8.1"

SCRIPTDIR=$(cd "$(dirname "$0")"; pwd)
PHOTONDIR=$(cd "$SCRIPTDIR/.."; pwd)
BINARY="$SCRIPTDIR/build/nominatim-converter-$VERSION"
BASE_URL="https://github.com/entur/nominatim-converter/releases/download/$VERSION"

usage() {
    echo "Usage: $0 <config-file> [-z]"
    echo ""
    echo "Arguments:"
    echo "  config-file    Converter config (e.g., import/config/converter-prod.json)"
    echo "  -z             Compress output with gzip"
    echo ""
    echo "Available configs:"
    for f in "$SCRIPTDIR"/config/converter-*.json; do
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
    curl -sfL --retry 2 -A "entur-geocoder" "$BASE_URL/$ARTIFACT" -o "$BINARY" \
        || fail "could not download nominatim-converter $VERSION - is that release published?"
    chmod +x "$BINARY"
fi

# Cache downloaded source archives (OSM PBF, Geonorge matrikkel/stedsnavn, stop place ZIPs)
# so re-runs skip the multi-GB downloads. Default persists across reboots via XDG cache;
# override with NOMINATIM_CACHE_DIR=/path, or point it at ${TMPDIR}/... for ephemeral caching.
CACHE_DIR="${NOMINATIM_CACHE_DIR:-${XDG_CACHE_HOME:-$HOME/.cache}/nominatim-converter}"

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

# Resolve config path relative to photon/ if not absolute
case "$CONFIG_FILE" in
    /*) ;;
    *) CONFIG_FILE="$PHOTONDIR/$CONFIG_FILE" ;;
esac

[ -f "$CONFIG_FILE" ] || fail "Config file not found: $CONFIG_FILE"

echo "Using config: $CONFIG_FILE"

# The converter reads each source's `input` (URL / file / Geonorge region / Lantmateriet
# municipality) and the scoring from this one file, then writes a combined NDJSON. Source
# selection, ordering, and downloading all happen inside `build`.
START_TIME=$(date +%s)

rm -f nominatim.ndjson
"$BINARY" build -c "$CONFIG_FILE" -o nominatim.ndjson --warn-if-stale=24 --cache-dir "$CACHE_DIR" -f

END_TIME=$(date +%s)
echo "Created nominatim.ndjson in $((END_TIME - START_TIME)) seconds."

if $COMPRESS; then
    echo "Creating compressed nominatim.ndjson.gz..."
    START_TIME=$(date +%s)
    gzip -k -1 nominatim.ndjson
    END_TIME=$(date +%s)
    echo "Done in $((END_TIME - START_TIME)) seconds."
fi
