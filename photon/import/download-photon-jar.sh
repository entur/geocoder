#!/usr/bin/env bash

set -euo pipefail

PHOTON_JAR=https://github.com/entur/photon/releases/download/2026-09-08/photon-1.3.0-f7a6c8a.jar
PHOTON_JAR_SHA256=8f6b3ab8bcb5aa8d8673d3e0a80ba4228ae776446273d7c0b9e23a5e4e02e168

curl -sfL --retry 2 -A "entur-geocoder" -o photon.jar "$PHOTON_JAR" || { echo "ERROR: failed downloading photon.jar from $PHOTON_JAR"; exit 1; }

# Release assets are mutable; the pin makes a replaced or corrupt jar fail here
# rather than produce an index nothing can open.
ACTUAL=$(sha256sum photon.jar | awk '{print $1}')
[ "$ACTUAL" = "$PHOTON_JAR_SHA256" ] || { echo "ERROR: photon.jar sha256 $ACTUAL, expected $PHOTON_JAR_SHA256"; exit 1; }

echo "photon.jar downloaded successfully from $PHOTON_JAR"
