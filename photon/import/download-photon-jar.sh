#!/usr/bin/env bash

set -euo pipefail

PHOTON_JAR=https://github.com/komoot/photon/releases/download/1.3.0/photon-1.3.0.jar

curl -sfL --retry 2 -A "entur-geocoder" -o photon.jar $PHOTON_JAR && echo photon.jar downloaded successfully from $PHOTON_JAR && exit 0

echo ERROR: failed downloading photon.jar from $PHOTON_JAR && exit 1
