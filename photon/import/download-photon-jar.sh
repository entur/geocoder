#!/usr/bin/env bash

set -euo pipefail

PHOTON_JAR=https://github.com/entur/photon/releases/download/1.3.0-0c16b33/photon-1.3.0-0c16b33.jar
PHOTON_JAR_SHA256=0366fe0f974c61d1e95ba47a9890016ce9770636b39460fbe1cee32008b3f427

curl -sfL --retry 2 -A "entur-geocoder" -o photon.jar "$PHOTON_JAR" || { echo "ERROR: failed downloading photon.jar from $PHOTON_JAR"; exit 1; }

# Release assets are mutable; the pin makes a replaced or corrupt jar fail here
# rather than produce an index nothing can open.
ACTUAL=$(sha256sum photon.jar | awk '{print $1}')
[ "$ACTUAL" = "$PHOTON_JAR_SHA256" ] || { echo "ERROR: photon.jar sha256 $ACTUAL, expected $PHOTON_JAR_SHA256"; exit 1; }

echo "photon.jar downloaded successfully from $PHOTON_JAR"
