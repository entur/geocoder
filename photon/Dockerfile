FROM bellsoft/liberica-runtime-container:jre-25-slim-musl

WORKDIR /srv

SHELL ["/bin/ash", "-eo", "pipefail", "-c"]

COPY --chown=1000:1000 photon.jar photon-start.sh ./

USER 1000:1000

CMD ["./photon-start.sh"]
