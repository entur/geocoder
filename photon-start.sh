#!/usr/bin/env sh

find photon_data -name '*.lock' -delete

cat > jmx-exporter.yaml <<YAML
startDelaySeconds: 0
ssl: false
lowercaseOutputName: true
lowercaseOutputLabelNames: true
rules:
  - pattern: ".*"    # export all MBeans; refine later if needed
YAML

export JAVA_TOOL_OPTIONS="-javaagent:./jmx_prometheus_javaagent-1.0.1.jar=9404:./jmx-exporter.yaml"
java -jar photon.jar -default-language no -listen-port ${SERVER_PORT:-2322}