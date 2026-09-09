#!/usr/bin/env bash

set -euo pipefail

PHOTON_JAR=https://github.com/entur/photon/releases/download/2026-09-08/photon-1.3.0-f7a6c8a.jar

curl -sfL --retry 2 -A "entur-geocoder" -o photon.jar $PHOTON_JAR && echo photon.jar downloaded successfully from $PHOTON_JAR && exit 0

echo ERROR: failed downloading photon.jar from $PHOTON_JAR && exit 1
