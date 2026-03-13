#!/usr/bin/env bash
# Download the latest Photon search index from the production Docker image.
# Useful for local debugging — extracts photon_data/ into the current directory.

set -euo pipefail

SCRIPTDIR=$(cd "$(dirname "$0")"; pwd)
DEST=$(mktemp -d)
"$SCRIPTDIR/../.github/actions/download-docker-artifact/extract.sh" eu.gcr.io/entur-system-1287 geocoder-photon:latest-prod "$DEST" false
mv "$DEST/srv/photon_data" .
echo "The latest Photon data is now in ./photon_data "
rm -rf "$DEST"
