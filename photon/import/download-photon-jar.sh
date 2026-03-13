#!/usr/bin/env bash

set -euo pipefail

PHOTON_JAR=https://github.com/entur/photon/releases/download/2026-03-05/photon-1.0.0-ca47acd.jar

curl -sfL --retry 2 -o photon.jar $PHOTON_JAR && echo photon.jar downloaded successfully && exit 0

echo ERROR: failed downloading photon.jar && exit 1
