#!/bin/sh

MAX_ATTEMPTS=5
SLEEP_SECONDS=1
ATTEMPT=1

while [ $ATTEMPT -le $MAX_ATTEMPTS ]; do
    echo "Starting photon-opensearch (attempt $ATTEMPT/$MAX_ATTEMPTS)..."
    java -jar photon-opensearch-0.7.0.jar && exit 0
    echo "photon-opensearch failed to start. Retrying in $SLEEP_SECONDS seconds..."
    ATTEMPT=$((ATTEMPT+1))
    sleep $SLEEP_SECONDS
done

echo "photon-opensearch failed to start after $MAX_ATTEMPTS attempts. Exiting."
exit 1

