#!/usr/bin/env bash
# Canonical tag for built artifacts: <branch>.<UTC-timestamp>-SHA<short>
# UTC + seconds avoids DST surprises and same-minute collisions when two
# builds run back-to-back.
#
# Usage: ./generate-tag.sh <full-sha> <branch-or-ref>

set -euo pipefail

SHA=${1:0:7}
REF=${2:-detached}

branch=$(echo "$REF" | sed -E 's/[^a-zA-Z0-9_-]+/_/g')
echo "${branch}.$(date -u +'%Y%m%d-%H%M%S')-SHA${SHA}"
