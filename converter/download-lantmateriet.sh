#!/usr/bin/env bash

set -euo pipefail

# Downloads Swedish address data (Belägenhetsadress) from Lantmäteriet's STAC API.
# The STAC catalog is open, but the actual zip downloads require authentication.
#
# Usage:
#   ./download-lantmateriet.sh -u <user:pass> [-o <output-dir>] [-f] [-m <kommunkod,...>]
#
# Authentication:
#   Get credentials at https://geotorget.lantmateriet.se/ by ordering the
#   "Belägenhetsadress Nedladdning, vektor" product.

SCRIPTDIR=$(cd "$(dirname "$0")"; pwd)
STAC_API="https://api.lantmateriet.se/stac-vektor/v1/collections/belagenhetsadresser/items"
OUTPUT_DIR="$SCRIPTDIR/build/lantmateriet"
FORCE=false
AUTH=""
MUNICIPALITIES=""
LIMIT=300

usage() {
    cat <<EOF
Usage: $(basename "$0") -u <user:pass> [options]

Downloads Lantmäteriet Belägenhetsadress GeoPackage files.

Required:
  -u <user:pass>        HTTP Basic Auth credentials

Options:
  -o <output-dir>       Output directory (default: build/lantmateriet)
  -m <kod1,kod2,...>    Only download specific municipality codes (e.g. 0180,0381)
  -f                    Force re-download existing files
  -h                    Show this help

Examples:
  $(basename "$0") -u myuser:mypass                       # Download all municipalities
  $(basename "$0") -u myuser:mypass -m 0180               # Stockholm only
  $(basename "$0") -u myuser:mypass -m 0180,0381,1480     # Stockholm, Enköping, Göteborg
EOF
    exit 1
}

fail() {
    echo "Error: $*" >&2
    exit 1
}

while [ $# -gt 0 ]; do
    case "$1" in
        -u) AUTH="$2"; shift 2 ;;
        -o) OUTPUT_DIR="$2"; shift 2 ;;
        -m) MUNICIPALITIES="$2"; shift 2 ;;
        -f) FORCE=true; shift ;;
        -h|--help) usage ;;
        *) fail "Unknown option: $1" ;;
    esac
done

[ -n "$AUTH" ] || usage

which curl >/dev/null 2>&1 || fail "curl not found"
which jq >/dev/null 2>&1 || fail "jq not found (brew install jq)"

mkdir -p "$OUTPUT_DIR"

# Fetch all download URLs from the STAC API (open, no auth needed)
echo "Fetching municipality list from STAC API..."

ALL_URLS=""
NEXT_URL="$STAC_API?limit=$LIMIT"

while [ -n "$NEXT_URL" ]; do
    RESPONSE=$(curl -sf "$NEXT_URL")

    PAGE_URLS=$(echo "$RESPONSE" | jq -r '.features[] | "\(.id) \(.assets.data.href) \(.properties.title)"')
    ALL_URLS=$(printf '%s\n%s' "$ALL_URLS" "$PAGE_URLS")

    NEXT_URL=$(echo "$RESPONSE" | jq -r '.links[] | select(.rel == "next") | .href // empty')
done

# Filter to requested municipalities if specified
if [ -n "$MUNICIPALITIES" ]; then
    FILTERED=""
    IFS=',' read -ra CODES <<< "$MUNICIPALITIES"
    for code in "${CODES[@]}"; do
        MATCH=$(echo "$ALL_URLS" | grep "^${code} " || true)
        if [ -n "$MATCH" ]; then
            FILTERED=$(printf '%s\n%s' "$FILTERED" "$MATCH")
        else
            echo "Warning: municipality $code not found in STAC catalog"
        fi
    done
    ALL_URLS="$FILTERED"
fi

# Remove empty lines
ALL_URLS=$(echo "$ALL_URLS" | sed '/^$/d')

TOTAL=$(echo "$ALL_URLS" | wc -l | tr -d ' ')
echo "Found $TOTAL municipalities to download"

DOWNLOADED=0
SKIPPED=0
FAILED=0

echo "$ALL_URLS" | while IFS=' ' read -r CODE URL TITLE_REST; do
    [ -z "$CODE" ] && continue

    ZIPFILE="$OUTPUT_DIR/belagenhetsadresser_kn${CODE}.zip"
    GPKGFILE="$OUTPUT_DIR/belagenhetsadresser_kn${CODE}.gpkg"

    if [ -f "$GPKGFILE" ] && ! $FORCE; then
        SKIPPED=$((SKIPPED + 1))
        continue
    fi

    printf "  [%s] Downloading kn%s... " "$((DOWNLOADED + SKIPPED + FAILED + 1))/$TOTAL" "$CODE"

    if curl -sf -u "$AUTH" "$URL" -o "$ZIPFILE" 2>/dev/null; then
        # Extract .gpkg from zip
        if unzip -o -q "$ZIPFILE" "*.gpkg" -d "$OUTPUT_DIR" 2>/dev/null; then
            rm -f "$ZIPFILE"
            SIZE=$(du -h "$GPKGFILE" 2>/dev/null | awk '{print $1}')
            echo "OK ($SIZE)"
            DOWNLOADED=$((DOWNLOADED + 1))
        else
            echo "FAILED (unzip)"
            rm -f "$ZIPFILE"
            FAILED=$((FAILED + 1))
        fi
    else
        echo "FAILED (download)"
        rm -f "$ZIPFILE"
        FAILED=$((FAILED + 1))
    fi
done

echo ""
echo "Done. GeoPackage files in: $OUTPUT_DIR"
echo ""
echo "To convert:"
echo "  $SCRIPTDIR/convert.sh -a -w $OUTPUT_DIR -o nominatim.ndjson"
