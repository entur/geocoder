#!/usr/bin/env sh

find photon_data -name '*.lock' -delete

# Align memory with helm/geocoder-photon/values.yaml. Helm values should be at least 25% higher.
java -Xms1g -Xmx1g -XX:+UseZGC -XX:+UseStringDeduplication \
     -Dspark.threadPool.maxThreads=20 -jar photon.jar -default-language no \
     -listen-ip 0.0.0.0 -listen-port "${SERVER_PORT:-2322}" \
     -metrics-enable prometheus