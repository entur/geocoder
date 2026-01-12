#!/usr/bin/env sh

find photon_data -name '*.lock' -delete

java -jar photon.jar -default-language no -listen-ip 0.0.0.0 -listen-port ${SERVER_PORT:-2322} -metrics-enable prometheus