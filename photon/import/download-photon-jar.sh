#!/usr/bin/env bash

set -euo pipefail

PHOTON_JAR=https://github.com/komoot/photon/releases/download/1.2.1/photon-1.2.1.jar

curl -sfL --retry 2 -A "entur-geocoder" -o photon.jar $PHOTON_JAR && echo photon.jar downloaded successfully && exit 0

echo ERROR: failed downloading photon.jar && exit 1
