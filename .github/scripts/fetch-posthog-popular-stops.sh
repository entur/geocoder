#!/usr/bin/env bash
# Fetch a PostHog insight and emit a semicolon-separated `id;name;usage` CSV
# (with header) on stdout. Used to feed the nominatim-converter --usage flag.
#
# Usage: ./fetch-posthog-popular-stops.sh {fra|til}
# Env:   TOKEN  - PostHog personal API key (insight:read + query:read)

set -euo pipefail

: "${TOKEN:?set TOKEN to your PostHog personal API key}"

case "${1:-}" in
  fra) SHORT_ID=hd0beH5A ;;
  til) SHORT_ID=LePQhnOg ;;
  *)   echo "Usage: $0 {fra|til}" >&2; exit 1 ;;
esac

PROJ=2283
LIMIT=1000

QUERY=$(curl -sG "https://eu.posthog.com/api/projects/$PROJ/insights/" \
             -H "Authorization: Bearer $TOKEN" \
             --data-urlencode "short_id=$SHORT_ID" \
        | jq ".results[0].query | .source.breakdownFilter.breakdown_limit = $LIMIT")

curl -s "https://eu.posthog.com/api/projects/$PROJ/query/" \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d "{\"query\": $QUERY}" \
  | jq -r '["id","name","usage"], (.results[] | [.breakdown_value[0], .breakdown_value[1], .aggregated_value]) | map(tostring) | join(";")'
