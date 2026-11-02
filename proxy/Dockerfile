FROM gcr.io/distroless/java25-debian13:nonroot@sha256:a90a5fadd78dc37b643da72dca2f744ef67eba625eca7227a5b375b548e6b1c2

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
