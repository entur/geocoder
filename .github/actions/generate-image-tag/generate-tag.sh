#!/usr/bin/env bash

set -euo pipefail

SHA=${1:0:7}
REF=${2:-detached}

branch=$(echo "$REF" | sed 's/[^a-zA-Z0-9_-]\+/_/g')
echo "${branch}.$(date +'%Y%m%d')-SHA${SHA}"