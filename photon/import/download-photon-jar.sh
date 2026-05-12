#!/usr/bin/env bash

set -euo pipefail

PHOTON_JAR=https://github.com/entur/photon/releases/download/2026-05-12-apostrophe-s/photon-1.1.0-d3c2220.jar

curl -sfL --retry 2 -A "entur-geocoder" -o photon.jar $PHOTON_JAR && echo photon.jar downloaded successfully && exit 0

echo ERROR: failed downloading photon.jar && exit 1
