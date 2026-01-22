#!/usr/bin/env sh

# Align memory with helm/geocoder-proxy/values.yaml.  Helm values should be at least 25% higher.
java -Xms384m -Xmx384m --enable-native-access=ALL-UNNAMED -jar proxy.jar