#!/usr/bin/env sh

export JAVA_TOOL_OPTIONS="-javaagent:./jmx_prometheus_javaagent-1.0.1.jar=9404:./jmx-exporter.yml"
java -jar proxy.jar