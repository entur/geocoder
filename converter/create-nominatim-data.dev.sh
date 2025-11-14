#!/usr/bin/env sh

set -eu

ADRESSE_URL=https://nedlasting.geonorge.no/geonorge/Basisdata/MatrikkelenAdresse/CSV/Basisdata_03_Oslo_25833_MatrikkelenAdresse_CSV.zip
STEDSNAVN_URL=https://nedlasting.geonorge.no/geonorge/Basisdata/Stedsnavn/GML/Basisdata_03_Oslo_25833_Stedsnavn_GML.zip
TIAMAT_URL=https://storage.googleapis.com/marduk-production/tiamat/03_Oslo_latest.zip
POI_URL=gs://ror-kakka-dev/tiamat/geocoder/festival_poi_netex.zip

SCRIPTDIR=$(cd $(dirname $0); pwd)
CONVERT="$SCRIPTDIR/convert.sh"

COMPRESS=false
if [ "${1:-}" = "-z" ];then
    COMPRESS=true
fi

fail() {
    echo "Error: $*"
    exit 1
}

download() {
    URL="$1"
    OUTPUT="$2"
    EXTRACT_PATTERN="${3:-}"

    printf "Downloading: %s... " "$URL"

    if [ -n "$EXTRACT_PATTERN" ]; then
        curl -sfL --retry 2 "$URL" | bsdtar -xOf - "$EXTRACT_PATTERN" > "$OUTPUT"
    else
        curl -sfL --retry 2 "$URL" -o "$OUTPUT"
    fi

    if [ -f "$OUTPUT" ]; then
        SIZE=$(du -h "$OUTPUT" | awk '{ print $1 }')
        echo "Extracted $OUTPUT, size: $SIZE"
    else
        fail "Failed to download $URL"
    fi
}

which bsdtar >/dev/null 2>&1 || fail "bsdtar not found. Please install bsdtar to proceed."
which curl >/dev/null 2>&1 || fail "curl not found. Please install curl to proceed."
which gzip >/dev/null 2>&1 || fail "gzip not found. Please install gzip to proceed."
which java >/dev/null 2>&1 || fail "java not found. Please install java to proceed."
[ -f "$CONVERT" ] || fail "$CONVERT not found."

START_TIME=$(date +%s)

download "$ADRESSE_URL" adresse.csv '*.csv'
download "$STEDSNAVN_URL" stedsnavn.gml '*.gml'
$CONVERT -m adresse.csv -g stedsnavn.gml -o nominatim.ndjson
rm adresse.csv stedsnavn.gml

gsutil cp "$POI_URL" poi.zip
bsdtar -xOf poi.zip '*xml' > poi.xml
$CONVERT -a -s poi.xml -o nominatim.ndjson
rm poi.xml poi.zip

download "$TIAMAT_URL" tiamat.xml '*.xml'
$CONVERT -a -s tiamat.xml -o nominatim.ndjson
rm tiamat.xml

$CONVERT -a -p "$SCRIPTDIR/src/test/resources/oslo-center.osm.pbf" -o nominatim.ndjson

END_TIME=$(date +%s)
echo "Created nominatim.ndjson in $((END_TIME - START_TIME)) seconds."

if $COMPRESS; then
  echo "Creating compressed nominatim.ndjson.gz..."
  START_TIME=$(date +%s)
  gzip -k nominatim.ndjson
  END_TIME=$(date +%s)
  echo "Done in $((END_TIME - START_TIME)) seconds."
fi
