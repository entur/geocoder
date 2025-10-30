#!/usr/bin/env sh

find photon_data -name '*.lock' -delete

export JAVA_TOOL_OPTIONS="-javaagent:./jmx_prometheus_javaagent-1.0.1.jar=9404:./jmx-exporter.yml"
java -jar photon.jar -default-language no -listen-port ${SERVER_PORT:-2322}