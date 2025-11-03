#!/usr/bin/env sh

set -eu

ADRESSE_URL=https://nedlasting.geonorge.no/geonorge/Basisdata/MatrikkelenAdresse/CSV/Basisdata_03_Oslo_25833_MatrikkelenAdresse_CSV.zip
STEDSNAVN_URL=https://nedlasting.geonorge.no/geonorge/Basisdata/Stedsnavn/GML/Basisdata_03_Oslo_25833_Stedsnavn_GML.zip
TIAMAT_URL=https://storage.googleapis.com/marduk-production/tiamat/03_Oslo_latest.zip

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

which bsdtar >/dev/null 2>&1 || fail "bsdtar not found. Please install bsdtar to proceed."
which curl >/dev/null 2>&1 || fail "curl not found. Please install curl to proceed."
which xz >/dev/null 2>&1 || fail "xz not found. Please install xz to proceed."
which java >/dev/null 2>&1 || fail "java not found. Please install java to proceed."
[ -f "$CONVERT" ] || fail "$CONVERT not found."

curl -sfL --retry 2 $ADRESSE_URL |  bsdtar -xOf - '*.csv'  > adresse.csv
curl -sfL --retry 2 $STEDSNAVN_URL |  bsdtar -xOf - '*.gml'  > stedsnavn.gml
$CONVERT -m adresse.csv -g stedsnavn.gml -o nominatim.ndjson
rm adresse.csv stedsnavn.gml

curl -sfL --retry 2 $TIAMAT_URL | bsdtar -xOf - '*.xml' > tiamat.xml
$CONVERT -a -s tiamat.xml -o nominatim.ndjson
rm tiamat.*

$CONVERT -a -p "$SCRIPTDIR/src/test/resources/oslo-center.osm.pbf" -o nominatim.ndjson

if $COMPRESS; then
  xz -zk nominatim.ndjson
  echo "nominatim.ndjson.xz created."
else
  echo "nominatim.ndjson created."
fi
