#!/usr/bin/env sh

cat > jmx-exporter.yaml <<YAML
startDelaySeconds: 0
ssl: false
lowercaseOutputName: true
lowercaseOutputLabelNames: true
rules:
  - pattern: ".*"
YAML

export JAVA_TOOL_OPTIONS="-javaagent:./jmx_prometheus_javaagent-1.0.1.jar=9404:./jmx-exporter.yaml"
java -jar proxy.jar