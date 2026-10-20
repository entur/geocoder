#!/usr/bin/env bash

set -euo pipefail

PHOTON_JAR=https://github.com/entur/photon/releases/download/2026-10-20/photon-1.3.0-19ac161.jar
PHOTON_JAR_SHA256=988af8e9ccdc4c6a36a66a704f6011a0b5decd645823e95a49a5660c4261a07c

curl -sfL --retry 2 -A "entur-geocoder" -o photon.jar "$PHOTON_JAR" || { echo "ERROR: failed downloading photon.jar from $PHOTON_JAR"; exit 1; }

# Release assets are mutable; the pin makes a replaced or corrupt jar fail here
# rather than produce an index nothing can open.
ACTUAL=$(sha256sum photon.jar | awk '{print $1}')
[ "$ACTUAL" = "$PHOTON_JAR_SHA256" ] || { echo "ERROR: photon.jar sha256 $ACTUAL, expected $PHOTON_JAR_SHA256"; exit 1; }

echo "photon.jar downloaded successfully from $PHOTON_JAR"
