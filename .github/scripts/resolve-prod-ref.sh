#!/usr/bin/env bash
# Extract the geocoder commit from a build tag, with or without a leading image
# reference. Tags are <branch>.<UTC-timestamp>-SHA<short>, see generate-tag.sh.
# Aliases like latest-prod carry no commit and are rejected.
#
# Usage: ./resolve-prod-ref.sh <build-tag|image-reference>

set -euo pipefail

REF=${1:-}
if [ -z "$REF" ]; then
  echo "Usage: resolve-prod-ref.sh <build-tag|image-reference>" >&2
  exit 1
fi

TAG=${REF##*:}
if [[ ! $TAG =~ -SHA([0-9a-f]{7,40})$ ]]; then
  echo "Error: no -SHA<sha> suffix in tag '$TAG'" >&2
  exit 1
fi

echo "${BASH_REMATCH[1]}"
