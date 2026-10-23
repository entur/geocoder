FROM gcr.io/distroless/java25-debian13:nonroot@sha256:f7ef4e6822a6f57b90cdc91ad19e7e02923f3f491eccda918fd0be012d3bd8ac

WORKDIR /srv

COPY ./build/libs/proxy-all.jar proxy.jar

# No shell in distroless, so the java invocation that used to live in
# proxy-start.sh is inlined here. Keep -Xmx aligned with
# helm/geocoder-proxy/values.yaml (helm request >= 50% above heap).
ENTRYPOINT ["java", \
  "-Xms640m", "-Xmx640m", \
  "-XX:+UseG1GC", "-XX:+ExitOnOutOfMemoryError", "-XX:MaxGCPauseMillis=75", \
  "--enable-native-access=ALL-UNNAMED", \
  "-jar", "proxy.jar"]
