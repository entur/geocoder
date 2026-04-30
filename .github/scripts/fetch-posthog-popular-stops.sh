#!/usr/bin/env bash
# Fetch both PostHog insights (boardings + alightings) and emit a merged
# semicolon-separated `id;name;usage` CSV (with header) on stdout. `usage`
# is the sum across both directions per id. Used to feed the
# nominatim-converter --usage flag.
#
# Env: TOKEN - PostHog personal API key (insight:read + query:read)

set -euo pipefail

: "${TOKEN:?set TOKEN to your PostHog personal API key}"

PROJ=2283
LIMIT=1000

# Fetch one insight by short_id and emit its query result JSON to stdout.
fetch() {
    local short_id=$1
    local query
    query=$(curl -sG "https://eu.posthog.com/api/projects/$PROJ/insights/" \
                 -H "Authorization: Bearer $TOKEN" \
                 --data-urlencode "short_id=$short_id" \
            | jq ".results[0].query | .source.breakdownFilter.breakdown_limit = $LIMIT")
    curl -s "https://eu.posthog.com/api/projects/$PROJ/query/" \
         -H "Authorization: Bearer $TOKEN" \
         -H "Content-Type: application/json" \
         -d "{\"query\": $query}"
}

# Fetch both insights, slurp into one stream, sum usage per id.
{
    fetch hd0beH5A   # fra (boardings)
    fetch LePQhnOg   # til (alightings)
} | jq -rs '
    [.[].results[]]
    | map(select(.breakdown_value[0] != "$$_posthog_breakdown_null_$$"))
    | map(.breakdown_value[0] |= sub("^OSM:TopographicPlace:"; "OSM:PointOfInterest:"))
    | group_by(.breakdown_value[0])
    | map({
        id:    .[0].breakdown_value[0],
        name:  .[0].breakdown_value[1],
        usage: (map(.aggregated_value) | add)
      })
    | sort_by(-.usage)
    | (["id","name","usage"]),
      (.[] | [.id, .name, .usage])
    | map(tostring) | join(";")
'
