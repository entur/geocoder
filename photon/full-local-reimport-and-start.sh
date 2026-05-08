#!/usr/bin/env bash

rm -rf nominatim.ndjson photon_data && \
  import/create-nominatim-data.sh import/config/sources-prod.conf && \
  import/create-photon-data.sh && \
  touch photon_data/.ready && \
  ./photon-start.sh
