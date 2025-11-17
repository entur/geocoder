#!/usr/bin/env bash

SCRIPTDIR=$(cd $(dirname $0); pwd)
$SCRIPTDIR/../.github/actions/download-docker-artifact/extract.sh eu.gcr.io/entur-system-1287 geocoder-photon-data:latest .
