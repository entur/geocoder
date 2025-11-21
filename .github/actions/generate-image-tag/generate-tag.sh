#!/usr/bin/env bash

set -euo pipefail

branch=$(echo "${GITHUB_REF_NAME:-detached}" | sed 's/[^a-zA-Z0-9_-]\+/_/g')
echo "${branch}.$(date +'%Y%m%d')-SHA${GITHUB_SHA:0:7}"