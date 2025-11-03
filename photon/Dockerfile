ARG IMPORT_IMAGE=geocoder-import:latest
FROM ${IMPORT_IMAGE} AS import-image

FROM bellsoft/liberica-runtime-container:jre-21-musl

ARG PHOTON_JAR_URL

WORKDIR /srv
RUN apk add --no-cache curl
RUN curl -sfL --retry 2 -o photon.jar ${PHOTON_JAR_URL}

COPY photon-start.sh ./
#COPY --from=import-image /photon.jar photon.jar
COPY --from=import-image /photon_data photon_data
RUN chown -R 1000:1000 /srv
USER 1000:1000

CMD ["sh", "-c", "exec ./photon-start.sh"]
