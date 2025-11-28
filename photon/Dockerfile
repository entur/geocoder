FROM bellsoft/liberica-runtime-container:jre-21-slim-musl

ARG PHOTON_JAR_URL

WORKDIR /srv

SHELL ["/bin/ash", "-eo", "pipefail", "-c"]

COPY photon.jar ./
COPY photon-start.sh ./
COPY photon_data ./photon_data

RUN chown -R 1000:1000 /srv
USER 1000:1000

CMD ["sh", "-c", "exec ./photon-start.sh"]
