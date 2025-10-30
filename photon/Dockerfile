ARG IMPORT_IMAGE=geocoder-import:latest
FROM ${IMPORT_IMAGE} AS import-image

FROM bellsoft/liberica-runtime-container:jre-21-musl

WORKDIR /srv
RUN apk add --no-cache curl
RUN curl -sfLO --retry 2 \
    https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/1.0.1/jmx_prometheus_javaagent-1.0.1.jar

COPY . .
COPY --from=import-image /photon.jar photon.jar
COPY --from=import-image /photon_data photon_data
RUN chown -R 1000:1000 /srv
USER 1000:1000

CMD ["sh", "-c", "exec ./photon-start.sh"]
