#!/usr/bin/env sh

PHOTON_JAR=https://github.com/entur/photon/releases/download/housenumbers-temp-patch/photon-0.7.0.jar

curl -sfL --retry 2 -o photon.jar $PHOTON_JAR && echo photon.jar downloaded successfully && exit 0

echo ERROR: failed downloading photon.jar && exit 1
