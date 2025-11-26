#!/usr/bin/env bash

SCRIPTDIR=$(cd $(dirname $0); pwd)
DEST=$(mktemp -d)
$SCRIPTDIR/../.github/actions/download-docker-artifact/extract.sh eu.gcr.io/entur-system-1287 geocoder-photon:latest "$DEST" false
mv "$DEST/srv/photon_data" .
echo "The latest Photon data is now in ./photon_data "
rm -rf "$DEST"
