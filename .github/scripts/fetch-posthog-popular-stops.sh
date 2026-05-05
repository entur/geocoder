#!/usr/bin/env bash
# Fetch both PostHog insights (fra + til) and emit a merged
# semicolon-separated `id;name;usage` CSV (with header) on stdout. `usage`
# is the sum across both directions per id. Used to feed the
# nominatim-converter --usage flag.
#
# Env: TOKEN - PostHog personal API key (insight:read + query:read)

set -euo pipefail

: "${TOKEN:?set TOKEN to your PostHog personal API key}"

PROJ=2283
LIMIT=1000
INSIGHTS=(
    rbJJwGSg   # fra (https://eu.posthog.com/project/2283/insights/rbJJwGSg)
    Tv4Eb6Lz   # til (https://eu.posthog.com/project/2283/insights/Tv4Eb6Lz)
)

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

# Slurp all insight results from stdin, sum usage per id, emit CSV on stdout.
to_csv() {
    jq -rs '
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
}

# Validate CSV file at $1: header, row shape, id/usage formats, dupes,
# minimum row count, and a sanity threshold on top usage.
verify() {
    awk -F';' '
        NR == 1 {
            if ($0 != "id;name;usage") { print "ERROR: bad header: " $0; exit 1 }
            next
        }
        NF != 3 || $1 == "" || $2 == "" || $3 == "" {
            print "ERROR: line " NR ": malformed row: " $0; exit 1
        }
        $1 !~ /^([A-Za-z]+:[A-Za-z]+:[A-Za-zæøåÆØÅ0-9-]+|[0-9]+)$/ {
            print "ERROR: line " NR ": unexpected id: " $1; exit 1
        }
        $3 !~ /^[0-9]+$/ {
            print "ERROR: line " NR ": non-integer usage: " $3; exit 1
        }
        seen[$1]++ { print "ERROR: line " NR ": duplicate id: " $1; exit 1 }
        { rows++; total += $3; if ($3 + 0 > top) top = $3 + 0 }
        END {
            printf "Validated %d body rows, total usage %d, top %d\n", rows, total, top
            if (rows < 990) { print "ERROR: only " rows " body rows"; exit 1 }
            if (top < 1000)  { print "ERROR: top usage " top " unrealistically low"; exit 1 }
        }
    ' "$1"
}

tmp=$(mktemp)
trap 'rm -f "$tmp"' EXIT

for id in "${INSIGHTS[@]}"; do
    fetch "$id"
done | to_csv > "$tmp"

verify "$tmp" >&2
cat "$tmp"
